# Code43 checkpoint — month-end forecast title fix

Version 0.1.42 / code43; PR #63; public code42 unchanged until merge. User instructed on 2026-11-12 to shrink the forecast title by 3px and proceed through to production publication as code43.

## Origin
Pixel 10 physical check of code42 (2026-11-12) found one defect: in the calendar month-end forecast cell, the fourth glyph of the title 「月末予想」 (「想」) was entirely missing (not clipped). Other checklist items (in-place update retention, settings persistence, offline, restore) were not reported. The user first requested a −2px mockup, then instructed −3px for the fix itself.

## Source changes (branch fix/forecast-title-minus3)
- a1934b7 "Shrink month-end forecast title by 3": CalendarWidget.java gains a dedicated forecastTitleSize = max(1f, dateSize − 3f); both setTextViewTextSize and the setTextScaleX fit calculation use it. Title only; amounts, dates, XML unchanged.
- 104b760 "Bump version to 0.1.42 (code43)": build.gradle versionCode 43 / versionName 0.1.42.
- e93180d "Update forecast geometry check to code43 spec": WidgetInstrumentation forecast assertion now requires row0 (title) at daily size − 3 while amount rows 1–3 keep exact daily size and all rows keep shared row position/height.
- 11c661e "Bump AndroidManifest version to 0.1.42 (code43)": AndroidManifest.xml versionCode 43 / versionName 0.1.42. Required because the ops unsigned build links the manifest directly (aapt2), so the APK self-reported 42 / 0.1.41 when only build.gradle was bumped. Committed through the GitHub web UI after the connector approval expired twice; remote blob verified byte-identical to the prepared file (45577e704dd4b0ae34cd06813610285618fcad2d). This commit is the pinned signing source: 11c661eb845d049277629a48def2ba5736f0b1c6.

## Software gates
- CI run 43236326335 (#233, at 104b760) and the run at a1934b7 failed with instrumentation AssertionError "forecast shares daily glyph size row0" — the pre-fix invariant; resolved by the e93180d spec update above. Not an app defect.
- Native gate run 43239515527 (#234, at e93180d): SUCCESS (job 6m52s; summary page display lagged, job page confirmed).
- Native gate run 43277843525 (#235, at pinned 11c661e): SUCCESS, job 131390923817. Normal 25302, offline 2767, reboot 2759, backup 46 + reboot 4 — all PASS. Verification screenshot calendar-forecast-0.png shows all four title glyphs 「月末予想」 rendered.

## Signing (aruno-platform-ops)
Route derived from the code42 originals with only the intended substitutions; ops files committed through the GitHub web UI because the connector integration cannot write to aruno-platform-ops (403 on tree creation). Order enforced: workflow (5cc7f28), projects (6e11cda), request last (7d605f0, trigger). Remote blob SHAs verified identical to the prepared files.
- Run 1 (43264554238): unsigned job failed at the final badging assertion — APK built from e93180d still self-reported 42 / 0.1.41 (manifest not yet bumped). Restore jobs succeeded; stage/upgrade skipped. No signed output.
- Pin updated to 11c661e (workflow 161ba76, request 4274862; SHAs re-verified).
- Run 2 (43285649920): SUCCESS in 7m56s. unsigned 131418235152, restore primary 131418236318 / backup 131418235791, stage 131418654974, upgrade-android17 131419068604 — all success.
- Signed APK: 3198455 bytes, SHA-256 0a9f25626442a1dfdea2226766274e5013f25df7cbeec6e5f9c0a57672cf31b8, certificate SHA-256 8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730 (existing key), package com.aruno.foolquest.widget.next, versionCode 43 / versionName 0.1.42, minSdk 26 — all re-verified locally with apksigner/aapt2. Unsigned SHA-256 3e9a1c4f291f7ea60daec26ce8b5fa4bef2ef334a985297c2c18cfa03e9f29ff. Stage job asserted all 45 non-signature payload entries match the tested unsigned APK.
- Official upgrade (disposable Android 17 emulator): code42 → code43 via install −r, UID and all 42 fixture fields preserved, saved daily CSV / manual entries / per-widget calendar settings retained, legacy v0.1.12 untouched, reboot preferences preserved exactly. Result: PASS_DISPOSABLE_EMULATOR. Physical device NOT verified.

## Publication stage (code43)
Executor: Muse, 2026-11-13 JST, under the user's 2026-11-12 instruction to publish as code43. No rebuild, no re-signing, no new keys.
- Distribution registration on PR #63 branch: android-widget/releases/aruno-result-0.1.42.apk (verified signed bytes above, uploaded through the GitHub web UI); manifest.json updated to 0.1.42/code43 (previousRelease = code42, archives keep code41 and older, old APK bytes retained); aruno-result-code43-release-evidence.json created in the code42 format; widget-download.html updated (badges v0.1.42 / code 43, download link, verification text with run links 43277843525 / 43285649920, previous-version list, SHA-256); this checkpoint. publicationStatus is PENDING_PUBLICATION in manifest and evidence.
- Next: set PR #63 description to the final state and merge to main (production publication, record the merge commit); re-download page/manifest/APK from both origins and verify HTTP, size, SHA-256, certificate, versionCode and that the code42 APK is retained; then set PUBLISHED_VERIFIED with publicVerification in manifest and evidence, write release-tests/code43/public-verification.json, and append the final record here.
- Pixel 10 physical acceptance of the title fix remains UNCONFIRMED; emulator evidence is not physical-device verification.
