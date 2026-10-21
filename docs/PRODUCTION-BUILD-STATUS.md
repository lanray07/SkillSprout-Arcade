# Amazon draft and build status — 21 October 2026

The Amazon draft contains the publisher-signed **review-only** APK, **0.2.2-dev-review / version code 4**, labelled `ReviewOnlyDoNotPublish`. Its SHA-256 is `5db196bfbf919cf099cba1552c13f441bc59b54378b1fe106e857e2cc7c821d1`. Purchasing is disabled, development content is explicitly disclosed, and the build is non-debuggable with no development-access bypass. It has not been submitted for publication. Current machine-readable records are `release/amazon-setup-status.json` and `release/production-attempt.json`.

## Production build result

The latest `:app:assembleAmazonRelease` attempt passed the production Amazon configuration and signing-configuration checks, then stopped at `verifyReleaseEvidence`. No production APK was created or uploaded. All **444 activities still await teacher review**. A signing key and a working receipt endpoint cannot approve those activities or replace the remaining reviews.

The following evidence is still required:

| Review | What remains |
|---|---|
| Learning content | A qualified teacher reviews the actual activity pack and records approval of each item. |
| Fire devices and accessibility | Complete the device/OS matrix, offline outage/recovery, performance, voice, keyboard, large-text and screen-reader checks on the exact signed candidate. |
| Amazon billing | Complete the remaining physical outage/recovery and live-store candidate checks; App Tester results alone are partial evidence. |
| Publisher privacy and brand | Review the final privacy/child-directed declarations and product-name clearance. |
| Signing | Confirm a secure backup of the publishing key and approve the exact signed candidate. |
| Languages | Review shipped language content and the translated store copy with native speakers. Marketing translations do not translate the app. |

`release/evidence.json` requires real reviewer identities, dates and hash-bound artifacts. Pending approvals have not been invented or bypassed. See `RELEASE-BLOCKERS.md` for unfinished product scope as well as review requirements.

## Completed setup and partial verification

The Family Premium subscription draft has accepted prices of **£4.99/month and £29.99/year**, saved with GBP as the base and Amazon-calculated prices for other marketplaces. It remains unpublished; no trial is advertised as live. The app-specific Amazon authentication public key and separate sandbox/production receipt keys are configured outside Git.

Both receipt services are deployed on Cloudflare Workers Free hosting, with encrypted secret bindings and separate keys. The latest readiness probes returned HTTP 200 and `ready: true` for both. Readiness probes do not prove production receipt processing.

A physical **KFTRWI / API 28** passed App Tester catalogue, fresh purchase and cache-cleared manual restore through the hosted sandbox. Extended checks cover cancellation, account switching, UK sandbox currency/billing-period labels, signed-lease expiry and clock rollback. Results are in `release/fire-billing-qa.json` and `release/fire-billing-qa-extended.json`; they do not approve a complete Fire/accessibility/billing matrix or establish a live production purchase.

Store localization now includes 176 PNGs and matching editable SVGs across 11 language variants. Nine description profiles and six image profiles are saved and verified in Amazon's unpublished draft. Unsupported profile types remain local. The interface and learning content are English only; native-language review is pending. See `release/amazon-localization-status.json` and `marketing/SUBMISSION.md`.

The portal's **Ready to Submit** badge reflects form/file validation, not production approval. The existing DRM-integration/outdated-SDK notice remains documented in `AMAZON-INTEGRATION.md`; it is not resolved by the localized listing work. The current Amazon draft must remain unpublished until the production requirements are satisfied.
