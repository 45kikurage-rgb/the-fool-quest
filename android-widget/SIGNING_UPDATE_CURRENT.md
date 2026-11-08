# code36 白背景アイコン候補：未配布（2026-11-08）

- 変更：元の透過PNGを変更せず、白い下地を明示するLayerDrawableを追加。icon/roundIconを同じXMLへ統一。文字・枠・サイズ・配置、アプリ本体Java、保存形式、packageと署名は維持。
- 候補：0.1.35 / versionCode36、326812 bytes、package com.aruno.foolquest.widget.next。
- 実APKのアプリソース：`a3cac172991fa804ea1455dbca05acd8a88d2b8e`。試験期待値のみの後続修正：`5264f038a255787d0b741ef390d728bebc2753d2`。
- APK SHA-256：`9c0829758631d7b1a129d8a0883fd9ce6dcd3dfa5f6a1a024060f8c5caeefee4`。証明書は従来の8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730。
- 最終独立復元：run42470171563、primary128699257991 / backup128699257863、各別runner成功。署名128699307692成功。新しい鍵・復号情報の作成・移設はなし。
- 同一署名候補を非公開opsの `staging/icon-white-result-42470171563`、`releases/icon-white-20261108/result/candidate.apk.b64` と `trial-result.json` に保管。これは公開配布ではない。
- Android15通常8413・オフライン2759・再起動2749項目はrun42469943501のnative job128698128846で成功。Android17 backup job128698129011は通常37項目成功後、再起動後の保存先一覧取得で失敗。原因は未確定で、白背景変更が原因とは断定しない。
- 正式同一署名35→36上書き、39項目の履歴/手入力/損失/表示/日別接続/ウィジェット設定保持と、元画像を維持した白背景描画はrun42470589105で検証中。APKの再build・再署名はしない。
- **配布停止。** 公開ページと配布manifestはcode35を維持。Pixel10物理端末は未接続で、実機上書き保持・HOME/フォルダ/アプリ一覧は未確認。利用者のデータ保持ゲートを免除しない。

以下のcode35公開記録と署名保管先は履歴を含めて維持する。

---

# Aruno-Result 署名・更新台帳 CURRENT

更新日：2026-11-08。秘密の値は記載しない。ソース・試験・配布・実機を分ける。

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
| 署名経路 | ops .github/workflows/aruno-result-code35-existing-signature.yml、projects/github-signing/trial_candidate.py |
| 最終独立復元試験 | 2026-11-08。run42459999363、primary job128667046500 / backup job128667046511、両成功。今回の最終候補の証跡は下記release JSONを優先 |
| 鍵の置換・新規作成 | なし |
| 実機確認 | Pixel 10未接続。未確認 |

主/backupは独立repo・別runnerで検証するが、GitHubサービス全体から独立した災害復旧を新たに証明したものではない。外部保管コピーや復号情報の外部UIからの再取得は今回未実施。

最新のAPK・ソースcommit・versionCode・APK SHA-256・証明書・試験run・配布URL・公開後照合は [code35更新記録](releases/aruno-result-code35-release-evidence.json) と [配布manifest](releases/manifest.json) を参照。これらの状態を最優先し、候補作成を公開済みと扱わない。

旧package com.aruno.foolquest.widget の鍵・証明書は別系列。流用・統合・新鍵への置換・アンインストールによる更新回避はしない。新しいチャットも、統括CURRENTとこの台帳・最新のrelease証拠を読み、GitHub接続とCI操作権限を確認して引き継ぐ。Library/利用者PCを通常の鍵取得元として必須にしない。

## code35 公開後照合（2026-11-08）

0.1.34 / code35、326,750 bytes。source `e8707316316200ecc8c779cca970d2817683ab0e`。APK SHA-256 `51e4755362a22bb729afc0d9f2dd07b0e08a4259f11f0e7e6260e4eefa4417ff`。既存証明書と一致。署名run42459999363、Android17 run42461997333、正式上書きrun42460331078、両公開先照合run42462642329、GitHub Pages公開run42462642110。

通常サイトとGitHub PagesのHTML/manifest/code35 APKを再取得し、HTTP200・全SHA-256・サイズ・APK MIME・code35を確認。前code34の公開バイトも維持。Pixel10実機・正式運用開始は未確認。

- 配布ページ：https://the-fool-quest.45kikurage.workers.dev/widget-download.html
- APK：https://the-fool-quest.45kikurage.workers.dev/android-widget/releases/aruno-result-0.1.34.apk
- 代替：https://45kikurage-rgb.github.io/the-fool-quest/widget-download.html

公開結果は配布manifest・更新証跡・本台帳に同じ版番号とハッシュで記録しています。検証結果の公開反映時にもAPKは再生成せず、上記の署名済みバイトを継続配布します。
