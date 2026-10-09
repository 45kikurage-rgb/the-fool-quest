# THE FOOL QUEST Android収益ウィジェット

独立APK `com.aruno.foolquest.widget`、v0.1.3 / versionCode 4。Android 8.0以上。既存のTHE FOOL QUEST、ARUNOMATIC、Vault、Ledgerへの書き込みを行いません。

## 現在のデータ責任

- Coupon：既存Ledger `GET /api/v1/revenue/monthly?month=YYYY-MM`、`coupon_revenue`。認証情報を含まない既存公開Readerを再利用。
- TikTok：THE FOOL QUESTのブラウザ内の月別CSV金額＋当月の手動チャージ。`widget-export.html` が保存済み値を読み、ユーザーの確認後、アプリの表示キャッシュへ渡します。
- 目標：同じブラウザの `foolQuestGoalAmounts`。未設定時は既存サイトと同じTotal 1,000,000円、TikTok/Coupon各500,000円。
- Total：同一月のTikTok＋Coupon。未連携のTikTokを0円とは扱いません。

**制限：** 現行サイトにはTikTok・目標を取得するAPIがありません。30分・タップ更新はCouponのみ。TikTokと目標はサイトで変更後に再連携が必要です。完全自動同期を実装済みとは扱いません。新DB、共有API契約、認証は追加していません。

## 表示と操作

5×2初期サイズ、縦3行、現在額・スラッシュ・目標額を個別の固定欄にし、足りない桁は空白の幅を保持。単幅数字、7桁金額2個・小数2桁パーセント。ゲージは100%で上限、数値は実際の割合。文字・余白が収まらない設定は描画時に縮小し、設定画面で警告。

タップで手動取得、JobSchedulerで30分間隔の取得（省電力時は遅延可）。重複要求はプロセス内AtomicBooleanで抑制。通信失敗・不正応答時は正常値を保持。再起動時は保存値を先に描画。長押しのサイズ変更・設定はランチャー依存。設定はアプリからも可能。表示設定は全インスタンス共通。

## ビルド

標準Gradleプロジェクトを含みます。依存ライブラリのない同じソースをAndroid SDKの `aapt2` / Java 17 / D8 / zipalign / apksignerで直接ビルドできます。

```bash
export ANDROID_JAR=/path/to/sdk/platforms/android-35/android.jar
export ANDROID_BUILD_TOOLS=/path/to/sdk/build-tools/35.0.0
export SIGNING_STORE=/private/foolquest-widget.jks
# SIGNING_STORE_PASSWORDは秘密管理から環境変数へ設定。ログ・Gitへ入れない。
bash scripts/test.sh
bash scripts/build.sh
```

固定署名鍵はGitHubに保存しません。初回公開APKの証明書SHA-256：`18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1`。今後の更新は同じ鍵で署名しversionCodeを増やしてください。

## 検証区分

純Javaの数学・固定列・JST月境界テストと、Nodeの読み取り専用エクスポートテストを実行。Android native instrumentationは別テストAPKのみで、公開APKには入らない。エミュレーター・実機結果は `VERIFICATION.md` で事実として分離する。

本アプリのGitHub管理位置はTHE FOOL QUESTリポジトリの `android-widget/`。Web既存ファイルの変更はなく、新規読み取り専用エクスポートページのみを追加。

## 過去の変更：v0.1.1 表示修正

文字のすぐ下にゲージを置き、文字サイズと行間設定から行の高さを決定します。ホーム画面の空き高さに合わせて行を引き伸ばしません。Android 12以上ではランチャーから提供された実サイズごとのRemoteViewsを使用し、未対応ランチャーと旧Androidでは縦横のサイズを切り替えます。

前のAPKと同じ署名、versionCode 2のため、アンインストールせずに更新できます。表示キャッシュと設定キーは維持します。

## 過去の変更：v0.1.2 実ランチャーの表示修正

ホーム画面の文字・金額・達成率はAndroid標準TextViewを直接表示し、画像全体の縮小を廃止します。5つの固定欄（ステータス・現在額・スラッシュ・目標額・達成率）を3行すべて同じ割合で配分。設定プレビューも同じRemoteViewsを適用します。

ランチャーのOPTION_APPWIDGET_SIZESによるビットマップ候補切り替えは使用しません。画面幅と最小幅から安全な共通文字サイズを計算し、実際の欄幅は標準LinearLayoutが決めます。外周8dpで角による欠けを防止。初期5×2を維持し、最小リサイズ高さを65dpに変更して縦方向を縮められる範囲を増やします。ホームアプリが確保する配置セル数そのものはアプリから変更できません。

背景・細枠は標準View、ゲージだけ軽量ビットマップ。versionCode 3、同じ署名。データ連携・保存済み設定・定期更新の処理は変更なし。

## v0.1.3 リサイズ追従・時刻削除

背景カード・外枠を配置枠の高さへ追従させ、3行を等分の高さに配置します。ゲージは各行の文字の直下を維持します。文字サイズ設定を基準として枠の大きさに合わせて調整し、7桁の固定金額欄・小数2桁の達成率が収まる上限で止めます。縦方向だけを広げた場合、行間は広がりますが、横幅による上限に達した文字はそれ以上拡大しません。

更新日時などの時間表記を削除。正常取得・連携済みの場合は下部の補助行も非表示。更新中・通信失敗・未連携の状態のみ表示します。内部の取得日時はキャッシュ管理・入力検証のため保持します。

versionCode 4、同じ署名で上書き更新。収益API、連携の責任分担、保存設定・キャッシュ、定期更新処理は変更していません。
