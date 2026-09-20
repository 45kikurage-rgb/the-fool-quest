# THE FOOL QUEST 完成整理版

ZIP内の全ファイルをGitHubへアップロードし、同名ファイルを置き換えてください。
ロゴと3色の透過スライム画像も同梱しています。

## 維持している機能

- 合計目標の達成率を整数で示すレベル表示
- Day+14表示
- CSVからTikTokチャージ合計を取込み
- Coupon当月収益のAPI同期と前回正常値の保持
- 重複確認済みの継続箱だけをCoupon収益へ反映（一時箱は除外）
- 同じ月の収益が突然0円になった場合の誤上書き防止
- Total自動合算
- 目標額の変更と端末内保存
- 月の日割り進捗による青・赤ゲージ
- 目標超過時の炎表示
- 6個のポータルリンク（IMAGE EDITORを削除、COUPON CHECKER・LIGHTWEIGHT LINKを表示）
- PWA・オフラインキャッシュ
- Lv1で1匹、以後10Lvごとに増える3色スライム（最大20匹）
- 増えたスライムの色は固定し、配置だけ起動ごとに変更
- 月初の当月収益リセットと月別保存
- 翌月6日以降のCoupon前月確定と月別一覧

## データ互換性

既存のlocalStorageキーを維持しているため、同じURLへ上書き公開すれば端末内のTikTok値、Coupon正常値、CSV履歴、目標額を引き継ぎます。

## ChatGPT Magic Point / Povo / Grokリンク

- ホームはChatGPTを左2/3、Povoを右1/3で表示します。境界は細い縦線で、既存の枠高とページ全体構成を維持します。
- Povoは `09/16 19:27` の形式で番号と年を省き、内部では年・日本時間を保持します。
- Androidの画像共有、または管理画面の「ChatGPT・Povo スクショ読取」で画像を取り込みます。複数選択に対応しています。
- Magic Point見出し内の「Grok ▶」からCursor Proの利用状況ページを開けます。GrokゲージとGrokスクショ解析は行いません。
- Cursor/Grok画面はChatGPTのスクリーンショットとして扱わず、Magic Pointの残量を変更しません。
- Codex / Workの現行Proアナリティクス画面（`Codex と Work`、`週間利用上限 xx% 残り`）を認識し、週間残量をMagic Pointへ反映します。
- 共有画像は個別に一時保存して順番に読み取り、新しいPovo期限を先頭へ追加します。同じ期限の2枚も別件として残し、最新2件を表示します。
- 期限と受信IDは `foolQuestPovoExpiryV1` に端末内保存します。読取失敗では既存期限を変更しません。画像は処理後に削除します。
- テスト: `node --test tests/*.test.js`

## 2026-09-21 ポータル更新

- IMAGE EDITORを削除。
- URL CAPTUREをCOUPON CHECKERへ変更し、`coupon-capture.../checker/`へ接続。
- FOOL LINKをLIGHTWEIGHT LINKへ変更し、`lightweight-links.pages.dev`へ接続。
- 旧リンクを端末に保存済みの場合も、旧既定値だけは新しい既定URLへ自動移行します。
