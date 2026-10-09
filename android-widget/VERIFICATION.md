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
