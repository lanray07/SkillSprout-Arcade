# Privacy policy

Effective 2 November 2026. Covers SkillSprout Arcade’s billing-enabled Amazon edition, purchasing-disabled offline editions and this parent website.

O. Bankole is the UK-based publisher responsible for this app and website. Parents can contact banksmi@mail.com about privacy, access, correction or deletion. The app is designed for children aged 5–11 with a parent-managed area. The publisher does not operate online child accounts.

For children: choose a nickname rather than your real name. Your tablet remembers your games and practice. We do not record your voice or show adverts. Ask a grown-up to change settings, remove your explorer or manage purchases. Buying and restoring access are for grown-ups in Parent area.

Information on your device: local explorer profiles contain a nickname, selected avatar, year or ability setting and a randomly generated local profile identifier. The app stores activity answers, attempts, hints, practice duration, skill estimates, assignments, mission-completion records, selected decorations, progress, time limits and other preferences. A parent PIN protects adult controls. A nickname is sufficient; do not enter a child’s full name.

The app has no advertising, analytics or tracking SDK, public chat, location collection or microphone recording. No external AI service receives answers or homework. Narration uses an installed offline English voice.

Purpose: these records run the games, remember progress, adjust practice difficulty through deterministic rules, display parent summaries and enforce this app’s time controls. Skill estimates describe in-app performance and are not educational or clinical diagnoses. No external AI service receives answers or homework.

Storage and security: records remain in Android app-private storage on the device. Android backup is disabled. Parent credentials use salted PBKDF2 hashing with Android Keystore-backed AES-GCM protection. Device security still matters: use a device lock and keep the operating system updated. Records remain until a parent removes them or clears app storage.

Parent control and deletion: unlock Parent area to view or correct profile settings, export progress, or delete an explorer. Deleting an explorer removes its local progress and assignments. Clearing Android app storage or uninstalling removes this app’s local records and PIN. There is no online PIN recovery. The publisher cannot remotely access or erase records stored only on your tablet.

Exports and imported packs: a parent can deliberately export progress through Android’s document picker. A selected cloud provider may upload the exported file under its own privacy terms. Protect those files and delete unwanted copies separately; deleting a profile does not delete an export. Importing a local content pack does not automatically send child records elsewhere.

Support: if a parent emails support, the publisher receives the sender’s email address and whatever information the parent chooses to include. This is used to answer the enquiry and resolve problems, with the email service processing the correspondence. Please send an app version and device model rather than children’s names or learning histories. Resolved support correspondence is normally deleted within 90 days after resolution, or sooner when no longer needed. A record may be kept longer only for an identified legal or security purpose, with a documented review date. This process covers replies, attachments and other copies under the publisher’s control; provider-held records follow the provider’s own retention practices. Contact support to request deletion or ask about a retained message.

Website hosting: this adult-facing static website has no analytics scripts, account forms or advertising cookies in its code. It is hosted on GitHub Pages. GitHub logs visitors’ IP addresses for security and operates under its own privacy statement, including its arrangements for international processing. Visiting the website is separate from offline app use. External links are subject to their operators’ notices.

Amazon edition: the following receipt processing applies only when Amazon purchasing is enabled. The separate offline edition has no Internet permission and does not verify receipts.

The Amazon edition asks the Amazon SDK for its current app-account identifier, product information and purchase updates when the app comes to the foreground. Existing receipts may be checked automatically on foreground/resume or near the signed access deadline, not only when a parent presses Buy or Restore. Purchase and manual restore require a valid parent PIN session. Amazon handles payment confirmation and payment details under its own terms.

To verify an existing purchase, the app sends the Amazon app-account identifier, receipt identifier and product SKU over HTTPS to the publisher receipt service on Cloudflare Workers. The service sends the account and receipt identifiers to Amazon Receipt Verification Service and checks the returned product and dates. It never accepts learning records, child nicknames, answers, PINs, email addresses, payment-card numbers or speech.

The verifier returns a signed, account-bound access token containing a one-way account hash, product SKU, receipt identifier and time limits. These are pseudonymous identifiers, not anonymous data. The app stores the token privately on the device and enforces its signed deadline. The current service normally issues leases of at most six hours and can shorten them at renewal/cancellation boundaries. A failed check cannot extend access.

The Worker code has no receipt database or application request logging; configured Workers observability is disabled. Request data is processed transiently for verification. Cloudflare processes network metadata including source IP addresses to deliver and protect the service; the rate limiter uses an HMAC-derived client key. Provider infrastructure/security records and international processing are governed by the applicable provider terms. This is not a claim of zero provider retention.

Local child data remains subject to the existing parent deletion/export controls. Clearing app storage removes the local entitlement cache; restoration can obtain a new token from Amazon. Uninstalling the app does not cancel an Amazon subscription. Subscription cancellation/refunds and Amazon-held purchase records are managed through Amazon. Support can answer privacy questions but cannot delete Amazon purchase records or change provider retention unilaterally.

Purchasing-disabled offline editions only: Purchasing is disabled in this build. It does not perform Amazon receipt verification. Narration uses an installed offline English voice and does not record speech.

Processing purposes and bases: the publisher uses the purchase identifiers needed to supply a subscription requested by an adult to perform that subscription contract. Support correspondence is used to answer requested help and resolve problems. The publisher’s interests in operating a secure, reliable service support necessary security and abuse prevention, with the minimum information described above. Local practice records provide the parent-requested learning experience and stay on the device; no advertising profile is created. Contact support to object to processing based on legitimate interests or ask how these purposes apply to your circumstances. Information is not sold or used for targeted advertising.

Providers and international processing: Amazon operates the store, payment and receipt systems; Cloudflare provides the publisher’s verification infrastructure; GitHub hosts the website; the email provider processes support correspondence. These providers may process information outside the UK and your country. Their applicable terms, data-processing arrangements and transfer safeguards govern that processing. Provider privacy notices are linked on the public policy page. The publisher does not claim that provider security logs have zero retention.

Parents may ask about access, correction, erasure, restriction or objection where applicable. The publisher can handle support correspondence but cannot retrieve local-only records; use the app controls for those records. If a UK privacy concern remains unresolved, you may contact the Information Commissioner’s Office. Other applicable regulators and rights depend on your location.

Current policy and updates: https://lanray07.github.io/SkillSprout-Arcade/privacy/

Changes: review this effective date when app or website data practices change. The app is not yet published in the Amazon Appstore. This notice describes data processing and does not assert independent legal certification.
