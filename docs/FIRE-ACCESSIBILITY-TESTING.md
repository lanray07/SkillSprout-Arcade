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
