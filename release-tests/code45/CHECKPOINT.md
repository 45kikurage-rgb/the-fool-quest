# Code45 checkpoint — daily total underline + unknown amounts display as 0.0万

Version 0.1.44 / code45. User instructed on 2026-11-14 to implement the
user-selected design "mock 4" together with the zero-display rule below,
through production publication.

## Origin (user decisions, 2026-11-13/14)
1. Daily totals should be easier to read. Four mockups were compared with the
   shipped font and the user's real November data, all fitted to the same
   width budget as the current build. The user selected mock 4: large digits
   with a small 万 (plan 1) plus a thin underline under the daily total,
   without dimming zero detail rows.
2. Days with no revenue data currently render blank/dash/未取得. The user
   decided: from 00:00 JST a today cell starts at 0.0万 for total, coupon (C)
   and TikTok (T), and past days without data also display 0.0万. Future days
   stay blank. The user accepted that a genuinely-zero day and a
   not-yet-imported day become indistinguishable in the day cells.

## Finding: plan 1 was already shipped — then retuned to 0.35x
The 万 in the daily total was already drawn at 0.43x via
DAILY_TOTAL_UNIT_SCALE (RelativeSizeSpan in amount()), smaller than the
0.55x used in the mockups, so the first code45 commit changed only the
underline and the zero-display rule. The user then asked for the unit scale
itself to become 0.35x (2026-11-14); DAILY_TOTAL_UNIT_SCALE is now 0.35f,
which also frees width budget for the digits. The C/T detail rows keep
their 0.55x unit scale.

## Source changes (branch fix/daily-total-underline-zero-display, from main 6f1d688)
- CalendarWidget.java: DAILY_TOTAL_FLAGS gains Paint.UNDERLINE_TEXT_FLAG
  (applies to the day total and, through the same constant, the month-end
  forecast total). Day cells: showTikTok becomes isPast||isToday so today's
  TikTok row is visible from 00:00; total/coupon/TikTok texts use
  CalendarAmountFormatter.compactOrZero.
- CalendarAmountFormatter.java: compactOrZero(yen, known) returns "0.0万"
  when the amount is unknown, else compact(yen, true). Display layer only:
  CalendarRevenueData is untouched, so stored unknowns stay unknown for
  month totals, forecasts and the exact-yen detail view.
- Version bump to code45 / 0.1.44 in both build.gradle and
  AndroidManifest.xml (signing build reads the manifest — code43 lesson).

## Test changes
- CalendarAmountFormatterTest: +6 assertions for compactOrZero (44 total).
  Local JVM run: PASS 44.
- WidgetInstrumentation: the coupon-only fixture assertion that encoded the
  old behaviour ("unknown today TikTok stays hidden without inventing zero
  revenue") is replaced by the new spec (row visible, text 0.0万); new
  assertions cover a past no-data day (total/coupon/TikTok all 0.0万) and
  the day total's UNDERLINE_TEXT_FLAG. digitInkHeight now strips
  UNDERLINE_TEXT_FLAG from the probe paint (it measures digit glyph height;
  the underline is decoration below the baseline) so the code41 ink-height
  deltas keep their meaning.

## Gates / signing / publication
- Native gate at d027df5 (underline + zero display, unit scale still 0.43):
  run 43516136698 SUCCESS in 5m36s — native 25314, offline 2767, reboot 2759,
  backup 46 + 4, all PASS (code44 native was 25312; +2 net from the new
  assertions).
- The 0.35x unit-scale change (4f9e313) failed the gate twice (runs
  43517974664 attempts 1-2) with a silent instrumentation crash. The job log
  held no error; the downloaded result artifact's logcat showed the cause:
  AssertionError "only the daily total unit uses the small suffix size" at
  WidgetInstrumentation.java:581 — the span-size assertion still expected
  the old 0.43f. The code42 ink-height delta checks had already passed with
  0.35 before that point. Fix: the assertion now expects 0.35f, mirroring
  DAILY_TOTAL_UNIT_SCALE. Gate re-run on the fix commit a829af5 passed in
  run 43523621212 (native 25314, offline 2767, reboot 2759, backup 46+4).
- Signing: the first ops run (43525224097) failed before building on a
  stale version_code assertion (44) left in the newly adapted code45
  workflow; the workflow was fixed (ops commit 54c5bf5) and the request
  re-issued. Ops run 43527159429 then succeeded end to end, including the
  official code44 -> code45 upgrade job (UID and 42 fixture fields
  preserved, disposable Android 17 emulator, first attempt).
- Signed APK verified locally: 3,198,455 bytes, SHA-256
  d2c5ba1b8ea03245d1dbeb5cb31a0d0962cd25ad05de105a09d387d7e6687a2f,
  existing signer certificate 8495f4b4..., versionCode 45 / 0.1.44
  (keytool + aapt2). Unsigned build SHA-256
  5d42ea46c86b6574b63dfb63fc33dfd9fa5bb102399c9b2ae64a878f8738bcb0;
  stage job asserted all 45 payload entries match the tested build.
- Publication registration (manifest publicationStatus
  PENDING_PUBLICATION), merge and post-publication verification are
  appended below as they complete.
- Registration commits on the PR branch: 05bb1ec (manifest, evidence,
  download page, this checkpoint) and 9b1c801 (signed APK via web UI
  upload). PR #65 merged to main as
  9c9c7e22a875a13f7ff14756a50957ba4bac5a73 — code45 is published.
- Post-publication live verification 2026-11-14T01:25:05Z: both origins
  (Workers and GitHub Pages) serve the code45 page, an identical
  manifest (0.1.44 / 45) and the signed APK (3,198,455 bytes, SHA-256
  d2c5ba1b..., certificate and badging verified on the downloaded
  copies); the code44 APK remains available on both.
  release-tests/code45/public-verification.json records the detail, and
  manifest/evidence publicationStatus is finalized to
  PUBLISHED_VERIFIED in the finalization commit on main.
- Remaining: Pixel 10 physical acceptance by the user (underline,
  0.35x unit, 0.0\u4e07 days, forecast-title regression check).
  Pixel 10 physical acceptance will remain UNCONFIRMED until the user
  checks the device.
