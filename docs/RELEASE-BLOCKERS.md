# Release gates — not production-ready

This build is not ready to submit or sell. A compiled debug APK and passing automated checks do not close the following gates.

| Gate | Evidence required |
|---|---|
| Educational content | Review 396 development items; author a complete, age-appropriate approved library and prerequisite graph. Year 5–6 depth, story variety, shape/sorting reasoning, phonics/dictation and measurement coverage are incomplete. Store teacher approval only with actual reviewer identity, qualification, timestamp, pack digest and evidence. |
| Amazon integration | The publisher console draft exists. The isolated adapter now has an HTTPS verifier client, environment/account-bound signed cache and tests; `billing-service` implements RVS verification and bounded signed leases. Authentication PEM, actual SKUs, parent purchase UI, production deployment, entitlement enforcement and real cancellation/expiry/restore/offline tests remain pending. The offline app does not consume the adapter. |
| Monetisation access policy | Replace unrestricted development access with the actual free tier (one profile, restricted starter selection) and verified Family Premium. No price or seven-day trial claim until store configuration supports it. |
| Fire compatibility | Physical 7–10-inch Fire devices and supported Fire OS versions; missing TTS engine; low-memory/performance/battery; keyboard, rotation, parent gate, app restarts, process death, reboot and time tampering. No physical device is connected. The emulator fails to run because acceleration is unavailable. |
| Accessibility | Narration volume/preview, missing-voice feedback and wrapping compact controls are implemented. TalkBack exploration, focus order, hardware keyboard, large-font/contrast review of every state and physical-device validation remain pending. Native controls have 48dp+ targets; this is not a complete accessibility audit. |
| Localisation | Direct/template labels, parent/PIN notifications and game feedback have substantially migrated to resources. Audit remaining constructed labels, diagnostics, number/plural formatting and layout; run the actual translation API, review each requested language and separately review educational packs. Pending languages must not be advertised as released. |
| Content delivery | Publisher signatures, atomic offline replacement, backup recovery and persistent downgrade protection are implemented and tested; see `CONTENT-DELIVERY.md`. A hosted HTTPS catalogue and network download UI remain absent. |
| Product polish | Practice-earned selectable decoration badges, visible explorer avatars and seven-day multi-child summaries are implemented. More gameplay variety, district environments, mascot animation states, production art and richer individual plans remain further work. |
| Marketing capture | Eight real-device Fire and phone captures with approved content and final UI; replace JVM preview captures. Validate each locale and current console rules. Screenshot #6 currently needs a dedicated game-selection capture. |
| Brand and signing | Production RSA-4096 publishing identity and Gradle signing configuration are now provisioned locally; see `PRODUCTION-SIGNING.md`. Secure backup, exact signed-candidate QA and name/trademark clearance remain pending. No clearance is implied. |
| Legal and privacy | Publisher-reviewed final privacy notice, retention/rights procedures, applicability assessment, data-safety declarations and child-directed store policy review. |
| Website | GitHub Pages hosts the support/privacy URLs with project-path canonical/hreflang/sitemap configuration. Deployment and live navigation were verified. Production publisher review and the official Amazon link after listing remain pending. Current preview is intentionally noindex. |

To inspect the content gate:

```powershell
python tools/review_content.py release-check
```

It must fail while activities remain unapproved. There is no synthetic approval or production PIN bypass. Do not publish debug screenshots as proof of compatibility, or refer to the development library as complete, teacher-reviewed, fully localised or legally certified.
