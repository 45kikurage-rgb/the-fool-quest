# Aruno-Result code48: overlay underline and one-pixel total shift

User authorized implementation through production publication on 2026-11-16 JST.
Target: 0.1.47 / versionCode48, existing com.aruno.foolquest.widget.next signer.
CURRENT 2026-11-08.1 / Library16 / modified2026-11-08T09:53:06.782331Z unchanged.
Latest own signing ledger was read in full before work. Existing certificate:
8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730.

## Change

- Overlay the one-physical-pixel rule on the actual digit ink bottom, sharing
  one continuous line across the reduced minus and digits.
- Keep the former stack centering reference height, then shift the daily total
  and forecast total upward by exactly one physical pixel. Removing the rule
  row cannot silently undo this shift through integer centering.
- The unit retains its own measured native row; the removed rule row becomes
  unit clearance. Numeric sizes/scales, C/T positions, loss/date positions,
  monetary calculation, package, signer and stored-data paths are unchanged.
- Native regression covers exact shift, overlaid rule position and continuity,
  digit/unit pixels surviving parent drawing, and RemoteViews apply/reapply.

## Validation and release

Resource XML parse and whitespace checks passed locally. Native Android17
run43764726052 succeeded (first candidate run43764178091 failed on a real
geometry assertion and was corrected in 5c7ae3c; see below). Existing-signer
restoration (primary and backup), signing and official code47-to48
upgrade/data retention succeeded in ops run43765368932; the signed APK
SHA-256 9855b131b95fb13b8e53e9a8ee76ec7bc9efcc4668d6858d0a67709b666d8909
matches the artifact registered in PR #68 byte for byte (Git blob
e615f2ed3208495ebd9774b1e420a89b814c64e9), certificate unchanged.
Publication completed: PR #68 merged to main as 0d9477f6b80add68184785dcec78ad00180be3db.
Both origins (Cloudflare Workers and GitHub Pages) were re-fetched and serve
code48 with the identical APK SHA-256 and bytes; the code47 APK is retained.
Details are recorded in releases/aruno-result-code48-release-evidence.json.
Pixel10 physical acceptance remains separate and unconfirmed.
Only Android17/API37 is an execution target. Pixel10 physical acceptance is
separate and unconfirmed. Do not rebuild or re-sign a validated artifact for
transfer; use hash-verified Git blob transfer without giant conversation output.
Do not mark publication commits skip-ci when Workers must deploy their contents.

## Candidate 1 failure and correction

Run43764178091 compiled and passed local checks, but native geometry rejected a
one-pixel rule offset on a small daily-total view (digitsTop1, inkBottom8,
ruleTop9). This was a real assertion failure, not a runner outage. Font metric
baseline approximation was replaced with the same styled native TextView's
measurement and rasterized current text. The 81-geometry matrix now also
checks an integer forecast111万 whose digit bottoms differ from decimal totals.
No signing request or public APK was produced for the rejected candidate.
