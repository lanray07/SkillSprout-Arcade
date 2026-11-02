# Connected-edition privacy assessment — 2 November 2026

Prepared by Codex under O. Bankole’s instruction to finish the full release. This is a documented engineering assessment and proposed operating position, not independent legal advice, regulator approval or evidence that the publisher personally reviewed every paragraph. Further independent reviews were waived; their outcomes are not fabricated.

The publisher is UK based. Intended audience is 5–11, with Amazon’s 0–12 audience selection. Distribution remains all selected Amazon territories. High privacy defaults apply to every user: no advertising, public chat, microphone recording, location permission, analytics SDK or remote child account. An age or birthdate is not collected. Learning activities remain English; 19 interface variants and an English parent policy are disclosed.

## Processing, necessity and retention

| Processing | Need and alternative considered | Minimized implementation and intended basis |
|---|---|---|
| Local nickname/avatar, age band/year, attempts, assignments and practice estimates | Remember practice and provide parent summaries. A real name, birthdate, cloud account or advertising profile is unnecessary. | Device-private Room/preferences; nicknames encouraged; parent export/delete; Android backup disabled. Parent-requested learning service; no server receives these records. Estimates are not diagnoses or consequential educational decisions. |
| Amazon account/receipt identifiers and product SKU | Validate the adult’s subscription and renewal/cancellation status. Trusting a client flag would enable forged access. | HTTPS, strict request fields, bounded payload/timeouts, Amazon RVS, account-bound signature and deadline. Adult subscription performance; periodic automatic foreground/renewal verification is disclosed. |
| Source IP/network security metadata | Deliver HTTPS and limit abuse. Learning histories and email addresses are unnecessary. | HMAC-derived rate-limit key; no Worker receipt database or application request logging; Workers observability disabled in configuration. Necessary security/service operation interests, with no targeting or sale. Provider security records have their own retention. |
| Signed access cache | Permit offline practice between verification deadlines without storing payment-card information. | Private device cache; lease normally at most six hours, shorter at applicable boundaries; failed requests cannot extend it; clearing storage removes it. |
| Support correspondence | Answer parent-requested help and rights requests. | Ask for app version/model rather than child data. Resolve cases and delete within 90 days or sooner; document justified legal/security exceptions and review dates. Human review process, tracker and mailbox folders set up; actual case deletion is never inferred. |
| Optional exported progress | Parent chooses a destination; a cloud document provider may receive the file. | Explicit Android picker and parent action, with notice that parent controls exported copies. No automatic upload or inclusion in receipt requests. |
| Parent website | Publish support and privacy information. | Static GitHub Pages without analytics/forms/advertising cookies in site code. GitHub has its own network/security processing. |

The operating bases above explain purpose/necessity; they do not represent consent to optional marketing. No consent-based marketing, speech recording or child cloud profile is implemented. Rights requests go to banksmi@mail.com. The publisher cannot retrieve local-only records or delete Amazon’s payment records. The notice distinguishes those limits.

## Child-focused risk assessment

Likelihood and impact are qualitative engineering estimates after the safeguards; unknown facts are not treated as verified.

| Risk | Safeguards and evidence | Residual assessment |
|---|---|---|
| Learning history exposed through remote processing | Receipt request schema accepts identifiers/SKU only. Child data stays local. No external AI, analytics or ads. Bounded exports need parent action. | Low likelihood; exported files/device compromise remain risks. |
| Parent-area disclosure on shared device | Keystore-backed PIN protection, capture protection, relock on background, lockout and asynchronous result/session checks. Representative publisher VoiceView and isolated runtime checks recorded. | Moderate impact if device/PIN is shared; PIN cannot prove legal guardianship or replace a device lock. |
| Unauthorised spending or unfair pressure | Prices from Amazon; explicit parent gate; free starter access; no countdown, public ranking or speed penalty. Manual restore and subscription management information. | Checkout/return acceptance must pass before publication; a PIN is a gate, not identity verification. |
| Persistent account/network identifiers on a child-used tablet | No advertising use; minimum receipt verification; pseudonymous token; providers disclosed; no zero-retention claim. Apply high defaults to all users rather than collecting a birthdate. | Provider processing/overseas access remains an operational dependency; do not describe identifiers as anonymous. |
| Loss of autonomy or excessive practice | Parent-controlled year, app time allowance, bedtime, breaks, hints and retries; no public competition or clinical/educational diagnostic claim. | Parent must choose suitable practice; controls are app-only, not device-wide. |
| Incorrect suitability or translation | All 444 actual review statuses preserved; teacher-reviewed/full-curriculum claims excluded. Machine/assistant interface provenance and English content disclosed. | Independent teacher/native reviews waived; correctness and suitability are not independently certified. |
| Accessibility exclusion | Offline English voice/fallback, no microphone need, representative six-game VoiceView and diagram checks, large text, locale resource/render tests. | Wider devices/languages and physical keyboard coverage remain limited; do not claim universal accessibility. |
| International service-provider processing | Amazon for store/RVS; Cloudflare for verifier; GitHub for public site; mail.com for support. Policy links to current provider notices/terms. | Account-specific contractual application, provider-held retention and individual territory obligations are not independently certified. Do not claim UK-only storage or worldwide legal clearance. |

## Consultation and conclusion

The publisher’s representative parent/game/diagram VoiceView feedback and editorial decisions are recorded in dated release artifacts. External enquiries were sent at the publisher’s direction; a reply, educational endorsement or regulator approval is not inferred. There is no claim of consultation with representative children, a DPO appointment, or an independent legal review.

The engineering design minimizes remote child data and avoids advertising/social/recording risks. Publication still requires passing functional checkout and exact-candidate checks and reconciling the public notice, binary and Amazon declarations. No unmitigated risk has been certified away. Reassess before introducing cloud child records, ads, analytics, microphone features, materially changed entitlement processing or a new provider. Escalate a high residual risk rather than claiming this document authorises it.

## Current primary references

- [Amazon privacy labels](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html): includes SDK/service-provider collection; third-party transfer exceptions include processing on the developer’s behalf. Purchase history, account IDs and IP/network identifiers are mapped separately; payment-card details entered outside the app and never accessible to the developer are excluded.
- [ICO children’s-code DPIA standard](https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/childrens-information/childrens-code-guidance-and-resources/age-appropriate-design-a-code-of-practice-for-online-services/2-data-protection-impact-assessments/): consider children’s rights and risks before launch; unresolved high residual risk needs escalation.
- [Cloudflare DPA](https://www.cloudflare.com/cloudflare-customer-dpa/): describes provider processing and overseas-transfer arrangements; this reference does not prove an account-specific contract was independently audited.
