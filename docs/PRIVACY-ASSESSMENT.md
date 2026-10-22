# Draft privacy and child-safety assessment

Date: 20 October 2026. Publisher: O. Bankole. Parent contact: banksmi@mail.com. This is an engineering assessment and draft notice, not independent legal certification. The online-service applicability, territorial scope, controller identity/address, legal bases, retention schedule and rights response process need publisher/legal review before launch.

21 October update: the assessment below describes the offline build. The billing-enabled Amazon variant additionally performs foreground SDK account/purchase refresh and sends account/receipt/SKU identifiers through Cloudflare Workers to Amazon RVS; see `release/review-kit/privacy-production-draft.json` for the concrete proposed sections and unresolved publisher decisions. The current live notice is still the offline notice and has not been silently expanded or approved. Finalise the connected notice, provider/retention/territory assessment and store privacy declarations before enabling that variant. The uploaded review-only APK still has purchasing disabled.

## Data inventory

| Data | Purpose | Storage / disclosure | Erasure |
|---|---|---|---|
| Nickname, avatar, chosen year | Distinguish shared-tablet explorers | Private Room storage; no automatic upload | Parent deletes explorer |
| Answers, correctness, hints, duration, skill | Local learning history and estimates | Private Room; parent CSV export only | Parent deletes explorer |
| Assignments and repetition metadata | Parent-directed practice | Private Room | Parent deletes explorer |
| Daily allowance and used time | App-only time controls | Private preferences | Explorer deletion / app storage clear |
| Salted PIN verifier | Protect parent functions | AES-GCM encrypted preference; Keystore key | App storage clear; no remote recovery |
| Pack files and locale | Offline content | App-private files | Parent removes optional packs |
| Narration text | Read instructions | Installed offline TTS engine | No audio recording in app |
| Future store receipts | Family entitlement | Not collected by this APK | Future integration review required |

The database relies on Android application sandboxing and device encryption rather than claiming application-level database encryption. Keystore availability and behaviour need device testing. PINs have limited entropy: random salts, slow hashing and persisted throttling reduce risk but cannot defeat an attacker controlling a rooted tablet. Parent operations are unavailable until authentication; backgrounding ends the unlocked session.

## Child-oriented defaults

No advertisements, tracking SDKs, public communication, public content, photographs, precise location, contacts or real-name requirement. No leaderboards, loot boxes, random paid rewards or gameplay purchases. Mistakes do not deduct stars. Sessions finish after a short set and offer a calm stopping point. All teaching content is identified as development content pending teacher review. No diagnosis or guaranteed outcome is claimed.

Microphone recognition is disabled and permission is absent. No external speech recognition service can be selected by the app. Installed offline English TTS is optional; all text and answers remain available by touch and keyboard. External links, email and web support are on the adult-facing website, not in child gameplay.

## Applicable frameworks to assess

- [UK GDPR children’s information guidance](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/): determine legal bases and parent/child rights for any processing that is within scope, including future commerce or hosting.
- [ICO Children’s Code](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/age-appropriate-design-a-code-of-practice-for-online-services/): best interests, high privacy defaults, data minimisation, transparency, parental controls and harmful engagement design. Complete a publisher-owned DPIA before any connected child feature.
- [FTC COPPA guidance](https://www.ftc.gov/business-guidance/privacy-security/childrens-privacy): assess US availability and any collection, including future service providers. Do not assume offline design itself establishes legal compliance.
- [Amazon child-directed app policy](https://developer.amazon.com/docs/policy-center/privacy-children.html): complete current audience, data-safety and policy declarations in the actual Developer Console. Store approval is not legal certification.

## Risks and required decisions

| Risk | Current mitigation | Release follow-up |
|---|---|---|
| Child edits parent settings | PIN gate, throttling, no reset link, parent session ends on background | Physical-device navigation/intent/rotation/accessibility bypass tests |
| Wrong educational material | Structured metadata, validation, visible development label | Real teacher review and complete age/ability QA |
| Progress seen by sibling | Per-profile data; optional parent-only switching | Decide family switching default and privacy explanation |
| Parent exports sensitive history | Explicit SAF export in gated area, CSV formula escaping | Explain downstream retention in final notice |
| Content tampering | Size/schema/checksum validation | Publisher signatures before public downloads |
| PIN lost | No insecure recovery mechanism | Parent support guidance; clear-storage data loss notice |
| Time controls bypassed by clock change | Monotonic charging; persisted ticks; backwards date protection | Physical Fire clock-change, reboot and power-loss tests |
| Website hosting logs | No analytics, forms or cookies in source | Review hosting provider, IP logs, retention and final notice |
| Future Amazon billing | Isolated and inactive | Receipt-service and Amazon SDK data-flow assessment |

Do not send children’s nicknames, answers or speech to a translation provider. The translation script accepts interface resources only and runs at development time. Language-specific teaching packs require separate educators and test evidence.


22 October engineering follow-up: `release/privacy-asset-engineering-audit.json` binds the checked sources and three existing APK samples. Main manifest disables Android backup and cleartext transport; sampled APKs contain no microphone/location/contact/advertising-ID permission or private signing filenames. These checks do not establish all SDK practices or legal compliance. `release/review-kit/privacy-production-draft.json` now includes a separately composed Amazon-edition draft, avoiding offline-only no-Internet/no-payments claims. The public offline notice is unchanged. `release/privacy-decision-workbook.json` records flows, mitigations and still-pending publisher decisions; no legal basis, retention schedule, DPIA or store declaration has been approved.

Current primary references checked: [Amazon privacy labels](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html), [Amazon child-directed policy](https://developer.amazon.com/docs/policy-center/privacy-children.html), [ICO child-focused DPIAs](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/age-appropriate-design-a-code-of-practice-for-online-services/2-data-protection-impact-assessments/) and [Cloudflare DPA](https://www.cloudflare.com/cloudflare-customer-dpa/). Amazon requires developers to assess applicable SDK data transfers as well as their own app code; the engineering observations do not replace that assessment.


Publisher decisions on 22 October: keep the existing all-countries Amazon availability scope and the SkillSprout Arcade name pending clearance; accept normal support-email deletion within 90 days after resolution, earlier when no longer necessary, with documented necessary legal/security exceptions. The operational policy and implementation checklist are in `release/support-retention-policy.json`. No mailbox messages were read, changed or deleted, and mailbox implementation has not been verified. The public offline notice is unchanged; the connected-edition draft incorporates the accepted retention wording. Acceptance of one policy is not full privacy/DPIA or worldwide legal approval.

Support retention setup completed 22 October 2026: support alias and three empty folders verified; publisher confirms Unlimited storage/no automatic deletion. Private tracker ready and monthly human-review reminder active at 09:00 on day 1. No mailbox messages opened, moved or deleted. Ongoing case handling and final provider/territory privacy assessment remain pending publisher/reviewer work; overall privacy gate unchanged.
