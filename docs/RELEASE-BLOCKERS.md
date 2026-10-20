# Release gates — not production-ready

This build is not ready to submit or sell. A compiled debug APK and passing automated checks do not close the following gates.

| Gate | Evidence required |
|---|---|
| Educational content | Review 396 development items; author a complete, age-appropriate approved library and prerequisite graph. Year 5–6 depth, story variety, shape/sorting reasoning, phonics/dictation and measurement coverage are incomplete. Store teacher approval only with actual reviewer identity, qualification, timestamp, pack digest and evidence. |
| Amazon integration | Actual publisher console app, authentication PEM, SKUs, parent purchase UI, deployed RVS verification/signing service, verified entitlement enforcement, cancellation/expiry/restore/failure/offline testing. The adapter is compiled in isolation and the offline app does not consume it. |
| Monetisation access policy | Replace unrestricted development access with the actual free tier (one profile, restricted starter selection) and verified Family Premium. No price or seven-day trial claim until store configuration supports it. |
| Fire compatibility | Physical 7–10-inch Fire devices and supported Fire OS versions; missing TTS engine; low-memory/performance/battery; keyboard, rotation, parent gate, app restarts, process death, reboot and time tampering. No physical device is connected. The emulator fails to run because acceleration is unavailable. |
| Accessibility | TalkBack exploration, focus order, hardware keyboard, large fonts, colour contrast of every state, motion and sound controls. Native controls have 48dp+ targets; this is not a complete accessibility audit. Individual sound-volume control and fully adaptive compact headers need more work. |
| Localisation | Complete resource migration, all requested locale drafts, actual translation API run, placeholder/plural/RTL validation, human interface review, and separate educational pack review. Pending machine translations must not be advertised as released languages. |
| Content delivery | HTTPS catalogue, publisher-signed manifests, atomic version replacement and rollback policy. Current parent file import works offline but is not a network download manager. |
| Product polish | More gameplay variety and animations, six production raster game icons, district-specific environments, mascot states, cosmetic accessory selection, polished empty states, individual plans, complete multi-child summaries. |
| Marketing capture | Eight real-device Fire and phone captures with approved content and final UI; replace JVM preview captures. Validate each locale and current console rules. Screenshot #6 currently needs a dedicated game-selection capture. |
| Brand and signing | Name/trademark clearance, final app ID and publisher identity, production keystore and store signing plan. Brand and label currently configurable in resources/site config; no clearance is implied. |
| Legal and privacy | Publisher-reviewed final privacy notice, retention/rights procedures, applicability assessment, data-safety declarations and child-directed store policy review. |
| Website | Real domain, hosted support/privacy URLs, canonical/hreflang/sitemap configuration, hosted-site review, official Amazon link after listing. Current preview is intentionally noindex. |

To inspect the content gate:

```powershell
python tools/review_content.py release-check
```

It must fail while activities remain unapproved. There is no synthetic approval or production PIN bypass. Do not publish debug screenshots as proof of compatibility, or refer to the development library as complete, teacher-reviewed, fully localised or legally certified.
