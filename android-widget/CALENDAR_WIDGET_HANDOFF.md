# Pixel 10 収益カレンダーウィジェット — 開発引き継ぎ（監査・公開前）

## 範囲
- 新規の **別 AppWidgetProvider** `CalendarWidget` を追加。既存の3段ゲージ（`RevenueWidget`、レイアウト、色、目標線）は変更しない。
- ヘッダー左：表示（3種類／合計／Coupon／TikTok Lite）、中央：年月選択、右：月計（合計→Coupon→TikTok Lite）。
- 日別は日付と3金額。値を取得できない場合 **「—」**。0円と不明を混同しない。
- 端末内状態はウィジェットIDごと。年月・表示モードのみ保存。日付はJST基準。
- 最小幅はPixel 10のホーム横幅を念頭に設計。6週・文字切れ・タップ領域の**実機受入未確認**。

## 確認した既存取得経路
1. **Coupon**: 当選管理のVault D1 `items.received_at` + `item_campaign_assignments` + `ledger_products.unit_price` から、`getRevenueSummary()` は **当日だけ** を計算できる。中央Ledger `product_monthly_revenue` は月単位の集約のみ。
2. **TikTok Lite**: ARUNO ASSISTはCSV **原本を端末ローカル** `files/pay-sync/YYYY-MM.csv` に保存する。THE FOOL QUESTネイティブは従来CSV月合計＋手動追加の日付を保存。新CSV取込処理は日付列を解釈できた場合だけ `nativeRevenueV1.months[YYYY-MM].csvDaily` に合計と一致する日別内訳を保存する。
3. 公開のLedger `/api/v1/revenue/monthly?month=YYYY-MM` は月合計のみ。中央LedgerにTikTok Lite日別・CSV原本が同期されている証跡はない。
4. 本番D1への読み取り権限がこの作業ではないため、実レコード件数・日別データの実保存有無は未確認。

## Workで監査・本番公開前に必ず行うこと
- Android SDK 35 compile、Android 35 AppWidgetHostでPixel 10に近いサイズ／6週表示／文字切れ／端末再起動／月跨ぎ／タップ別PendingIntentを検証。
- 端末内旧履歴・ゲージ設定・Couponキャッシュ・CSV手動追加・バックアップ復元の上書き保持を確認。
- **Couponの過去日**をカレンダーに出すには、統括CURRENTで共有API契約の承認を受けたうえで、Vaultの既存`received_at`ベースの日別計算を読み取り専用で月単位に取得するReader契約（Vault→Ledger→表示）を追加し、その結果の合計照合後に`couponRevenueDailyV1`へ保存する。認証や元データ正本を勝手に移管しない。今回のブランチにはこの新API・書込は追加していない。
- **TikTok原本**は既存のARUNO ASSISTから自動連携されるわけではない。現在はネイティブアプリでCSVを選択すると日別も保存する。ARUNO ASSISTからの自動日別連携は別途検討が必要。
- 日付未記載／複数月混在／CSV金額不一致は日別として確定しない。既存月額は維持する。保存済み過去の月額を日割りで偽造しない。
- 統括§20・対象アプリ署名台帳の全文確認、既存証明書SHA-256・別runner復元検証、versionCodeの正式採番、アップグレード・データ保持、正式署名、公開URL・APK SHA-256、実機受入は**Workで実施**。
- この作業では本番DB・外部API・正式APK・配布先を変更しない。

## 状態
設計・実装候補。既存の月額と手動日付を読み取り、CSV日別を保存してカレンダー描画するコードを提案。Coupon日別Readerは未連携。ソフトウェアテスト、本番反映、実機確認、正式運用は未実施。
