package com.skillsprout.arcade

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.amazon.device.drm.LicensingService
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable, separately installed App Tester fixture. Never uses the publisher's real PIN. */
@RunWith(AndroidJUnit4::class)
class IsolatedBillingDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private fun present(text:String)=runCatching {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)
    private fun click(text:String) {
        compose.waitUntil(30000) {runCatching {compose.onAllNodesWithText(text).fetchSemanticsNodes().size==1}.getOrDefault(false)}
        val node=compose.onNodeWithText(text);runCatching {node.performScrollTo()};node.performSemanticsAction(SemanticsActions.OnClick);compose.waitForIdle()
    }
    @Test fun isolatedSandboxCatalogueAndRestore() {
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.billingqa")
        check(context.packageName==BuildConfig.APPLICATION_ID && BuildConfig.RVS_ENVIRONMENT=="sandbox")
        val device=UiDevice.getInstance(instrumentation)
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        check(device.executeShellCommand("getprop debug.amazon.sandboxmode").trim()=="debug")
        compose.waitUntil(10000) {LicensingService.getAppstoreSDKMode()!="UNKNOWN"}
        check(LicensingService.getAppstoreSDKMode()=="SANDBOX") {"Refusing a live store session"}
        // This fixed PIN belongs only to a disposable test application; no private PIN is read.
        compose.waitUntil(30000) {present("Save PIN") || present("Parent area")}
        if(present("Save PIN")) {
            compose.onNodeWithText("Parent PIN").performTextReplacement("764829")
            compose.onNodeWithText("Repeat PIN").performTextReplacement("764829")
            click("Save PIN")
        } else {
            click("Parent area")
            compose.onNodeWithText("Parent PIN").performTextReplacement("764829")
            click("Unlock")
        }
        compose.waitUntil(30000) {present("A little practice. A clearer picture.")}
        click("Premium")
        compose.waitUntil(60000) {compose.onAllNodesWithText("Continue with Amazon",substring=true).fetchSemanticsNodes().size==2 || present("Your Amazon receipt has been verified.")}
        val offers=compose.onAllNodesWithText("Continue with Amazon",substring=true).fetchSemanticsNodes().size
        assertTrue("Must receive store offers or verified existing sandbox access",offers==2 || present("Your Amazon receipt has been verified."))
        compose.onNodeWithText("Restore purchases").performScrollTo().assertIsDisplayed()
        click("Restore purchases")
        compose.waitUntil(60000) { !present("Checking Amazon availability") &&
            (present("Your Amazon receipt has been verified.") || present("Free starter access")) }
        compose.onNodeWithText("Refresh store").performScrollTo().assertIsDisplayed()
        val verifiedBeforeLanguageChange=present("Your Amazon receipt has been verified.")
        val originalLanguage=AppLanguages.selection(context)
        try {
            click(context.getString(R.string.tab_controls))
            compose.waitUntil(30000) {present(context.getString(R.string.interface_language))}
            val currentLabel=if(originalLanguage==AppLanguages.SYSTEM) context.getString(R.string.interface_language_system) else AppLanguages.nativeLabel(originalLanguage)
            compose.onNode(hasText(currentLabel,substring=true) and hasClickAction()).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText(AppLanguages.nativeLabel("ar")).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
            val arabic=AppLanguages.wrap(context)
            compose.waitUntil(30000) {present(arabic.getString(R.string.parent_area))}
            compose.onAllNodesWithText(arabic.getString(R.string.privacy_tab)).assertCountEquals(0)
            click(arabic.getString(R.string.parent_area))
            compose.onNodeWithText(arabic.getString(R.string.ui_addd9d0383)).performTextReplacement("764829")
            click(arabic.getString(R.string.pin_unlock))
            compose.waitUntil(30000) {present(arabic.getString(R.string.tab_premium))}
            click(arabic.getString(R.string.tab_premium))
            compose.waitUntil(30000) {present(arabic.getString(R.string.purchase_restore))}
            compose.onNodeWithText(arabic.getString(R.string.purchase_restore)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(arabic.getString(R.string.purchase_refresh)).performScrollTo().assertIsDisplayed()
        } finally {
            check(AppLanguages.save(context,originalLanguage))
            compose.activityRule.scenario.onActivity {it.recreate()}
        }
        val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(folder,"isolated-billing.json").writeText(JSONObject()
            .put("deviceModel",android.os.Build.MODEL).put("api",android.os.Build.VERSION.SDK_INT)
            .put("package",context.packageName).put("sdkSandboxMode",true).put("offersObserved",offers)
            .put("restoreControlReachable",true).put("refreshControlReachable",true)
            .put("verifiedAccessObserved",verifiedBeforeLanguageChange)
            .put("languageRecreationRelocksParent",true).put("arabicPurchaseControlsRemainAvailable",true)
            .put("activation","Compose accessibility semantics; physical gestures and spoken output not verified")
            .put("purchaseInitiated",false).put("privatePinOrProfilesRead",false)
            .put("liveAppTestingEnrolment",false).put("productionApproval",false).toString(2))
    }
}
