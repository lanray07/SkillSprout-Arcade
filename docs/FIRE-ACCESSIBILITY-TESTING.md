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

The 22 October Amazon-preview JVM run failed during SDK receiver setup
(`ResponseReceiver.onReceive`, JVM stackmap-frame VerifyError). Those historical
failures remain in `release/production-follow-up-2026-10-22.json`. The 23 October
harness fixes below resolve that startup failure; all 77 preview tests now pass
with JVM verification enabled. See `release/production-follow-up-2026-10-23.json`.
This adds desktop component coverage, not Fire speech or production approval.

23 October physical follow-up: the publisher confirms the updated preview Restore/Refresh controls read clearly and fit at 150% text in both orientations. With Wi-Fi disabled, sandbox Restore produced clear/reachable waiting/verification feedback and both controls remained reachable. The exact installed APK hash was verified; Wi-Fi, VoiceView, font scale and sandbox property were restored and checked. See `release/fire-billing-accessibility-2026-10-23.json`. Automatic announcement timing was not separately established. This is partial preview evidence, not final-candidate approval.

## Separate installation when the original parent PIN is unavailable

Build `:app:assembleVoiceQa :app:lintVoiceQa` for the offline-only debug app
`com.skillsprout.arcade.voiceqa`, labelled **SkillSprout Voice QA**. It uses the
normal MainActivity, PIN setup and parent Controls implementation, with separate
Android app storage and UID. It has no preset PIN or PIN bypass. The publisher
chooses a new PIN privately and uses made-up explorer data. The original
`com.skillsprout.arcade` installation must not be cleared or reset.

This variant has development content/access and purchasing disabled. It cannot
supply Amazon billing coverage or exact production-candidate approval. Verify its
APK identity, permissions, lack of Amazon authentication assets and different
installed UID before the human test.

After the publisher finishes its initial PIN/profile setup, run:

```powershell
python tools/fire_missing_voice_review.py --serial DEVICE_SERIAL --prepare
```

The helper is limited to the reviewed KFTRWI model, user 0 and this separate debug
package. With VoiceView off, it journals the original engine/Wi-Fi settings outside
Git, temporarily disables the two observed TTS engines, verifies no TTS service is
available, turns Wi-Fi off and cold-starts Voice QA. It removes no engine data and
does not change the default voice choice, original app data or Amazon settings.
This is an ordinary-touch fallback check: VoiceView cannot speak without an engine.

Check the missing-voice warning, disabled Preview narration, unchanged-options Save,
and a learning question's touch/hint/answer flow. Record actual publisher findings;
opening the screen is not a pass. Restore immediately afterward (also after a
failed/interrupted check):

```powershell
python tools/fire_missing_voice_review.py --serial DEVICE_SERIAL --restore
```

Restoration returns the exact engine enabled states and Wi-Fi setting, verifies
that accessibility/default-engine settings did not change, and closes Voice QA.
Failure during preparation automatically attempts restoration. Keep the journal
private and retain limitations: shared offline code on one device is not the
Amazon or production candidate, and silent touch fallback is not screen-reader QA.

If the shared Kotlin compiler is busy with other builds, use the documented [in-process compiler strategy](https://kotlinlang.org/docs/compiler-execution-strategy.html) for this invocation: add `-Pkotlin.compiler.execution.strategy=in-process --max-workers=1`. This changes where the test build compiles; it does not change app behaviour or release gates.

Automated component coverage can be built with:

```powershell
.\gradlew.bat -PskillsproutInstrumentationVariant=voiceQa :app:assembleVoiceQa :app:assembleVoiceQaAndroidTest :app:lintVoiceQa
```

Install the resulting Voice QA and Voice QA AndroidTest APKs with `adb install -r`,
using the selected device serial. Unlock the tablet privately first. Then prepare
with the helper and run:

```powershell
adb -s DEVICE_SERIAL shell am instrument -w -e class com.skillsprout.arcade.FireMissingVoiceTest -e missingVoiceExpected true com.skillsprout.arcade.voiceqa.test/androidx.test.runner.AndroidJUnitRunner
```

Always run the restore command in a host `finally` block, including after timeout
or test failure. The runner probes actual Android TTS initialization and requires
ERROR with zero enabled engines and Wi-Fi off. It mounts shared ParentDashboard
and GameScreen components with synthetic data; no parent PIN or database profile
is opened. It checks warning/disabled preview/Save and representative hint, answer,
feedback and Next flows in all six games. No-op speech callbacks intentionally
model unavailable narration. The host may display synthetic test content over the
lock screen, but never unlocks the device; this is insufficient on the reviewed
Fire when the secure keyguard blocks activity resume. Preparation now requires a
privately unlocked tablet. Audible speech, normal PIN navigation, actual stored
settings persistence and production-candidate checks are separate evidence.

23 October automation: the first three runs failed UI setup behind the secure
keyguard. After the publisher privately unlocked the tablet, the unchanged APKs
passed both tests in 13.657 seconds: actual zero-engine/TTS ERROR, parent warning,
disabled preview, Save callback and six representative Year 1 game component
hint/answer/feedback/Next flows. Failed attempts remain in
`release/fire-missing-voice-2026-10-23.json`. Both engine states and Wi-Fi were
restored and verified after every attempt; the original installed APK is unchanged.
Normal PIN/navigation, stored control persistence, audible speech and exact
production candidate checks are outside this component-test evidence.


23 October desktop harness follow-up: Amazon Preview now explicitly declares
`ui-test-manifest` as a preview-only dependency, because custom debug build types
do not inherit the `debugImplementation` test host. Its JVM test resources also
configure `instrumentedPackages=com.amazon.device.iap`, resolving the SDK receiver
stack-map VerifyError during application setup. This follows Robolectric's
[package-level configuration mechanism](https://robolectric.org/configuring/).
The receiver stays in the runtime manifest and JVM verification stays enabled.
Neither setting is added to Amazon Release/Review. Existing physical evidence stays
bound to its original installed APK, rather than the rebuilt desktop-test preview.
See `release/production-follow-up-2026-10-23.json` for final measured test results.

The generic home-screen fixture explicitly injects development access and an
unavailable purchase gateway for its made-up sample missions. It therefore has the
same intended sample access in offline and Amazon-preview JVM runs. Tests of
restricted access continue to request `developmentAccess=false`; this fixture
change does not alter runtime access checks or test real Amazon callbacks.


Further progress while external reviews are pending: the desktop integration test
`missingVoiceControlsPersistAcrossDatabaseReopenAndBackgroundRelock` runs in both
phone/tablet layouts and both offline/Amazon-preview variants. It enters through
ArcadeApp's PIN UI using a synthetic TestVault, saves nickname/volume via the real
parent callback to a temporary file-backed Room database, backgrounds/resumes the
host and requires parent re-entry, then disposes the app, closes/reopens Room and
recreates the app composition. All Child fields are compared with the intended
saved record, including the unchanged voice-enabled preference. Warning/disabled
preview and persisted displayed values are checked after re-entry.

Four new flow configurations pass. The command also ran all 79 offline tests;
the Amazon-preview portion ran the two new flow cases only. Prior full-suite
results stay in their original report. No physical data or private PIN is touched.
Speech readiness is injected false and purchases are unavailable; this does not
test actual MainActivity TTS/keystore, OS process death or spoken output. The
physical tablet was ADB-unauthorized at this check. See
`release/parent-controls-persistence-2026-10-23.json`; physical confirmation stays
pending. CI's existing app test tasks include this new regression flow.


Narration lifecycle update: the app now retries installed offline English voice
selection on foreground entry and releases failed/background clients. Headless
physical recovery on KFTRWI/API 28 passes: no engines -> actual init failure ->
restore engines -> same controller ready with an offline en-GB voice -> background
release -> fresh foreground engine -> final close. This test emitted no speech and
opened no profile or PIN. Original engine/Wi-Fi settings were restored and verified;
accessibility/default-engine settings and the base installed APK were unchanged.
See `NARRATION-RECOVERY.md` for the bounded headless procedure and
`release/narration-recovery-2026-10-23.json` for exact APK/test hashes and results.
The new runtime invalidates any assumption that earlier listening checks approve
the current candidate; audible narration/VoiceView confirmation remains pending.


24 October parent export update: choosing/saving/saved/cancelled/failed messages
expose polite live-region semantics inside the PIN-gated parent Progress tab. Busy
export controls are disabled. New desktop phone/tablet tests verify feedback,
enablement and gate visibility. Physical headless tests use synthetic private-cache
file URIs for UTF-8/truncation/close and actual missing-destination/fresh-retry
behaviour; both pass without profiles, PIN entry, document-picker UI or settings
changes. See `PROGRESS-EXPORT.md` and `release/progress-export-device-2026-10-24.json`.
Actual spoken feedback, external provider/picker acceptance and the exact signed
candidate must still be tested separately.
