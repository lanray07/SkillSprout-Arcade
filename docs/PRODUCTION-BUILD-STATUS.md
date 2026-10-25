# Amazon draft and build status — 25 October 2026

Latest app checks: 165 offline and 165 Amazon-preview tests pass, plus the unchanged
13 billing-adapter checks. Debug, Amazon Preview and Voice QA APKs build and their
signatures/alignment verify. Both app variants have zero lint errors
(72 and 73 warnings respectively). Pack file selection now belongs to the activity and
survives the required parent relock in memory. Reading is bounded/cancellable with
safe failure/retry and polite parent-only feedback; installation still requires an
explicit parent action and existing pack validation. Two private-file ContentResolver
checks pass on KFTRWI/API 28 without installing a pack, opening profiles/PINs or
changing the base app/observed settings. Exact results:
`release/pack-file-software-2026-10-25.json` and
`release/pack-file-device-2026-10-25.json`. External document-picker/provider UI,
full install lifecycle and final-candidate acceptance remain pending. These remain
development/QA builds; all production approvals are unchanged. Earlier engineering
evidence remains in its dated reports.

The Amazon draft contains the publisher-signed **review-only** APK, **0.2.2-dev-review / version code 4**, labelled `ReviewOnlyDoNotPublish`. Its SHA-256 is `5db196bfbf919cf099cba1552c13f441bc59b54378b1fe106e857e2cc7c821d1`. Purchasing is disabled, development content is explicitly disclosed, and the build is non-debuggable with no development-access bypass. It has not been submitted for publication. Current machine-readable records are `release/amazon-setup-status.json` and `release/production-attempt.json`.

## Production build result

The latest `:app:assembleAmazonRelease` attempt passed the production Amazon configuration and signing-configuration checks, then stopped at `verifyReleaseEvidence`. No production APK was created or uploaded. All **444 activities still await teacher review**. A signing key and a working receipt endpoint cannot approve those activities or replace the remaining reviews.

The following evidence is still required:

| Review | What remains |
|---|---|
| Learning content | A reviewer with relevant primary teaching experience reviews the actual pack and records approval of each item and the final app checks. |
| Fire devices and accessibility | Complete wider device/OS, performance, missing offline voice, physical keyboard and exact signed-candidate checks. Representative VoiceView, six exact diagrams and 150% text/rotation pass on the earlier preview. |
| Amazon billing | Sandbox signed-lease outage/expiry/recovery passes; complete live-store and exact-candidate checks. App Tester results remain partial evidence. |
| Publisher privacy and brand | Review the final privacy/child-directed declarations and product-name clearance. |
| Signing | Verified encrypted backup and separate backup/passphrase retention are confirmed. Validate the exact signed candidate certificate/hash and QA. |
| Languages | Review shipped language content and the translated store copy with native speakers. Marketing translations do not translate the app. |

`release/evidence.json` requires real reviewer identities, dates and hash-bound artifacts. Pending approvals have not been invented or bypassed. See `RELEASE-BLOCKERS.md` for unfinished product scope as well as review requirements.

## Completed setup and partial verification

The Family Premium subscription draft has accepted prices of **£4.99/month and £29.99/year**, saved with GBP as the base and Amazon-calculated prices for other marketplaces. It remains unpublished; no trial is advertised as live. The app-specific Amazon authentication public key and separate sandbox/production receipt keys are configured outside Git.

Both receipt services are deployed on Cloudflare Workers Free hosting, with encrypted secret bindings and separate keys. The latest readiness probes returned HTTP 200 and `ready: true` for both. Readiness probes do not prove production receipt processing.

A physical **KFTRWI / API 28** passed App Tester catalogue, fresh purchase and cache-cleared manual restore through the hosted sandbox. Extended checks cover cancellation, account switching, UK sandbox currency/billing-period labels, signed-lease expiry and clock rollback. Results are in `release/fire-billing-qa.json` and `release/fire-billing-qa-extended.json`; they do not approve a complete Fire/accessibility/billing matrix or establish a live production purchase.

Store localization now includes 176 PNGs and matching editable SVGs across 11 language variants. Nine description profiles and six image profiles are saved and verified in Amazon's unpublished draft. Unsupported profile types remain local. The interface and learning content are English only; native-language review is pending. See `release/amazon-localization-status.json` and `marketing/SUBMISSION.md`.

The portal's **Ready to Submit** badge reflects form/file validation, not production approval. The existing DRM-integration/outdated-SDK notice remains documented in `AMAZON-INTEGRATION.md`; it is not resolved by the localized listing work. The current Amazon draft must remain unpublished until the production requirements are satisfied.

22 October software follow-up: 77 shared-app tests, 13 billing-adapter tests, tooling/website/receipt-service tests and local Worker runtime checks pass. The refreshed Amazon preview builds and lints (zero errors, 75 warnings). Billing status announcements and wrapping recovery controls are checked at 150% text. The Amazon-preview JVM suite has a retained SDK receiver setup VerifyError; it is not reported as passing. No new physical or production-candidate test is inferred. Full results and hashes: `release/production-follow-up-2026-10-22.json`.


23 October follow-up: the latest automated missing-engine component checks pass on
KFTRWI/API 28 in the separate offline Voice QA app. Normal-app navigation/saved
settings, audible speech, keyboard, wider devices and exact-candidate checks remain
outside that result. The current eight pending gates, review owners and evidence
requirements are enumerated in `release/outstanding-approvals-2026-10-23.json`.
Read `release/production-follow-up-2026-10-23.json` for today's software results and
Amazon-preview JVM harness fixes; older failure records remain historical evidence.
No new approval is implied by this update. Key-backup and support-retention setup
are already complete and need not be repeated.


24 October break-reminder follow-up: break intervals now persist separately for
each explorer, survive app recreation and reset independently from daily/lifetime
usage and bonus allowance. Existing reminder acknowledgement, bedtime and daily
limits are preserved. Upgrade begins an interval at zero because previous versions
never persisted it. Automated boundary/UI checks and separate Fire storage evidence
are documented in `BREAK-REMINDERS.md` and the dated break-reminder reports.
Full-app physical switching/restart, audible reminders and exact-candidate acceptance
remain pending; no gate is approved or production APK created by this change.


25 October game recovery follow-up: in-progress checkpoints now validate format,
UUID, counter bounds, activity membership and seen-list consistency, and bind to
content/assignment settings. Invalid or legacy checkpoints start a new partial run;
recorded Room attempts and completed missions remain intact. Seen IDs use JSON
arrays, and restored tile selections reject out-of-range/duplicate indices.
See `GAME-CHECKPOINTS.md` and the dated game-checkpoint evidence for exact checks
and limitations. No content or approval status is changed; full-app physical
restart/rotation and exact production-candidate acceptance remain pending.


25 October pack-file follow-up: file picking now belongs to the activity, retaining
selected bytes in memory across the required parent relock. Installation still
requires PIN re-entry and an explicit parent action with existing pack checks.
Bounded/cancellable reads handle empty, missing, oversized and failing streams;
provider errors use generic polite feedback and overlapping controls are disabled.
Clearing/destroying the owner releases the selection and prevents stale results.
See `PACK-FILE-SELECTION.md` and the dated pack-file reports for evidence and limits.
Full document-picker/provider UI, install lifecycle and exact-candidate acceptance
remain pending; no content or approval status is changed.
