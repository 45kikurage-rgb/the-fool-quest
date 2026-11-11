# Aruno-Result code41 completion checkpoint

Updated: 2026-11-12 JST

## Goal and authorization
Complete the confirmed Aruno-Result code41 changes through production publication. User explicitly authorized production release and requested GitHub checkpoints. Report only completion or a blocker.

## Verified state
- PR #61, branch `feature/result-code41-header`, head `79073174cf5536989eeaf6f1c0721367b20e81c8`.
- App version 0.1.40 / versionCode 41; package and existing signer unchanged.
- Signed APK `android-widget/releases/aruno-result-0.1.40.apk`, 3190018 bytes, SHA-256 `40b9c4153b370a2da813c69a970a49a3caec8335a8bbab736665963c301456c0`.
- Source/test commit `ea913218b07167cfd18d3fced9c5ebb0631949a3`.
- Confirmed scope: Total/Coupon/TikTok header; fixed seven-digit yen amount column; right-aligned category immediately to its left; optical vertical centering; existing font, heights, daily display and storage retained.
- Android17 native run43112625516 success: 23386 normal / 2767 offline / 2758 reboot / 37 backup / 4 backup reboot checks.
- Existing-signer and code40->41 upgrade run43112773673 success: UID and 39 fields retained, startup safety backup and reboot retained. Signed/tested payloads match all 42 entries.
- Additional PR APK upgrade run43117986022 / job130850820952 success.
- Visually inspected committed seven-digit Coupon screenshot; no clipping/wrapping observed.
- Local javac absent; use completed CI results, no new local Java test pass claimed.
- Workers preview build check failed; production and GitHub Pages must be verified after merge. Previous releases also had preview failures before successful production builds.
- Pixel10 physical acceptance remains unconfirmed.

## Production merge completed
- PR #61 merged as `ef6b4733b321b31c47a82cda6318f27775414750`.
- Public deployment and both-origin byte verification are pending. Do not repeat the merge.

## Next steps
1. DONE: merged PR #61 at the exact verified head, retaining signed APK bytes.
2. DONE: saved production merge SHA above.
3. Wait for production Pages deployment and public verification workflow; inspect failures if any.
4. Retrieve both distribution pages, manifests and actual APKs, compare version/bytes/hash and prior code40 retention.
5. Update manifest, release evidence and SIGNING_UPDATE_CURRENT.md with observed publication results, update this checkpoint to COMPLETE, commit/push GitHub.
6. Final response only published version, verification results (physical-device limitation) and download-page link.

## URLs
- https://the-fool-quest.45kikurage.workers.dev/widget-download.html
- https://45kikurage-rgb.github.io/the-fool-quest/widget-download.html

## Resume rule
Read this checkpoint and the release evidence first. Inspect current main and PR state before mutating. Reuse the verified signed APK; do not rebuild/re-sign or change package/storage. Never claim physical-device acceptance without evidence. Preserve existing APKs and unrelated PRs.
