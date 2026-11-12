# Code42 checkpoint — revision2 native positioning fix

Version0.1.41/code42, authorized through production. Source implementation saved on feature/result-code42-layout. Public baseline code41 remains unchanged.

Scope: independent saved top/bottom/visible-center settings for year-month/category/amount; total+6px vs41, C/T/loss-2px; red loss label; forecast identical daily geometry; header-only partial category update with no monthly JSON reread or gauge reconstruction. Preserve fonts, seven-digit yen column, package/signature/data and backups.

Initial native run43147236261/job130947789951 compiled and host checks passed, then failed a new whole-parent pixel test: bottom-aligned category at160dpi retained4px bottom gap. Paint metrics alone omit TextView font leading. Revision2 measures an equivalent native TextView baseline at the fixed34dp header height before calculating visible glyph translation. Same modes keep explicit gravity; no image-based partial updates. New resource ids force old launcher XML reinflation after upgrade. Updated initialLayout XML as well.

Added real settings Spinner save, native reboot retention and backup restore tests for all three positions, in addition to actual-host rapid taps and code41 renderer size oracle. Revision2 native CI pending; not signed/published. Read actual logs/artifacts before claiming pass.

Next: complete Android17 native/backup/offline/reboot and screenshot review; pin successful source in ops42 existing-signer workflow derived from code41; verify both independent key restores, exact signed/tested payload equality, signed41->42 update preserving42 fixture fields/UID/startup backup and reboot. Publish exact artifact via PR, keep old APKs, verify both origins. Pixel10 physical verification remains unconfirmed. Download pages unchanged.
