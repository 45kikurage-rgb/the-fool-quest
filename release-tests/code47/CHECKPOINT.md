# Code47: recovery, coherent updates and calendar total rendering

User authorized implementation through production publication on 2026-11-15
(Japan time), following the read-only code46 audit. Target: Aruno-Result
0.1.46 / versionCode47, same package and existing certificate.

## Changes

- Restore a valid backup even if the live allowlisted data is malformed.
  Before replacing it, preserve the damaged values without credentials in
  a separately named, checksum-verified quarantine, both locally and in
  the chosen device folder. Quarantines are never regular restore generations.
- Keep malformed/unreadable local journal files and report their counts;
  continue exporting subsequent valid generations. Destination permission
  or write failures still fail the operation.
- After native revenue management is initialized, reject old website
  snapshot imports before confirmation and again at commit. Existing CSV,
  manual entries, losses, goals and JSON migration remain the native path.
  A website snapshot before native initialization still migrates normally.
- Validate loss aggregates for months containing only loss entries, including
  the combined per-day upper bound used by the calendar.
- Serialize gauge redraw requests on the UI thread and read the latest state
  when the queued request executes. Drawing failures cannot strand the
  refresh flag or skip completion callbacks.
- Record daily reader status independently of monthly success and show
  configuration, authorization, network, validation and storage failures.
  Credential values are never included in that status or in recovery files.
- Give daily/forecast total digits, the small unit and the underline separate
  native elements: reserve unit height and draw one continuous line across
  the reduced minus and digits. Preserve C/T slots and monetary computation.
- Show exact yen for losses in tapped day details.
- Test the JavaScript actually loaded by the published export page, and
  require its retained compatibility copy to match.

## Policy and identity

CURRENT document version 2026-11-08.1; Library version16 and modified time
2026-11-08T09:53:06.782331Z unchanged from the full read in this chat.
Certificate: 8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730.
Existing package: com.aruno.foolquest.widget.next. No new signing key,
server contract, source authority, account or database changes.

## Verification / release state

Implementation and local software checks in progress. Native Android tests,
independent primary/backup signer restoration, code46-to47 official upgrade,
signed-versus-tested APK payload equality, publication and public retrieval
remain pending until their results are recorded here and in release evidence.
Physical Pixel10 acceptance and formal operation are separate and unconfirmed.

Old renderer removal and combined-widget preview expansion are deferred:
they are maintenance/feature work rather than required fixes for these bugs.
The historical code32 data-loss cause remains undetermined.

## Accepted candidate and resumed publication

Source `d8ac5a3c05fba5ef044d9e913843731c000ee42d`. Android17 run43703619901/job132779875073 passed: normal68358, offline2767, reboot2759, backup54, backup-reboot4;81 native display geometries. Existing signer primary/backup restoration and stage/official upgrade run43704249663 passed, preserving42 fields and UID. Signed APK SHA-256 `a862d0dd77672048270f8631c2360d333c231de8f8e0cbc7f46b76a6a8f7ecdb`, 3202551 bytes;45 non-signature entries match the tested APK.

Browser file selection stalled twice; exact hash-verified artifact is registered through Git transfer. No rebuild, re-signing, new key, uninstall or data clearing. Publication pending public verification. Physical Pixel10 remains unconfirmed.
