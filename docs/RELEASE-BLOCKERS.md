# Release gates - not production-ready

The production evidence gate remains blocked. Passing automated tests, a signing key,
a healthy receipt service or Amazon's Ready to Submit badge cannot replace actual
review decisions and exact-candidate QA. These are repository release requirements;
they do not imply Amazon requires a teacher certificate.

## Current approval work

| Gate | Completed preparation / partial checks | Still required |
|---|---|---|
| Educational content | Publisher English editorial review and authorised assistant revisions cover the 444-item development pack. | Relevant teacher suitability review of all required items, final app visuals/narration, corrections and coverage limitations. Publisher claims no teaching qualification. |
| Fire compatibility | KFTRWI/API 28 sandbox catalogue, purchase/restore, cancellation/account changes, outage/expiry/recovery and partial app lifecycle checks. | Wider supported device/OS coverage, low-memory/performance/battery and exact signed-candidate testing. One tablet does not establish the matrix. |
| Accessibility | Publisher-confirmed parent and six-game VoiceView flows; six exact diagrams; PIN error/lockout/recovery; 150% text in both orientations; menus/delete-Cancel/return focus. Actual missing-engine/TTS ERROR and parent/six-game component tests pass in separate offline Voice QA. New narration controller also passes headless physical engine restoration and foreground rebind. | Physical normal-app missing-voice navigation/persistence, automatic billing-error announcement timing, hardware keyboard (publisher has none), wider devices and exact signed-candidate checks. Automated semantics cannot prove spoken output. |
| Amazon billing | Parent-gated purchase/restore, starter/Family enforcement, signed cache and sandbox physical lease lifecycle checks; separate deployed sandbox/production verifiers. | Approved live products and live-store/exact-candidate validation. No paid transaction without an agreed test and spending limit. |
| Privacy | Engineering data-flow review, draft connected notice, support alias/folders, manual 90-day resolution-date process and monthly review reminder configured. | Final lawful-basis/DPIA, provider retention/transfers, child/parent transparency, rights/escalation process, actual launch jurisdictions and Amazon declarations. Operational setup is not legal certification. |
| Brand | Publisher retains SkillSprout Arcade pending clearance; related education uses and one limited UK register query documented. | Appropriate exact/similar-mark and unregistered-use assessment for relevant goods/services and actual launch territories, plus asset/licence review. No global clearance inferred. |
| Signing | Encrypted external backup, saved-file verification and five-file recovery comparison passed; publisher confirms separate backup and passphrase retention. | Exact final production APK certificate/hash and candidate QA. No new backup confirmation needed unless the arrangement changes. |
| Listings/languages | Eleven other locale packages and saved Amazon draft profiles exist, including development image renders. | Fluent review of exact text/images and truthful English-only disclosure, final Fire/phone captures and layout checks. Arabic/Hindi lack Amazon slots; some locales use default-image fallback. |
| Hosted support/privacy | GitHub Pages support/privacy navigation verified; support URL available. | Connected-edition notice and publisher declarations approved before billing-enabled launch; official Amazon link after publication. Public offline-edition notice is not approval of connected data flows. |

Review requests have been initiated; no external findings or reviewer approvals have
been recorded. Free/volunteer eligibility, availability and scope are not guaranteed.
See `release/free-reviewer-routes-2026-10-22.json`. Correspondence and completed reviews
remain private outside Git.

## Known product scope limits

- The learning library is English only. Years 3-6 share a seed passage; warm-up
  questions and year tags do not establish full curriculum progression or sufficient
  older-year depth. Phonics/dictation and further content variety remain unfinished.
- Signed local content import, atomic replacement and downgrade protection exist;
  a hosted content catalogue and download UI do not.
- Interface localisation tooling and draft store translations do not establish a
  fully localised app or language-specific educational packs.
- Current marketing images are development renders. Replace them with approved
  final UI/device captures, including a dedicated game-selection image where needed.
- The public website remains a noindex development preview. Product polish and
  further production art/animation are described in `SOFTWARE-PROGRESS.md`.

## Evidence and next steps

Run `python tools/review_content.py validate` for structural content checks and
`python tools/verify_release.py` for the approval gate. The latter must fail while
required records or bundled activities remain unapproved. No synthetic approval,
production PIN bypass or debug screenshot can supply missing evidence.

The Amazon draft still contains only the publisher-signed code-4 review APK with
payments disabled; no production APK has been built or uploaded. Current build facts
are in `PRODUCTION-BUILD-STATUS.md` and `release/production-build-check-2026-10-22.json`.
Prices are draft settings, not proof of an approved live subscription.

Use `release/review-kit/review-plan.json` for actions and `release/evidence.json` for
actual gate status. Each eventual approval must identify a real reviewer, date,
review scope and exact artifact/hash. Corrected artifacts require appropriate
re-review. Partial historical checks remain bound to the original APK/version.

Detailed results (including earlier failed attempts and limitations) remain in:

- `release/fire-billing-qa.json`, `fire-billing-qa-extended.json`,
  `fire-billing-outage-restore-2026-10-22.json` and `fire-readiness-2026-10-22.json`.
- `release/fire-parent-accessibility-2026-10-22.json`,
  `fire-voiceview-2026-10-22.json`, `fire-voiceview-publisher-2026-10-22.json`,
  `voiceview-learning-checklist.json` and `fire-voiceview-diagrams-states-2026-10-22.json`.
- `release/signing-backup-verification.json`, `support-retention-policy.json`,
  `privacy-decision-workbook.json` and the brand preliminary/register reports.

Generate the handoff with `python tools/prepare_reviewer_handoff.py`; extract its ZIP
and read START-HERE.txt. It contains public inputs and unreviewed forms, not private
completed reviews. Use dedicated debug QA data and restore tablet settings after
physical checks; see `FIRE-ACCESSIBILITY-TESTING.md`.

23 October device update: updated-preview Premium controls and offline sandbox restore feedback pass the publisher listening/navigation checks described in `release/fire-billing-accessibility-2026-10-23.json`. All temporary settings were restored. Separate offline QA physical missing-engine component tests also pass; see `release/fire-missing-voice-2026-10-23.json`. Normal-app missing-voice navigation/persistence, automatic announcement timing, keyboard, wider devices and final production-candidate checks remain open.


The exact outstanding gate checklist is
`release/outstanding-approvals-2026-10-23.json`: it names the required reviewer,
remaining scope and evidence fields for each of the eight Pending gates. The
additional bundled-activity check remains blocked for all 444 unapproved items.
Enquiry acknowledgements or referral offers from ICO, an IP clinic, a teacher
association or a language-review scheme are not review decisions. They do not
need to be described as official certificates; retain actual scoped findings and
publisher/reviewer decisions instead. Exact APK signature/hash and final device,
accessibility and live-store acceptance necessarily need a real candidate; do not
claim those results in advance or reuse a development preview as that candidate.
The current build gate remains conservative and has not been relaxed.


Development can continue while review decisions are pending. New desktop
ArcadeApp/Room integration coverage now verifies parent controls saved to disk,
background relocking and persisted values after database/app composition reopen
in four configurations; see `release/parent-controls-persistence-2026-10-23.json`.
This supplements the physical component checks and does not close the physical
accessibility or final-candidate gate.


Offline narration now retries on foreground entry, checks successful installed
local English voice selection and handles initialization/synthesis failures and
stale callbacks. Both 101-test app suites pass, along with the 13 adapter tests and
zero-error lint. On KFTRWI/API 28 the same controller recovered from actual missing
engines after restoration, then released/rebound its client. No speech was emitted,
PIN/profile opened or base app APK changed; temporary device settings were restored.
Details: `release/narration-software-2026-10-23.json` and
`release/narration-recovery-2026-10-23.json`. This is additional software/physical
partial evidence; audible narration and VoiceView must be rechecked on the candidate.


24 October export update: progress reports now use one in-memory snapshot, busy
request protection, UTF-8 background I/O and parent-only choosing/saving/result
feedback. Missing/denied/null destinations and partial write/flush/close failures
are handled; cancellation and missing snapshots after recreation do not silently
save an empty report. Both 118-test app suites pass. Two physical private-file
ContentResolver checks pass on KFTRWI/API 28, with temporary test files removed and
base APK/device settings unchanged. See `PROGRESS-EXPORT.md` and the two 24 October
export reports. External picker/provider UI, VoiceView speech and final candidate
acceptance remain outside these checks; approval statuses remain unchanged.


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


25 October bounded-pack follow-up: stored content and metadata now use bounded
stream reads before allocation/parsing. Oversized optional files are skipped while
valid packs remain available. Starter assets and AtomicFile recovery/replacement
reads are bounded too; file selection shares the reader with cancellation checks.
Signature, digest, namespace, review-status and version-ledger rules are unchanged.
See `BOUNDED-PACK-READS.md` and the dated bounded-pack reports. Per-file limits do
not establish total-library memory/performance or full startup/candidate acceptance;
no content or production approval status is changed.
