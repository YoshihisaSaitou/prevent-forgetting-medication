# Design

## 1. 目的
`feature-separate-medication-schedule` を実装可能な粒度で、データ設計・画面設計・処理フローを定義する。

## 2. データ設計
### 2.1 Entity定義（案）
```sql
-- medications: 時刻責務を持たない薬マスタ
CREATE TABLE medications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  mealTiming TEXT,
  memo TEXT
);

-- schedules: 服薬実行単位
CREATE TABLE schedules (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  slot TEXT NOT NULL,            -- MORNING/NOON/EVENING
  timeMinutes INTEGER NOT NULL,  -- 0..1439
  isActive INTEGER NOT NULL DEFAULT 1,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL
);

-- schedule_medications: N:N関連
CREATE TABLE schedule_medications (
  scheduleId INTEGER NOT NULL,
  medicationId INTEGER NOT NULL,
  displayOrder INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (scheduleId, medicationId)
);

-- intake_history: 既存テーブルを拡張
ALTER TABLE intake_history ADD COLUMN scheduleId INTEGER;
CREATE UNIQUE INDEX ux_history_schedule_med_taken
  ON intake_history(scheduleId, medicationId, takenAt);
```

### 2.2 DAO要件
- ScheduleDao
  - `getAllWithMedications()`
  - `insertScheduleWithMedications()`（Transaction）
  - `updateScheduleWithMedications()`（Transaction）
  - `deleteSchedule()`
- IntakeHistoryDao
  - `insertBatchForSchedule(scheduleId, takenAt)`
  - `existsScheduleTakenAt(scheduleId, takenAt)`
  - `getGroupedHistory()`

## 3. 画面設計
### 3.1 一覧画面（Schedule List）
- 行要素
  - スケジュール名
  - 時間帯+時刻
  - 薬サマリ（例: 3件）
  - `飲んだ` / `編集` / `削除`
- 空状態
  - スケジュール未登録メッセージ

### 3.2 スケジュール登録画面
- 入力項目
  - スケジュール名
  - 時間帯
  - 時刻
  - 薬選択（複数）
- 保存時
  - バリデーション
  - 保存成功で一覧へ戻る

### 3.3 薬マスタ管理画面
- 一覧+CRUD
- スケジュールに使用中の薬削除時は確認ダイアログ

### 3.4 履歴画面
- デフォルト: スケジュール単位のグループ表示
- 詳細: 薬内訳
- アクション: 手動追加、誤登録トグル

## 4. 処理フロー
### 4.1 スケジュール実行フロー
1. UIから `scheduleId` を受け取る
2. `TakenStateStore` で5分抑止確認
3. `existsScheduleTakenAt(scheduleId, nowRounded)` で重複確認
4. Transactionで対象薬ぶん `intake_history` を一括insert
5. 5分抑止セット
6. ウィジェット更新・UI再描画

### 4.2 通知フロー
1. スケジュール変更時に全アラーム再構成
2. 発火時にスケジュール有効性を再評価
3. 通知表示
4. 次回時刻を再スケジュール

## 5. 移行設計
### 5.1 データ移行アルゴリズム
1. 既存 `medications` を読み込み
2. 旧 `timing` と旧時刻設定から `schedules` を生成
3. 各スケジュールに薬を1件紐づけ
4. 旧履歴は `scheduleId = null` で保持し、補完可能なもののみ関連付け

### 5.2 互換性
- 移行期間は `scheduleId null` 履歴も表示対象に含める。
- 旧ウィジェットレイアウトIDは段階的置換し、クラッシュを防ぐ。

## 6. テスト設計
- Unit
  - バリデーション
  - 重複防止
  - 5分抑止
- Integration
  - スケジュール保存→通知再構成
  - 実行→履歴→ウィジェット反映
- Migration
  - v6サンプルDBからの移行検証

## 7. 実装マッピング（DDD + Clean Architecture）
- `presentation`
  - Activity + ViewModel（StateFlow）
  - `MainViewModel`, `ScheduleFormViewModel`, `HistoryViewModel`, `MedicationMaster*ViewModel`
- `application`
  - UseCase + Port
  - `GetScheduleList`, `CreateOrUpdateSchedule`, `ExecuteSchedule`, `AddManualHistory`, `ToggleIncorrect`, `SyncAlarms`
- `domain`
  - Entity/ValueObject/Policy/Repository interface
  - `ScheduleId`, `MedicationId`, `TakenAt`, `IntakePolicy`
- `infrastructure`
  - Room Repository実装、Alarm/Widget/TakenState Adapter
- `di`
  - Hilt Module (`DatabaseModule`, `BindingModule`)

## 8. DB移行の実装方針
- DB名: `medications.db`（維持）
- Migration: `v7 -> v8`
  - `intake_history` の重複行を事前削除
  - `scheduleId IS NOT NULL` 条件付きで `(scheduleId, medicationId, takenAt)` の一意インデックス追加
- `allowMainThreadQueries` は使用しない

## 9. テスト実装方針（必須ツール）
- JUnit: `IntakePolicy` バリデーションテスト
- MockK + kotlinx-coroutines-test: UseCase / Repository 単体テスト
- AndroidX Test（core/runner/rules）+ Espresso: `MainActivity` UI検証
- Robolectric: `BootReceiver` JVMテスト
- Hilt Testing: Repository差し替え統合テスト
