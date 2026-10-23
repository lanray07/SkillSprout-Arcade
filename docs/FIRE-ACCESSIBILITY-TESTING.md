# Fire accessibility review

Use a dedicated debug QA installation with synthetic explorer data. Never enter a
PIN, real child's details or purchase identifiers into the public evidence files.

`AccessibilityReviewActivity` is compiled only from `src/amazonPreview`; its
manifest is selected only for that build type. Production and review-only store
variants continue to use `src/amazon/AndroidManifest.xml` without this activity.
The shortcut renders the existing `GameScreen` with an allowlisted bundled
activity, synthetic explorer, app narration disabled and no database attempt or
completion writes. It is a diagram review harness: its session/pause callbacks
are stubs, so review pause, parent controls and app lifecycle in the normal app.
Synthetic session preferences are cleared when the harness opens and closes.

Build and install the preview. If Kotlin incremental compilation fails to resolve
existing top-level symbols after adding the source set, use a full compilation:

```powershell
.\gradlew.bat --max-workers=2 '-Pkotlin.incremental=false' :app:assembleAmazonPreview
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/amazonPreview/app-amazonPreview.apk
python tools/fire_accessibility_review.py --serial DEVICE_SERIAL --case clock-3
```

Cases are `clock-3`, `clock-830`, `halves`, `fruit`, `bus` and `shapes`. They map
to exact activity IDs in the helper. It verifies the installed APK hash and debug
flag, saves the original VoiceView settings privately, and enables VoiceView.
Run each case separately. A person must listen and use touch exploration; opening
the screen is not a pass. Record the actual description, navigation and result.
The `number` metadata does not render a separate diagram. The `array` metadata
drives the Times Table Heroes group/counter controls, reviewed separately.

For real app states:

```powershell
python tools/fire_accessibility_review.py --serial DEVICE_SERIAL --app --large-text
```

This temporarily sets text to 150%. Review PIN errors/temporary lockout/recovery,
dropdowns, confirmation warnings and Cancel, long text, Premium controls, learning
feedback, scrolling, rotation, pause/resume and return focus. Use a synthetic
explorer and cancel destructive dialogs. Keep physical-keyboard, missing offline
TTS, offline billing errors, wider devices and final signed-candidate results
separate; never infer them from a touch test or injected configuration.

Always finish with:

```powershell
python tools/fire_accessibility_review.py --serial DEVICE_SERIAL --restore
```

The helper restores and verifies the prior VoiceView and font settings, closes
the harness and opens the normal app. Private restoration state stays under the
ignored `artifacts/amazon-submission/fire-qa` directory. Earlier publisher checks
are bound to their original APK; new results must identify the installed APK hash.
See `release/fire-voiceview-diagrams-states-2026-10-22.json`. Passing these cases
does not approve every activity, all failure states or the production release.

## Billing status and large-text component checks

Billing status text now exposes a polite accessibility live region, so a transition
from loading to pending/verification can be announced by a screen reader. Restore
and Refresh wrap onto separate lines when the available width is small.
`pendingPurchaseStatusIsAnnouncedAndRecoveryControlsFitLargeText` checks the shared
component at 320dp width and 150% text on both JVM phone/tablet configurations:
busy Restore is disabled, pending feedback has live-region semantics, and both
recovery actions stay within the panel and invoke their callbacks.

These are injected component checks, not actual Amazon callbacks or Fire speech
validation. Recheck the changed billing UI with VoiceView on the exact candidate.
Earlier physical passes remain tied to their earlier APK hashes.

The Amazon-preview JVM suite currently fails during application setup for tests
that load the Amazon SDK receiver (`ResponseReceiver.onReceive`, JVM stackmap-frame
VerifyError). Do not count these startup failures as exercised UI tests or disable
JVM bytecode verification to make them pass. The shared offline app suite and
isolated billing adapter suite can run independently; compiling/linting the Amazon
preview and physical sandbox testing provide separate evidence. The remaining
Amazon-preview JVM harness compatibility issue is recorded in
`release/production-follow-up-2026-10-22.json`.

23 October physical follow-up: the publisher confirms the updated preview Restore/Refresh controls read clearly and fit at 150% text in both orientations. With Wi-Fi disabled, sandbox Restore produced clear/reachable waiting/verification feedback and both controls remained reachable. The exact installed APK hash was verified; Wi-Fi, VoiceView, font scale and sandbox property were restored and checked. See `release/fire-billing-accessibility-2026-10-23.json`. Automatic announcement timing was not separately established. This is partial preview evidence, not final-candidate approval.
