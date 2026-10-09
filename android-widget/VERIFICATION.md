# v0.1.5 ゲージ内の今日までの目標額 — 2026-10-09 JST

- 各ゲージの右端へ、今日までの目標額を白文字＋黒い影で表示。ネイティブTextViewを使用し、3行で位置・文字サイズを統一。
- 月間目標 × 日本時間の当月日数経過 / 当月総日数。今日を含み、1円未満を切り上げ。10月9日で月間目標1,200,000円なら348,388円、600,000円なら174,194円。残額ではなく目標額そのもの。
- 月間目標が不明、または保存データが現在の日本時間の月と一致しない場合は「—」。既存の収益・達成率計算、データ正本、API、DBは変更しない。
- 30分更新・タップ・サイズ変更・設定保存・再起動で描画するときに再計算。0時ぴったりの再描画は保証せず、常駐処理・新規アラームは追加しない。
- ゲージはサイズ追従を維持し、文字の高さを確保。各行の45%を上限にゲージ・間隔・文字を安全に調整。新規配置の最小リサイズ高さ100dp、設定プレビュー126dp。既存ウィジェットの小さい枠も安全に縮小。外枠・時刻は引き続き表示しない。
- APK v0.1.5 / code 6 / 51,081 bytes。SHA-256: 4c21d43bf79ae5074767e21adfeb1c1c71f8db3dea4fa536a9c6120bea193b07。
- v2/v3署名を検証。既存証明書SHA-256: 18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1。上書き更新で保存設定とデータを維持。
- Javaの計算・配置190件と読み取り連携7件に合格。JST月境界、月末、2月・うるう年、端数切り上げ、不明値、古い月、最大安全整数を検査。
- Android 35のネイティブ表示・実API・タップ・リサイズ1855件、通信断5件、再起動2件に合格。280〜400dp幅、100/126/196/300dp高、160/320dpiと1080×1920・density 480の実AppWidgetHostで確認。
- ゲージ内の目標値・右寄せ・白文字・黒い影・文字の実描画範囲・文字切れ・3行同一サイズを検査。7桁金額・100%以上、黒白背景、極端な文字と余白、サイズに応じたゲージ高さも確認。検証画像の数値はテスト用。
- 実行: https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37883470610
- 検証ソース: 024421e0cc3e47815a9c1025443ba0a824d6b792。CIは一時署名の同一ソース。配布APKは秘密の固定署名で別途ビルド。秘密鍵はGitHubへ保存していない。
- v0.1.5更新後のユーザー実機・Novaでの操作は未確認。実機の画面OFF時の周期も未確認。ビルド・Android検証と実機受入を区別する。
- 配布APK・ページ・検証画像のみ本番公開対象。既存サイト本体、API・DB、キャッシュ、認証・権限、更新処理は変更なし。
- TikTok・目標の完全自動同期は引き続き未対応。現行サイトのブラウザ保存値から再連携が必要。

---

# v0.1.4 ゲージ高さ追従・枠線削除 — 2026-10-09 JST

- ユーザー提供のv0.1.3 Nova実機画像で、更新日時の非表示・カードと3行の高さ追従・実データ表示を確認。
- ゲージ太さをカードの利用可能な高さに比例させる。126dp高・標準上下余白で保存済みの太さを基準にし、文字の横幅による拡大上限とは独立して追従。
- 各行の20%を上限とし、文字との重なりを防止。小さいカードでは安全に縮小。設定太さは全3行共通の基準値として維持。
- 固定dp候補のゲージから、160dpiを基準にしたビットマップ内在高さを利用する標準ImageViewへ変更。余分な依存ライブラリや常駐処理は追加しない。
- ホーム・アプリ内プレビュー・ホーム選択用プレビューの白い枠線を削除。Novaの長押し中の編集枠はランチャーの標準機能。
- APK v0.1.4 / code 5 / 46,822 bytes。v2/v3署名と既存証明書の一致を確認。保存設定とデータは引き継いで上書き更新可能。
- Android 35 / 1080×1920 / density 480の実AppWidgetHost、280〜400dp幅、100/126/196/300dp高、160/320dpiで確認。実際のゲージ高さ・サイズごとの増減・3行の共通太さ・文字との重なり・枠線の不在を検査。白黒反転・大きな文字と余白・ゲージ設定でも安全な表示を確認。
- 1513件のネイティブ表示・実API・タップ・サイズ変更、通信断5件、再起動2件に合格。179件のJava計算・配置、7件の読み取り連携テストも合格。
- 最終実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37875265206
- 最終検証ソース：4857d0b175e39ed644a0b9f456133d97b2526afe
- CIは一時署名の同一ソース。配布APKは秘密の固定署名で別途ビルド。秘密鍵はGitHubへ保存していない。
- v0.1.4をユーザーの実機へ更新した後のゲージ追従と枠線削除は未確認。実機の画面OFF時の周期は未確認。
- 配布APK・ページ・検証画像を本番公開。既存サイト本体、収益API・DB、設定とキャッシュ、連携と更新処理は変更なし。
- TikTok・目標の完全自動同期は未対応。現行サイトのブラウザ保存値から再連携が必要。

---

# v0.1.3 時刻削除・リサイズ追従 — 2026-10-09 JST

- ユーザー提供のv0.1.2実機画像で、実データ3項目の表示は確認。カードが配置枠の高さへ追従しない点と時刻表示を今回修正。
- 背景と細枠を配置枠いっぱいに表示。3行は高さを等分し、文字とゲージは各行の中で直近に維持。
- 文字サイズ設定を基準に安全な拡大縮小。7桁の固定金額欄2個・百分率欄を常に予約し、金額の桁数で列を動かさない。横幅の上限に達した文字は、縦だけを広げてもそれ以上拡大しない。
- 更新日時を非表示。正常時は補助行を非表示。更新中・通信失敗・未連携だけ状態表示。内部タイムスタンプはデータ検証と正常キャッシュ保持に維持。
- 旧版のホーム配置も新しい静的レイアウトへ切り替わるよう、RemoteViewsのレイアウト識別子を分離。
- APK v0.1.3 / code 4 / 46,667 bytes。署名v2/v3と既存証明書の一致を確認。
- Android 35 / 1080×1920 / density 480で実AppWidgetHostを確認。280〜400dp幅、100/196/300dp高、160/320dpiの実TextViewの幅・共通列・文字描画・背景高さ・等分の行・日時非表示を検査。白黒反転と余白・文字サイズ・ゲージの大きい設定も確認。
- 1301件のネイティブ表示・実API・タップ・サイズ変更、通信断5件、再起動2件に合格。179件のJava計算・配置、7件の読み取り連携テストも合格。
- 最終実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37872284107
- 最終検証ソース：8e881cff87187b28322ef66cd4d340c1dc8eec68
- CIは一時署名の同一ソース。配布APKは秘密の固定署名で別途ビルド。GitHubへ秘密鍵は保存しない。
- v0.1.3をユーザーのNova実機へ上書き更新した後の表示と長押し・サイズ変更操作は未確認。実機の画面OFF時の周期は未確認。
- 本番に反映するのはAPK・配布ページ・検証画像のみ。既存サイト本体、収益API・DB、連携・設定とキャッシュ・バックグラウンド更新の処理は変更なし。
- TikTok・目標の完全自動同期は引き続き未対応。既存サイトのブラウザ保存値から再連携が必要。

---

# v0.1.2 実ランチャー向け表示修正 — 2026-10-09 JST

- 文字・金額・達成率を標準TextViewへ変更。画像全体を縮小する経路とサイズ候補マップを廃止。
- 固定5欄（ラベル16%、現在額29%、スラッシュ3%、目標額29%、達成率23%）。各金額は7桁分を確保し、少額でも右寄せ、余り幅は空白のまま。
- 設定プレビューもホームと同じRemoteViews。外周8dpでランチャーの角による欠けを防止。
- 初期5×2は維持。最小リサイズ高さ65dp。配置セル数そのものはホームアプリが管理するため、必要なら手動で縦方向を縮める。
- APK v0.1.2 / code 3 / 46,590 bytes、元の署名証明書を維持。
- Android 35、1080×1920 / density 480の実AppWidgetHostで表示・タップ・リサイズを検証。160/320dpiの表示コンテキスト、280〜400dp幅、100/196dp高で各文字幅・共通列・実際の文字描画を検査。
- 表示・実API・ネイティブ文字の842チェック、通信断5チェック、再起動2チェックが合格。
- 最終実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37870012031
- 最終検証ソース：1b81cbce5adb395eaa0df437faa6374252a90886
- CIは一時署名の同一ソース。配布APKは固定秘密署名で別途ビルドしv2/v3署名を確認。
- 中間の高密度実行では通信を止めるADB命令が一時的な接続断で失敗。最終実行は接続待ちとネットワーク断・前回更新終了の前提確認を追加して合格。
- 添付実機画像でv0.1.1の表示縮小・角での欠けを確認。v0.1.2をNova実機に更新した後の受入は未確認。
- 収益API、保存済み表示設定とキャッシュ、更新処理、THE FOOL QUEST本体は変更なし。

---

# v0.1.1 表示修正 — 2026-10-09 JST

- 現在額・「／」・目標額を独立した固定幅へ変更。各金額に7桁、カンマ2個、円記号の幅を確保。小さい金額は右寄せし、左側の余った桁幅は空白のまま保持。
- 文字直下にゲージ。ホーム画面の余剰高さへ行を引き伸ばさない。画像の縦方向の引き伸ばしを廃止。
- Android 12以降はランチャー提供の実サイズ別RemoteViews。未提供時は縦横別RemoteViews。
- 同じ署名、versionCode 2。保存済みデータ・設定のキーは変更なし。
- 179件のJava配置・計算、7件の読み取り連携、既存サイト38件のテストに合格。APKビルド・v2/v3署名確認成功。
- v0.1.0についてはユーザー提供の実機スクリーンショットでホーム配置と3項目の受け取り後表示を確認。v0.1.1の実機更新・実ランチャー操作は未確認。

Android 35ネイティブ検証：表示・桁位置・実API・タップ・サイズ変更91件、通信断3件、再起動2件に合格。
検証実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37865564298
検証ソース：9fb4ed831b65388fc9c7247b446707536565371e
CIは一時的なテスト署名で同一アプリソースをビルド。配布APKは固定秘密署名で別途ビルド・署名検証済み。
描画画像はAndroid Canvasと実AppWidgetHostViewから取得し、目視で固定スラッシュ、3行、7桁、100%以上、文字直下のゲージを確認。

---

# v0.1.0 verification — 2026-10-09 JST

Released APK: versionCode 1 / Android 26+ / targetSdk 35 / 33,483 bytes.

SHA-256: `4922e2948d343f16550bb73c1c35767b9f9ad7420cb1595ef5e0494d9d54791b`

Certificate SHA-256: `18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1`

## Passed

- 129 Java math, fixed-column, large-value and JST boundary checks.
- 7 Node source-browser snapshot tests, including read-only behavior and missing-data rejection.
- 38 existing THE FOOL QUEST regression tests.
- [Android 35 run 37859978752](https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37859978752): 77 native checks, 3 offline checks and 2 reboot checks. Actual Android Canvas, RemoteViews in AppWidgetHostView, production Coupon Reader, tap refresh, resize, settings and last-good cache. Retrieved Coupon at test time: ¥322,990; this is a time-specific result, not a fixed product value.
- Render PNGs visually inspected for seven-digit amounts, percentages above 100%, white/black mode and adjusted spacing.
- Shipped APK v2/v3 signature, SDK metadata and exact public-download hash verified.
- Public installation page and both export-page origins returned HTTP 200 and matched prepared source.

CI installs an equivalent product-source build signed with an ephemeral CI test key. The private release key is not sent to GitHub. Released APK signature/hash checks are separate evidence.

## Public installation

https://the-fool-quest.45kikurage.workers.dev/widget-download.html

Existing web application source (`app.js`, `index.html`, `style.css`, `sw.js`) and business APIs/DBs were not modified. Only the isolated Android project and new read-only/export/download assets were added.

## Still unverified or incomplete

- Physical-device installation and placement in the user's real launcher, including Nova long-press menus and actual 5×2 dimensions.
- Browser-to-app transfer through the confirmation UI on the user's physical device.
- Real-device battery management while the screen is off. Periodic work is not an exact timer.
- Fully automatic TikTok and goal synchronization. The existing source stores them in browser localStorage and exposes no Reader API. This release imports a user-confirmed display snapshot; changes require another transfer. Coupon alone supports automatic/API tap refresh. Missing TikTok remains unknown, not zero.

The final physical-device acceptance condition is all three real values visible on the home screen after source-browser transfer. Build, emulator checks, deployment and physical acceptance are separate states.
