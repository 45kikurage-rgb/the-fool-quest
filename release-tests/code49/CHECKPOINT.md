# Aruno-Result code49 checkpoint

Status: PUBLISHED_VERIFIED
Target: versionCode 49 / versionName 0.1.48
Change: user decision 2026-11-16 — the calendar totals (daily total and
month-end forecast total) no longer display the 万 unit. C/T detail rows,
the corner loss and every other amount keep their units. Digit size,
weight, color, the overlaid one-pixel rule and the C/T row positions are
unchanged from code48.

## Exact behavior

- CalendarAmountFormatter gains totalCompact, totalCompactOrZero and
  totalForecast: the same rounding as compact/compactOrZero/forecast with
  no trailing 万. Unknown totals still render "—" (forecast) or "0.0"
  (day cells, user decision 2026-11-14 carried over without the unit).
- CalendarWidget routes only the two totals through the new formatters:
  the day total (including both width-fit passes) and forecast index 0.
  Forecast C/T and all day-cell C/T rows keep forecast/compactOrZero.
- totalAmount now treats a unit-less total as a digits-only stack: the
  calendar_total_unit view is set GONE (an empty view would still reserve
  its row) and the stack minimum height is digit height + 1 physical
  pixel of clearance, centered in the unchanged weighted slot and still
  shifted up exactly one physical pixel. The digit-size computation is
  untouched, including the historical 0.35 unit cap, so glyph size is
  identical to code48.

## Tests

- Host CalendarAmountFormatterTest: 58 checks (15 new for the total
  formatters; every pre-existing compact/forecast expectation retained).
- WidgetInstrumentation: total expectations are unit-less ("−99.9",
  "99.9", forecast "218"/"111", matrix via totalCompact); the unit view
  is asserted GONE in completeCalendarTotal, checkTotalGeometry,
  checkDailyCompaction and the calendar content checks; the geometry
  contract now pins the digits-only stack (digits + 1px, centered, 1px
  up). totalUnitAndRuleChecks renamed to totalRuleChecks.
- Version bumped in both places that carry it: app/build.gradle and
  app/src/main/AndroidManifest.xml (build.sh reads the manifest; a build
  with only the Gradle bump still reported code48 and was caught by an
  aapt badging re-check before anything was pushed).
- Local runs (this workstation): scripts/test.sh passes except
  RevenueHttpTest, which fails identically on unmodified main in this
  sandbox (localhost slow-socket timing) and is left to CI; node tests
  14 pass; test-widget-render-order.py PASS; scripts/build.sh and
  scripts/build-android-tests.sh both build clean, and the built APK
  reports versionCode 49 / 0.1.48.


## Validation and release

Native Android17 run 43815899229 succeeded on the pull request head f1c0289c.
Existing-signer restoration (primary and backup), signing and official code48-to-49
upgrade/data retention succeeded in ops run 43839652944 (attempt #3; attempts #1 and #2
failed at the same dump_ui step because Pixel Launcher went ANR immediately after
emulator boot, before the code49 APK was installed — an emulator-environment fault, not
an app fault; the core upgrade checks passed in both failed attempts as well). The signed
APK SHA-256 c2110cd814dd78b4046de775bd45c7bfcc9685d1a91000c6fa0e85e146a9305a matches the
artifact registered in PR #69 byte for byte (Git blob 37b8fa1ab3d74395db99a0d0407ac5dc7d19158e), certificate unchanged.
Publication metadata is registered in PR #69. Publication completed: PR #69 merged to main as
2b13d2edbb1f0b2e4a98d1a055b933823fd5d68a. Both origins (Cloudflare Workers and GitHub Pages)
were re-fetched and serve code49 with the identical APK SHA-256 and bytes; the code48 APK is
retained. Details are recorded in releases/aruno-result-code49-release-evidence.json.
Pixel 10 physical acceptance remains separate and unconfirmed.

## Remaining gates

- Pixel 10 physical-device check (separate item).
