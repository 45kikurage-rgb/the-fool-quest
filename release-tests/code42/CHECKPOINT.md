# Code42 checkpoint — revision7 Android singleLine semantics corrected

Version0.1.41/code42; PR62 draft; public code41 unchanged. User authorized through production via GitHub with minimal chat.
Scope: independent saved top/bottom/visible-center header settings defaultcenter, total+6px vs41/C/T/loss-2px, loss label, daily-like forecast, smooth summary switch, preserve signer/package/data/fonts/backups/old APK bytes.

Revision6 run43151433406/job130961264023 compiled and passed host checks but actual END-aligned text vanished. AOSP TextView constructor/source confirms singleLine(true) unconditionally calls setHorizontallyScrolling(true), overriding XML false. setHorizontallyScrolling itself is NOT remotable. Revision7 uses remotable setSingleLine(false) followed by setLines(1); XML lines1 + ellipsizeEND guard + scrollfalse, and identical native probe settings. Full text/no-ellipsis and parent-raster checks remain mandatory. No viewport/clipping workaround accepted.

Header positions: native styled paint measures widths; shaping margin16dp; actual ink raster measured in matching RemoteViews XML/theme, bounded96float offset cache, temporary rasters recycled (no bitmaps in partial updates). Complete existing38dp header footprint preserved with0top padding/38field. Daily row transfer preserves weighted first-passsum0: Total9px/-3px, C/T/loss each bottom margin-2px. Forecast identical geometry; red loss label left/value right. Last failure screenshots already show the daily overlap fixed.

Performance: tiny private noncredential summary cache, no monthly revenue parse/gauge bitmap generation on cached cycle; serialized full/partial updates, stable API31+ children for calendar/combined/gauges; fallback full update on cache/date miss. Tests compare actual day CHILD identity after17rapid PendingIntent taps on standalone+combined.

Tests include3densities, all modes/categories/full strings/pixel positions, exact raster font delta vs41 test-only renderer, forecast slots, loss columns, settings UI save, typed backup/reboot. Await revision7 CI and visual inspection. Failed candidates1-6 were not signed or published.

Next gate: final Android17 normal/offline/reboot/backup success; pin exactsource into prepared ops42 signer route derived from41; independent existing-key restores, sign, compare exact tested/signed payload entries, signed41->42 preservation of42fixture fields/UID/startup backup/reboot. Publish same signed APK viaPR62, retain old APKs, verify both distribution origins. Pixel10 physical acceptance unconfirmed. No APK/base64 or huge logs in chat.
