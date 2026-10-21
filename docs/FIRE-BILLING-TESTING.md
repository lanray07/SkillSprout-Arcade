# Physical Fire billing checks

`FirePurchaseTest` runs the real app, Android Keystore, Amazon SDK and hosted sandbox verifier. It refuses non-debug builds, production receipt configuration and a device without `debug.amazon.sandboxmode=debug`. Use a disposable installation of SkillSprout only: the test creates/uses its own fixed QA parent PIN. Never run it against an installation containing a family's data.

Build `:app:assembleAmazonPreview :app:assembleAmazonPreviewAndroidTest` sequentially. Install both APKs from `app/build/outputs/apk/amazonPreview` and `app/build/outputs/apk/androidTest/amazonPreview`. Do not build the same variant concurrently: Gradle daemons share intermediate outputs.

Before testing, preserve the device's existing `/sdcard/amazon.sdktester.json`, sandbox property and App Tester settings. Merge the two exported SkillSprout term products into the catalogue without changing other products. Exported prices must be checked: the console export used US zero prices despite configured UK prices, so the private test fixture used the accepted values. This does not change live store pricing. The completed physical run used App Tester's US marketplace and synthetic prices; UK display and live charging remain separate checks.

Run the AndroidJUnitRunner with `-e class com.skillsprout.arcade.FirePurchaseTest` and `-e stage catalogue`, `purchase` or `restore`. The purchase stage requires no existing SkillSprout test transaction or cached lease. It opens the actual Amazon confirmation and waits for a separately inspected App Tester confirmation; never confirm a live Appstore purchase. The hierarchy under the app's external `files/fire-qa` folder identifies the confirmation package and buttons. `tools/fire_qa.py` restricts taps to SkillSprout/App Tester and saves private device evidence in an ignored folder. A standalone UIAutomator dump conflicts with active instrumentation; read the test's own hierarchy while it runs.

The restore stage removes only this QA app's signed access fields immediately before pressing Restore, then requires a newly saved response and verified UI. Reports contain stage/model/API, parent screen capture protection, offer count and signed-cache flags. Account IDs, receipt IDs, raw receipts, private preferences and full device logs must stay out of Git.

Afterward restore the original catalogue, sandbox property and any changed App Tester settings. Do not clear App Tester data or delete another app's transactions. `FLAG_SECURE` remains enabled in the parent area throughout these checks. The test temporarily keeps its own Activity awake and clears that flag when finished.

Passing App Tester is sandbox evidence only. It does not certify the production signing candidate, real store charging, cancellation/expiry, account changes, offline recovery, accessibility or the full Fire device matrix.
