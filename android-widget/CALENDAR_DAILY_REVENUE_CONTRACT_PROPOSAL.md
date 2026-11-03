# 収益カレンダー用・日別Reader契約案（統括承認前／非公開提案）

**状態：提案のみ。中央Ledger・Vault・ARUNO ASSISTの本番API/DBには変更を加えない。**

## 現状の取得元

- Vaultの `items.received_at`（JST換算）、`item_campaign_assignments`、`ledger_products.unit_price` から日別のCoupon金額を集計可能。既存の `GET /api/revenue/summary` は **当日** 集計に限定され、過去月/全日一括取得には非対応。
- Ledgerの `product_monthly_revenue` は商品×月集約で、現在の公開Reader `GET /api/v1/revenue/monthly?month=YYYY-MM` が返す `coupon_revenue` は **月計** のみ。
- TikTok Liteについて、Ledgerに日別保存の設計・実装証拠なし。ARUNO ASSISTは原本CSVをそのアプリの専用領域に保存している。

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

## TikTok Lite CSV日別連携のWork検討事項

- 現行ARUNO ASSISTは `PaySyncResult` で `month,total,count,fingerprint,fileName` だけを返し、CSV原本をアプリ専用領域 `files/pay-sync/YYYY-MM.csv` に保存している。
- Pixel 10のTHE FOOL QUESTネイティブが直接CSVを選択したときは、このPRの `RevenueManager.parseCsv` で日別値を保存可能。
- **ASSISTからの自動連携は未実装**。Workで、ユーザーが明示して起動する `ACTION_SEND` + `FileProvider` の一時的な読取許可など、既存境界を維持した安全な受け渡し方法を比較する。ASSISTの内部ファイルを外部から無制限公開しない。
- 原本CSVが残らない古い月は月計だけ表示し、日別内訳は「—」。手動追加分は既存の日付付き記録を使う。
- CSV再取込時の月計は **置換** であり、手動追加は別集計。再送や重複を累積加算しない。

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
