# Amazon app integration configuration

The `debug` APK is the offline development build. The `amazonPreview` APK includes the Amazon SDK and INTERNET permission, uses development content, enforces starter/verified Family access, and is debug signed. It is for App Tester integration only. `amazonRelease` uses production signing, rejects development content, disables development access and runs the same release-evidence gate as `release`, plus store configuration validation. Neither release target currently passes its evidence gate.

Build commands:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleAmazonPreview
.\gradlew.bat :app:assembleAmazonRelease
```

The second command must fail while release evidence/content/configuration remain incomplete. Never upload `amazonPreview` as a production binary.

Configuration is read from `SKILLSPROUT_AMAZON_PROPERTIES` or `%USERPROFILE%/.skillsprout/amazon.properties`, outside Git. Use Java properties syntax and forward-slash absolute file paths:

```properties
# Populate from actual Amazon products and your deployed verifier; no default SKUs/prices.
productMappings=
verifierUrl=
environment=sandbox
entitlementPublicKey=
authenticationPem=
```

`verifierUrl` is the credential-free HTTPS `/verify` endpoint. `entitlementPublicKey` points to an X.509 DER P-256 public key matching the backend's private signing key. `authenticationPem` points to the actual app's `AppstoreAuthenticationKey.pem` downloaded from Amazon. The build copies only these public authentication/verification assets into generated variant assets. The RVS shared secret and private signing key stay server-side. Preview builds additionally require `environment=sandbox`, preventing production purchasing configuration in a debug APK. Missing or malformed configuration leaves the purchase UI unavailable and cannot grant Family access. Production requires `environment=production`; use separate sandbox and production keys.

The SDK listener is registered when the app gateway is created. Foreground refresh clears the prior in-memory account binding and ignores superseded response request IDs. It asks Amazon for its current app account and product metadata; successful account callbacks start a paginated full restore. Purchase and user-triggered restore require an active parent PIN session. Backgrounding the app relocks the parent area; an App Tester overlay can leave the Activity foregrounded. While foregrounded, the app checks the signed lease and requests refresh near its deadline, at most once a minute. Failure cannot extend access past the signed deadline. A successful store response grants nothing until the HTTPS service returns a valid signed receipt lease. The UI uses Amazon titles, descriptions and prices, opens Amazon confirmation and offers restore/refresh. It makes no trial or price promises.

Starter access allows one persistent primary explorer, three bundled starter activities per game/year and no new parent assignments. Family access supports up to four profiles, assignments and all installed content. Expiry locks additional profiles without deleting their history; existing data remain exportable/deletable in the parent area. The primary profile is persisted independently of nickname sorting. New profile writes are serialised against the saved database count. Every session filters its question pool through the current access policy. A profile switch caused by expiry exits the current game, and assignments exit on expiry.

Before activating an online build, deploy and validate `billing-service`, configure the actual products/authentication assets, update the public privacy notice and Amazon declarations for account/receipt verification, and complete App Tester and physical Fire purchase/restore/cancellation/account-switch/offline tests. No live Amazon transaction or deployment was performed by the automated tests.

References: [Amazon IAP implementation](https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html), [receipt verification](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html).


Subscription SKU handling: Amazon uses buyable term SKUs for purchase and a non-buyable parent SKU for the returned receipt. Configure `productMappings` as comma-separated `purchaseSku=receiptSku` pairs. The app queries both, offers only the purchase keys with store prices, and accepts/verifies only the receipt values. The backend `AMAZON_PRODUCTS_JSON` allowlist uses receipt parents, for example `{"com.skillsprout.arcade.family":"SUBSCRIPTION"}` after that product is actually approved. A one-time entitlement maps to itself. Never offer the parent as a buy button or allow only the term in receipt validation. [Amazon documents this distinction](https://developer.amazon.com/docs/in-app-purchasing/iap-faqs.html).

The real console now has an unpublished Family Premium parent `com.skillsprout.arcade.family` and monthly/annual terms `.monthly` and `.annual`. These draft identities are not live products. The publisher accepted UK prices of £4.99/month and £29.99/year on 21 October 2026. Both prices are saved in Amazon, using GBP as the base and Amazon-calculated prices for other marketplaces. The IAP draft shows Ready to Submit and has not been submitted. The app-specific RSA public key was copied from the console, parsed and stored outside Git at `%USERPROFILE%/.skillsprout/amazon/AppstoreAuthenticationKey.pem`; its PEM SHA-256 is `9fdac1d72f17d070cca29be88b30b0898a0a26aab6aad734f8bac5f9aa75fe5a`. This is separate from the P-256 receipt-service keys: sandbox and production keys are generated outside Git with restricted local ACLs. Their public keys are configured separately for the deployed sandbox/production Workers; private keys stay server-side. Physical sandbox purchase/restore checks passed, while full billing and publisher privacy reviews remain pending.


`amazonReview` is a separate, non-debuggable pre-release review target signed with the existing publisher key. It deliberately allows the clearly labelled development content, restricts access to the free starter tier, and sets `PURCHASING_ALLOWED=false` regardless of local store configuration. It is intended only for the explicitly requested Amazon draft upload/testing. It does not run or satisfy the production evidence gate and is not a production-ready release. `release` and `amazonRelease` retain their existing evidence/signing gates and reject unapproved content; `amazonRelease` additionally requires production receipt configuration. Build review with `:app:assembleAmazonReview :app:lintAmazonReview`. Never change review flags, names or evidence records to imply approval. Do not submit the draft for public publication while production blockers remain open.

The portal accepts the file but displays a DRM-integration/outdated-SDK notice. The dependency remains pinned to 3.0.9, the newest version in the official published release notes checked for this task. No DRM license-check integration is enabled for this free starter review. Amazon describes DRM as optional for free apps using IAP; do not silently add a launch-time license gate solely to hide a notice. Resolve/revalidate the portal notice as part of final production review. Sources: [SDK release notes](https://developer.amazon.com/docs/appstore-sdk/release-notes.html) and [DRM overview](https://developer.amazon.com/docs/in-app-purchasing/drm-overview.html). Automatic filtering selected 12 Fire tablets and no Fire TV/Automotive devices; this is manifest-based eligibility, not physical-device QA.


Receipt-service preparation on 21 October 2026 adds mounted-file secret configuration, a non-root Docker/Compose package, process health, bounded concurrency and graceful shutdown. The local service suite has 19 passing tests; GitHub CI additionally builds/runs the container. See `billing-service/README.md` for deployment and key setup. Cloudflare Workers sandbox and production HTTPS endpoints are now deployed, with secrets held in encrypted bindings; purchasing stays disabled in the uploaded review build.


Free hosting is deployed and recorded in release/receipt-hosting-status.json. External default sandbox properties now point to the sandbox HTTPS endpoint and matching public DER. Separate amazon-production.properties outside Git contains the production endpoint/public DER and passed :app:verifyAmazonConfiguration. Neither private signing keys nor the RVS secret enter app properties. Physical App Tester catalogue, fresh purchase and cache-cleared signed restore now pass on KFTRWI/API 28. See release/fire-billing-qa.json for the exact tested stages and limits. The uploaded review variant remains disabled for purchasing.


The physical subscription test exposed an RVS renewal boundary issue: sandbox RVS returns a valid, uncancelled receipt whose renewalDate is the response time. Amazon defines cancelDate as cancellation/expiry; renewalDate alone does not establish cancellation. The verifier now gives a due renewal only a 60-second signed lease, requiring another RVS check. Actual cancellation and exhausted grace still deny access, and a nearer future cancellation still caps the lease. This applies to both environments and has explicit regression coverage. Source: [Amazon RVS fields](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html).


21 October production-readiness follow-up: all 13 billing-library tests pass. Four additional tests exercise the actual HTTPS verifier with a controlled transport: the minimal receipt request and timeouts, redirect/status handling, malformed/oversized response rejection, and lease expiry/recovery through an outage. A failed request cannot extend the existing signed deadline; after expiry, a new valid signed response is required. These are Robolectric tests, not physical network-loss or live-store evidence. See `release/software-production-checks.json`. The production build again passed configuration/signing checks and stopped at the existing review-evidence gate; no production binary was created or uploaded.
