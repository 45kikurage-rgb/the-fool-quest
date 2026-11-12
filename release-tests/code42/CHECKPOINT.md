# Code42 checkpoint — revision3 visible-raster and stable child reuse

Authorized production completion, minimal chat. Version0.1.41/code42. Baseline published code41 preserved.
Scope unchanged: independent saved header top/bottom/actual-center (default center); total visible height+6px vs41, C/T/loss-2px; loss label; forecast matching day cells; fast summary switch.

Native attempts43147236261 and43147898140 compiled and ran host checks but new whole-parent pixel test exposed4px bottom gap on category at160dpi. Measuring font bounds or a TextView baseline alone did not fix visible raster alignment. Revision3 rasterizes an equivalent native TextView offscreen, scans actual foreground pixels, caches only the resulting bounded float offset. These tiny temporary bitmaps are recycled, NEVER attached to partial RemoteViews. Daily physical-height calculations and test oracle also use actual raster digits (published code41 renderer remains test-only).

Android service merges partial updates into cached RemoteViews and replays cached child actions. Added public API31+ addStableView for calendar cells, combined panels and gauge child views, with older-OS fallback. Actual-host rapid-tap tests now assert DAY CHILD object identity, not merely a static week container. No monthly JSON read or gauge bitmap creation on cached summary cycle.

Tests include three density contexts, independent modes/category reapply, pixel alignment, exact font deltas vs41, loss label separation, matching forecast row sizes/positions, settings UI Spinner save, typed backup restore and reboot positions, standalone+combined actual PendingIntent burst. Await revision3 Android17 CI; no pass/sign/publication claimed yet.

Next: inspect actual failure logs if any; fix evidenced defects, save here. After final native success, pin source in ops code42 workflow (prepared locally from code41 verified route, not triggered yet); restore existing signer independently, sign and verify same signed/tested payload, signed41->42 preservation/UID/startup backup/reboot. Publish exact signed artifact via PR, retain old APK bytes, independently check both public origins. Physical Pixel10 remains unverified. Never output base64/APK bodies in chat.
