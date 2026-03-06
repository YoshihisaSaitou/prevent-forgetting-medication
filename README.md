# PreventForgettingMedicationAndroidApp

## プロジェクトの概要
- 服薬忘れ防止を目的とした Android アプリです。
- AndroidアプリのソースコードはPreventForgettingMedicationAndroidAppディレクトリにあります。
- 薬の登録、服用記録、リマインド通知、ホームウィジェット表示を提供します。
- 詳細仕様は [docs/product-spec.md](docs/product-spec.md) を参照してください。

## セットアップ
1. Android Studio (最新安定版推奨) を用意する
1. JDK 11 を利用できる状態にする
1. プロジェクトルートで Gradle Sync を実行する
1. エミュレータまたは実機 (Android 7.0 / API 24 以上) を準備する

## 開発方法
1. デバッグ実行: Android Studio から `app` モジュールを実行
1. CLI ビルド: `./gradlew assembleDebug`
1. テスト実行: `./gradlew test` / `./gradlew connectedAndroidTest`

## 入口
- 仕様の入口: [docs/product-spec.md](docs/product-spec.md)
- 設計の入口: [docs/architecture.md](docs/architecture.md)
- 実装の入口: `app/src/main/java/com/example/preventforgettingmedicationandroidapp/MainActivity.kt`