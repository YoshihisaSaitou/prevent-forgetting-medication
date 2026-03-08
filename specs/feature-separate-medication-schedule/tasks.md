# Tasks

## Phase 0: 現状分析
- T0-1: 現行データモデル（Medication/IntakeHistory）と画面導線の差分整理
- T0-2: 既存通知・ウィジェット処理の依存箇所一覧化
- 完了条件: 影響範囲一覧が `design.md` と整合している

## Phase 1: データモデル変更
- T1-1: `Schedule` / `ScheduleMedication` エンティティ追加
- T1-2: `Medication` から時刻責務を分離（列移行）
- T1-3: `IntakeHistory` に `scheduleId` を追加
- T1-4: 重複防止インデックス追加
- T1-5: v6 -> 新版へのRoom Migration実装
- 完了条件: マイグレーションテストで既存データが保持される

## Phase 2: ドメインロジック実装
- T2-1: スケジュールCRUDユースケース実装
- T2-2: スケジュール実行（一括履歴記録）実装
- T2-3: 5分抑止をスケジュール単位へ移行
- T2-4: 手動追加ロジックをスケジュール対応
- 完了条件: ユニットテストでBR-1..BR-6を満たす

## Phase 3: UI/ナビゲーション
- T3-1: `MainActivity` をスケジュール一覧UIへ変更
- T3-2: 登録画面をスケジュール登録UIへ変更
- T3-3: 薬マスタ管理画面を新設
- T3-4: フッターメニュー導線と文言更新
- 完了条件: AC-01..AC-08を手動確認できる

## Phase 4: 通知・ウィジェット
- T4-1: `AlarmScheduler` をスケジュール単位へ改修
- T4-2: `NotificationReceiver` の判定ロジック改修
- T4-3: 一覧ウィジェットをスケジュール投影へ変更
- T4-4: 履歴ウィジェットをスケジュール表示へ最適化
- 完了条件: AC-16..AC-22を実機またはエミュレータで確認

## Phase 5: 検証・仕上げ
- T5-1: 受け入れ条件チェックリスト実行
- T5-2: ドキュメント更新（requirements/product-spec/architecture/glossary）整合確認
- T5-3: 回帰確認（既存履歴、通知、削除フロー）
- 完了条件: AC-23..AC-27を含む全ACが合格

## Phase 6: テストツール必須実施
- T6-1: JUnitによるドメインロジック単体テスト作成・実行
- T6-2: MockK + kotlinx-coroutines-test による非同期ユースケーステスト作成・実行
- T6-3: AndroidX Test（core/runner/rules）+ EspressoによるUIテスト作成・実行
- T6-4: Robolectric によるJVM Android依存テスト作成・実行
- T6-5: Hilt Testing を用いたDI連携テスト作成・実行
- 完了条件: 指定7ツールの実行ログが確認できる
