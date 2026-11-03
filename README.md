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
- Nineteen packaged interface variants with device detection, parent-only language selection, RTL support, plural validation and a documented machine-translation review waiver. English educational exercises and narration remain English. See [localisation workflow](localisation/README.md).
- An offline parent privacy-policy reader with a translated summary and separate disclosures for purchasing-enabled builds.

## Deliberately unavailable until release work is finished

Amazon rejected **1.0.0 / code 5** on 3 November 2026 for Read Aloud and checkout failures. **1.0.1 / code 6** is built, signed and uploaded to the replacement draft. Voice playback, monthly/annual sandbox purchase and restore, and exact-APK starter journeys pass. Production checkout remains blocked by unavailable offers while DevTest registration is pending; old publication acceptance is withdrawn and the app is not live. Signature/configuration checks and exact-binary Fire starter journeys passed; sandbox checkout and signed restore passed separately. See [current release status](docs/PRODUCTION-BUILD-STATUS.md) and [submission receipt](release/amazon-submission-2026-11-02.json). Independent reviews were waived; no teacher, native-language, legal or brand certification is claimed. Debug development access cannot charge users.

The [content update](docs/CONTENT-UPDATE.md) describes the 48 new development exercises and remaining educational gaps. Implemented work and remaining software scope are in [SOFTWARE-PROGRESS.md](docs/SOFTWARE-PROGRESS.md); current build/upload facts are in [PRODUCTION-BUILD-STATUS.md](docs/PRODUCTION-BUILD-STATUS.md). `billing-service` and the isolated Android verifier client implement the receipt-verification protocol with local tests, deployed Workers and partial physical sandbox purchase/restore verification. Run `python tools/run_checks.py` for tooling, website and receipt-service checks. After successful Android checks, `python tools/package_handoff.py` creates labelled offline and Amazon-preview APK handoffs using actual reports. GitHub Actions also builds and checks the development app and uploads its APK as a workflow artifact.

Interface resources cover all 19 configured variants; full translated layout/accessibility acceptance and final store assets remain incomplete. The learning library remains English, and none of its 444 activities is labelled teacher-reviewed. Final-candidate Fire validation remains incomplete. Production signing is configured locally (see [PRODUCTION-SIGNING.md](docs/PRODUCTION-SIGNING.md)); it does not bypass the release evidence gate. Support and privacy pages are hosted on GitHub Pages. JVM Android screen renders are not physical Fire device captures.

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
GitHub Pages publishes website changes from main. The site remains a preview while Amazon reviews the app. Connected privacy text is maintained in website/privacy.json and mirrored in PRIVACY.md with a consistency test.

The signed production APK replaces the code-4 review draft. Family Premium is Approved at accepted UK prices of £4.99/month and £29.99/year; production receipt verification is configured. Twelve Amazon descriptions and eight available image profiles are complete. Assets for 19 interface variants are in `marketing/candidate/`; learning content and narration remain English. See [exact-candidate acceptance](release/candidate-acceptance.json) for scoped evidence and limitations.
