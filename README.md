# AI Document Hub

AI Document Hubは、Spring BootとGemini AIを使用したRAG（Retrieval-Augmented Generation：検索拡張生成）アプリケーションです。

![メイン画面](images/スクリーンショット%202026-02-11%20235750.png)

## できること

### 1. ドキュメントの自動分析

PDF、文章などのファイルをアップロードすると、AIが内容を分析して保管します。

- **多様なファイル形式対応**: PDF、Word、Excelなどあらゆる形式のファイルから文字を抽出
  - Apache Tika（アパッチ ティカ）を使用 - 世界中で使われている実績のあるツール
- **チャンク分割**: 長い文章を意味のある単位に分割して保存
- **ベクトル化**: 文章を数値ベクトルに変換し、意味検索を可能に

### 2. 自然言語での質問応答

アップロードしたドキュメントの内容について、日本語での質問応答が可能です。

- 「この契約書の主要な条件は？」
- 「去年の売上報告書に書かれている成長率は？」
- 「このマニュアルのトラブルシューティング手順は？」

AIが関連ドキュメントを検索し、正確な回答を生成します。

### 3. セマンティック検索

従来のキーワード検索とは異なり、意味を理解した検索が可能です。

- 「予算」と「経費」は同じ意味として検索
- 質問の意図に合ったドキュメントを発見
- 類似度の高い順に結果を並べ替え

## メリット

| メリット | 説明 |
|---------|------|
| **時間とコストの削減** | 手作業で文書を探す必要がなくなり、数秒で関連情報を取得 |
| **情報の見逃し防止** | すべての文書をAIが網羅的に分析 |
| **専門知識不要** | 自然言語で質問するだけ |
| **社内ナレッジの活用** | 既存の文書資産をAIが活用可能に |

## セールスポイント

### 1. ハイブリッドAIアプローチ
- **Gemini 2.0 Flash** - 高速で費用対効果の高い回答生成
- **pgvector** - 高速でスケーラブルなベクトル検索
- **ONNX Embeddings** - ローカル埋め込みでAPI依存を最小化

### 2. エンタープライズ対応
- **PostgreSQL + pgvector** - 既存のDBインフラを活用
- **Spring Boot 3.5** - 安定性とセキュリティ
- **Docker対応** - どこにでもデプロイ可能

### 3. 使いやすいUI
- **Thymeleaf + HTMX** - シンプルなシングルページアプリケーション
- **リアルタイム検索** - ページ再読み込みなしで結果表示
- **レスポンシブデザイン** - PCでもスマートフォンでも使用可能

### 4. 堅牢なエラーハンドリング
- **レート制限対応** - API制限に達しても適切にエラーメッセージ表示
- **タイムアウト処理** - 長時間処理の自動終了
- **ユーザー向けメッセージ** - 技術的エラー内容ではなくわかりやすいエラーメッセージ

## 技術スタック

- **フレームワーク**: Spring Boot 3.5.10
- **AI**: Spring AI 1.0.0-M1, Google Gemini 2.0 Flash
- **データベース**: PostgreSQL + pgvector
- **埋め込み**: Local ONNX Embeddings
- **フロントエンド**: Thymeleaf + HTMX
- **コンテナ**: Docker Compose

## セットアップ

1. PostgreSQLを起動（pgvector拡張付き）:
```bash
docker-compose up -d
```

以下のコマンドで状態を確認できます
```bash
docker-compose ps
```

2. 環境変数を設定:
```bash
cp .env.example .env
# .envにGEMINI_API_KEYを設定
```

3. ビルド・実行:
```bash
./mvnw spring-boot:run
```

4. ブラウザでアクセス:
```
http://localhost:8080
```

## 環境変数

| 変数 | 説明 | デフォルト値 |
|------|------|-------------|
| `GEMINI_API_KEY` | Google Gemini APIキー（必須） | - |
| `SPRING_DATASOURCE_URL` | JDBC URL | jdbc:postgresql://localhost:5432/aidoc |
| `SPRING_DATASOURCE_USERNAME` | DBユーザー名 | postgres |
| `SPRING_DATASOURCE_PASSWORD` | DBパスワード | password |
| `POSTGRES_USER` | Docker Compose用DBユーザー名 | postgres |
| `POSTGRES_PASSWORD` | Docker Compose用DBパスワード | password |
| `POSTGRES_DB` | Docker Compose用DB名 | aidoc |

## APIキー取得方法

1. [Google AI Studio](https://aistudio.google.com/) にアクセス
2. 「Create API Key」をクリック
3. 生成されたAPIキーをコピー
4. `.env`ファイルまたは環境変数に設定

## ライセンス

MIT License
