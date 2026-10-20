# Amazon purchasing integration seam

This Android library compiles against Amazon Appstore SDK 3.0.9. It is deliberately **not a dependency of the offline app**. There is no active purchase button, entitlement bypass, invented product price or trial.

The adapter retrieves store product data, starts only parent-authorised requests, pages purchase updates, restores, receives cancellations, calls an injected server receipt verifier, and caches ECDSA-signed access with bounded offline validity. An expired or invalid signature never grants access. The server is responsible for Amazon RVS, app/product/user binding, subscription expiry, cancellation semantics and signing. No Amazon shared secret belongs in an APK.

Before integrating: obtain `AppstoreAuthenticationKey.pem` from the actual developer app and use actual store SKUs; pin the receipt service's signing public key; deploy an authenticated, rate-limited HTTPS verifier; bind claims to the Amazon user and receipt; implement the parent-only price/confirmation/recovery UI; enforce the free/premium feature limits; and test all failure paths using Amazon App Tester and a physical Fire tablet. Harden receipt-order handling so an older cancellation cannot displace a newer valid entitlement. A network failure during restore must preserve an otherwise valid signed cache.

Official sources checked 20 October 2026: [SDK integration](https://developer.amazon.com/docs/appstore-sdk/integrate-appstore-sdk.html), [IAP implementation](https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html). This seam is not an assertion that live store integration has passed.
