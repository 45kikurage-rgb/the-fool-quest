# Aruno-Result 署名・更新台帳 CURRENT

更新日：2026-11-12（日本時間）。秘密の値は記載しない。ソース・試験・配布・実機を分ける。

| 項目 | 現行値 |
|---|---|
| 用途 | 本番更新用の既存next系列署名 |
| package | com.aruno.foolquest.widget.next |
| alias | foolquestwidget |
| app_id | foolquest-widget-next |
| key_id / revision | foolquest-widget-next-existing-01 / 1 |
| 証明書 SHA-256 | 8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730 |
| 管理者 | 45kikurage-rgb / 所有者 |
| 取得正本 | 非公開45kikurage-rgb/aruno-platform-ops の config/signing-github.json |
| 共通台帳 | 同repo config/signing-registry.json の foolquest-widget-next |
| 主保管先 | aruno-platform-ops、signing-store/foolquest-widget-next/foolquest-widget-next-existing-01/r1/r2.escrow.json |
| 主固定commit / blob | 252816e474b9d1b0ab52bf7ea5b35229ce90279b / 2e51b1dd79a7ff98559f13f68c2cfa5099095640 |
| 主暗号文 SHA-256 | 51ef58676540e6380bbe7946a9d2c96e7b5374d7ad6adf8e61e019f54f179952 |
| 独立バックアップ | 非公開45kikurage-rgb/aruno-dpc-signing-backup、keys/foolquest-widget-next/foolquest-widget-next-existing-01/r1/github.escrow.json |
| backup固定commit / blob | 5f718b740bb1ea3d8eb1ecaed4a235b82d8a872c / e56a7409013bded712571cfc9165ad783ce753fe |
| backup暗号文 SHA-256 | 38bc420fac1555130c16b89b2ee2700252e7b9e1c3a29b5f8c54b5e71cad673d |
| 復号情報の管理 | opsの既存GitHub Actions Secret ARUNO_SIGNING_ROOTS_JSON と config/signing-recovery.json。値は非公開・暗号文と別管理 |
| アクセス方法 | GitHub接続で固定repo/commit/path/blobを確認。権限のあるopsの既存署名runnerだけで復元し、別runnerの両経路検証後に署名 |
| 署名経路 | ops .github/workflows/aruno-result-code40-existing-signature.yml、projects/github-signing/trial_candidate.py |
| 最終独立復元試験 | 2026-11-12（日本時間）。run43067714301、primary job130687001586 / backup job130687002261、別runnerで両成功 |
| 鍵の置換・新規作成 | なし |
| 実機確認 | Pixel 10未接続。未確認 |

主/backupは独立repo・別runnerで検証するが、GitHubサービス全体から独立した災害復旧を新たに証明したものではない。外部保管コピーや復号情報の外部UIからの再取得は今回未実施。

最新のAPK・ソースcommit・versionCode・APK SHA-256・証明書・試験run・配布URL・公開後照合は [code40更新記録](releases/aruno-result-code40-release-evidence.json) と [配布manifest](releases/manifest.json) を参照。これらの状態を最優先し、候補作成を公開済みと扱わない。

旧package com.aruno.foolquest.widget の鍵・証明書は別系列。流用・統合・新鍵への置換・アンインストールによる更新回避はしない。新しいチャットも、統括CURRENTとこの台帳・最新のrelease証拠を読み、GitHub接続とCI操作権限を確認して引き継ぐ。Library/利用者PCを通常の鍵取得元として必須にしない。

## code40 カレンダー上段・日別合計更新（2026-11-12 日本時間）

- 0.1.39 / versionCode40 / 3,190,018 bytes。source `1cbd2592f4ff801aec75900a3dcbe362b12883a5`。
- 上段の上下中央、年月＋全角スペース＋合計/Coupon/TikTokLite、7桁円右詰め。日別の3間隔を各1px縮め、合計数字を拡大。既存フォント・データ保存形式・署名を保持。
- APK SHA-256 `03b40d20c6fc51a4d094fd4ba2a5027269cddaff14c1294723dcc195b89341c5`。証明書は上表と一致。主/backup別runner復元と署名run43067714301成功。
- Android17 run43067036225 / job130684650442：通常23187、offline2767、reboot2758、backup37＋再起動4成功。試験APKと正式署名APKの署名以外42項目が全バイト一致。
- code39→40上書きrun43067714301 / job130687687514：39項目・UID・更新前バックアップ・再起動後保持・旧package維持に合格。
- 本番公開後の両origin照合は未完了。Pixel10実機・正式運用開始は未確認。code32消失原因は引き続き未特定。
- 検証済み署名APKを再利用し、再ビルド・再署名なし。旧APKを保持。会話への巨大Base64出力を避ける。

## code39 フォント統一・上段表示更新（2026-11-12 日本時間）

- 0.1.38 / versionCode39 / 3,190,018 bytes。source `52953b9ba2cd235921cafbc35a47d1865310e24d`。
- コーポレート・ロゴ（ラウンド）Boldの日本語を含む完全フォントをアプリ・ウィジェットへ適用。年月・上段合計の文字サイズ・基準線・横比率を統一し、既存高さを維持。
- APK SHA-256 `d3345cfcfb9888910f77f54a33151820b81d6ff387055006359e45432ceca200`。証明書・packageは上表と同一。主/backup別runner復元と署名run42988482090のstageまで成功。
- Android17 run42987581407 / job130412347118：通常11067、offline2767、reboot2758、backup37、backup再起動4成功。試験済みAPKと正式署名APKの署名以外42項目が全バイト一致。
- 正式code38→39上書きrun42996233873 / job130440914963：39項目・UID・更新前スナップショット・再起動後保持・旧package維持を確認。以前のADB切断を試験側の再接続待ちで解消。APKは再生成していない。
- 本番公開済み。Cloudflare・GitHub Pages両配布先のページ・manifest・APKを再取得し、code39・3,190,018 bytes・全SHA-256一致、前code38 APKの維持を確認。公開merge `aa7c4c927ac39b09a2970c878125d01bd0be5ce1`、公開照合run43062389949 / job130668136235成功。照合 2026-11-11T16:52:29.531302+00:00。Pixel10実機・正式運用は未確認。
- 追加Android15上書き試験：run43062999420 / job130670304704成功。旧試験run43062324188のAPK導入前ADB準備失敗を、試験側の90秒上限の再接続待ちで解消。同じcode39 APKを再利用。アプリ変更なし。
- 引き継ぎ時はGit clone/checkoutまたはartifactファイル転送でAPKを取得し、ハッシュと短い結果だけを会話に返す。Base64全文を会話へ出力しない。既存の署名済みバイトを再利用。

## code38 日別合計・アイコン・CSV導線（2026-11-11 日本時間）

- 0.1.37 / versionCode38 / 335,106 bytes。source `9c32a5181bb3a752b2717f0b362df9700aac72b7`。
- 日別合計の数字を大きく太くし、「万」は数字の43%のサイズ。負の合計は赤。C・Tの色と位置を維持。
- 添付の透過1024×1024 pxアイコンを既存の白背景レイヤーで表示。ポイント履歴をChromeで開くボタンを「CSVを取り込む」の直上へ移動。
- APK SHA-256 `bbedbf177e7e3aea68f9ddfce19f0ea12c5a4754c16d0f5e7221a5eaa088d50b`、証明書 SHA-256 `8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730`。同一署名・同一packageを維持。
- Android 17の表示・タップ／オフライン／再起動／バックアップは run42909212586。試験済み未署名APKと署名済みAPKの42ペイロード項目が全バイト一致。
- 正式署名・code37→38上書きは run42908246014。既存UID・設定と収益等39項目、更新前バックアップ、再起動後の保持と管理画面のボタン順を確認。
- 本番公開済み。両配布先のページ・APK・manifest・1024pxアイコンを再取得し、版・全SHA-256・335106 bytesを照合。公開照合run42910559334 / job130159788718成功。公開merge `d3a6b1cedc28731fb65c464ee17002427be5307b`。照合 2026-11-10T22:02:04Z。Pixel 10実機・正式運用は未確認。

## code37 カレンダー・月末予想更新（2026-11-10 日本時間）

- 実装・ソフトウェア試験・既存署名・正式上書き検査・本番公開済み。両配布originのページとAPKを再取得し、版・SHA-256一致を確認。公開照合run42709893084/job129476339195、2026-11-09T22:13:58.5438543Z。公開merge d4a39e5c9c10867d61573cf39c24875251f2a35e、Pages run42709891821成功。Pixel 10実機・正式運用は未確認。
- 0.1.36 / versionCode37 / 331010 bytes。source `e93a236bf77c69689d9c5156fc1f17ba49f5df69`、試験用CI変更のみを加えた検証commit `29c5dc5ec6e6f92cb83ff464604e992f91ce1851`。
- APK SHA-256 `4140c2d7cd5dc02dfb786edec32fcb2eff5d47e508378c68a3c270cc16e58124`。証明書・package・鍵の保管先と復号情報に変更なし。主/独立バックアップの復元、既存鍵の署名run42708371365成功。
- 日別金額は万・小数点第1位（99.9万を基準に固定文字サイズ、超過値の打ち切りなし）、日付左詰め、年月と上段合計は日別合計より大きい文字。
- 最下段右から3つ目の月末予想：月累計÷日本時間の経過日数×月日数。合計/C/T別に計算、整数の万で切捨て。枠タップで見出し以外を表示/非表示。初期表示、ウィジェット別に保持・バックアップ対象。正確な日別円額はタップで確認。
- API37のみ：通常8463、offline2759、reboot2750、backup37、backup再起動4成功。run42708319758 / job129470911332。正式APKと試験済み未署名APKの署名以外42エントリーが全バイト一致。
- 正式36→37上書き：run42708371365 / job129471725123。39項目、UID、再起動後の完全保持、更新前安全スナップショット、旧package維持が成功。
- 以前の失敗試行と修正理由はcode37 release JSONへ記録。失敗を成功へ読み替えない。code32データ消失原因は引き続き未特定。
- 配布先：`https://the-fool-quest.45kikurage.workers.dev/widget-download.html`（代替GitHub Pages）。前code36 APKは保持。

## code36 白背景アイコン更新記録（2026-11-08）

実装済み・正式署名済み・ソフトウェア上書き保持合格。公開先のHTML・manifest・APKを両originから取得し、HTTP200・版・326812 bytes・SHA-256の一致、前code35 APKの維持を確認。公開照合run42473255459/job128708763875成功。公開merge2653fdba5ae703637178ee651b6fc544bf4e7d78、Pages run42473255004成功。照合2026-11-08T10:55:33.615104+00:00。物理Pixel10は未接続・未確認、正式運用開始も未確認。

- package com.aruno.foolquest.widget.next / 0.1.35 / versionCode36 / 326812 bytes。
- 実APKソース a3cac172991fa804ea1455dbca05acd8a88d2b8e。後続変更は検証・資料・配布資材。既存PNG・アプリ本体Java・保存形式は変更なし。白#FFFFFFの下地のみ追加。
- APK SHA-256: 9c0829758631d7b1a129d8a0883fd9ce6dcd3dfa5f6a1a024060f8c5caeefee4。証明書SHA-256: 8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730。従来と一致。
- 既存鍵の主/独立backup復元・署名run42470171563、primary128699257991 / backup128699257863 / sign128699307692、全成功。保管先・権限・復号情報は変更なし。
- Android15通常8413/オフライン2759/再起動2749成功（run42469943501 job128698128846）。その試験APKの署名以外41エントリー全件と正式署名候補のバイト一致確認。
- 正式同一署名35→36上書き：API35 run42471512339、API37 run42472448522、39項目・UID・再起動後保持・startup safety snapshot・旧保存領域維持に合格。
- Android17のbackup/復旧37項目と再起動後4項目（既存SAF許可維持・再起動後新規保存）run42471512355 job128703132842成功。
- Androidネイティブ描画：48/96/512/1024pxすべて元PNGと白下地の合成画素に完全一致。icon/roundIcon同一、黒文字・枠・配置は維持。
- 配布URL https://the-fool-quest.45kikurage.workers.dev/android-widget/releases/aruno-result-0.1.35.apk
- 代替URL https://45kikurage-rgb.github.io/the-fool-quest/android-widget/releases/aruno-result-0.1.35.apk
- 実機HOME/フォルダ/アプリ一覧と実データの上書きは未確認。code32消失原因は未特定。端末初期化・アンインストール・保存内容削除は実施しない。

以前の候補停止は上書き試験未完了が理由。上記の同一署名・データ保持・復旧ゲートが全合格し、既存の2配布先で公開・再取得照合済み。失敗試行のActions履歴は保持し、成功と置換しない。

## code35 公開後照合（2026-11-08）

0.1.34 / code35、326,750 bytes。source `e8707316316200ecc8c779cca970d2817683ab0e`。APK SHA-256 `51e4755362a22bb729afc0d9f2dd07b0e08a4259f11f0e7e6260e4eefa4417ff`。既存証明書と一致。署名run42459999363、Android17 run42461997333、正式上書きrun42460331078、両公開先照合run42462642329、GitHub Pages公開run42462642110。

通常サイトとGitHub PagesのHTML/manifest/code35 APKを再取得し、HTTP200・全SHA-256・サイズ・APK MIME・code35を確認。前code34の公開バイトも維持。Pixel10実機・正式運用開始は未確認。

- 配布ページ：https://the-fool-quest.45kikurage.workers.dev/widget-download.html
- APK：https://the-fool-quest.45kikurage.workers.dev/android-widget/releases/aruno-result-0.1.34.apk
- 代替：https://45kikurage-rgb.github.io/the-fool-quest/widget-download.html

公開結果は配布manifest・更新証跡・本台帳に同じ版番号とハッシュで記録しています。検証結果の公開反映時にもAPKは再生成せず、上記の署名済みバイトを継続配布します。
