# THE FOOL QUEST Android 管理統合（v0.1.14 / code15 署名・上書き検証済）

## 対象（利用者の2026-10-25変更を反映）

移行対象は、Total/TikTok/Coupon収益・自動合算、えらべるPay CSV取込、TikTok手動収益の登録/編集/削除、Ledger Coupon読取、月間目標、既存ウィジェットのゲージ、月/年/全期間収益履歴、表示設定、えらべるPay履歴をChromeで開く操作です。

**対象外：** Lvと月末予測、TikTok日別稼働登録、Task End、ChatGPT利用枠、povo期限、Grok、**稼働数・収益検証ページ**。これらはAndroid管理画面に追加しません。既存Webサイトのデータや機能は削除しません。

## 既存資産と分界

- Android package: `com.aruno.foolquest.widget.next` のまま。versionCode 15、versionName 0.1.14。既存のインストール済みアプリ/設定の上書き保護には、既存証明書 SHA-256 `8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730` による正式署名と検証が必要。
- Couponの当月収益は既存Ledger Reader APIから30分定期＋手動取得。新DB/書込APIを設けない。通信失敗時は前回正常値を保持。
- Couponの取得済み月別金額は端末内の独立キー `couponRevenueMonthlyV1` に保存。月をまたいでも前月が消えず、アプリの全履歴バックアップに含める。Ledgerの月次確定処理を変更しない。
- TikTok当月収益はアプリ内のCSV金額 + 当月の手動追加金額。CSV再取込ではCSV元金額だけ置換。手動分は保持。
- 目標はTikTok/Coupon各額をアプリ内で変更。Total目標は両者の合算。ウィジェット表示は既存の描画クラスを再利用。
- 履歴と入力はAndroid SharedPreferencesの`nativeRevenueV1`。Webデータは移行前後を通じて変更・削除しない。インポート前の全件バックアップ推奨。
- Androidの`allowBackup=false`は既存のまま。アプリ内「JSONでバックアップ」で選んだ安全な保管先へユーザーがコピーする。アンインストールはデータ消失のため厳禁。

## 一度限りの移行手順

1. 元データが見えている**同じサイトURL・同じChrome**で`manage-export.html`を開く。通常サイト`https://the-fool-quest.45kikurage.workers.dev/manage-export.html`、GitHub Pagesなら`https://45kikurage-rgb.github.io/the-fool-quest/manage-export.html`。
2. 「移行データをJSONで保存」。ファイルにはサイトの月別収益（CSV元金額・過去月Coupon）・手動追加履歴・目標のみが入る。稼働数検証の設定は含めない。
3. 既存Android管理画面で「アプリの全履歴をJSONでバックアップ」を先に実行。
4. 「旧サイトのJSONを取り込む」から保存したファイルを選択。件数・上書き条件を確認して反映。
5. 当月Total/TikTok/Coupon、目標、手動履歴、過去月の数字をサイトと突合する。Couponの当月額は中央台帳の取得結果と比較する。ウィジェットの再描画と月をまたぐ履歴保存を確認する。
6. データ一致・APK上書き・実機確認が完了するまで、Web元データを消さず、旧アプリをアンインストールしない。

ChromeのURLごとにlocalStorageは別です。ブラウザが違う場合、移行ページは旧データを読めません。旧ブラウザ内のデータは自動でAndroidアプリへ直接同期できません。

## 正式APK更新の検証記録（2026-10-25）

- アプリソース変更統合：`45kikurage-rgb/the-fool-quest` PR #21 / main merge `7351d68889d60e8dcec6021b2f469d2b1fdcdce6`。
- 再試験・未署名候補の固定ソースコミット：`635290ee7c89371a55d2644a0aeb659f814bb7df`。Android35 run `40189070355` 全試験成功。
- 未署名 APK：SHA-256 `44e605ccca73f2003ad396d6e506425a35463eb41e7f0bacc54d4e9b7592b51d`。Android標準ビルドを秘密鍵のないrunnerで生成。
- 署名：既存の新パッケージ鍵をGitHub主保管・独立バックアップの別runnerで復元。両成功後のrunnerで署名したrun `40189680605`。新規鍵なし。証明書SHA-256 `8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730`。
- 署名APK `releases/fool-quest-widget-0.1.14.apk`：71,470 bytes、SHA-256 `4d0441de77bfb27577ddefe598f98a57af73be8f4851d5929525901115e5f8f2`。
- Android35 ソフトウェア受入：描画/収益等3,398件、通信断123件、再起動113件に合格（code15）。
- 旧正式v0.1.13/code14からの`adb install -r`・署名一致・UID維持・既存29項目の保持・旧パッケージ不変・新管理画面への収益/目標引継ぎを検証：run `40190235293` 合格。実機受入ではない。
- 旧 v0.1.13、より旧版の正式APKと配布URLはそのまま保管。本番公開はGitHub main統合および公開先HTTP200/SHA/版の再取得で別に確認する。
- 正式ソース公開コミット：`beca8bc9ab6ce38f4be0d77e9b3e10373ac792b9`（PR #24）。GitHub Pages配信 run `40190629813` 成功。main上書き受入 run `40190630578` 成功。
- 2公開先独立照合 run `40190813547`：GitHub Pages・Cloudflareの両URLでHTML、manifest、署名APKをHTTP取得し、APK SHA-256 `4d0441de77bfb27577ddefe598f98a57af73be8f4851d5929525901115e5f8f2`、71,470 bytes、code15、既存証明書と完全一致。**両配布先PASS**。
- GitHub Pages: `https://45kikurage-rgb.github.io/the-fool-quest/widget-download.html`。Cloudflare: `https://the-fool-quest.45kikurage.workers.dev/widget-download.html`。Android実機上書き・サイトからの本番履歴の一致は未確認。

## 試験/公開の区別

GitHub署名・CI・公開・実機確認・正式運用は別の状態です。GitHub Actionsの試験APKを配布物として使用しません。正式署名されたAPKのハッシュと公開URLの応答を照合した後に公開完了とします。利用者のPixel/Nova等の実機上書き・サイトからの全履歴照合は引き続き未確認です。アンインストールや初期化はしないでください。
