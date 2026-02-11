package com.aidoc.service;

import com.aidoc.exception.RagException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.HttpClientErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * RAG（Retrieval-Augmented Generation）サービス
 * ベクトル検索とAI回答生成を組み合わせて、文書に基づく回答を生成
 */
@Service
public class RagService {

    /** ロガー */
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    /** AIチャットクライアント */
    private final ChatClient chatClient;
    /** ベクトルストア */
    private final VectorStore vectorStore;

    /** RAG用のプロンプトテンプレート */
    private static final String RAG_PROMPT = """
            あなたは文書分析を支援する有能なアシスタントです。
            以下の「Context」として提供される情報を基に、最後の「Request」（質問）に日本語で答えてください。
            もし答えがわからない場合は、無理に答えを捏造せず、「提供された情報からはわかりませんでした」と答えてください。

            Request: {question}

            Context:
            {context}

            Answer:
            """;

    /**
     * コンストラクタ
     * @param chatClientBuilder チャットクライアントビルダー
     * @param vectorStore ベクトルストア
     */
    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    /**
     * 質問に回答を生成
     * 類似文書を検索し、コンテキストとしてAIに渡して回答を生成
     * @param question ユーザーの質問
     * @return AIが生成した回答
     */
    public String generateAnswer(String question) {
        logger.info("質問を受信しました: {}", question);

        try {
            // 1. 類似文書の検索（ベクトル検索）
            List<Document> similarDocuments = retrieveSimilarDocuments(question);

            // 2. コンテキストの準備（検索結果のテキストを結合）
            String context = prepareContext(similarDocuments);

            // 関連ドキュメントがない場合の処理
            if (context.isEmpty()) {
                logger.info("関連ドキュメントが見つかりませんでした");
                return "関連するドキュメントが見つかりませんでした。まずドキュメントをアップロードしてください。";
            }

            // 3. AIにコンテキストと質問を渡して回答生成
            return generateAnswerFromContext(question, context);

        } catch (RagException e) {
            logger.error("RAG処理中のエラー: {} - {}", e.getErrorType(), e.getMessage());
            throw e;
        } catch (Exception e) {
            // レート制限エラーのチェック
            if (isRateLimitException(e)) {
                logger.error("予期しないエラー (レート制限): {}", e.getMessage());
            } else {
                logger.error("予期しないエラーが発生しました", e);
            }
            throw new RagException(
                    RagException.ErrorType.UNKNOWN,
                    "予期しないエラー: " + e.getMessage(),
                    e);
        }
    }

    /**
     * ベクトルストアから類似文書を取得
     * @param question 検索クエリ
     * @return 類似度の高い文書リスト
     */
    private List<Document> retrieveSimilarDocuments(String question) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.query(question).withTopK(3));
        } catch (Exception e) {
            if (isRateLimitException(e)) {
                logger.error("ドキュメント検索中にレート制限エラーが発生しました: {}", e.getMessage());
                throw new RagException(
                        RagException.ErrorType.RATE_LIMIT_EXCEEDED,
                        "AI APIのリクエスト制限を超過しました。しばらく待ってから再度お試しください。",
                        e);
            }
            logger.error("ドキュメント検索中にエラーが発生しました", e);
            throw new RagException(
                    RagException.ErrorType.NO_DOCUMENTS_FOUND,
                    "ドキュメント検索エラー: " + e.getMessage(),
                    e);
        }
    }

    /**
     * 文書リストからコンテキスト文字列を生成
     * @param documents 文書リスト
     * @return 結合されたコンテキスト文字列
     */
    private String prepareContext(List<Document> documents) {
        return documents.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * コンテキストと質問からAI回答を生成
     * @param question ユーザーの質問
     * @param context 参考となる文書内容
     * @return 生成された回答
     */
    private String generateAnswerFromContext(String question, String context) {
        try {
            logger.debug("AI回答生成を開始します...");

            // プロンプトを作成
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT);
            Prompt prompt = promptTemplate.create(
                    Map.of("question", question, "context", context));

            // AIに回答を生成させる
            String answer = chatClient.prompt(prompt).call().content();
            logger.info("AI回答生成が完了しました");
            return answer;

        } catch (TransientAiException e) {
            // 一時的なAIエラーのハンドリング
            logger.error("AI API 暫定エラー (リトライ失敗): {}", e.getMessage());

            if (e.getMessage().contains("429") || e.getMessage().contains("RESOURCE_EXHAUSTED")) {
                throw new RagException(
                        RagException.ErrorType.RATE_LIMIT_EXCEEDED,
                        "AI APIのリクエスト制限を超過しました。しばらく待ってから再度お試しください。",
                        e);
            }

            throw new RagException(
                    RagException.ErrorType.API_ERROR,
                    "AI API 一時エラー: " + e.getMessage(),
                    e);

        } catch (HttpClientErrorException e) {
            // HTTPエラーのハンドリング
            logger.error("HTTP エラー: {} - {}", e.getStatusCode(), e.getMessage());

            // レート制限(429)のチェック
            if (e.getStatusCode().value() == 429 ||
                    e.getMessage().contains("429") ||
                    e.getMessage().contains("RESOURCE_EXHAUSTED") ||
                    e.getMessage().contains("Resource exhausted")) {
                throw new RagException(
                        RagException.ErrorType.RATE_LIMIT_EXCEEDED,
                        "Rate limit exceeded: " + e.getMessage(),
                        e);
            }

            throw new RagException(
                    RagException.ErrorType.API_ERROR,
                    "API エラー: " + e.getMessage(),
                    e);

        } catch (ResourceAccessException e) {
            // タイムアウト・ネットワークエラーのハンドリング
            logger.error("タイムアウトまたはネットワークエラー", e);
            throw new RagException(
                    RagException.ErrorType.TIMEOUT,
                    "タイムアウト: " + e.getMessage(),
                    e);

        } catch (Exception e) {
            // その他のエラーのハンドリング
            if (isRateLimitException(e)) {
                logger.error("AI回答生成中にレート制限エラーが発生しました: {}", e.getMessage());
                throw new RagException(
                        RagException.ErrorType.RATE_LIMIT_EXCEEDED,
                        "Rate limit exceeded: " + e.getMessage(),
                        e);
            }

            logger.error("AI回答生成中にエラーが発生しました", e);

            // タイムアウト関連のエラーかどうか確認
            if (e.getCause() instanceof TimeoutException) {
                throw new RagException(
                        RagException.ErrorType.TIMEOUT,
                        "処理タイムアウト: " + e.getMessage(),
                        e);
            }

            throw new RagException(
                    RagException.ErrorType.API_ERROR,
                    "AI API エラー: " + e.getMessage(),
                    e);
        }
    }

    /**
     * 例外がレート制限エラーかどうか判定
     * @param e 判定する例外
     * @return レート制限エラーならtrue
     */
    private boolean isRateLimitException(Throwable e) {
        if (e == null) {
            return false;
        }
        String message = e.getMessage() != null ? e.getMessage() : "";
        String lowerMessage = message.toLowerCase();

        // 429またはRESOURCE_EXHAUSTEDが含まれているかチェック
        if (lowerMessage.contains("429") ||
                lowerMessage.contains("resource_exhausted") ||
                lowerMessage.contains("resource exhausted")) {
            return true;
        }

        // 原因例外を再帰的にチェック
        if (e.getCause() != null && e.getCause() != e) {
            return isRateLimitException(e.getCause());
        }

        return false;
    }
}
