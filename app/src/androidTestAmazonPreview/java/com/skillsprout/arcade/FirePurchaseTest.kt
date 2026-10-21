package com.skillsprout.arcade

import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Configurator
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File

/** Physical-device sandbox only: real Activity, Keystore, Amazon SDK and hosted verifier. */
@RunWith(AndroidJUnit4::class)
class FirePurchaseTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val instrument get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrument.targetContext
    private val folder get()=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
    private fun present(text:String)=runCatching {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)
    private fun waitFor(text:String,timeout:Long=30000)=compose.waitUntil(timeout) {present(text)}
    private fun click(text:String){val node=compose.onNodeWithText(text,substring=false);runCatching {node.performScrollTo()};node.performClick()}
    private fun unlock(){
        if(present("A little practice. A clearer picture."))return
        if(present("Save PIN")){
            compose.onNodeWithText("Parent PIN").performTextReplacement("764829")
            compose.onNodeWithText("Repeat PIN").performTextReplacement("764829")
            click("Save PIN")
        }else{
            if(!present("Unlock"))click("Parent area")
            compose.onNodeWithText("Parent PIN").performTextReplacement("764829")
            click("Unlock")
        }
        waitFor("A little practice. A clearer picture.")
    }
    @Test fun physicalSandboxPurchaseAndRestore(){
        check(BuildConfig.DEBUG && BuildConfig.RVS_ENVIRONMENT=="sandbox" && BuildConfig.PURCHASING_ALLOWED) {"Never run this test against production purchasing"}
        val device=UiDevice.getInstance(instrument)
        check(device.executeShellCommand("getprop debug.amazon.sandboxmode").trim()=="debug") {"Amazon App Tester sandbox must be explicitly enabled"}
        val stage=InstrumentationRegistry.getArguments().getString("stage") ?: "catalogue"
        check(stage in setOf("catalogue","purchase","restore"))
        val idle=Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout=0
        try{
            File(folder,"checkpoint.txt").writeText("starting $stage")
            device.wakeUp()
            device.executeShellCommand("wm dismiss-keyguard")
            compose.activityRule.scenario.onActivity {it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
            compose.waitUntil(30000) {present("Save PIN") || present("Parent area") || present("Unlock")}
            unlock()
            File(folder,"checkpoint.txt").writeText("parent unlocked $stage")
            var secure=false
            compose.activityRule.scenario.onActivity {secure=it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE!=0}
            assertTrue("Parent capture protection must remain enabled",secure)
            click("Premium")
            File(folder,"checkpoint.txt").writeText("premium open $stage")
            if(stage=="catalogue" || stage=="purchase"){
                compose.waitUntil(45000) {compose.onAllNodesWithText("Continue with Amazon",substring=true).fetchSemanticsNodes().size==2}
            }
            if(stage=="purchase"){
                check(!context.getSharedPreferences("verified_amazon_access",0).contains("payload")) {"Remove only this app's sandbox transaction and cached lease before a fresh purchase test"}
                val offers=compose.onAllNodesWithText("Continue with Amazon",substring=true)
                val index=InstrumentationRegistry.getArguments().getString("offerIndex")?.toInt() ?: 0
                offers[index].performScrollTo().performClick()
                // The external confirmation is inspected/confirmed separately in App Tester only.
                val deadline=android.os.SystemClock.elapsedRealtime()+120000
                while(!context.getSharedPreferences("verified_amazon_access",0).contains("payload") && android.os.SystemClock.elapsedRealtime()<deadline){
                    device.dumpWindowHierarchy(File(folder,"purchase-confirmation.xml"))
                    File(folder,"prompt-ready.txt").writeText("Sandbox confirmation opened; never confirm a live store purchase.")
                    Thread.sleep(1000)
                }
                assertTrue("A sandbox purchase must yield a verified signed lease",context.getSharedPreferences("verified_amazon_access",0).contains("payload"))
                unlock();click("Premium")
                waitFor("Your Amazon receipt has been verified.",120000)
            }
            if(stage=="restore"){
                // Discard only this QA app's lease, so the button must obtain a new signed response.
                context.getSharedPreferences("verified_amazon_access",0).edit().remove("payload").remove("signature").commit()
                click("Restore purchases")
                File(folder,"checkpoint.txt").writeText("restore requested")
                compose.waitUntil(120000) {context.getSharedPreferences("verified_amazon_access",0).contains("payload")}
                waitFor("Your Amazon receipt has been verified.",120000)
            }
            val verified=present("Your Amazon receipt has been verified.")
            if(stage!="catalogue")assertTrue(verified)
            val cache=context.getSharedPreferences("verified_amazon_access",0)
            val report=JSONObject().put("stage",stage).put("deviceModel",android.os.Build.MODEL).put("androidApi",android.os.Build.VERSION.SDK_INT)
                .put("sandbox",true).put("parentScreenCaptureProtected",secure).put("verified",verified)
                .put("signedAccessSaved",cache.contains("payload") && cache.contains("signature"))
                .put("offerCount",compose.onAllNodesWithText("Continue with Amazon",substring=true).fetchSemanticsNodes().size)
            File(folder,"$stage.json").writeText(report.toString(2))
        }finally{
            compose.activityRule.scenario.onActivity {it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
            Configurator.getInstance().waitForIdleTimeout=idle
        }
    }
}
