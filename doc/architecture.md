# Architecture

## システム構成
- クライアント単体構成（Android ネイティブアプリ 1 モジュール）
- アプリのソースコードはPreventForgettingMedicationAndroidAppディレクトリ
- 永続化:
  - Room(SQLite): `medications`, `intake_history`
  - SharedPreferences: 共通時刻設定、5 分無効状態
- OS 連携:
  - AlarmManager + BroadcastReceiver で通知起点管理
  - NotificationManager で通知表示
  - AppWidgetProvider/RemoteViewsService でホームウィジェット表示

## レイヤ構成
1. Presentation Layer
- Activity: `MainActivity`, `MedicationRegistrationActivity`, `HistoryActivity`, `SettingsActivity`
- Adapter/ViewFactory: `MedicationAdapter`, `HistoryListAdapter`, `MedicationWidgetService.Factory`, `HistoryWidgetService.Factory`
- 役割: 画面描画、入力受付、ナビゲーション、UI更新

1. Application Layer
- `AlarmScheduler`: アラームのスケジュール/解除
- `WidgetUtils`: ウィジェット更新の統一ユーティリティ
- `TimePreferences`, `TakenStateStore`: ドメイン設定・一時状態管理
- 役割: ユースケース実行の調停（通知再設定、状態反映）

1. Data Layer
- Entity: `Medication`, `IntakeHistory`
- DAO: `MedicationDao`, `IntakeHistoryDao`
- DB: `MedicationDatabase` + `Converters`
- 役割: ローカルデータの CRUD、クエリ、型変換

## Bounded Context
1. Medication Catalog Context
- 対象: 薬マスタ（名前、食事タイミング、服用時間帯、メモ、薬別時刻）
- 主な操作: 登録/更新/削除/一覧取得

1. Intake Tracking Context
- 対象: 服薬履歴（服用時刻、手動追加、誤登録）
- 主な操作: 記録追加、重複判定、誤登録トグル、履歴参照

1. Reminder Scheduling Context
- 対象: 朝/昼/夕の通知スロット
- 主な操作: アラーム再構成、通知発火、再起動復元

1. Home Widget Projection Context
- 対象: ホーム画面向け投影ビュー（薬一覧、履歴一覧）
- 主な操作: RemoteViews 生成、更新通知、ウィジェット操作受付

1. Time Settings Context
- 対象: 共通時刻設定・薬別時刻上書き
- 主な操作: 設定保存、表示用フォーマット、通知再スケジュール

## 外部連携
- Android Framework
  - `AlarmManager`: 時刻通知トリガー
  - `NotificationManager` / Notification Channel: リマインダー通知
  - `AppWidgetManager`: ホームウィジェット更新
  - `BroadcastReceiver`: 通知受信、端末起動受信、ウィジェット操作受信
- 外部サーバ/API 連携はなし

## 技術的制約
- 言語/ビルド:
  - Kotlin 2.0.21
  - AGP 8.13.2
  - Java 11 ターゲット
- Android 条件:
  - `minSdk=24`, `targetSdk=36`, `compileSdk=36`
  - Android 13 以上で通知権限 (`POST_NOTIFICATIONS`) の実行時許可が必要
- データ制約:
  - `intake_history.medicationId` に外部キー制約なし（論理参照のみ）
  - スキーマ変更時は破壊的マイグレーション許容
- 実装制約:
  - `allowMainThreadQueries` 使用により、今後データ量増加時の性能対策が必要
  - クリーンアーキテクチャ/DI フレームワーク未導入のため、結合度は比較的高い