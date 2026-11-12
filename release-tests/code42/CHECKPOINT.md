# Code42 checkpoint — implementation saved, Android17 CI pending

User authorized production completion, minimal chat. Baseline main877704bf, code41. Version candidate0.1.41/code42.
Implemented: independent top/bottom/visible-glyph center header settings (default center, backup allowlisted); fresh layout ids to avoid stale launcher XML after upgrade; daily total+6px and C/T/loss-2px compared to published code41; red fixed loss label; forecast uses day row geometry and typography; header-only partial summary updates from tiny noncredential cache without rereading revenue or regenerating gauge bitmaps. Cache misses/date change fall back to full update.

Native regression adds exact published code41 renderer in test APK only as font-size oracle; whole-parent raster alignment for each independent mode; loss label/amount separation; forecast geometry; 17 rapid actual PendingIntent taps through AppWidgetHostView for standalone+combined, preserving daily view identity and no full render; settings/backup roundtrip. Existing full Android17 normal/offline/reboot and backup suites retained. Not yet passed.

Next: inspect CI compile/test failures; fix only evidenced issues; save checkpoint. After final CI success pin source in code42 ops workflow derived from code41 existing-signer route, sign with unchanged key, verify signed-vs-tested payload and code41->42 exact data/UID/backup/reboot retention. Publish exact signed APK via PR after safety checks; verify both public origins; preserve code41 and all legacy APKs. Do not claim real Pixel10 verification. No base64 blobs in conversation.

Distribution https://the-fool-quest.45kikurage.workers.dev/widget-download.html ; fallback https://45kikurage-rgb.github.io/the-fool-quest/widget-download.html .
