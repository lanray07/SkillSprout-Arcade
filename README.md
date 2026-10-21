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

The local website preview is served at http://localhost:4173. It has no npm dependencies. All 14 pages render to HTML at build time; GitHub Pages uses the configured project-path canonical URLs. Development pages deliberately remain noindex. The Amazon listing is a saved draft; no store price or trial is invented.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. A debug signature is for local testing and must not be used as a production publishing identity.

## Implemented

- Six interactive activity modes with tile assembly, ordered sequences, visible equal-group building, original stories, sentence completion and a maths crossing scene.
- Shared deterministic activity selection, hints, first-try evidence, per-skill mastery estimates, retries and persistent sessions.
- Room persistence for four local development profiles, assignments, progress and idempotent mission-completion events; versioned migration, CSV export and profile erasure with guarded late writes.
- Parent PIN using PBKDF2-HMAC-SHA256 (120,000 iterations, random salt), AES-GCM and Android Keystore. Persisted retry throttling, parent lock on background, secure parent window, and no PIN reset backdoor.
- App-only daily allowance, persisted monotonic foreground accounting, bedtime, parent bonus, breaks and calm time-up screen.
- Optional offline English TTS with parent volume/preview and missing-voice feedback; no microphone permission or speech upload. A constrained parser exists for a future reviewed push-to-talk implementation.
- 444 English development activities with selected England curriculum mappings, metadata, an acyclic prerequisite graph, revised spelling clues, additional older-year maths/passages, SHA-256 manifest and validation. None is labelled teacher-reviewed.
- Parent-controlled signed pack import with pinned publisher authentication, atomic replacement and downgrade protection. Unsigned imports are development-only. No network pack catalogue or cloud sync.
- Practice-earned selectable decoration badges, visible explorer avatars and seven-day family summaries.
- Original generated Pip and Sproutland art, editable vector assets, adaptive launcher icon and marketing source tooling.
- Development-time UI extraction, translation API adapter, caching, glossary/placeholder checks, missing-string reports and human-review-gated resource export. English educational exercises are kept separate.

## Deliberately unavailable until release work is finished

Read [RELEASE-BLOCKERS.md](docs/RELEASE-BLOCKERS.md) before describing this as production-ready. This is a development build with an unpublished Amazon review draft. The Amazon variants integrate the adapter, PIN-gated store-price/restore UI and starter/Family access limits. Actual product drafts, authentication assets and separate hosted receipt services are configured; partial physical App Tester evidence exists. Final content, device/accessibility, live-store and publisher reviews remain open. See [AMAZON-INTEGRATION.md](docs/AMAZON-INTEGRATION.md). The offline debug APK retains explicit development access and cannot charge users.

The [content update](docs/CONTENT-UPDATE.md) describes the 48 new development exercises and remaining educational gaps. Implemented work and remaining software scope are in [SOFTWARE-PROGRESS.md](docs/SOFTWARE-PROGRESS.md); current build/upload facts are in [PRODUCTION-BUILD-STATUS.md](docs/PRODUCTION-BUILD-STATUS.md). `billing-service` and the isolated Android verifier client implement the receipt-verification protocol with local tests, deployed Workers and partial physical sandbox purchase/restore verification. Run `python tools/run_checks.py` for tooling, website and receipt-service checks. After successful Android checks, `python tools/package_handoff.py` creates labelled offline and Amazon-preview APK handoffs using actual reports. GitHub Actions also builds and checks the development app and uploads its APK as a workflow artifact.

Full interface localisation, language-specific educational packs, a complete approved Years 1–6 library and physical Fire validation are not complete. Production signing is configured locally (see [PRODUCTION-SIGNING.md](docs/PRODUCTION-SIGNING.md)); it does not bypass the release evidence gate. Support and privacy pages are hosted on GitHub Pages. JVM Android screen renders are not physical Fire device captures.

## Repository map

| Path | Purpose |
|---|---|
| `app/` | Offline and Amazon-variant Android app, access policy and tests |
| `billing-amazon/` | Isolated Amazon SDK adapter; not included in offline APK |
| `billing-service/` | RVS verification/signing service, deployed Workers and tests |
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

A publisher-signed, non-debuggable review-only APK (0.2.2-dev-review/code 4) is uploaded to the existing Amazon draft. It has purchasing disabled and explicitly labelled development content; production release gates remain blocked. See [production-build status](docs/PRODUCTION-BUILD-STATUS.md) and `release/production-attempt.json`. Family Premium parent/monthly/annual product drafts and accepted prices are saved; receipt services are deployed. Live production billing and public publication remain unapproved. Localized store assets and exact draft-profile coverage are in `marketing/localized/index.html` and `release/amazon-localization-status.json`; app learning content remains English.
