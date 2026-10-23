# Offline narration recovery

The app now rechecks narration on foreground entry. If a parent enables an offline
English voice while the app is in the background, returning to the app retries
initialization without requiring a full app restart. Parent volume/voice settings
and saved explorers are unchanged by this operation.

`OfflineNarration` owns readiness and the engine lifecycle. `AndroidNarrationEngine`
is the platform adapter. MainActivity starts the controller in onStart, releases
its engine in onStop and closes it permanently in onDestroy. Readiness is observed
by the existing app UI, including the unavailable-voice warning and disabled
narration preview.

| Condition | Behaviour |
|---|---|
| Initialization pending/fails | Narration stays unavailable; no text is queued. |
| Installed offline English voices exist | Prefer UK English, then another offline English voice; try a fallback if voice selection fails. |
| Network-only, other-language or SDK-reported uninstalled voice | Exclude it from selection. |
| Voice selection accepted | Enable narration availability. |
| Immediate or asynchronous synthesis failure | Clear availability and release the engine. |
| App backgrounded | Stop/release the client and reject its late callbacks. |
| App foregrounded again | Create a new engine and retry availability. |
| Blank text, zero/negative or non-finite volume | Emit no speech. Positive volume is bounded to 1.0. |

Android's [TextToSpeech API reference](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
describes immediate initialization-failure callbacks, success/error return values
for voice selection and utterance-progress errors. The adapter posts callbacks to
the main thread after construction and listener setup. A generation token rejects
callbacks from released clients. Only the application context is retained by the
engine factory. No new permission, remote narration service or speech logging was
added.

## Verification

The JVM tests use controllable engine ports and Robolectric's Android TTS shadow.
They cover synchronous/delayed initialization, offline/installed/language filtering,
UK preference and fallback, failed selection, initialization/synthesis exceptions,
volume bounds, stale callbacks, foreground recovery, background release and final
close. A MainActivity test exercises its actual lifecycle wiring. These tests do
not establish audible output quality.

Physical automation uses the separate `com.skillsprout.arcade.voiceqa` package.
Build with `-PskillsproutInstrumentationVariant=voiceQa` and the tasks
`:app:assembleVoiceQa :app:assembleVoiceQaAndroidTest`. Install only those two QA
APKs with `adb install -r`; preserve the base app and the existing Voice QA data.

Prepare with `python tools/fire_missing_voice_review.py --serial DEVICE_SERIAL
--headless --prepare` as one command. The headless option opens no app screen and
does not need the device lock screen dismissed. The original accessibility-off,
known-engine/default-state, model, installed-debug-package and APK-hash checks
remain required. Settings are journalled outside Git.

Run instrumentation class `com.skillsprout.arcade.NarrationRecoveryDeviceTest`
using runner `com.skillsprout.arcade.voiceqa.test/androidx.test.runner.AndroidJUnitRunner`
and argument `-e missingVoiceExpected true`. It verifies zero TTS services and
Wi-Fi off, actual initialization failure, recovery in the same controller after
restoring the two known engines, and another foreground rebind. It opens no saved
profile or PIN and its engine wrapper rejects speech requests. It restores only
the known engines to the states validated by the host helper.

Always run `python tools/fire_missing_voice_review.py --serial DEVICE_SERIAL
--restore` in a host finally block, including on failure/timeout. The helper
restores and verifies engine/Wi-Fi settings and checks that accessibility and the
default-engine setting stayed unchanged. The headless option is for this test;
UI checks still require a privately unlocked device.

Measured results and exact hashes are recorded in
`release/narration-software-2026-10-23.json` and
`release/narration-recovery-2026-10-23.json`. Earlier physical and publisher-listening
results remain tied to their original APKs. Recheck narration/VoiceView interaction
on the final candidate; controller tests do not approve speech quality, keyboard,
wider devices, content, privacy, brand, billing or the production release.
