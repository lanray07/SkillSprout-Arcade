# Progress report export

Progress CSV export now uses one in-memory snapshot per request. The parent-area
export button is disabled while the file picker or writer is active. The parent
Progress tab displays choosing/saving/saved/cancelled/failed feedback with polite
live-region semantics. No report text, destination URI or provider error detail is
shown outside the parent area.

MainActivity uses CreateDocument and opens the selected URI in explicit `wt` mode.
[Android's ContentResolver reference](https://developer.android.com/reference/android/content/ContentResolver)
documents nullable streams, file-not-found errors and provider-specific write-mode
behaviour. Unavailable/denied destinations and write/flush/close failures now produce
failure feedback instead of escaping the export coroutine. A failed destination
can contain a partial file; the message directs the parent to choose a new file
and retry. The app does not delete a user-selected document on failure.

## Request lifecycle

| Event | Result |
|---|---|
| Parent requests export | Capture the CSV snapshot and launch one picker. |
| Another request while busy | Keep the original snapshot; launch no second picker. |
| Picker cancelled | Drop the pending snapshot and allow another export. |
| Destination returned | Consume the snapshot and write UTF-8 off the UI thread. |
| Stream open/write/flush/close fails | Report failure and allow a fresh request. |
| Write, flush and close all return successfully | Report saved. |
| Activity recreated with no pending snapshot | Report failure; never open a stream or write an empty replacement. |
| Owner destroyed | Drop its pending snapshot and suppress late result/status updates. |

CSV snapshots are not persisted in saved state, Room or an app cache. A report lost
through process/activity recreation must be exported again after parent re-entry.
Coroutine cancellation is propagated; stream cleanup runs when blocking I/O
returns. A provider write is not a generic atomic transaction and is not guaranteed
to be immediately interruptible. Success is not reported before close completes.
Parent PIN gating and the existing CSV field escaping/format remain unchanged.
No new permission or remote export service was added.

## Verification scope

JVM tests cover UTF-8 data, null/denied destinations, partial write, flush/close
errors, cancellation, busy/duplicate requests, retry with a fresh snapshot, lost
state after recreation and stale completion after owner close. Phone and tablet
UI tests check every status, button enablement, polite semantics and feedback
remaining behind the normal parent PIN screen with synthetic test credentials.

The headless physical class `ProgressExportDeviceTest` targets only the separate
debug package `com.skillsprout.arcade.voiceqa`. It uses generated private-cache
files and Android ContentResolver file URIs to check UTF-8 bytes, truncation,
close, an actual missing destination and successful retry. Test-owned files are
deleted in finally blocks. No real report, profile, PIN or external document picker
is used, and no device settings need changing.

Build using `-PskillsproutInstrumentationVariant=voiceQa` with
`:app:assembleVoiceQa :app:assembleVoiceQaAndroidTest`; install only those QA APKs
with `adb install -r`. Run class `com.skillsprout.arcade.ProgressExportDeviceTest`
using `com.skillsprout.arcade.voiceqa.test/androidx.test.runner.AndroidJUnitRunner`.
No missing-voice preparation helper or system-setting changes are required.

Exact results/hashes are in `release/progress-export-software-2026-10-24.json` and
`release/progress-export-device-2026-10-24.json`. External document-provider/picker
UI, actual VoiceView announcement timing and final signed-candidate checks remain
separate acceptance work. The new English status messages do not establish
translation, privacy or production approval.
