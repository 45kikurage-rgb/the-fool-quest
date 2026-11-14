# Code46 checkpoint — total digits centered, unit below, loss in the corner

Version 0.1.45 / code46. User instructed on 2026-11-14 to implement the
layout developed across four mockup rounds the same day, through
production publication.

## Origin (user decisions, 2026-11-14)
1. "I want the daily total bigger so it is easier to read." Analysis of
   the shipped sizing code showed the total is capped by a width
   reservation for the widest string "-99.9万": in this font the minus
   sign is a full em wide (wider than a digit) and the unit also sits on
   the digits line. Vertical room alone cannot raise the total.
2. Losses are rare (about once a month), so the user moved the loss out
   of its own bottom row: it is now a two-line block ("損失" over the
   amount) in the top-right corner of the date band, right-aligned. The
   freed row height belongs to the total.
3. C and T move to the bottom of the cell; the total is vertically
   centered between the date band and the C/T rows — centered on the
   DIGITS, with the unit hanging below them.
4. The total's unit （万） leaves the digits line entirely: it hangs
   below the digits, right-aligned. With the minus also reduced, the
   digits reach about the size the integer-only plan would have given,
   while keeping 0.1万 precision.

## Source changes (branch fix/total-unit-below-loss-corner)
- calendar_day.xml: the date band becomes a horizontal container
  (calendar_day_top) holding the day number and the loss block include.
  calendar_amount_rows now includes only Total/C/T.
- calendar_row_loss.xml: rewritten as calendar_loss_block, a vertical
  two-line block (label over amount, gravity end), still INVISIBLE
  unless the day has a loss.
- calendar_row_total.xml / calendar_forecast.xml: total weight 1.12 ->
  2.02 (the old loss weight), maxLines 2 for the two-line total. The
  forecast cell drops its empty 0.90 spacer so its C/T rows stay in the
  same slots as the day cells.
- CalendarWidget.java:
  - totalAmount() styles the totals (day + forecast): digits line, then
    "\n" + unit at DAILY_TOTAL_UNIT_SCALE (0.35); RelativeSizeSpan
    DAILY_TOTAL_SIGN_SCALE (0.55) on a leading minus; UnderlineSpan over
    the digits only. DAILY_TOTAL_FLAGS loses UNDERLINE_TEXT_FLAG (the
    span replaces it, so the hanging unit is not underlined).
  - The width reservation now models the digits line as drawn (three
    digits + reduced minus + point; no unit), which is what lets the
    shared size grow: at the user's 411x520 geometry the model gives
    about 23dp vs about 19dp in code45.
  - dailyTotalSize gains a two-line clamp so digits + unit lines fit
    the 2.02 band at every widget height.
  - Bindings shift the total view up by half the unit line so the
    digits are what centers in the band.
  - lossSize is capped at about half the date band per line; the loss
    amount is fitted into the width remaining beside the widest
    two-digit date.
- calendar_legacy_ids.xml: keeps the removed calendar_loss_row id
  defined so the frozen code41 reference renderer in the verification
  suite still compiles. No product layout uses it.
- Version bump to code46 / 0.1.45 in build.gradle and
  AndroidManifest.xml (both — code43 lesson).

## Test changes (WidgetInstrumentation)
- Total text expectations become two-line ("99.9\n万" etc.);
  completeCalendarText already verifies per-line fit.
- New assertions: negative-total span set (minus 0.55 at 0, unit 0.35
  on the second line), underline span covers the digits only, loss
  block position (label above amount, right-aligned, red, inside the
  date band), amount-rows order is now Total/C/T with T at the bottom,
  corner-loss ink stays inside the date band (checkDailyCompaction).
- The code41 ink-delta contract is restated for the new design: C/T
  keep their -2 delta; the total must exceed the code41 reference
  (was a fixed +6 in the one-line design); the corner loss must be
  smaller than the old full-row loss but visible.
- Filter and reapply row-slot checks address the loss as
  calendar_loss_block.

## Gates / signing / publication
- (appended as they complete)
