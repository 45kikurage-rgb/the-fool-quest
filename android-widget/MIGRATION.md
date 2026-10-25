# THE FOOL QUEST Android 管理統合（v0.1.14 開発候補）

## 対象（利用者の2026-10-25変更を反映）

移行対象は、Total/TikTok/Coupon収益・自動合算、えらべるPay CSV取込、TikTok手動収益の登録/編集/削除、Ledger Coupon読取、月間目標、既存ウィジェットのゲージ、月/年/全期間収益履歴、表示設定、えらべるPay履歴をChromeで開く操作です。

**対象外：** Lvと月末予測、TikTok日別稼働登録、Task End、ChatGPT利用枠、povo期限、Grok、**稼働数・収益検証ページ**。これらはAndroid管理画面に追加しません。既存Webサイトのデータや機能は削除しません。

## 既存資産と分界

- Android package: `com.aruno.foolquest.widget.next` のまま。versionCode 15、versionName 0.1.14（開発候補）。既存のインストール済みアプリ/設定の上書き保護には、既存証明書 SHA-256 `8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730` による正式署名と検証が必要。
- Couponの当月収益は既存Ledger Reader APIから30分定期＋手動取得。新DB/書込APIを設けない。通信失敗時は前回正常値を保持。
- TikTok当月収益はアプリ内のCSV金額 + 当月の手動追加金額。CSV再取込ではCSV元金額だけ置換。手動分は保持。
- 目標はTikTok/Coupon各額をアプリ内で変更。Total目標は両者の合算。ウィジェット表示は既存の描画クラスを再利用。
- 履歴と入力はAndroid SharedPreferencesの`nativeRevenueV1`。Webデータは移行前後を通じて変更・削除しない。インポート前の全件バックアップ推奨。
- Androidの`allowBackup=false`は既存のまま。アプリ内「JSONでバックアップ」で選んだ安全な保管先へユーザーがコピーする。アンインストールはデータ消失のため厳禁。

## 一度限りの移行手順

1. 元データが見えている**同じサイトURL・同じChrome**で`manage-export.html`を開く。通常サイト`https://the-fool-quest.45kikurage.workers.dev/manage-export.html`、GitHub Pagesなら`https://45kikurage-rgb.github.io/the-fool-quest/manage-export.html`。
2. 「移行データをJSONで保存」。ファイルにはサイトの月別収益（CSV元金額・過去月Coupon）・手動追加履歴・目標のみが入る。稼働数検証の設定は含めない。
3. 既存Android管理画面で「アプリの全履歴をJSONでバックアップ」を先に実行。
4. 「旧サイトのJSONを取り込む」から保存したファイルを選択。件数・上書き条件を確認して反映。
5. 当月Total/TikTok/Coupon、目標、手動履歴、過去月の数字をサイトと突合する。Couponの当月額は中央台帳の取得結果と比較する。ウィジェットの再描画を確認する。
6. データ一致・APK上書き・実機確認が完了するまで、Web元データを消さず、旧アプリをアンインストールしない。

ChromeのURLごとにlocalStorageは別です。ブラウザが違う場合、移行ページは旧データを読めません。旧ブラウザ内のデータは自動でAndroidアプリへ直接同期できません。

## 試験/公開の区別

この資料にあるv0.1.14は開発候補で、正式署名・配布・実機受入までを主張しません。GitHub PRとCI、旧正式v0.1.13の署名・バックアップ復元・上書き検証、配布URLとAPKのSHA照合、利用者端末でのCSV・履歴照合はそれぞれ独立した工程です。既存v0.1.13正式APKは残し、検証なしに差し替えないでください。
