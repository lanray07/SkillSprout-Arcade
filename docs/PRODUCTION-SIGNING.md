# Production signing and upload status

The publishing key was created locally on 20 October 2026 for `CN=O. Bankole`, alias `skillsprout-production`, using RSA-4096 and a randomly generated password. It is distinct from the Android debug identity.

The keystore, password properties and public certificate are stored outside the repository in `C:\Users\User\.skillsprout\release-signing`. Windows permissions restrict this directory to the current user and SYSTEM. Back up this entire directory securely before publishing; retain the same publishing identity for subsequent updates. The password properties are plaintext protected by the directory permissions, so keep backups encrypted. Do not upload these private files to GitHub or Amazon.

Gradle loads `%USERPROFILE%/.skillsprout/release-signing/signing.properties` by default. `SKILLSPROUT_SIGNING_PROPERTIES` can point to an existing properties file on another build machine. Required fields are `storeFile` (absolute path using forward slashes), `storePassword`, `keyAlias` and `keyPassword`. `tools/setup-signing.ps1` provisions a new identity only when no local signing material exists; it refuses to overwrite an existing identity.

Run from the repository root:

```powershell
.\gradlew.bat :app:verifyProductionSigning
.\gradlew.bat :app:assembleRelease
```

The release remains non-debuggable, has `DEVELOPMENT_CONTENT=false`, and requires both signing configuration and the existing release evidence checks. Signing alone does not make the app production-ready. The current 396 bundled activities remain unapproved; the release runtime rejects them. There is no physical Fire device connected for validation. The APK must not be uploaded as a production candidate until the content, exact-candidate QA and remaining publisher reviews in `RELEASE-BLOCKERS.md` are complete.

After the evidence gate passes, the signed output is `app/build/outputs/apk/release/app-release.apk`. Verify its signature with Android SDK `apksigner verify --verbose --print-certs`, check alignment with `zipalign -c -P 16 -v 4`, install that exact file on supported Fire devices, and retain the SHA-256 and QA evidence. Only then upload the tested APK to the existing Amazon draft. Uploading is separate from final submission/publication.

Amazon reference: [Understanding Amazon Appstore Submission](https://developer.amazon.com/docs/app-submission/understanding-submission.html).
