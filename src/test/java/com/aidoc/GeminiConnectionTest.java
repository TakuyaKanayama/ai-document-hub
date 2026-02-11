package com.aidoc;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gemini APIとの接続をテストするクラス
 * APIキーが設定されている場合のみテストを実行
 */
public class GeminiConnectionTest {

    /**
     * Gemini APIへの接続をテスト
     * 環境変数または.envファイルからAPIキーを読み取り、
     * Gemini 2.0 Flashモデルで接続を確認
     */
    @Test
    void testNativeGeminiConnection() {
        // APIキーを環境変数または.envファイルから読み込み
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            try {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                apiKey = dotenv.get("GEMINI_API_KEY");
            } catch (Exception e) {
                // .envファイルがない場合は無視
            }
        }

        // APIキーが設定されていない場合はテストをスキップ
        if (apiKey == null || apiKey.isEmpty() || "test-key".equals(apiKey)) {
            System.out.println("テストをスキップ: GEMINI_API_KEYが見つからないか無効です。");
            return;
        }

        // Google GenAI SDKを使ってAPIに接続
        try {
            System.out.println("Google GenAI SDK経由で接続を試みています...");

            // BuilderパターンでAPIキーを明示的に設定
            Client client = Client.builder().apiKey(apiKey).build();

            /*
             * 利用可能なモデル一覧を取得（デバッグ用）
             * var models = client.models.list(null);
             * System.out.println("SUCCESS: API is reachable.");
             */

            // gemini-2.0-flashモデルでコンテンツ生成をテスト
            System.out.println("gemini-2.0-flashでコンテンツ生成を試みています...");
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.0-flash",
                    "Explain how AI works in a few words",
                    null);

            // 応答が返ってきているか確認
            System.out.println("SUCCESS RESPONSE: " + response.text());
            assertThat(response.text()).isNotBlank();

        } catch (Exception e) {
            System.out.println("接続失敗: " + e.getMessage());
            // ヒント：OS環境変数にGEMINI_API_KEYを設定する必要がある場合がある
            System.out.println("ヒント: SDKが読み込めるようOS環境変数にGEMINI_API_KEYを設定してください。");
            throw new RuntimeException(e);
        }
    }
}
