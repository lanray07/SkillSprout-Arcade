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
| Accessibility | Publisher-confirmed parent and six-game VoiceView flows; six exact diagrams; PIN error/lockout/recovery; 150% text in both orientations; menus/delete-Cancel/return focus. Actual missing-engine/TTS ERROR and parent/six-game component tests pass in separate offline Voice QA. | Physical normal-app missing-voice navigation/persistence, automatic billing-error announcement timing, hardware keyboard (publisher has none), wider devices and exact signed-candidate checks. Automated semantics cannot prove spoken output. |
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
