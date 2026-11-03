# Production candidate workflow

Candidate building and publication acceptance are separate phases. Optional independent reviews were waived by the publisher. That does not turn unreviewed content into teacher-approved content or certify brand/privacy compliance.

`python tools/verify_release.py --phase candidate` checks the source-bound bundled-content waiver, manifest/pack integrity and verified signing backup. Gradle also checks actual interface-resource coverage, signing configuration and production Amazon configuration. Release builds keep development access/content disabled. Only the immutable starter bytes pinned in ContentPacks have the publisher waiver; public parsing, signed imports, signature/namespace/version checks and rejection rules are unchanged.

Set SKILLSPROUT_AMAZON_PROPERTIES to the private production configuration and build `:app:assembleAmazonRelease`. The app is version 1.0.1 / code 6. Keep the signing key, passwords and private service credentials outside the repository.

After building, copy the APK to an immutable artifact path. Run `python tools/check_production_apk.py --apk <path>` to validate its publisher signature, alignment, actual DEX flags/production settings, permissions, manifest and bundled assets. This checks the binary, rather than relying only on Gradle source configuration.

Run physical starter-game and parent-state checks on that exact installed hash. ProductionCandidateDeviceTest is an external UiAutomator test hosted in the separate debug Billing QA package; it neither instruments nor modifies the production APK. It requires a clean test installation and creates synthetic data. Never point its setup/reset workflow at real family records. Test-only PINs are public fixtures, not publisher credentials. Leave the production installation at fresh setup after removing only the synthetic test state.

Use the separate sandbox-only Billing QA APK for App Tester purchase/restore checks. It fails closed outside confirmed SANDBOX mode. App Tester’s test subscription is not a live store purchase or Live App Testing enrolment. The scoped reset utility can remove one owned Billing QA test subscription; it never clears all transactions or acts on another app’s records. The isolated runner restores catalogue bytes, sandbox mode and observed settings, and keeps earlier evidence files.

`python tools/check_candidate_marketing.py` checks the 114 actual localised Compose-screen exports across 19 interface variants. These use synthetic explorer data and native JVM Android rendering; they are not labelled physical Fire screenshots. Learning activities and narration remain English. Only languages offered by Amazon’s respective description/media selectors can be uploaded; unsupported slots retain documented fallback images.

Before publication, prepare release/candidate-acceptance.json referencing hash-bound artifacts for exact signature/manifest and device checks, sandbox checkout/restore, matching localised assets and actual public privacy/store declarations. Run `python tools/verify_release.py --phase publish --apk <path>`. Missing checks still block publication; candidate assembly alone is not approval. Inspect the portal’s selected devices, age/certification choices, IAP state, version and final review page before submission. Do not submit the old review-only code-4 binary.
