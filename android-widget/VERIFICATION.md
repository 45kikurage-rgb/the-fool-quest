# v0.1.12：正式署名・上書き更新・公開検証 — 2026-10-10 JST

- 統括CURRENTはVersion 2026-10-09.1、更新日時2026-10-08T20:50:11.617998Zから変更なし。製品ソースは指定main `c86b1468ebfefe3ca8fda7d0069a89117f9c1563`をそのまま使用。既存機能の再実装・製品ソース変更なし。
- 前回と同じ非公開の固定JKSを既存ビルド環境で利用。秘密鍵・パスワードをGitHub・公開ログ・APKへ保存しない。新しい署名鍵は作成しない。
- 正式APK：`com.aruno.foolquest.widget` / v0.1.12 / code 13 / minSdk26 / targetSdk35 / 59,182 bytes。SHA-256 `8218ea59dc2cd4c1aac82f12bffd350f8c5736e54e506a931c1400c581c97ffc`。v2/v3署名合格、既存v0.1.11の証明書SHA-256 `18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1`と一致。
- 多周回ゲージ：前周の満タンを残し、現在周の余りを色交互で重ねる。188.59%=2周目88.59%、250%=3周目50%、300%=3周目満タン。金額と達成率は累計を維持。白い2dp境界線・右端の×2/×3表示を含む。掲載画像は実Androidの検証用値で、実収益ではない。保存済みの4色を初期色の変更で上書きしない。
- 指定mainのAndroid 35 CI実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38042433222 。Java計算/配置248件、HTTP28件、読み取り連携7件、ネイティブ2891件、通信断12件、再起動2件に合格。CIのテスト署名APKは配布しない。
- 実際の正式APK同士の上書き更新：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38043031048 。v0.1.11/code12へ保存済み4色・独自色・余白・文字・背景濃度・進歩カラー・目標額・TikTok/Couponキャッシュ・取得日時・通信履歴29項目を準備し、`adb install -r`でv0.1.12/code13へ更新。同一UID・29項目が完全一致。新版初回起動後も28項目を保持し、lastFailureAtのみ既存の通信断取得で更新。実UIの0.1.12・累計115.00%・1,150,000を確認。実際の設定保存後も4色と既存設定・収益キャッシュを保持。結果：`../widget-verification/v0.1.12/official-upgrade-result.txt`。
- RevenueStore、RevenueUpdate、RevenueJob、RevenueWidget、RefreshActivityは前回v0.1.11製品ソースからバイト一致。今回の公開変更はAPK・manifest・ダウンロードページ・検証記録のみ。旧版APKを削除・置換しない。
- 既存のmain公開手順でGitHub Pagesへ反映し、Cloudflare配布URLにも反映。公開後に両配布先からページ・manifest・正式APK・旧v0.1.11を再取得して照合する。
- ユーザーのNova実機でのv0.1.12受入は未確認。Android 35検証と実機受入を区別する。

---

# v0.1.11：正式APKの公開 — 2026-10-10 JST

- 保存済み正式署名APKを再ビルドせず公開。製品ソースは `63720b2e7b6f72954197e0513f78697499c77fb1`。4色設定・満タンの1周目に黄緑の2周目を重ねる表示を含む。
- v0.1.11 / code 12 / 55,086 bytes / SHA-256 `31174acf7025d3ea5f7907bc652e2b4b86e0b8ef2a839797f785d459debf9450`。APK v2/v3署名合格。既存v0.1.10と証明書SHA-256 `18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1`が一致。秘密鍵・パスワードは公開ファイルに含めない。
- 生成時の検証：Java計算・配置229件、HTTP28件、読み取り連携7件。最新版Android 35検証2162件、通信断12件、再起動2件に合格。実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38033384866 。115.00%・緑100%に黄緑15%の画像を配布ページへ掲載。
- 正式APK v0.1.10/code11からv0.1.11/code12への `adb install -r` 合格。同一UID・保存設定/収益キャッシュ/通信履歴25項目がインストール前後で一致。初回起動後も24項目を保持し、lastFailureAtのみ既存の通信断取得で更新。実UIの新版・累計115.00%を確認。実際の設定保存で4色の既定値を追加しても既存設定・キャッシュを保持。実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38034164167 。結果：`../widget-verification/v0.1.11/official-upgrade-result.txt`。
- 今回の変更は配布APK・manifest・ダウンロードページ・検証記録のみ。更新処理、収益連携、ウィジェット設定の製品ソースは変更しない。旧版APKを削除・置換しない。
- 既存のmain公開手順でGitHub Pagesへ反映し、Cloudflare配布URLでも反映を確認する。公開後に両URLからAPKを再取得してハッシュ・署名を照合する。
- ユーザーのNova実機でのv0.1.11受入は未確認。Android 35検証と実機受入を区別する。

---

# v0.1.10 正式署名・公開検証 — 2026-10-10 JST

- 統括CURRENTを確認：Version 2026-10-09.1、更新日時2026-10-08T20:50:11.617998Z。FOOL QUESTの表示責務・Vault→Ledger→FOOL QUESTの収益経路は変更なし。
- 基準main：59c0c5e4df3ff5246342f821eaa1c2095e963f62（PR #1統合済み）。Total/TikTok/Couponは月間目標超過時に濃い緑 #147A39で次の周回を表示。数値％は累計のまま、115.00%ならゲージ15%、100%は通常緑の満タン、200%は濃い緑の満タン。
- 前回v0.1.9の作業環境に保管されている非公開の固定JKS・パスワードファイルとAndroid 35 platform / aapt2・D8・zipalign・apksignerを再利用。鍵・パスワードはGitHubにも公開ログにも保存していない。新規鍵生成なし。
- PRのGradle設定はcode11だったが、手動APKビルドに使用するAndroidManifestがcode10 / 0.1.9のまま残っていたため、11 / 0.1.10へ統一。設定画面の版番号表示も0.1.10へ更新。機能・設定保存処理は変更なし。
- 正式APK：com.aruno.foolquest.widget / v0.1.10 / versionCode11 / minSdk26 / targetSdk35 / 55,086 bytes。
- APK SHA-256：d76fbdad7019b29fe90105e4665156c4a70a9b08bec6cb5053886da400fdd626。
- 署名証明書SHA-256：18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1。v2/v3署名検証合格。証明書DN：CN=THE FOOL QUEST Widget, O=ARUNO, C=JP。
- 公開済みv0.1.9のAPKを通常サイトから再取得し、55,086 bytes / SHA-256 b7854797b7567a0f8a8c26239eb44d57afe51044389ba26b31913e9a2a09f8ad、および証明書の一致を確認。既存v0.1.9のAPKは保持。
- ローカル：Javaの表示・数学・配置229件、HTTP28件、読み取り連携7件に合格。
- 基準mainのAndroid 35自動テスト：実表示・Reader・更新2078件、通信断12件、再起動2件に合格。実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38029899491 。115.00%と濃い緑15%ゲージの実Android描画画像を目視確認。
- 最終製品ソース（版番号修正後）のAndroid 35回帰検証も合格：表示・Reader・更新2078件、通信断12件、再起動2件。実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38030475667 。配布ページの2周目画像と検証ログはこの実行から取得。CIの一時署名APKは公開しない。
- 更新処理、RevenueStore、RevenueJob、RevenueWidget、RefreshActivity、既存app.js/index.html/style.css/sw.js/widget-export.jsはv0.1.9からバイト一致。設定の保存キーと保存処理は維持。
- 正式署名APK同士の上書き更新テスト合格：Android 35でv0.1.9/code10をインストールし、独自色・余白・フォント・背景濃度・進歩カラー設定・目標額・TikTok/Couponキャッシュ・取得日時・通信履歴25項目を準備。v0.1.10/code11をadb install -rで更新し、同一UID・25項目の完全保持を確認。新版初回起動後も設定・キャッシュ24項目を保持し、通信失敗日時のみ既存の通信断取得処理で更新。実UIの0.1.10・115.00%・1,150,000表示も確認。実行：https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38030681439 。
- v0.1.10のユーザー実機受入は未確認。Android 35での検証と、ユーザーのNova/Android実機での上書き更新・表示確認は別状態として扱う。

---

# v0.1.9：タップ更新フロートを隠す — 2026-10-10 JST

- ユーザー提供15764.mp4（5.775秒）をローカルで確認。ホームの3項目とウィジェット内更新中、および中央フロートを確認。版番号は動画に出ていないため特定しない。フロート非表示の要望を受け、表示だけ変更。
- RefreshActivityの文字・背景を削除し、透明Activityテーマ・起動プレビュー無効、Window alpha=0、1×1px、FLAG_NOT_TOUCHABLE・FLAG_NOT_FOCUSABLE、開始/終了アニメーションなし。直接取得を開始後、onResumeのmoveTaskToBack(true)で専用singleTaskをホームの背面へ移動。タスクは最近の一覧から除外し終了時削除。Android 12以降の別UIDタッチ透過条件もWindow alpha=0で満たす。
- 専用Activityからの直接取得、同時呼び出しの完了合流、待機中の単発ジョブ取消、再描画と自動終了は維持。連続タップはsingleTaskとアプリ単位の同時通信抑止を使用。OSによる背面Activityの終了/置換を許容し、通信中の正常キャッシュ保持・完了反映・次回タップ更新を検査。ウィジェット内の更新中表示を維持。設定保存・初期化は実行しない。OSによる背景プロセス停止に対する即時完了保証はしない。
- Java216件、HTTP28件、読み取り連携7件に合格。固定署名APKと別テストAPKのビルド成功。
- Android 35で1988件の実API・全体タップ・固定列・色・サイズ変更に合格。完全透明のWindow・非フォーカス・非タッチ・ホーム側のフォーカス・実ポインタ入力・ウィジェット実描画ピクセル・ウィジェット内の更新中を確認。通信中の追加タップ、背面Activityの終了/置換後の正常キャッシュ保持と完了反映、完了後の次のタップによる新しいAPI更新も確認。通信断12件、再起動2件も合格。検証画像を目視し中央フロートがないことを確認。
- 実行: https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38016785866
- 検証ソース: 9d9eae85fda35b5961c4692f12d1a18bcf6354c5。CIは一時署名、配布APKは同一ソースを秘密の固定署名で別途ビルド。
- v0.1.9 / code 10 / 55,086 bytes / SHA-256 b7854797b7567a0f8a8c26239eb44d57afe51044389ba26b31913e9a2a09f8ad。v2/v3署名合格、既存証明書SHA-256 18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1と一致。
- 11:30 JST以降、通常サイトのページとAPKをcurlでHTTP200取得し、ページのバイト一致・APK 55,086 bytes / SHA-256一致を確認。APKのContent-Typeはapplication/vnd.android.package-archive。GitHub Pagesの代替ページとAPKもHTTP200・完全一致。配布コミット34ee2bac7b898dbbf2d22a0becbff860641a148b、Pages実行38017068064は成功。最初のPython urllib照会は403だったが同環境のcurlは成功。クライアント差の原因は未確定で、認証・WAF・既存サイトを変更しない。実機のダウンロード受入は別途必要。
- 本番対象は配布APK・ページ・検証証跡。既存サイト本体、Vault/LedgerのAPI・DB・認証・正本・計算、保存設定・キャッシュ・30分ジョブ・文字列/進歩カラー/ゲージ寸法は変更なし。新規サービス・権限・WakeLockなし。
- 新版のユーザーNova実機での透明化・ホーム操作・画面OFF中の定期更新・4G/省電力条件は未確認。Android検証と実機受入を区別する。TikTok/目標の完全自動同期は未実装で、変更後は再連携が必要。

---

# v0.1.8：タップ更新を設定画面と同じ直接取得へ — 2026-10-10 JST

- ユーザー画像でv0.1.7の設定画面・正常取得・実データ3項目を確認し、設定保存後にホームへすぐ反映されるとの報告。設定Activityの作成時は直接取得、保存は再描画、ウィジェットタップは単発JobSchedulerという経路の差をソースで確認。遅延原因そのものを実機ログで確定したとは扱わない。
- 全体タップをimmutable PendingIntent.getActivityへ変更し、非exportedのRefreshActivityからSettingsActivityと同じRevenueUpdate.startを直接実行。「更新中」だけの小さな画面を表示し、成功・失敗とも既存の再描画後にfinishAndRemoveTask。別taskAffinity・singleTask・最近のアプリ一覧から除外。設定保存・設定初期化は実行しない。
- 同時呼び出しは一度の通信へ合流し、終了通知は実際の通信完了後に行う。従来のbusy時即時callbackによる完了誤認をなくす。直接タップ開始時は待機中の単発手動ジョブのみ取消。実行中の通信・30分ジョブは維持。
- 30分更新・初回配置は標準JobScheduler。新規サービス・WakeLock・権限なし。ネットワーク遅延や画面を離れた後のOSプロセス停止に対して即時完了を保証しない。失敗では正常値を保持する。
- ローカルで計算・配置216件、HTTP遅延・分類28件、読み取り連携7件に合格。署名APKと別テストAPKのビルド成功。
- Android 35の実API・全体タップ・完了通知合流・単発ジョブ待機の回避・画面の表示と自動終了・固定列・色・リサイズ1969件、通信断12件、再起動2件に合格。実AppWidgetHostViewのPendingIntentから起動し、更新画面のフォーカスとアクセシビリティ上の「更新中」表示も確認。初回の検証画像は描画前だったため、表示確認を追加して再実行し、新しい画像を目視確認。
- 実行: https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/38012735253
- 検証ソース: 68c3609093626a0dcf32696c06530cbda44d8a8e。CIは一時署名。配布APKは同一ソースを秘密の固定鍵で別途署名。
- v0.1.8 / versionCode 9 / 55,086 bytes / SHA-256 1d4344b1532baacb09ff95edddf6af4a2937926edb3f39c3648943cb42c1aba3。v2/v3署名合格、既存証明書SHA-256 18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1と一致。
- 既存サイト本体app.js/index.html/style.css/sw.js/widget-export.jsのバイト一致を確認。API・DB・認証・正本・計算方法は変更なし。配布APKにテストクラス・秘密署名鍵は含めない。
- 本番公開対象はAPK・配布ページ・検証証跡。設定・キャッシュの削除なし。TikTok/目標の完全自動同期は未対応、変更後の再連携が必要。
- v0.1.8更新後のユーザーNova・Android 17実機、4G回線、画面OFFの30分更新、省電力管理の受入は未確認。Android 35エミュレーター検証と実機受入は別。

---

# v0.1.7：一時的な通信遅延と失敗理由の確認 — 2026-10-09 JST

- ユーザー画像で実データ3項目・進歩カラー・枠線なし・ゲージ内目標額なしの表示を確認。画像の版番号は確認できない。タップ時に「通信失敗・前回値を表示」が出た後、再タップで復旧したとユーザーから報告。
- 本番の同じ公開Readerを読み取り確認。HTTP200、month=2026-10、coupon_revenueの正常整数、43bytes。開発環境の測定でTLS接続6.85秒・全体7.86秒。端末の通信経路や失敗理由は未取得のため、今回の原因自体は断定しない。旧版の接続・読み込み各2.5秒は一時的遅延に弱い設定であるため改善。
- 接続・読み込み各10秒、本文の時間検査10秒、32KB上限、リダイレクト拒否。HTTPS接続先・API契約・認証方式は変更なし。再試行を無制限に繰り返す処理は追加しない。
- 初回配置とタップ更新は単発JobScheduler要求へ変更。BroadcastReceiverから短時間で返し、長いネットワーク待ちは既存JobServiceで処理する。標準OS管理のため開始時刻は制御される。30分ジョブ・AtomicBooleanによる重複抑止は維持。常駐・独自WakeLock・新規権限なし。
- 未接続・時間切れ・DNS・TLS・HTTPステータス・不正応答を制御コードへ分類。現在の理由を状態行へ表示し、直近の失敗理由と日時は設定画面だけに表示。生の例外、応答本文、URL、秘密情報は保存しない。
- 成功すると現在の失敗を消し、直近失敗のコード・日時は端末内に保持。通信失敗では正常値・取得日時を変更しない。新しい収益DBやサーバー側ログは作成しない。
- 純Javaの計算・配置216件、HTTPタイミング・失敗分類28件、読み取り連携7件に合格。実際のループバックHTTPでヘッダー前3.2秒、本文前3.2秒を遅延させ、合計6.4秒超でも成功。HTTP301/401/403/429/500/503、32KB超応答、理由の機微情報非露出、有限タイムアウト・ストリーム終了を確認。
- Android 35の実API・単発ジョブでのタップ・正常値保持・失敗理由・復旧と履歴保持・描画1946件、通信断8件、再起動2件に合格。従来の文字・ゲージ・色とリサイズ回帰検証も維持。
- 実行: https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37901627641
- 検証ソース: c07b43d067b9ba4e03131c0907dbdbed1e73a29e。CIは一時署名、配布は同一ソースを秘密の固定鍵で別途署名。
- v0.1.7 / versionCode 8 / 50,990 bytes / SHA-256 df39660edf35c948182f7f5117d931cb1f5b6cfb287f7d81e2b0455cf51971f9。v2/v3署名を検証し、既存証明書SHA-256 18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1と一致。上書き更新を維持。
- 本番反映はAPK・配布ページ・検証証跡。既存サイト本体、Vault/LedgerのAPI・DB・収益計算は変更なし。TikTok・目標の完全自動同期は未対応のまま。
- v0.1.7を更新したユーザー実機の4G回線・省電力下の受入は未確認。今回の端末の一時失敗を修正版で再現したとは扱わない。

---

# v0.1.6：0.1.4表示への復帰・本体と同じ進歩カラー — 2026-10-09 JST

- ユーザー指示によりゲージ内の日割り目標額を削除。v0.1.4のNativeWidgetViewsの寸法計算と行レイアウトへ復帰。固定7桁の現在額／目標額、小数2桁の割合、枠線なし、時刻なし、ゲージの高さ追従を維持。最小リサイズ高さ65dp、基準126dpのゲージ4dp・行高さ20%上限へ戻す。
- 上書き更新を維持するため、配布版はv0.1.6 / versionCode 7。0.1.4へのversionCode降下やアンインストールは行わない。
- THE FOOL QUESTの実装 `app.js:renderMetric` と `style.css` を確認。本番app.jsとGitHubソースの一致を確認。今日までの目標比100%以上は緑 #31D158、50%以上100%未満は黄 #FFD43B、50%未満は赤 #FF4545。ゲージの長さと数値％は従来の月間目標比を維持。
- 今日を含む日本時間の日付／当月日数で内部計算し、整数の収益額を正確な100%・50%切り上げ境界で比較。MAX_YEN近傍でもオーバーフローしない。収益・目標が不明、保存データが古い月の場合は灰 #888888で未判定を表す。
- 既存設定にも進歩カラーは初期ON。OFF時には従来の固定色を使用し、保存済みの色を削除・初期化しない。設定プレビューは実描画と同じ126dp高。30分・タップ・リサイズ・保存・再起動などの再描画で色を再計算。新規常駐や0時専用アラームを追加しない。
- Javaの計算・配置216件、読み取り専用連携7件に合格。50%と100%直前／直後、当日から翌日のJST境界、月末・2月・うるう年・最大整数、不明・古い月を検査。
- Android 35で1941件のネイティブ表示・実API・タップ・サイズ変更、通信断5件、再起動2件に合格。280〜400dp幅、100/126/196/300dp高、160/320dpi、1080×1920・density 480の実AppWidgetHostで確認。
- 実際のゲージ画像ピクセルと緑／黄／赤の一致、埋め込み文字なし、0.1.4の4dp基準への復帰、サイズ変更時の増減、固定色OFFと色の保持、古い月の灰色を検査。7桁×2、100%以上、白黒反転、極端な余白・文字設定も検査。検証画像の金額はテスト用。
- 実行: https://github.com/45kikurage-rgb/the-fool-quest/actions/runs/37889416298
- 検証ソース: e4346e58d53607fdeb5d06252d1abb39991de6d1。CIは一時署名の同一ソース。配布APKは秘密の固定署名で別途ビルド。
- APK 50,990 bytes / SHA-256 e9ea3eae6e503710bdb476565dff679e42cf1e59fece1a8591c460e46ef963ff。v2/v3署名に合格。既存証明書SHA-256 18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1と一致。秘密鍵はGitHubへ保存しない。
- v0.1.6更新後のユーザー実機・Nova操作と、実機画面OFF時の定期周期は未確認。Android検証と実機受入は区別する。
- 配布APK・ページ・検証画像を本番公開。既存サイト本体、収益API・DB・認証、設定キーと収益キャッシュ、連携・更新処理は変更しない。追加した表示設定キーはpaceColorsのみ。
- TikTok・目標の完全自動同期は未対応。既存サイトからの再連携が必要。

---

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
