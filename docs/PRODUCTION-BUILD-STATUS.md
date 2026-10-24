# Amazon draft and build status — 24 October 2026

Latest app checks: 118 offline and 118 Amazon-preview tests pass, plus the unchanged
13 billing-adapter checks. Debug, Amazon Preview and Voice QA APKs build and their
signatures/alignment verify. Debug and Amazon-preview lint have zero errors
(69 and 70 warnings respectively). New progress-export snapshot/error/retry and
parent-only status handling pass software checks and two physical private-file
ContentResolver tests on KFTRWI/API 28. No personal profile/PIN or document-picker UI
was used, and the base app/settings were unchanged. Exact results and limits:
`release/progress-export-software-2026-10-24.json` and
`release/progress-export-device-2026-10-24.json`. These remain development/QA builds;
production approvals are unchanged.

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
