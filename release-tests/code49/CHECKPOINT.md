# Aruno-Result code49 checkpoint

Status: IMPLEMENTED_ANDROID17_PENDING
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

## Remaining gates

- Android 17 native CI on the pull request must pass in full.
- Existing-signature signing in aruno-platform-ops, the code48 -> code49
  emulator upgrade check, publication metadata, both-origin
  verification, and the Pixel 10 physical-device check follow the
  code48 path.
