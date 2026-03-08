# Architecture

## 1. 設計方針
- 服薬対象（薬）と服薬運用（スケジュール）を明確に分離する。
- UI都合ではなく、ドメイン責務に沿ってデータ構造を再定義する。
- 既存データを移行可能な形で段階的に変更する。
- 既存のオフライン完結・通知・ウィジェット連携の特性を維持する。
- 本機能の実装方式はドメイン駆動設計（DDD）を採用し、ドメインモデルをアプリケーション層/データ層から明確に分離する。

## 2. 論理レイヤ構成
### 2.1 Presentation Layer
- `ScheduleListActivity`（既存 `MainActivity` の置換）
- `ScheduleRegistrationActivity`（既存 `MedicationRegistrationActivity` の置換）
- `MedicationMasterActivity`（新設）
- `HistoryActivity`, `SettingsActivity`, Widget UI
- 役割: 入力受付、画面遷移、表示更新、エラー表示

### 2.2 Application Layer
- `ScheduleExecutionService`
  - スケジュール実行と履歴一括記録
  - 重複防止、5分再実行抑止
- `ScheduleAlarmScheduler`
  - スケジュール単位のアラーム再構成
- `WidgetProjectionService`
  - スケジュール/履歴ウィジェット用投影
- 役割: ユースケース調停、トランザクション境界の制御

### 2.3 Data Layer
- Room Entity/DAO
  - `MedicationEntity`
  - `ScheduleEntity`
  - `ScheduleMedicationCrossRef`
  - `IntakeHistoryEntity`（scheduleIdを保持）
- SharedPreferences
  - 5分抑止状態（scheduleId単位）
- 役割: 永続化、クエリ、整合性の担保

## 3. ドメイン境界
- Medication Catalog Context
  - 薬マスタのライフサイクル管理
- Schedule Planning Context
  - スケジュール作成・編集・薬紐づけ
- Intake Tracking Context
  - スケジュール実行、履歴、誤登録管理
- Reminder Context
  - 通知スケジュール作成、再起動復元
- Widget Projection Context
  - ホーム画面表示向けRead Model

## 4. データ設計方針
### 4.1 To-Be テーブル
- `medications`
  - `id`, `name`, `mealTiming`, `memo`
- `schedules`
  - `id`, `name`, `slot`, `timeMinutes`, `isActive`, `createdAt`, `updatedAt`
- `schedule_medications`
  - `scheduleId`, `medicationId`, `displayOrder`
- `intake_history`
  - 既存列に加え `scheduleId` を追加
  - 重複防止のため `(scheduleId, medicationId, takenAt)` の一意制約を追加

### 4.2 移行方針
- 既存 `medications` の各レコードを薬マスタとして残す。
- 既存薬の時間帯/時刻設定から初期スケジュールを生成する。
- 履歴は可能な範囲で `scheduleId` を補完し、不明分は `null` 許容で保持する。
- 破壊的マイグレーションではなく、段階的マイグレーションを採用する。

## 5. 外部連携方針
- Android Framework のみ使用
  - AlarmManager
  - BroadcastReceiver
  - NotificationManager
  - AppWidgetManager / RemoteViewsService
- 外部API連携は行わない。

## 6. 採用技術
- Kotlin 2.0.21
- Android Gradle Plugin 8.13.2
- Room 2.6.1
- minSdk 24 / targetSdk 36 / compileSdk 36

## 7. 設計上の制約
- 単一モジュール構成を維持（大規模分割は本機能外）
- 既存画面資産を活かしつつ、名称・責務のみ刷新する
- 履歴・通知・ウィジェットの整合性を優先し、局所最適な実装分岐を避ける

## 8. 品質戦略
- ユニットテスト
  - 重複判定、未来日時禁止、5分抑止
- 結合テスト
  - スケジュール実行→履歴反映→ウィジェット更新
- 移行テスト
  - v6データからのマイグレーション整合
- 手動確認
  - 通知発火、再起動復元、画面導線




## 9. テスト基盤方針（必須）
品質戦略の実施手段として、以下のテストツールを必須とする。
- JUnit
- MockK
- kotlinx-coroutines-test
- AndroidX Test（core/runner/rules）
- Espresso
- Robolectric
- Hilt Testing
