# Amazon draft and build status — 21 October 2026

The Amazon draft now contains the publisher-signed review-only APK, version 0.2.1-dev-review, version code 3, labelled ReviewOnlyDoNotPublish. Amazon visibly reported one uploaded file and DRM Applied: No. It has not been submitted for publication.

The review target is non-debuggable, has purchasing and development-access bypass disabled, allows the explicitly disclosed development content, and requires the existing publisher signing identity. Its signature matches the provisioned certificate; signature/alignment/flags/assets checks passed. See artifacts/AMAZON-REVIEW-REPORT.json for the exact SHA-256 and public certificate fingerprint. This is a pre-release testing candidate, not a production-ready product.

Both production targets retain the existing evidence/signing gates; amazonRelease also requires live receipt configuration. The gates were run again and rejected the current state. None of release/evidence.json was promoted to Approved. All 444 content items still await teacher review, and no physical Fire device is connected.

Amazon setup completed: the app-specific RSA public key was validated and stored outside Git; a Family Premium parent and monthly/annual term drafts were created; the English description and both icon sizes were saved. The buyable-term/receipt-parent distinction is now explicit in the Android catalog and covered by tests. The proposed UK prices are £4.99/month or £29.99/year, awaiting publisher acceptance; no prices were set. Seven-day trial options exist in the console but are not represented as a live offer.

The local software checks pass: 66 Android/library tests plus 12 tooling, 4 website and 15 receipt-service checks, 97 total. All three locally built variants have zero lint errors. The candidate was not executed on a physical device. The receipt service still needs a hosting destination, protected server secrets/signing key, deployed HTTPS endpoint and actual Amazon testing; public privacy/declarations must be reviewed before activation. Language/content/device/accessibility/publisher approvals remain pending.

Testing instructions in Amazon explicitly describe the review status, one-profile starter limits, disabled purchasing, absent online sync and outstanding reviews. The portal's Ready to Submit badge only reflects its form/file validation and is not evidence that the product is ready to publish.

A subsequent portal check shows the review label persisted, all 1,759 characters of testing instructions persisted, 12 automatically eligible Fire tablets and no Fire TV/Automotive targets. The portal also displays a DRM-integration/outdated-SDK notice, documented in AMAZON-INTEGRATION.md. No device QA or SDK/DRM clearance is implied by file acceptance.
