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
