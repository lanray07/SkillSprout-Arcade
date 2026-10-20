# Signed offline content delivery

Optional publisher packs now use a single `.sproutpack` JSON envelope: `format: 1`, a manifest (`packId`, positive `version`, `locale`, SHA-256 of the decoded JSON payload), Base64 payload and DER ECDSA signature. The app pins a P-256 public key in `assets/content-publisher.der`. SHA-256 protects payload integrity; the signature authenticates the publisher. Signature approval does not imply teacher approval.

The signed bytes are UTF-8 `SkillSprout-Pack-v1`, pack ID, decimal version, locale and lowercase SHA-256, separated by LF with no trailing newline. Activity IDs must start with `packId:`. The current educational locale remains `en-GB`. The payload limit is 5 MB; the envelope limit is 7 MB. Release parsing requires every activity to be approved. Unknown game/question types and invalid tile/grouping data are rejected.

`python tools/content_delivery.py keygen` was run once. Its encrypted private key and password file are in the restricted local APK-signing directory outside Git; only the public verification key ships. Back them up securely. This content identity is separate from the APK publishing identity and must remain separate from any entitlement-service key.

To sign an approved optional pack, record the real review of its exact digest with `tools/review_content.py`, then:

```powershell
python tools/content_delivery.py sign --pack reviewed.json --pack-id optional-numbers --version 1 --out optional-numbers.sproutpack
```

The tool requires `cryptography` (tested with 48.0.0) and refuses unapproved release content or an absent exact-digest review. `--development` can sign test packs, but does not mark activities approved and cannot make them load in release mode. No production pack or teacher review was fabricated.

Parents import signed envelopes through the existing document picker without entering a digest. Unsigned JSON plus a manually entered SHA-256 is retained only for development builds. There is no child-area file picker or automatic transmission of progress.

Each signed pack has one `AtomicFile`; validation and ID-conflict checks finish before replacing the installed version. Startup recovers an interrupted write's backup. A durable high-water version and the currently installed signed version prevent downgrades, including after optional packs are removed. Failed validation leaves the previous pack intact. To revert content, publish the corrected older exercises in a higher, newly reviewed/signed pack version. Removing optional packs preserves learning history and version high-water marks.

A network catalogue/download UI is still absent. Shipping one requires an actual HTTPS endpoint, approved catalogue and privacy review. Offline signed imports do not require an Internet permission.

Crypto API reference: [PyCA cryptography ECDSA](https://cryptography.io/en/latest/hazmat/primitives/asymmetric/ec/).
