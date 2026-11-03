## Resubmitted to Amazon - 3 November 2026

Amazon confirms **SUBMITTED / App Submission Successful** for **1.0.1 / code 6**, SHA-256 `65624cd7850867f60b453f2c354f8f4d4eb235e3ac0141648c036baeaef546e4`. The corrected APK has been resubmitted for review; it is not approved or live. Confirmation: `release/amazon-resubmission-2026-11-03.json`.

Renewed scoped technical acceptance covers signature/configuration, exact-APK six-game starter journeys and parent relock, supported sandbox monthly/annual checkout/automatic return/signed restore, automated localization assets and public privacy/store disclosures. The withdrawn code-5 record is preserved separately. Native TTS callback/audio-buffer evidence is not a human listening claim.

The earlier sideloaded no-offers test used unsupported DevTest on a Fire tablet and cannot establish an application defect. Amazon-distributed LAT checkout/fulfillment remains pending (Test 1 submitted), explicitly disclosed in the saved 2,713-character reviewer instructions. No paid production purchase or independent teacher/native-language/legal/brand approval is claimed. Existing publisher waivers remain unchanged. Amazon's displayed 8 November estimate is not a guarantee. The sections below are historical checkpoints, superseded by this submission record.

## Fire billing test route corrected - 3 November 2026

Amazon's DevTest FAQ explicitly excludes Fire tablets: purchasing calls on them fail. DevTest registration has now succeeded, but waiting for it was the wrong path for KFTRWI. The previous Pending Action/no-offer result is retained as an actual failed test, not evidence of successful production billing.

The corrected 1.0.1/code 6 APK has been submitted to Amazon Live App Testing (Test 1, status Test submitted) using the previously verified tablet tester group. LAT supports Fire tablets and zero-cost test IAP through production services. This is a test distribution, not public resubmission or store approval. See `release/lat-remediation-2026-11-03.json`. The draft production binary remains unchanged while Amazon-distributed checkout and fulfillment are verified.

Sources: [DevTest FAQ](https://developer.amazon.com/docs/app-testing/appstore-devtest-iap-faq.html), [Prepare App Testers](https://developer.amazon.com/docs/app-testing/prepare-app-testers.html).

## Rejection remediation - 3 November 2026

Amazon rejected 1.0.0/code 5 (report 44808749) for silent Read Aloud and non-responsive IAP controls. The old exact-candidate acceptance is withdrawn; the app is not published. The signed 1.0.1/code 6 replacement (SHA-256 `65624cd7850867f60b453f2c354f8f4d4eb235e3ac0141648c036baeaef546e4`) is uploaded as Production101 in the Amazon draft, not resubmitted.

Read Aloud now follows actual offline voice readiness and profile volume; unavailable speech has a visible warning instead of an active silent button. Native Fire tests observed speech start/completion and non-empty audio buffers for Maths Dash and Spelling Sprint. This is playback-callback evidence, not a human listening assessment. Amazon's listener now registers in Activity.onCreate, offers require both a valid account and product metadata, duplicate purchase taps are blocked and missing callbacks time out with translated recovery guidance. Authenticated Amazon publishing/LAT receipts are accepted by the deployed verifier while invalid bindings and inactive receipts remain rejected.

Sandbox monthly and annual checkout, automatic return and cache-cleared signed restore pass after the final account-readiness fix. The exact final signed APK passes fresh synthetic setup, all six starter journeys, hints/feedback/next/pause/resume, parent billing controls and background relock on KFTRWI/API28. Synthetic production data was cleared and temporary wake/sandbox settings restored. Shared JVM tests: 226, zero failures, one expected skip; startup regressions: 3; billing library: 13; tooling: 46; website: 4; receipt service: 29. All 19 interface variants and 114 localized exports pass automated checks. Amazon DevTest certificate registration currently shows Registration is in progress. The exact-APK production checkout test cannot find Amazon-priced offers and is blocked. Registration delay has not been established as the sole cause. No paid order was confirmed. Await activation, investigate/retest both offers, then renew exact-APK acceptance before resubmitting. See `release/remediation-progress-2026-11-03.json`. Previous submission/review and broader review-waiver records below remain historical. See release/amazon-rejection-2026-11-03.json and the dated remediation reports.

## Submitted to Amazon — 2 November 2026

Amazon confirmed **SUBMITTED / App Submission Successful** for **1.0.0 / code 5**, SHA-256 `41f7c001f67c3238be16ca18f99932226a56234a0d9e9cca53050bce92773c75`. Signature, alignment and actual production constants passed inspection. The exact signed APK passed all six starter-game journeys, hints, correct feedback, next/pause/resume, protected PIN setup, billing controls and background relocking on KFTRWI/API28. Synthetic data was cleared afterward; the previous development installation was removed under the publisher’s delegated choice. Tablet stay-awake and sandbox settings were restored.

Sandbox checkout/automatic return/signed restore passed separately. Family Premium was promoted from TEST and shows Approved. Twelve description and eight available localized image profiles are complete; all 19 interface variants have local exports. Educational activities/narration remain English. Independent teacher/native-language/brand/legal review was waived by the publisher, not granted. Physical keyboard, live paid checkout and full device/accessibility coverage remain untested.

The app awaits Amazon review and is not yet approved or live. Amazon’s estimated date is 7 November 2026 at 13:30 PST, not guaranteed. Evidence: `release/amazon-submission-2026-11-02.json`, `release/candidate-acceptance.json`, `release/production-fire-acceptance-2026-11-02.json`. Entries below are historical and superseded.

## Final candidate uploaded — 2 November 2026

Version 1.0.0 / code 5, SHA-256 `41f7c001f67c3238be16ca18f99932226a56234a0d9e9cca53050bce92773c75`, is signed and uploaded to the Amazon draft as Production100. The subscription was promoted from TEST and shows Approved. Twelve description profiles and eight available localized image profiles are complete. Public connected privacy disclosures are deployed. Exact-candidate device acceptance is still in progress; the app has not been submitted. This paragraph supersedes candidate hashes/status below; older evidence remains historical.

# Latest production candidate â€” 2 November 2026

A publisher-signed 1.0.0 / code 5 candidate now exists. Actual DEX inspection confirms production receipt verification, purchasing enabled, development access/content disabled and the production entitlement key. Full debug tests: 222 total, zero failures, one intentionally release-only check skipped; both release-policy tests pass separately. Sandbox checkout and fresh restore pass in the isolated QA installation. There are 114 exports with real localised controls across 19 interface variants. Twelve Amazon description profiles are updated; image replacement and exact-candidate Fire acceptance are in progress. The app is not submitted. See `release/production-candidate-progress-2026-11-02.json` and `RELEASE-CANDIDATE.md`.

## Historical status before candidate work

# Amazon draft and build status â€” 2 November 2026

The latest language/device increment is recorded in `release/production-language-checks-2026-11-02.json`; the earlier `release/submission-readiness-2026-11-02.json` is historical.

Nineteen interface variants now ship in Android resources with a parent-only selector, device detection, English fallback, RTL/plural checks and a bundled parent privacy disclosure. All 19 resource variants pass a direct KFTRWI/API 28 check after correcting Indonesian's Android folder mapping. This is resource validation, not a native-speaker or audible VoiceView review. Learning activities and narration remain English. Independent language review was waived, with no human approval invented.

Amazon has approved Family Premium in TEST only (2 November); no live subscription or app submission is implied. See `release/amazon-iap-test-2026-11-02.json`. A separate Billing QA application avoids the publisher's saved profiles and PIN; direct App Tester evidence is distinct from LAT enrolment.

Direct Fire checks now pass for the two sandbox offers and closed-parent-session API gate, fresh signed receipt restoration, capture protection, and Arabic language/recreation/parent-relock semantics. These are recorded separately from the checkout run, which timed out after Subscribe and is not accepted as a passing checkout/automatic-return test. The original app APK, test catalogue, sandbox property and temporary stay-awake setting were restored/verified. No real PIN/profile was read and no live purchase was made. Current lint has zero errors (219 debug / 220 Amazon-preview warnings); the final focused regression suites pass 13 tests each, alongside the earlier full 218-per-variant suites and 13 adapter tests. Tooling has 42 passing tests, website 4 and receipt service 26.
The parent Premium disclosure now explains automatic foreground/renewal receipt
checks, Cloudflare network processing and the local data excluded from verification.
The public offline-edition notice includes the publisher's accepted 90-day support
retention policy. This does not make the connected-edition notice effective.

Ten controlled runtime tests pass on the separate Voice QA Fire installation;
the original app and checked device settings are unchanged. They use generated
storage and synthetic PIN vaults and do not verify the new disclosure UI, real
credentials, production purchases or the final candidate. Both hosted receipt
services pass readiness and fabricated-invalid-receipt probes; no valid production
purchase was attempted. Current app suites/builds/lint and exact hashes are in the
dated readiness report. Historical results remain in their original reports.

The publisher waived further optional external reviews, then explicitly retained
the full multilingual/subscription product scope and requested completion within
the available time. This waiver does not establish completed reviews or product
features. Final translated layout/store-asset acceptance, the connected privacy notice and consistent
store disclosures, live product/final-candidate billing verification, final signed
APK acceptance and matching final screenshots are still unfinished. The existing
repository gate remains blocked and no production APK has been created/uploaded.

The Amazon draft contains the publisher-signed **review-only** APK, **0.2.2-dev-review / version code 4**, labelled `ReviewOnlyDoNotPublish`. Its SHA-256 is `5db196bfbf919cf099cba1552c13f441bc59b54378b1fe106e857e2cc7c821d1`. Purchasing is disabled, development content is explicitly disclosed, and the build is non-debuggable with no development-access bypass. It has not been submitted for publication. Current machine-readable records are `release/amazon-setup-status.json` and `release/production-attempt.json`.

## Production build result

The latest `:app:assembleAmazonRelease` attempt passed the production Amazon configuration and signing-configuration checks, then stopped at `verifyReleaseEvidence`. No production APK was created or uploaded. All **444 activities retain their actual unapproved review status**. The publisher waived optional external reviews; the strict bundled-pack release policy and pre-build/final-candidate gate still need coherent software changes to honour that waiver without enabling unsigned imports or inventing approval.

The following evidence is still required:

| Review | What remains |
|---|---|
| Learning content | Preserve actual statuses and avoid teacher-reviewed claims. Implement a source-bound bundled-pack release policy consistent with the publisher waiver; imported packs must remain authenticated. |
| Fire devices and accessibility | Complete wider device/OS, performance, missing offline voice, physical keyboard and exact signed-candidate checks. Representative VoiceView, six exact diagrams and 150% text/rotation pass on the earlier preview. |
| Amazon billing | Sandbox signed-lease outage/expiry/recovery passes; complete live-store and exact-candidate checks. App Tester results remain partial evidence. |
| Publisher privacy and brand | Review the final privacy/child-directed declarations and product-name clearance. |
| Signing | Verified encrypted backup and separate backup/passphrase retention are confirmed. Validate the exact signed candidate certificate/hash and QA. |
| Languages | Complete translated layout/accessibility checks and final store assets. Independent language review was waived; machine translation is not native-speaker approval. |

`release/evidence.json` requires real reviewer identities, dates and hash-bound artifacts. Pending approvals have not been invented or bypassed. See `RELEASE-BLOCKERS.md` for unfinished product scope as well as review requirements.

## Completed setup and partial verification

The Family Premium subscription draft has accepted prices of **Â£4.99/month and Â£29.99/year**, saved with GBP as the base and Amazon-calculated prices for other marketplaces. It remains unpublished; no trial is advertised as live. The app-specific Amazon authentication public key and separate sandbox/production receipt keys are configured outside Git.

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


25 October PIN-submission follow-up: the gate captures immutable PIN/confirmation
values and disables editing/submission while busy. UI validation matches the existing
6â€“12 ASCII-digit vault contract; unsupported input cannot consume an attempt.
Vault operations and the single post-failure lock query run off the UI thread.
Escaping service errors keep the gate closed with generic polite feedback;
cancellation propagates and fields clear in finally. ParentSecurity.kt, stored
credentials, crypto and lockout policy are unchanged. See `PIN-SUBMISSION.md` and
the dated reports. Physical runtime fixtures use synthetic in-memory vaults only;
actual vault, UI/VoiceView and final-candidate acceptance remain pending.


25 October bounded-pack follow-up: stored content and metadata now use bounded
stream reads before allocation/parsing. Oversized optional files are skipped while
valid packs remain available. Starter assets and AtomicFile recovery/replacement
reads are bounded too; file selection shares the reader with cancellation checks.
Signature, digest, namespace, review-status and version-ledger rules are unchanged.
See `BOUNDED-PACK-READS.md` and the dated bounded-pack reports. Per-file limits do
not establish total-library memory/performance or full startup/candidate acceptance;
no content or production approval status is changed.
