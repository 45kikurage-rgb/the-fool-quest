# Aruno-Result code50 checkpoint

Status: SIGNED_VERIFIED_PUBLICATION_PENDING
Target: versionCode 50 / versionName 0.1.49
Change: user report 2026-11-17 — on the Pixel 10, a negative daily total
("-5.6") showed its underline starting partway into the minus sign, so
the number looked shifted right against its neighbours. The totals are
right-aligned (stack gravity "end", matching the C/T amounts below), and
the old rule width was measured in the app while the launcher rendered
the text with a substituted host font, so the measured rule came out
shorter than the rendered text; with right alignment the shortfall
appears on the left edge, exactly where the minus sits. Code50 renders
each total inside the app as a single raster (digits plus rule), so the
host font can no longer shift the rule. The code49 unit-less totals,
right alignment, digit size and positions are unchanged.

## Exact behavior

- New CalendarTotalRaster renders the styled total (existing spans,
  including the reduced minus, and the existing horizontal scale) with a
  native TextView probe, crops the transparent horizontal bearings, and
  draws the one-physical-pixel rule across the full ink width at the
  visible ink bottom. Empty text stays an empty native-height row with
  no rule.
- totalAmount keeps the text metadata for readback compatibility but
  hides the legacy text, unit and rule views (GONE) and sets the raster
  into the new end-aligned calendar_total_image view. Vertical geometry
  (native row height plus one pixel of clearance, slot centering, one
  physical pixel up) is retained from code49.
- Widget layouts move to widget_calendar_v50.xml and
  widget_combined_v50.xml; the forecast total uses the same totalAmount
  path. C/T rows, loss display, amounts, storage format, package and
  signer are unchanged.

## Tests

- WidgetInstrumentation pins the raster contract: the rule spans every
  pixel column at the actual digit bottom; the rendered total image,
  including a minus, ends at the slot right edge with no host-font right
  bearing; the negative fixture total is "-99.9" (code49: no unit); the
  legacy unit and rule views stay GONE; the raster retains the exact
  signed display metadata; reapply keeps geometry stable.
- Version bumped in both places that carry it: app/build.gradle and
  app/src/main/AndroidManifest.xml.
- Native Android17 CI run 43986719941 on PR #70 head
  3807e50ee36a5dd3a32f75cc08b779814e778d63 succeeded. (Workers Builds
  failure on the PR is the known non-required Cloudflare preview check.)

## Validation and release

Existing-signer restoration (primary and backup), signing and the
official code49-to-50 upgrade/data-retention verification succeeded in
ops run 43987610880 (upgrade job on attempt #2; attempt #1's upgrade
failure is recorded in PROGRESS50). Signed APK SHA-256
3ebfd7ba7d0d5ff3f5f1b93ae07ce97bde21610e887b783fd5eff4b766d7f808,
3,206,807 bytes, certificate unchanged
(8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730).
Publication metadata is registered in PR #70. Publication and the
two-origin re-fetch verification are recorded here when complete.
Pixel 10 physical acceptance remains separate and unconfirmed.

## Remaining gates

- Pixel 10 physical-device check (separate item).
