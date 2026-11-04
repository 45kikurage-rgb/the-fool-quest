# 収益カレンダー用・日別Reader契約案（統括承認前／非公開提案）

**状態：提案のみ。中央Ledger・Vault・ARUNO ASSISTの本番API/DBには変更を加えない。**

## 現状の取得元

- Vaultの `items.received_at`（JST換算）、`item_campaign_assignments`、`ledger_products.unit_price` から日別のCoupon金額を集計可能。既存の `GET /api/revenue/summary` は **当日** 集計に限定され、過去月/全日一括取得には非対応。
- Ledgerの `product_monthly_revenue` は商品×月集約で、現在の公開Reader `GET /api/v1/revenue/monthly?month=YYYY-MM` が返す `coupon_revenue` は **月計** のみ。
- TikTok Liteについて、Ledgerに日別保存の設計・実装証拠なし。**利用者の実運用はTHE FOOL QUESTからChromeでCSV取得→同アプリ内でCSVを取り込む方式**。ARUNO ASSISTの原本保存・転送はこの運用に関係しない。

## 統括確認が必要な提案

**Vault日別金額の正本はVault、横断ReaderはLedger、THE FOOL QUESTは表示のみ。** 現行の責任分界を変更せず、次の非破壊的な候補を提案する。

1. Vaultが1か月の日別集計値をJSTで確定。現行の月計と同じ `items.status='active'`・商品割当・単価の業務ロジックを使う。未価格設定品・未判定品を数値0として無言で計上しない。
2. 共有仕様を統括で承認した後、Vault→Ledgerの既存Service Binding・source_revision・冪等性を使った **日別投影** を追加する（新IDの勝手な発行を避ける）。LedgerはURL/コード本文を持たない。
3. Ledger Readerには指定月の31日分までを返す読取専用APIを追加する。認証/Originは既存Readerの方針を維持し、公開範囲は監査して決定する。クライアント側からVaultへ管理者トークンを埋め込んで直接アクセスしない。
4. 集計月額とReaderの月計が一致し、送信元revision・更新状態が確認できた月だけ、端末内 `couponRevenueDailyV1[YYYY-MM]={verified:true,total,days}` に保存する。日付キーは `YYYY-MM-DD`、単位円で0以上の整数。
5. 不一致・同期遅延・欠落・未確定は「—」表示とし、月計に架空の日付を割り当てない。再取得時は正常な前回投影を即時削除しないが、最新月計との差異が生じた場合は日別を未取得扱いにする。

**Readerレスポンス案（契約は未確定）**

```json
{
  "month": "2026-11",
  "coupon_revenue": 300,
  "daily_revenue": [
    {"date": "2026-11-01", "amount": 100},
    {"date": "2026-11-02", "amount": 200}
  ],
  "unpriced_count": 0,
  "is_complete": true,
  "source_revision": 12,
  "updated_at": "2026-11-03T12:00:00Z"
}
```

サンプル数値は架空のテストデータ。日付のない日は、当月全日が完全照合されたときだけ0とみなす。返答にURL/交換コード/カード情報は載せない。

## TikTok Lite CSV日別連携のWork検討事項（Chrome→取込の現行運用を維持）

- 実際の手順は **THE FOOL QUEST「えらべるPayのポイント履歴をChromeで開く」→ChromeでCSV取得→THE FOOL QUEST「CSVを取り込む」→確認→保存**。ARUNO ASSISTは利用しない。
- 既存 `ManagementActivity` は `ACTION_OPEN_DOCUMENT` を使ってCSVファイルを開く。日付・金額を `RevenueManager.parseCsv` で一度だけ解析し、同じ確認操作で月額と日別を更新する実装を優先する。
- 再取込は月額・日別の**一括置換**。手動追加分は別枠で保持し、二重加算しない。日付付きCSVと月額は突合する。
- 過去月のCSVが残っていれば、同じ「CSVを取り込む」操作で再解析できるか検証する。存在しない日付を作らない。
- **ARUNO ASSIST／`#paySync` 拡張／`ACTION_SEND`／`FileProvider`／アプリ間自動同期は今回不要**。必要性が生じない限り、追加開発しない。
- CSVの実物による日付形式・列名・文字コード（UTF-8 BOM / MS932）・対象月・操作の通し試験がWorkの主な確認項目。

## Workの優先検証

1. 仕様承認の可否と正本・データ保持を明確にする。
2. 複数月・日付なし・負金額・重複CSV・CSV再取り込み・JST境界・月末・閏日のテスト。
3. Vaultの日別計とLedgerの商品月次収益の一致を本番の読み取り専用で確認。確認できなければ表示は保留。
4. Android SDK35でのビルド、Pixel 10の実寸・5×5配置/4×5配置時の収まり、タップ切替、再起動、既存設定とデータ保持。
5. 統括CURRENT§20と対象署名台帳の確認後に正式署名・配布。**Chat段階では公開しない**。

## 再点検の追加留意点
- Reader月額と当選管理サイトの当日/日別集計の結果は集計タイミング・未価格設定・訂正・ステータスの差により一致しない可能性がある。**単純に当日分を31回GETして足す方式は推奨しない**。
- 同一JST日付の判定と日別完全集計はVault側で行い、Readerには指定月1回の軽量レスポンスが望ましい。新DBに原本を複製しない。
- クーポン表示だけを完成と報告しないこと。現在のPRでは公開日別Coupon APIがなく、日別は「—」のまま。

## Museレビューを踏まえた代替案比較（仕様未確定）

| 案 | データ流れ | 長所 | 懸念 |
|---|---|---|---|
| A：既存Ledgerへ日別投影追加 | Vaultが月別日別を集約→Ledger保存→FOOL QUEST Reader | 可用性・監査履歴をLedgerに集約 | 保存重複、revision・訂正・同期遅延・DB拡張 |
| B：閲覧時の都度集計（優先検討） | FOOL QUEST→Ledger Reader→Vault内部Reader（サービス認可）→Vaultで月31日分一括集計 | 正本Vault、不要な日別DB投影なし、訂正追随 | 現在Ledger→Vaultの逆方向サービス接続があるか未確認。新設の場合は統括承認・環境設定が必要 |
| C：Vault公開APIをFOOL QUESTが直接読取 | FOOL QUEST→Vault | 実装が少ない可能性 | 統括CURRENTのLedger集約・Reader責任と不整合の恐れ、認証/CORS/可用性の追加検討が必要 |

**推薦：Bを第一候補として権限・既存Service Binding・負荷とレスポンス契約を検証**。Aは既存の統括決定や性能/可用性要件により選択し得る。Cを独自確定しない。すべて本番変更前に統括確認。

**TikTok Lite側の推奨を訂正：** 利用者はARUNO ASSISTを使用せず、THE FOOL QUESTアプリからChromeでCSV取得後、同アプリでCSVを取り込む。従って`#paySync` payload拡張もアプリ間連携も**提案から除外**。最小変更はTHE FOOL QUESTの既存CSV取込から日別集計を同時に保持すること。

## Work独立監査と統括判断事項

Vaultの現行 `getRevenueSummary()` は、当日の収益を `items.received_at` のJST日界、`active`、商品割当、単価で計算する。月計側は `product_monthly_revenue` を読み、商品×月の件数・単価・収益・同期revisionを保持する。Vault→Ledgerのサービス接続は実コードにあり、Ledgerの公開Readerは月と収益額だけを返す。Ledger→Vaultの本番内部接続および日別Readerは未確認。日別の本番レコード・整合性は未検証。

| 候補 | 工数・保守 | 正確性と影響 | 認可・通信 |
|---|---|---|---|
| A. Vaultで閲覧時に集計し、Ledgerで中継 | 月の日付範囲で1回集計。新規DB不要。Ledgerの内部接続と読取契約は追加 | 再分類・単価訂正を再取得時に反映。確定月の元明細が失われた場合は復元できない | 読取専用のサービス認可と、Ledger上の利用者認可を確認。端末は月1応答 |
| B. Ledgerに日別投影を追加 | 保存・revision・再同期・復旧・訂正処理が追加 | 確定時点の履歴と応答可用性を維持しやすい。遅延・欠落に備える | Vault→Ledgerの既存方向を再利用。投影書込と端末Readerの契約・認可を追加 |
| C. 現行Readerの応答だけを拡張 | Ledger月次行からは日別を復元できない。AまたはBの取得元が必要 | 同じ月額でも日付の振替や複数商品の相殺を見逃すため、総額一致だけでは検証不足 | 現行公開月次Readerを維持。日別は認可範囲を別途決定 |

**第一候補はA。** まず確定月を含む代表月で、保持された元明細と商品・月ごとの件数、適用単価、金額、確定状態、同期revisionを照合する。Vaultの確定済み月次行は再集計で更新されず、元明細の削除や単価の変更後に現在の行だけから過去日別を復元できるとは限らない。該当月が再現できない場合は未取得として扱い、今後の確定日別履歴が必要であればBの保存範囲を統括で決める。旧月の推測補完は行わない。

統括判断は、Ledgerを窓口にする内部読取経路、端末の認可、確定月の保存範囲の3点。現行公開月次Readerに日別を無条件で追加しない。本番DB、他システム、公開先は変更対象外とした。
