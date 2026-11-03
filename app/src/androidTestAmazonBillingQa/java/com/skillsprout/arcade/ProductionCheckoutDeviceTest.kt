package com.skillsprout.arcade

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** Exact installed production binary; open each Amazon offer and cancel. Never confirm a paid order. */
@RunWith(AndroidJUnit4::class)
class ProductionCheckoutDeviceTest {
    @Test fun bothProductionOfferButtonsOpenAmazonCheckoutWithoutPayment() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        val host=instrument.targetContext
        check(BuildConfig.DEBUG && host.packageName=="com.skillsprout.arcade.billingqa")
        val expected=InstrumentationRegistry.getArguments().getString("candidateSha256")!!
        val device=UiDevice.getInstance(instrument)
        val path=device.executeShellCommand("pm path com.skillsprout.arcade").lineSequence().first {it.endsWith("/base.apk")}.removePrefix("package:").trim()
        assertEquals(expected,device.executeShellCommand("sha256sum $path").trim().substringBefore(' '))
        check(device.executeShellCommand("getprop debug.amazon.sandboxmode").trim()!="debug")
        val folder=File(host.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        val fixture=JSONObject(File(folder,"production-candidate.json").readText())
        check(fixture.getString("apkSha256")==expected && fixture.getBoolean("freshSyntheticPinAndExplorer"))
        val checks=JSONArray()
        val idle=Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout=0
        fun tap(node:UiObject2) {var button=node;while(!button.isClickable && button.parent!=null) button=button.parent;button.click();Thread.sleep(500)}
        fun find(text:String):UiObject2 {
            repeat(8) {device.findObject(By.text(text))?.let {return it};device.swipe(1600,950,1600,400,25);Thread.sleep(300)}
            repeat(8) {device.swipe(1600,400,1600,950,25);Thread.sleep(300);device.findObject(By.text(text))?.let {return it}}
            throw AssertionError("Expected synthetic test control: $text")
        }
        fun premium() {
            device.executeShellCommand("am start -W -n com.skillsprout.arcade/.MainActivity")
            if(!device.hasObject(By.text("Unlock"))) tap(find("Parent area"))
            assertTrue(device.wait(Until.hasObject(By.text("Unlock")),10000))
            device.findObject(By.clazz("android.widget.EditText")).text="764829"
            if(device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) device.pressBack()
            tap(find("Unlock"));assertTrue(device.wait(Until.hasObject(By.text("Premium")),10000));tap(find("Premium"))
        }
        try {
            val selected=mutableSetOf<String>()
            for(index in 0..1) {
                premium()
                val selector=By.textStartsWith("Continue with Amazon")
                var offer:UiObject2?=null
                repeat(32) { attempt->
                    if(offer==null) {
                        offer=device.findObjects(selector).firstOrNull {it.text !in selected}
                        if(offer==null) {if(attempt%16<8) device.swipe(1600,950,1600,400,25) else device.swipe(1600,400,1600,950,25);Thread.sleep(500)}
                    }
                }
                assertNotNull("Amazon must supply both production-priced offers",offer)
                val label=offer!!.text;selected+=label;tap(offer!!)
                val opened=device.wait(Until.hasObject(By.pkg("com.amazon.venezia")),20000)
                val productVisible=opened && device.wait(Until.hasObject(By.textContains("Family Premium")),10000)
                checks.put(JSONObject().put("offerLabel",label).put("amazonCheckoutOpened",opened).put("familyProductVisible",productVisible).put("paymentConfirmed",false))
                assertTrue("Continue with Amazon must open Amazon's checkout",opened)
                assertTrue("Amazon checkout must identify the Family Premium product",productVisible)
                device.pressBack()
                assertTrue(device.wait(Until.hasObject(By.pkg("com.skillsprout.arcade")),10000))
            }
            assertEquals(2,selected.size)
        } finally {
            if(device.currentPackageName=="com.amazon.venezia") device.pressBack()
            File(folder,"production-checkout-buttons.json").writeText(JSONObject().put("apkSha256",expected)
                .put("checks",checks).put("paymentConfirmed",false).put("testAccountConfigured",true).toString(2))
            Configurator.getInstance().waitForIdleTimeout=idle
        }
    }
}
