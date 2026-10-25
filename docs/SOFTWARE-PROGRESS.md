# Software update — 0.2.0 development

Implemented locally on 20 October 2026:

- Durable, idempotent mission completion in Room, with unique run records. Repeated challenges in small packs get distinct first-submission IDs. Late attempt/assignment writes cannot restore records for an erased profile.
- Non-destructive database migration from version 1 to 2, preserving profiles and practice while adding narration volume, decoration selection and completion events. Exported schema is checked in.
- Parent-controlled narration volume, direct volume preview, missing offline voice feedback, nickname/avatar editing, compact game headers and wrapping phone navigation.
- Visible explorer avatars, practice-earned selectable decoration badges and seven-day family summaries. Rewards never spend stars or affect difficulty. English reward/group counts use plural resources.
- Authenticated offline pack imports using a pinned publisher key, payload digests, namespace checks, atomic replacement/recovery and durable version downgrade protection. Signature checks never substitute for teacher approval. Import validation runs off the UI thread.
- Receipt verification service, Android HTTPS client and stronger signed-access caching. Production/sandbox, account, receipt, lease expiry and cancellation boundaries are checked. The offline APK remains isolated. The separate Amazon variants now connect the adapter to a PIN-gated price/restore UI and enforce starter/verified Family access, with external configuration required.
- Expanded native resource coverage, positional template preservation, Android escaping and support for reviewed locale-specific plural quantities. An English (US) baseline is explicitly pending regional review; no new locale is shipped or advertised.
- GitHub verification workflow, reproducible local non-Android checks and a handoff report derived from actual APK metadata, JUnit results and lint output.

The offline debug APK remains offline, has no microphone or Internet permission, and cannot charge users. A separate Amazon preview includes INTERNET for receipt verification, uses sandbox-only configuration and remains unavailable without the external store assets/settings. Publisher signing and content-signing private keys stay outside Git. See `QA-REPORT.md` and `artifacts/BUILD-REPORT.json` for the actual check results, rather than treating this implementation list as production approval.

This update also adds 48 exercises, six new passages, revised spelling clues and acyclic skill prerequisite metadata; see `CONTENT-UPDATE.md`. Game/district/skill/mastery labels are resource-backed, weekday/accuracy rendering uses the display locale, and language export binds review to both source and target digests.

Remaining work includes broader curriculum/content authoring and teacher approval; physical Fire/accessibility/voice testing; actual store SKUs/authentication assets, deployment and real-device purchasing validation; network catalogue delivery; requested translations and human language review; additional game/art polish; and publisher privacy/brand review. These are still open in `RELEASE-BLOCKERS.md`. No production APK was uploaded to Amazon as part of this software update.

Build variants and external Amazon configuration: `AMAZON-INTEGRATION.md`. No live products, prices, billing credentials or reviewer approvals have been invented.

Latest local verification: 93 tests passed; zero lint errors in both variants. Offline lint has 71 warnings; Amazon preview has 70. Both development APK signatures/alignment were checked; neither is a production submission.

21 October update: configured the real Amazon public authentication key locally, prepared parent/monthly/annual IAP drafts, corrected term-to-parent receipt SKU handling, and uploaded a production-key-signed but explicitly review-only APK to the submission draft. Version 0.2.1-dev/code 3 has 97 passing checks. Production approval and billing deployment remain pending; see PRODUCTION-BUILD-STATUS.md.

Receipt service deployment preparation (21 October 2026): 19 service tests pass, with strict three-field requests, file-based secret configuration, overload handling, health checks, graceful shutdown and a non-root Docker/Compose package. Sandbox/production P-256 keys are stored outside Git under restricted ACLs. Container runtime verification is added to CI. No live deployment or Amazon receipt has been verified.

Cloudflare Free-plan hosting deployed on 21 October 2026: separate sandbox/production endpoints and signing keys, encrypted secrets, rate-limit bindings, logs disabled, and live invalid-receipt denial checks passed. All 24 service tests and workerd runtime checks pass. A runtime redirect incompatibility was fixed while preserving redirect refusal. The production app configuration validates, while the uploaded review APK and human release gates remain unchanged.


21 October production-readiness follow-up: added four HTTP-verifier and signed-cache outage/recovery regression tests (13 billing-library tests pass). They cover request fields/timeouts, redirect and failure statuses, malformed/oversized responses, bounded offline access and signed recovery. The transport is controlled; physical network outage and live-store flows remain pending. Both hosted services returned ready/200 to Node-fetch probes. A new production build attempt passed Amazon configuration and signing checks, then stopped at the review-evidence gate. Updated `PRODUCTION-BUILD-STATUS.md`, README and signing instructions to reflect code 4, deployed receipt services, partial physical sandbox evidence and localized draft listings. No production runtime flags, approvals or Amazon APK were changed.


23 October narration recovery: returning to the foreground now rechecks narration
availability, allowing recovery after offline voices are enabled without requiring
a full app restart. Voice selection requires success, prefers installed offline UK
English and falls back to another offline English voice. Background clients are
released, stale callbacks ignored, and failed synthesis clears readiness for a
later retry. Muted/blank/non-finite requests remain silent. No new permissions,
remote narration, logging or profile/parent-control changes were introduced.
Both 101-test app suites and headless physical missing-engine recovery pass; see
`NARRATION-RECOVERY.md` and the two hash-bound narration reports. Audible output,
VoiceView timing and final production-candidate approval remain pending.


24 October progress export: file destination, permission and write/flush/close
failures are handled with parent-only feedback. Export holds one memory-only
snapshot, prevents overlapping requests, writes UTF-8 off the UI thread and handles
picker cancellation, missing state after recreation and stale owner completion.
A failed chosen file can be incomplete; retry uses a fresh snapshot/destination.
Both 118-test app suites and private-file Fire success/failure/retry checks pass.
No new permission, remote service, persisted CSV snapshot or personal-data test
was added. Details: `PROGRESS-EXPORT.md` and the 24 October export reports.


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
6–12 ASCII-digit vault contract; unsupported input cannot consume an attempt.
Vault operations and the single post-failure lock query run off the UI thread.
Escaping service errors keep the gate closed with generic polite feedback;
cancellation propagates and fields clear in finally. ParentSecurity.kt, stored
credentials, crypto and lockout policy are unchanged. See `PIN-SUBMISSION.md` and
the dated reports. Physical runtime fixtures use synthetic in-memory vaults only;
actual vault, UI/VoiceView and final-candidate acceptance remain pending.
