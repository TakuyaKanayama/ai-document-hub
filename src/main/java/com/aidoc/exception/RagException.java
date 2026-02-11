package com.aidoc.exception;

/**
 * RAG処理中に発生する例外を表すカスタム例外クラス
 * エラーの種類に応じてユーザーへの表示メッセージを切り替え
 */
public class RagException extends RuntimeException {

    /** エラーの種類 */
    private final ErrorType errorType;

    /**
     * エラーの種類を定義
     * それぞれに対応するユーザー向けメッセージを保持
     */
    public enum ErrorType {
        /** リクエスト过多（レート制限） */
        RATE_LIMIT_EXCEEDED("リクエストを処理しています。しばらく待ってから再度お試しください。"),
        /** 関連ドキュメントが見つからない */
        NO_DOCUMENTS_FOUND("関連するドキュメントが見つかりませんでした。"),
        /** AI API通信エラー */
        API_ERROR("AI APIとの通信中にエラーが発生しました。"),
        /** タイムアウト */
        TIMEOUT("処理がタイムアウトしました。もう一度お試しください。"),
        /** その他のエラー */
        UNKNOWN("予期しないエラーが発生しました。");

        /** ユーザー向け表示メッセージ */
        private final String userMessage;

        ErrorType(String userMessage) {
            this.userMessage = userMessage;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    /**
     * コンストラクタ
     * @param errorType エラーの種類
     * @param technicalMessage 技術的なエラーメッセージ（ログ用）
     */
    public RagException(ErrorType errorType, String technicalMessage) {
        super(technicalMessage);
        this.errorType = errorType;
    }

    /**
     * コンストラクタ（原因例外付き）
     * @param errorType エラーの種類
     * @param technicalMessage 技術的なエラーメッセージ
     * @param cause 原因となった例外
     */
    public RagException(ErrorType errorType, String technicalMessage, Throwable cause) {
        super(technicalMessage, cause);
        this.errorType = errorType;
    }

    /**
     * エラーの種類を取得
     * @return エラーの種類
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * ユーザー向けメッセージを取得
     * @return ユーザーに表示するメッセージ
     */
    public String getUserFriendlyMessage() {
        return errorType.getUserMessage();
    }
}
