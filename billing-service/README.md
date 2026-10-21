# Receipt verification service

Dependency-free Node.js service implementing Amazon RVS verification and P-256 signed, account-bound access leases compatible with `billing-amazon`. Separate Cloudflare sandbox and production endpoints are now deployed on the existing Free plan. The uploaded review APK still has purchasing disabled. No example product ID is a configured live product.

Required server-only configuration: `AMAZON_RVS_SECRET_FILE` (preferred mounted secret file) or `AMAZON_RVS_SECRET` (never both), `AMAZON_PRODUCTS_JSON` (a JSON object mapping each actual SKU to `SUBSCRIPTION` or `ENTITLED`), and `ENTITLEMENT_PRIVATE_KEY_FILE` (a dedicated unencrypted PKCS#8 P-256 PEM loaded from a protected deployment secret file). Keep the signing key distinct from APK signing and content signing. Never put the shared secret or signing key in the APK, site, logs or Git. Bundle only the entitlement public key in a future billing-enabled build.

`RVS_ENVIRONMENT` is `production` by default; explicitly select `sandbox` for App Tester. Use separate keys for these environments. Production rejects test/beta receipts; access tokens include their environment. `npm test` runs isolated tests with fabricated test receipts and ephemeral keys; it never calls Amazon.

`npm start` binds port 8787 on loopback by default. Container deployments explicitly set `HOST=0.0.0.0` inside the isolated container; Compose publishes only to host loopback. Deploy only behind HTTPS ingress with request/time/concurrency limits and no request-body or upstream-URL logging. The shared secret appears in the upstream RVS URL by Amazon's protocol, so redact it at every infrastructure layer. Do not expose the loopback HTTP listener directly. The in-memory limiter keys on the socket peer, ignores forwarded headers and limits all traffic together when behind a proxy; configure per-client rate limits at ingress as well. Multi-instance deployments require shared edge rate limiting.

POST `/verify` accepts only `{userId, receiptId, sku}`. It checks the configured product, Amazon response, receipt binding and access dates before issuing a six-hour maximum lease. A future cancellation or renewal boundary can shorten that lease; active grace can extend the renewal boundary within the same six-hour maximum. HTTP 403 means the receipt is invalid/inactive and the client must revoke matching cached access. HTTP 503 means temporary unavailability and must not grant new access. Cancellation can remain cached until the signed refresh deadline; shorten the configured interval or add verified real-time notification processing before promising faster revocation. No child profiles, learning records or email addresses are accepted or retained. The service keeps only short-lived IP rate counters in memory, but hosting providers may keep separate infrastructure logs.

Before connecting this service: configure actual Amazon products and authentication assets; deploy HTTPS and secret storage; update the privacy notice and Amazon declarations for purchase-account/receipt processing; validate the Amazon variants’ parent purchase/restore UI and free-tier enforcement; run App Tester and physical Fire purchase/cancellation/expiry/offline tests. The offline app remains unable to charge users until this integration is completed.

Protocol reference: [Amazon RVS for Appstore SDK IAP](https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html).


## Deployment package

`Dockerfile` copies only the four runtime files and runs as non-root UID 1000. `compose.yaml` uses a read-only filesystem, drops capabilities, sets resource limits and mounts secrets at runtime. The `.dockerignore` is an allowlist; no keys, tests or local configuration enter the image. Node 22 is the base major; rebuild regularly to receive patches and record the deployed image digest. The portable container remains an alternative to the deployed Cloudflare Worker adapter.

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


## Live free hosting (21 October 2026)

Production verification: `https://skillsprout-receipts-production.lanraybanks.workers.dev/verify`.
Sandbox verification: `https://skillsprout-receipts-sandbox.lanraybanks.workers.dev/verify`.
Each has a `/healthz` endpoint. Both returned ready, rejected empty requests with 400 and denied a fabricated invalid receipt through the appropriate Amazon RVS endpoint with 403. A valid store purchase/restore has not been tested. Exact versions, public-key fingerprints and probe results are in `release/receipt-hosting-status.json`. The publisher's uploaded review APK remains unable to purchase.

Cloudflare's dashboard confirmed the existing Workers **Free** plan. No paid plan, custom domain, database, KV namespace or storage subscription was enabled. The free allowance is 100,000 requests/day shared by all Workers in this account, with 10 ms CPU per invocation. This is an initial deployment within those limits, not a promise of free hosting at any traffic level. Exhaustion can interrupt verification; the app must keep its existing signed-lease expiry behavior. Monitor usage before launch. See [Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/).

`worker.mjs` reuses `service.mjs` and its P-256 DER signatures. The actual workerd test found that `redirect: error` was rejected by this runtime; the shared verifier now uses `manual` and rejects non-200 responses, including redirects. No Amazon secret is forwarded to a redirect target. The runtime smoke test checks this behavior.

`wrangler.jsonc` defines separate Worker names, environment markers and rate-limit namespaces. The default is sandbox. Both require `AMAZON_RVS_SECRET` and `ENTITLEMENT_PRIVATE_KEY_PEM` as Cloudflare encrypted secret bindings. Production uses the publisher account's Amazon Shared Key; sandbox uses an independent random non-empty value, as permitted by Amazon's sandbox protocol. Separate signing keys match the external app public DER files. No secrets are in the Wrangler configuration or Git. Runtime settings were inspected to confirm both credentials use `secret_text` bindings.

The account's existing Wrangler login was sufficient; no new Composio connection is needed. Run `npm ci`, `npm test` and `npm run test:worker` before deployment. The CLI is pinned to 4.151.3, with its matching Miniflare test runtime pinned in the lockfile. Deploy using the explicitly selected environment:

```sh
npm run deploy:sandbox
npm run deploy:production
```

For initial deployment, add `-- --secrets-file /protected/environment-cloudflare-secrets.json` to the selected npm command. The file is a JSON object containing the two secret names above, sourced from that environment's protected key/credential store. Existing secrets persist across code deployments. Never place secrets in `vars`, a command argument value, GitHub Actions output or a bundle. Check the selected account's plan and existing Worker names first. These names and account ID refer to this publisher's verified account; another publisher must change them.

Worker logs/observability and preview URLs are disabled in configuration. HMAC-derived client IP rate-limit keys avoid putting raw addresses in the limiter. Rate limits are 20 requests/minute per client and 120/minute for the service within each Cloudflare location; these are permissive, locally coordinated edge limits, not an exact worldwide counter. Missing bindings or ingress context fails closed. Cloudflare still processes network metadata and may retain infrastructure information under its own policies; update the public privacy notice and store disclosures before enabling any connected app build.

`node hosted-smoke.mjs sandbox` or `node hosted-smoke.mjs production` performs manual live readiness/input/invalid-receipt checks without making a purchase. Do not put real customer account/receipt values in source or CI. `node worker-smoke.mjs` runs locally against workerd with ephemeral keys and a mocked Amazon service; CI runs that test and compiles both environments. Production Android configuration validation passed using an external production properties file. The default external app properties remain sandbox; no production approval or live transaction is implied.
