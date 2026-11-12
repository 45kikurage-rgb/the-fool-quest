# Aruno-Result code42 checkpoint

Date: 2026-11-12 JST
Status: IMPLEMENTING, not published. User authorized completion through production via GitHub, minimal chat.
Baseline: code41 / 0.1.40, main 877704bfdd1e39c841bb7abd0aefbd0c72eb0a5b.

## Confirmed scope
- Header year/month, category and amount: independently saved top/bottom/center alignment, actual visible glyph bounds, default center. Keep existing heights, seven-digit fixed yen column, horizontal layout and Total/Coupon/TikTok names.
- Daily total visible glyph height +6 physical pixels versus code41; Coupon/TikTok/loss each -2 physical pixels. Keep bold total, small unit, colors, fixed rows (never collapse absent data).
- Red loss label at left, red signed amount at right, preserve values and calculations.
- Forecast: title left at date slot, total/C/T same row positions and sizes as day cells; keep calculation and whole-cell show/hide.
- Fast category switch: update header text/alignment only; avoid rereading monthly data and rebuilding calendar/gauge; preserve tap order and per-widget state.
- Corporate rounded Bold, package, existing signer, revenue/settings/backups and old APK bytes retained.

## Investigation
CalendarWidget.CYCLE currently calls renderOne, which reads the monthly JSON and rebuilds the full calendar; CombinedWidget also recreates bitmap gauges. Current actual-glyph correction used font metrics but glyph center was not correct in user's screenshot. Android17 native host tests must include global glyph coordinates and reapply paths, independent top/bottom/center, rapid taps, no full render on summary cycle, settings roundtrip/reboot and signed code41->42 data-preserving update.

## Execution route
1. Implement on feature/result-code42-layout. Save each meaningful checkpoint here in GitHub.
2. Run existing Android17 workflow with focused expanded native checks. Fix failures, don't claim success without logs.
3. Pin final source in an ops code42 workflow derived from verified code41 route; independently restore existing key, sign, verify signed/tested payload identity and signed code41->42 update on Android17.
4. Reuse exact signed artifact, publish version0.1.41/code42 using PR and retained legacy artifacts; verify both public pages/manifests/APK size/hash and code41 retention.
5. Mark complete only after public verification; final user response minimal download link. Pixel10 physical test unavailable unless user supplies it.

Source repo: 45kikurage-rgb/the-fool-quest. Ops: 45kikurage-rgb/aruno-platform-ops.
Downloads: https://the-fool-quest.45kikurage.workers.dev/widget-download.html and https://45kikurage-rgb.github.io/the-fool-quest/widget-download.html.
Do not output APK base64 to chat, rebuild signed APKs, rotate keys, clear device data or claim previous code32 losses are recovered.
