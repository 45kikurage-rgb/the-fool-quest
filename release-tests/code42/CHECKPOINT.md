# Code42 checkpoint — revision4 native theme and complete header frame

Authorized through production, minimal chat. Version0.1.41/code42. Main still publicly code41.
Confirmed scope: independent top/bottom/actual-center header settings defaultcenter; total+6physicalpx vs41, C/T/loss-2physicalpx; fixed red loss label; forecast day-like rows; fast category changes; existing font/package/signer/data/backups and all old APKs.

Revision3 run43148648162/job130952359336 compiled and passed earlier host checks, bottom-edge issue resolved, but whole-parent raster test found centered category2px high at160dpi (above4/below8). Revision4 probe is inflated through the SAME RemoteViews XML style/theme rather than a plain TextView constructor. Header now covers its entire existing38dp outer footprint: moved prior4dp top padding into the field's height (34->38), preserving total header footprint and all grid/day geometry. Visible raster translation cached96entries, temporary bitmap recycled and never sent in partial update. Added real failure screenshot pull to workflow and richer test diagnostics.

Revision3 already enabled stable child reuse with addStableView on API31+ for day cells, combined sections and gauge children, legacy fallback retained. Header partial updates never reread monthly JSON or rebuild gauge bitmaps. Revision4 serializes full and partial provider updates to avoid concurrent data refresh overwriting the latest tap.

Native tests: independent modes+category reapply, parent-raster pixel bounds, raster font deltas vs pinned code41 test-only renderer, matching forecast slots, red loss label/amount, settings UI save, backup restore, real reboot, 17rapid PendingIntent taps through actual host on standalone+combined while preserving day CHILD identity. Await revision4 results; do not sign or publish yet.

Next: inspect actual logs/artifacts, fix evidenced failures and save here. After final pass pin exact source into prepared ops42 existing-signer workflow derived from41, independent restores, sign, compare42payloadentries, signed41->42 exact data/UID/startup backup/reboot. Publish exact signed artifact through PR, verify both origins page/manifest/APK and old41 bytes. Pixel10 physical verification remains unconfirmed. No APK base64 in chat.
