# Aruno-Result 署名・更新台帳 CURRENT

更新日：2026-11-10（日本時間）。秘密の値は記載しない。ソース・試験・配布・実機を分ける。

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
| 署名経路 | ops .github/workflows/aruno-result-code37-existing-signature.yml、projects/github-signing/trial_candidate.py |
| 最終独立復元試験 | 2026-11-10（日本時間）。run42708371365、primary job129471088825 / backup job129471088718、別runnerで両成功 |
| 鍵の置換・新規作成 | なし |
| 実機確認 | Pixel 10未接続。未確認 |

主/backupは独立repo・別runnerで検証するが、GitHubサービス全体から独立した災害復旧を新たに証明したものではない。外部保管コピーや復号情報の外部UIからの再取得は今回未実施。

最新のAPK・ソースcommit・versionCode・APK SHA-256・証明書・試験run・配布URL・公開後照合は [code37更新記録](releases/aruno-result-code37-release-evidence.json) と [配布manifest](releases/manifest.json) を参照。これらの状態を最優先し、候補作成を公開済みと扱わない。

旧package com.aruno.foolquest.widget の鍵・証明書は別系列。流用・統合・新鍵への置換・アンインストールによる更新回避はしない。新しいチャットも、統括CURRENTとこの台帳・最新のrelease証拠を読み、GitHub接続とCI操作権限を確認して引き継ぐ。Library/利用者PCを通常の鍵取得元として必須にしない。

## code37 カレンダー・月末予想更新（2026-11-10 日本時間）

- 実装・ソフトウェア試験・既存署名・正式上書き検査済み。本番公開後の再取得照合は未完了。Pixel 10実機・正式運用は未確認。
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
