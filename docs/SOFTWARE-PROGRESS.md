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
