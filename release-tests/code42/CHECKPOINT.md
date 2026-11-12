# Code42 checkpoint — revision5 wrap/row-overlap fix

Authorized through production, minimal chat. Candidate0.1.41/code42, public main remains41. Preserve package/signer/data/fonts/backups/old APK bytes.

Confirmed scope: independent saved top/bottom/visible-center for month/category/amount (default center), total+6physicalpx vs41, C/T/loss-2physicalpx, red loss label, forecast like day cells, smooth summary switch.

Latest failed native run43149519083/job130955121836 exposed the ROOT cause with richer diagnostics: category Coupon actually had2line layout (52px) in38px field on reapply, so apparent vertical-centering error. Failure screenshot artifact13913007965 at verification/failed-header-160-1-2.png also exposed visible Total/C overlap. Revision5 enforces singleLine/horizontal no-wrap and doubles conservative horizontal shaping margin8->16dp. Complete line-width regression still must pass; no cropping accepted.

For daily rows, transfer precisely six physical pixels without changing outer/date/grid geometry: Total height9px/margin-3px (old3/-3), each C/T/loss bottom margin-2px. First-pass total remainszero, weighted base heights unchanged, total receives6px and detail rows move6/4/2px with smaller glyphs. Forecast uses exactly same geometry. Numeric GLYPH overlap is checked directly from actual native text raster; font leading is allowed outside text line box only if full digit ink remains present. Size oracle is exact published41 renderer in test APK only.

Prior revisions43147236261/43147898140/43148648162 failed strengthened pixel checks and were NOT signed/published. Header measurement now uses matching RemoteViews XML/theme and actual glyph raster (bounded cache96, recycled temporary bitmap, never in partial update). Header fields span the preserved38dp outer header footprint, eliminating unequal top padding. Full/partial updates serialized; Android31+ stable child reuse prevents cached RemoteViews replay from rebuilding day/gauge child objects.

Tests: parent raster each independent mode/category/density, exact size deltas, forecast geometry, loss label, settings UI save, backup restore/reboot, 17actual rapid PendingIntent taps through standalone+combined AppWidgetHostView with day CHILD identity. Await revision5 CI logs and screenshots.

Next final gate: successful native17+offline+reboot+backup, pin exactsource in prepared ops42 route, unchanged signer independent restores/sign, compare signed/tested payload, signed41->42 exact42fixture fields+UID+startup backup+reboot, publish same APK via PR, verify both origins and retained41. Pixel10 physical verification unconfirmed. Do not output APK/base64 or huge logs.
