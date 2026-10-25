# Bounded content-pack reads

Installed content files are now size-checked while reading, before creating a full
byte array or parsing JSON. Previously readBytes/readText loaded the whole file
before the existing envelope/payload checks could reject it. Oversized optional
files are skipped through the existing installed-pack failure path, so valid packs
and the mandatory starter pack can still load. Invalid files are not automatically
deleted; removal/replacement remains an explicit parent action.

The limits are 7,000,000 bytes for signed envelopes, 5,000,000 for raw payloads,
64 bytes for stored digest text, and 65,536 each for the bundled manifest and public
DER key. Envelope/payload limits are unchanged; the small metadata reads now have
explicit bounds. Starter assets, installed signed/unsigned files, AtomicFile backup
recovery and existing-file metadata reads before signed replacement all use the
bounded reader. Every opened stream has a caller-owned use block, including assets.
No new read uses File.length or available as its authority; growing or short-read
streams are limited by actual bytes consumed.

The reader consumes at most the limit plus one detection byte and throws on excess.
Zero bulk reads fall back to one-byte reads without spinning; that path has the same
limit. Callers retain stream ownership. File selection now shares this helper and
passes its existing coroutine cancellation check on every read, retaining its
empty-file/failure/retry behavior. ContentPacks remains a synchronous API called from
existing IO contexts; no new cancellation guarantee for JSON/signature work is made.

Publisher keys/signature validation, payload digests, namespaces, schema validation,
review-status rules and version-ledger rollback protection are unchanged. A damaged
current file does not reset its version ledger: a same/older replacement still
fails, while a correctly signed newer version can repair it. The helper does not
change bytes, approval status, content versions, permissions, Room data or endpoints.
These are per-file bounds, not a total installed-library memory or performance limit.
A blocked provider read may still take time to return before cancellation is seen.

BoundedInputTest checks limits, detection-byte consumption, short/zero reads,
misleading available values, cancellation and stream ownership. ProductionPathsTest
adds stored signed/raw/digest/backup oversize cases, newer signed recovery, unchanged
rollback denial and unsigned digest rejection, using an ephemeral test publisher key
inside isolated Robolectric storage. Existing signature/tamper/atomic recovery tests
and all file-selection regressions remain in the complete app suites.

BoundedInputDeviceTest uses generated private cache files on the separate offline
Voice QA installation: exact 7 MB acceptance, one-byte-over rejection/close and real
AtomicFile backup recovery. It does not enumerate installed packs, import content,
open a profile/PIN or exercise full-app startup UI. Generated files are deleted.
Broader low-memory/performance, installed-library startup and exact signed-candidate
acceptance remain pending. No production approval is inferred.

Actual results/hashes: release/bounded-pack-software-2026-10-25.json and
release/bounded-pack-device-2026-10-25.json.

Windows host-harness finding: API 35 AtomicFile commits by File.renameTo, which on
this Windows JVM refused an existing destination. The minimized two-small-pack
repro left base version 1, a pending .new file and ledger 2; no oversized read was
needed. A direct generated-file probe reproduced File.renameTo failure and NIO
ATOMIC_MOVE/REPLACE_EXISTING success. ProductionPathsTest now uses a narrowly scoped
API 29+ shadow of only AtomicFile.rename to model Android's replacement semantics.
All validation/version rules and the production AtomicFile write path are unchanged.
The regression asserts the committed version and installed pack after replacement.
This host adaptation does not establish native API 35 filesystem/power-loss coverage.
The prepared API 28 Fire check uses actual AtomicFile recovery and existing-file
replacement without this shadow. The retry passed both physical tests after USB authorization was restored.
The original app and checked settings were unchanged; the failed earlier attempt
is retained in the device report. The diagnostic probe is kept only under ignored artifacts/debug.
