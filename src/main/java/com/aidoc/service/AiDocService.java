package com.aidoc.service;

import com.aidoc.model.AiDoc;
import com.aidoc.repository.AiDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * ドキュメントの保存、取得、削除に関するビジネスロジックを処理するサービス
 * ファイルシステム、データベース、ベクトルストアとの連携を担当
 */
@Service
public class AiDocService {

    private static final Logger logger = LoggerFactory.getLogger(AiDocService.class);

    /** ドキュメントデータアクセスオブジェクト */
    private final AiDocRepository aiDocRepository;
    /** アップロードファイルの保存先パス */
    private final Path storageLocation;
    /** ベクトルストア（ pgvector ） */
    private final VectorStore vectorStore;

    /**
     * コンストラクタ
     * @param aiDocRepository リポジトリ
     * @param storageLocation ファイル保存先パス
     * @param vectorStore ベクトルストア
     */
    public AiDocService(AiDocRepository aiDocRepository,
            @Value("${app.storage.location:uploads}") String storageLocation,
            VectorStore vectorStore) {
        this.aiDocRepository = aiDocRepository;
        this.storageLocation = Paths.get(storageLocation);
        this.vectorStore = vectorStore;
        // 保存ディレクトリが存在しない場合は作成
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("ストレージディレクトリを作成できませんでした", e);
        }
    }

    /**
     * ファイルをアップロードして処理
     * ファイルを保存し、RAG用にベクトル化してからベクトルストアに登録
     * @param file アップロードされたファイル
     * @return 保存されたドキュメントエンティティ
     */
    @Transactional
    public AiDoc store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("空のファイルは保存できません。");
            }
            // ファイル名をユニークにするためタイムスタンプを付与
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path destinationFile = this.storageLocation.resolve(Paths.get(filename))
                    .normalize().toAbsolutePath();

            // ファイルを保存
            file.transferTo(destinationFile);

            // ドキュメントメタデータをDBに保存
            AiDoc aiDoc = new AiDoc(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    destinationFile.toString());

            aiDoc = aiDocRepository.save(aiDoc);

            // RAG ETLプロセス：ファイルを読み込み、ベクトル化して保存
            try {
                // 1. Tikaを使ってファイルからテキスト抽出
                Resource resource = new UrlResource(
                        destinationFile.toUri());
                TikaDocumentReader tikaReader = 
                        new TikaDocumentReader(resource);
                List<Document> originalDocs = tikaReader.read();

                // 2. テキストをチャンクに分割
                TokenTextSplitter splitter = 
                        new TokenTextSplitter();
                List<Document> splitDocs = splitter.apply(originalDocs);

                // 3. メタデータを追加（元のドキュメントIDへのリンク）
                for (Document splitDoc : splitDocs) {
                    splitDoc.getMetadata().put("source_document_id",
                            aiDoc.getId().toString());
                    splitDoc.getMetadata().put("filename", aiDoc.getFilename());
                }

                // 4. ベクトルストアに保存
                vectorStore.add(splitDocs);

                // インデックス完了フラグを立てる
                aiDoc.setIndexed(true);
                aiDocRepository.save(aiDoc);

            } catch (Exception e) {
                // インデックスに失敗してもアップロードは成功とする
                logger.warn("ドキュメントのインデックスに失敗しました: {}", e.getMessage());
            }

            return aiDoc;
        } catch (IOException e) {
            throw new RuntimeException("ファイルの保存に失敗しました。", e);
        }
    }

    /**
     * 全ドキュメントを取得（新しい順）
     * @return ドキュメントリスト
     */
    public List<AiDoc> getAllDocuments() {
        return aiDocRepository.findAllByOrderByUploadedAtDesc();
    }

    /**
     * ドキュメントを削除
     * ベクトルストア、ファイルシステム、DBから順次削除
     * @param id 削除するドキュメントのID
     */
    @Transactional
    public void deleteDocument(UUID id) {
        AiDoc aiDoc = aiDocRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ドキュメントが見つかりません: ID " + id));

        try {
            // 1. ベクトルストアから削除（インデックス削除）
            if (aiDoc.isIndexed()) {
                try {
                    vectorStore.delete(List.of(aiDoc.getId().toString()));
                    logger.info("ベクトルストアから削除しました: {}", aiDoc.getId());
                } catch (Exception e) {
                    logger.error("ベクトルストアからの削除に失敗: {}", e.getMessage());
                }
            }

            // 2. ファイルシステムから削除
            Path filePath = Paths.get(aiDoc.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("ファイルシステムから削除しました: {}", filePath);
            }

            // 3. データベースから削除
            aiDocRepository.delete(aiDoc);
            logger.info("データベースから削除しました: {}", aiDoc.getId());

        } catch (IOException e) {
            throw new RuntimeException("ファイルの削除に失敗: " + e.getMessage(), e);
        }
    }
}
