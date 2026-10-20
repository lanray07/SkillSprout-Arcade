# Verification — 20 October 2026

## Build and functional evidence

`gradlew --max-workers=2 :app:assembleDebug :app:assembleAmazonPreview :app:testDebugUnitTest :app:lintDebug :app:lintAmazonPreview :billing-amazon:testDebugUnitTest :app:verifyProductionSigning` completed successfully for 0.2.0-dev (version code 2). **62 Android/library tests passed**: 11 learning/domain tests, 4 access-policy tests, 6 Android integration tests, 6 production-path tests, 15 UI/mission/purchasing tests on each phone/tablet configuration and 5 signed-billing-cache/configuration tests. Python tooling: **12 passed**. Static website: **4 passed**. Receipt service: **15 passed**. Total: **93 passing checks**, no skipped tests. Offline lint: **0 errors, 71 warnings**; Amazon-preview lint: **0 errors, 70 warnings**. Both APKs passed `apksigner verify` and 16 KiB `zipalign` checks. Actual counts, APK bytes and SHA-256 values are in `artifacts/BUILD-REPORT.json`; the handoff script reads real results instead of hard-coding these totals.

The warnings include newer dependency/target recommendations, synchronous durable preference writes, KTX suggestions and unused/launcher resource follow-ups. Synchronous play-ledger and PIN-throttle writes are intentional to limit restart bypass; moving them off the UI dispatcher without losing durability is a performance follow-up. No claim of a warning-free build is made. SDK/dependency versions are pinned; updating the target needs fresh compatibility tests.

Tests additionally cover version-1 database migration, unique transactional mission completion, late writes after profile deletion, signed-pack authentication/update/rollback/backup recovery, selectable decorations, parent volume preview/save, billing account/environment/expiry binding, and receipt-service input/cancellation/grace/rate-limit behaviour. Tests cover six game answer flows, a complete three-challenge assignment with exactly one completion, no duplicate evidence IDs, incorrect/correct parent PIN navigation, parent relocking after background/resume, pack integrity/schema/locale rejection, profile/attempt/assignment deletion, restart-persistent play allowance, skill selection/difficulty, hint effects, voice-command allowlisting, CSV formula escaping, listing sizes and a failing release gate. UI PIN verification uses a test-only vault; hardware Keystore verification still needs a physical device.

Both `preReleaseBuild` and `preAmazonReleaseBuild` were invoked; release evidence and missing production Amazon configuration **failed as intended** for unapproved educational content and outstanding Fire/accessibility/live-billing/legal/brand/exact-candidate-signing/localisation approvals. Actual hosted-site verification is now recorded with its artifact hash; local signing configuration passes, but does not imply approval of a production candidate. This expected failure is not the debug build outcome.

The compiled APK's permissions were inspected with Android `aapt`: the only declared uses-permission is AndroidX's app-scoped non-exported receiver permission. INTERNET, RECORD_AUDIO, location and contacts permissions are absent. The Amazon integration library is not included in this offline APK. The separate Amazon preview includes INTERNET and the SDK, but has no microphone, location or contacts permission. Purchasing is unavailable in the unconfigured preview. Sandbox configuration, actual authentication assets and the deployed verifier are needed before App Tester use.

## Rendered-screen evidence and limits

Native screens were rendered from the actual Compose components with Robolectric API 35 native graphics at 1280×800 tablet and 400×800 phone qualifiers. The capture uses Android's view drawing into a bitmap; Compose PixelCopy capture was unsupported by this runtime. These are JVM Android renders with test fixture profiles, **not physical Fire screenshots or a successful emulator run**. An ordinary emulator start reported that the acceleration driver was missing, and software emulation did not remain running. No system hypervisor configuration was changed.

Eight editable marketing compositions per variant use those actual source screenshots, embed the source capture in SVG and disclose JVM preview status. They must be replaced/revalidated on physical target devices before submission. The game-selection caption uses the arcade screen, where game choices live; the phone view scrolls through the games. Caption 8 accurately refers to installed activities because no network catalogue ships.

## Visual comparison

Concept: `assets/concepts/arcade-and-site.png`. Latest website captures: `artifacts/qa/website-desktop.png` and `website-mobile-full.png`. The concept and final desktop implementation were inspected using `view_image`; native tablet home/parent screens and phone game screens were also visually inspected.

Browser/IAB was used first to verify navigation and FAQ expansion. Chrome through the browser tool supplied explicit 1440×960 and 390×844 responsive viewports because IAB did not offer a viewport override. The temporary Chrome override was reset. No standalone Playwright browser was needed. At 390px, the document scroll width was 375px, with no horizontal overflow; every image reported loaded after correcting SVG MIME delivery. Full-page browser screenshots avoid clipping to the physical browser window.

Five specific comparison points:

1. Cream background and dark forest typography match the selected palette; native game renders were corrected from the test harness's default purple surface.
2. The two-line wordmark, three-link navigation, left headline and green action preserve the website concept's hierarchy.
3. Original Pip and the four-island world are separate production assets and retain the green/orange/cream character identity. The website does not tint the original art.
4. Six game choices use coordinated pastel colours and original editable vector symbols; native tablet navigation remains a left rail and phone navigation stacks.
5. Website mobile content stacks copy, action, art and game choices without clipping. Native game headers and group-building rows were repaired after phone renders exposed wrapping.

Above-the-fold copy diff: the supplied tagline and “Explore the games” CTA match. Supporting copy intentionally uses the supplied positioning plus an explicit development/teacher-review notice. “Launch information” leads to an honest unconfigured pricing page. No launched-store or teacher-approval claim was added.

The implementation was visually checked against the selected design direction. It is **not a pixel-identical reproduction or final agency sign-off**: native controls use Material typography, vectors replace the concept's painted game emblems, the tablet uses a scrolling three-column game grid, and the mascot pose/world crop differ from the concept. These are visible development differences; complete animation/cosmetic polish remains on the release list. No 10/10 fidelity claim is made.

Additional captures `10-family-starter.png` and `11-parent-purchases.png` exercise starter access and the parent purchase component at both sizes. The purchase fixture uses a clearly labelled fake test offer and price; these are UI routing tests, not actual Amazon products, receipts or payments. Current phone/tablet components were visually inspected. The curriculum/tool tests cover the bundled acyclic skill graph, additional topic/passage coverage, source-bound translation review, complete formatter tokens and required Arabic plural forms. The catalog now has 307 source entries; no new reviewed language is shipped.

## Remaining validation

Physical Fire OS compatibility, Android Keystore behaviour, touch/keyboard/screen-reader exploration, process death, reboot and clock changes, real TTS availability, final approved curriculum, live Amazon purchase/restore/cancellation, complete localisation and publisher privacy review remain required. The support/privacy pages are hosted and browser-verified on GitHub Pages. Read `RELEASE-BLOCKERS.md` for the full handoff.
