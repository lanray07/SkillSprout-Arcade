# Receipt verification service

Dependency-free Node.js service implementing Amazon RVS verification and P-256 signed, account-bound access leases compatible with `billing-amazon`. It is not deployed or connected to the offline app. No example product ID is a configured live product.

Required server-only configuration: `AMAZON_RVS_SECRET_FILE` (preferred mounted secret file) or `AMAZON_RVS_SECRET` (never both), `AMAZON_PRODUCTS_JSON` (a JSON object mapping each actual SKU to `SUBSCRIPTION` or `ENTITLED`), and `ENTITLEMENT_PRIVATE_KEY_FILE` (a dedicated unencrypted PKCS#8 P-256 PEM loaded from a protected deployment secret file). Keep the signing key distinct from APK signing and content signing. Never put the shared secret or signing key in the APK, site, logs or Git. Bundle only the entitlement public key in a future billing-enabled build.

`RVS_ENVIRONMENT` is `production` by default; explicitly select `sandbox` for App Tester. Use separate keys for these environments. Production rejects test/beta receipts; access tokens include their environment. `npm test` runs isolated tests with fabricated test receipts and ephemeral keys; it never calls Amazon.

`npm start` binds port 8787 on loopback by default. Container deployments explicitly set `HOST=0.0.0.0` inside the isolated container; Compose publishes only to host loopback. Deploy only behind HTTPS ingress with request/time/concurrency limits and no request-body or upstream-URL logging. The shared secret appears in the upstream RVS URL by Amazon's protocol, so redact it at every infrastructure layer. Do not expose the loopback HTTP listener directly. The in-memory limiter keys on the socket peer, ignores forwarded headers and limits all traffic together when behind a proxy; configure per-client rate limits at ingress as well. Multi-instance deployments require shared edge rate limiting.

POST `/verify` accepts only `{userId, receiptId, sku}`. It checks the configured product, Amazon response, receipt binding and access dates before issuing a six-hour maximum lease. A future cancellation or renewal boundary can shorten that lease; active grace can extend the renewal boundary within the same six-hour maximum. HTTP 403 means the receipt is invalid/inactive and the client must revoke matching cached access. HTTP 503 means temporary unavailability and must not grant new access. Cancellation can remain cached until the signed refresh deadline; shorten the configured interval or add verified real-time notification processing before promising faster revocation. No child profiles, learning records or email addresses are accepted or retained. The service keeps only short-lived IP rate counters in memory, but hosting providers may keep separate infrastructure logs.

Before connecting this service: configure actual Amazon products and authentication assets; deploy HTTPS and secret storage; update the privacy notice and Amazon declarations for purchase-account/receipt processing; validate the Amazon variants’ parent purchase/restore UI and free-tier enforcement; run App Tester and physical Fire purchase/cancellation/expiry/offline tests. The offline app remains unable to charge users until this integration is completed.

Protocol reference: [Amazon RVS for Appstore SDK IAP](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html).


## Deployment package

`Dockerfile` copies only the four runtime files and runs as non-root UID 1000. `compose.yaml` uses a read-only filesystem, drops capabilities, sets resource limits and mounts secrets at runtime. The `.dockerignore` is an allowlist; no keys, tests or local configuration enter the image. Node 22 is the base major; rebuild regularly to receive patches and record the deployed image digest. No hosting provider has been selected and no public service has been deployed.

Generate distinct keys once on the protected deployment machine, using a new directory outside the repository:

```powershell
node billing-service/setup-keys.mjs C:/Users/User/.skillsprout/receipt-keys
```

The command refuses an existing directory. It writes separate PKCS#8 private PEM and X.509 SPKI public DER files for sandbox and production, printing only public-key SHA-256 fingerprints. On Windows restrict the directory/private-file ACL to the service owner and SYSTEM; POSIX modes do not enforce Windows ACLs. The current local key directory was created and ACL-restricted on 21 October 2026. Do not regenerate or replace deployed keys implicitly: existing signed leases depend on the public key in installed APKs. Securely back up private keys. Transfer them through the chosen host's secret manager, never through GitHub or chat.

For a Linux Docker host, configure the following environment variables in its protected deployment environment. Use absolute paths to secret files stored outside this checkout. Ensure only the dedicated service/deployment account can read the source files; mounted signing/RVS files must be readable by container UID 1000. Compose file mounts do not turn source files into an encrypted secret store.

```text
RVS_ENVIRONMENT=sandbox
AMAZON_RVS_SECRET_PATH=/protected/sandbox/amazon-rvs.txt
ENTITLEMENT_PRIVATE_KEY_PATH=/protected/sandbox/sandbox-private.pem
```

Then run from `billing-service`:

```sh
docker compose config --quiet
docker compose up --build -d
curl --fail http://127.0.0.1:8787/healthz
```

Use an independently deployed production instance and production key with `RVS_ENVIRONMENT=production`. The Amazon shared secret must come from the publisher account; do not invent it or use the CI fixtures. The Compose product allowlist uses the actual receipt-parent SKU `com.skillsprout.arcade.family`; its store draft remains unpublished. Term SKUs must not be used as receipt products.

Connect the host-loopback listener to the selected host's HTTPS ingress. Block direct external port 8787, restrict request bodies to 8 KiB, and apply per-client edge rate limits without logging request bodies, receipt identifiers or Amazon upstream URLs. Host proxies must disable buffering of sensitive request logs; infrastructure log retention/region must be established before enabling purchasing. Scale-out requires a shared ingress limiter; this package alone is not a distributed limiter. `/healthz` reports only process health and successful startup configuration, not Amazon availability or receipt validity. Verification has a 16-request concurrency cap and returns 503 when full; health remains available. SIGTERM/SIGINT stop accepting requests and allow bounded draining.

CI runs `node billing-service/container-smoke.mjs` on Linux with fabricated test-only credentials and ephemeral keys. It builds the actual image, mounts secrets, verifies non-root/read-only startup and health, rejects an invalid receipt without calling Amazon, and checks graceful shutdown. Local unit/HTTP tests use mocked upstream receipts. Neither test establishes live Amazon connectivity, physical-device behavior or production readiness.

Before enabling purchases, deploy HTTPS, validate sandbox purchase/restore/cancellation/account-switch flows on Fire, configure `verifierUrl` and the matching environment's public DER in the external Amazon properties, and repeat with production credentials and actual approved products. Update the privacy notice/Amazon declarations for Amazon account IDs, receipt IDs, SKU and infrastructure processing before activation. Do not send child nicknames, answers or learning progress; the API rejects fields beyond `userId`, `receiptId`, `sku`. Production release evidence remains pending.

References: [Docker Compose secrets](https://docs.docker.com/compose/how-tos/use-secrets/) and [Node HTTP server lifecycle](https://nodejs.org/api/http.html).
