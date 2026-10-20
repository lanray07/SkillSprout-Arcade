# SkillSprout Arcade

Little challenges. Big discoveries.

Native Kotlin/Jetpack Compose Android development build for offline family learning, plus a static, search-friendly parent website. Publisher: **O. Bankole**. Parent support: **banksmi@mail.com**. The brand is provisional.

## Run and build

Requirements: JDK 17, Android SDK platform 35, and Node.js for the website. Set `sdk.dir` in your local, ignored `local.properties`, or use Android Studio's SDK setup.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
python tools/review_content.py validate
python tools/localise.py report
cd website
npm test
npm run build
npm run dev
```

The website preview is served at http://localhost:4173. It has no npm dependencies. All 14 pages are rendered to HTML at build time. Set a real canonical origin in `website/site.config.json` before deployment; preview pages deliberately use `noindex` and a disallowing robots file. No domain, Amazon listing, store price or trial has been invented.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. A debug signature is for local testing and must not be used as a production publishing identity.

## Implemented

- Six interactive activity modes with tile assembly, ordered sequences, visible equal-group building, original stories, sentence completion and a maths crossing scene.
- Shared deterministic activity selection, hints, first-try evidence, per-skill mastery estimates, retries and persistent sessions.
- Room persistence for four local development profiles, assignments and progress; CSV export and profile erasure.
- Parent PIN using PBKDF2-HMAC-SHA256 (120,000 iterations, random salt), AES-GCM and Android Keystore. Persisted retry throttling, parent lock on background, secure parent window, and no PIN reset backdoor.
- App-only daily allowance, persisted monotonic foreground accounting, bedtime, parent bonus, breaks and calm time-up screen.
- Optional offline English TTS; no microphone permission or speech upload. A constrained parser exists for a future reviewed push-to-talk implementation.
- 396 English development activities with selected England curriculum mappings, metadata, SHA-256 manifest and validation. None is labelled teacher-reviewed.
- Parent-controlled validated pack file import and removal. The starter pack remains offline. No network pack catalogue or cloud sync.
- Original generated Pip and Sproutland art, editable vector assets, adaptive launcher icon and marketing source tooling.
- Development-time UI extraction, translation API adapter, caching, glossary/placeholder checks, missing-string reports and human-review-gated resource export. English educational exercises are kept separate.

## Deliberately unavailable until release work is finished

Read [RELEASE-BLOCKERS.md](docs/RELEASE-BLOCKERS.md) before describing this as production-ready. This is a development build, not an Appstore submission. Family features are available for testing without a fake purchase. The separately compiled Amazon adapter is not linked to the app or activated. It needs actual SKUs, the Amazon app authentication public key, a deployed receipt verification/signing service, parent UI integration and App Tester evidence.

Full interface localisation, language-specific educational packs, a complete approved Years 1–6 library and physical Fire validation are not complete. Production signing is configured locally (see [PRODUCTION-SIGNING.md](docs/PRODUCTION-SIGNING.md)); it does not bypass the release evidence gate. Support and privacy pages are hosted on GitHub Pages. JVM Android screen renders are not physical Fire device captures.

## Repository map

| Path | Purpose |
|---|---|
| `app/` | Offline Android app and tests |
| `billing-amazon/` | Isolated Amazon SDK adapter; not included in offline APK |
| `tools/` | Content generation/review, localisation and asset exports |
| `assets/` | Original images, design concept and editable source assets |
| `website/` | Fourteen static HTML pages and preview server |
| `docs/` | Architecture, privacy assessment, QA and release gates |
| `marketing/` | Listing drafts, keyword matrix, captions and templates |
| `artifacts/` | Debug handoff, rendered screens and verification evidence |

The attached brief ends at “Week 5 — Voice”; this implementation uses the complete requirements preceding that point.

Parent website: https://lanray07.github.io/SkillSprout-Arcade/
Privacy policy: https://lanray07.github.io/SkillSprout-Arcade/privacy/
GitHub Pages publishes website changes from main through the Pages workflow. The public site remains a development preview; production release blockers still apply. Privacy text is maintained in website/privacy.json and mirrored in PRIVACY.md with a consistency test.
