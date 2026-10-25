# Pack file selection and parent relock

The OpenDocument launcher and file-selection owner now live in MainActivity.
Opening the picker can stop the app, which still closes the parent area and requires
PIN re-entry. Selected bytes survive that parent-screen disposal in activity memory.
After unlocking, the parent sees File ready and can open Packs to review/install it.
Selection does not install anything, change the content library, or bypass the PIN.
The existing publisher signature, integrity, schema and downgrade checks remain
unchanged and run only when a parent explicitly chooses Install.

The selection owner accepts one picker/read operation at a time. A new request
releases the previous selection; cancellation and launch/read/permission/close
failures leave no pending bytes. Empty files and files over 7,000,000 bytes fail.
Reads run on Dispatchers.IO, check coroutine cancellation and close streams using
use. A stream returning zero bytes from a bulk read falls back to a single-byte
read rather than spinning. Size limits apply on both paths. Cancellation propagates.
A recreated owner rejects an unrequested callback without opening the provider.
Closing or clearing the owner prevents an old read from publishing its result.

No URI grant is persisted, no URI/filename/provider error is displayed or logged,
and no selected bytes are written into saved state or private staging files. Activity
destruction releases the selection, so the parent must choose again after recreation.
A blocked provider read may take time to return before cancellation can close it.
Read errors use generic resource-backed feedback. Status is a polite live region
inside the parent screen; child-facing screens do not show selected-file feedback.
Choose/install/remove controls are disabled while choosing/reading. Clearing the
selection or successful installation drops the memory reference. Selecting another
file resets the typed digest, preventing reuse of an earlier file's digest.

The file picker still accepts application/json, as before. Non-empty bytes being
ready means they were read, not that they are an authenticated or valid pack.
Installation/removal transactions and their broader lifecycle acceptance are outside
this selection fix. No permission, Room schema, content pack, network endpoint or
production flag changed. New interface strings are English; fluent review remains
pending with the other launch materials.

PackFileSelectionTest covers stream boundaries, failures, cancellation, overlap,
recreation, stale results and retry. Phone/tablet Compose checks cover busy controls,
polite feedback and parent relock with a selected-file fixture in both variants.
PackFileSelectionDeviceTest uses actual ContentResolver reads of generated private
cache files on the separate offline Voice QA installation, then deletes them. It
opens no profile or PIN and installs no pack. This is not document-picker UI or
full-app selected-pack lifecycle acceptance; audible feedback and exact signed
candidate checks remain pending.

Actual results/hashes: release/pack-file-software-2026-10-25.json and
release/pack-file-device-2026-10-25.json. No production approval is inferred.
