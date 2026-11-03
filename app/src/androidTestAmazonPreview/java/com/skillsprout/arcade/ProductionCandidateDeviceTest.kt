package com.skillsprout.arcade

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Configurator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** External accessibility automation: the installed production APK is never instrumented or modified. */
@RunWith(AndroidJUnit4::class)
class ProductionCandidateDeviceTest {
    @Test fun exactSignedCandidateStarterJourneys() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val host=instrumentation.targetContext
        check(BuildConfig.DEBUG && host.packageName=="com.skillsprout.arcade.billingqa")
        val expected=InstrumentationRegistry.getArguments().getString("candidateSha256")!!
        check(expected.matches(Regex("[a-f0-9]{64}")))
        val device=UiDevice.getInstance(instrumentation)
        val apkPath=device.executeShellCommand("pm path com.skillsprout.arcade").lineSequence().first {it.endsWith("/base.apk")}.removePrefix("package:").trim()
        val actual=device.executeShellCommand("sha256sum $apkPath").trim().substringBefore(' ')
        assertEquals("Operate only on the supplied signed candidate",expected,actual)
        val info=host.packageManager.getApplicationInfo("com.skillsprout.arcade",0)
        assertEquals(0,info.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
        check(device.executeShellCommand("getprop debug.amazon.sandboxmode").trim()!="debug")
        val originalIdle=Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout=0
        fun visible(text:String)=device.hasObject(By.text(text))
        fun find(text:String):UiObject2 {
            device.wait(Until.hasObject(By.text(text)),1500)
            repeat(18) {
                device.findObject(By.text(text))?.let {return it}
                device.swipe(device.displayWidth*4/5,device.displayHeight*4/5,device.displayWidth*4/5,device.displayHeight/3,25);Thread.sleep(350)
            }
            repeat(18) {
                device.swipe(device.displayWidth*4/5,device.displayHeight/3,device.displayWidth*4/5,device.displayHeight*4/5,25);Thread.sleep(350)
                device.findObject(By.text(text))?.let {return it}
            }
            throw AssertionError("Control not found: $text")
        }
        fun tap(text:String) {var node=find(text);while(!node.isClickable && node.parent!=null) node=node.parent;node.click();Thread.sleep(500)}
        fun waitFor(text:String) {assertTrue("Missing release state: $text",device.wait(Until.hasObject(By.text(text)),20000))}
        fun hideKeyboard() {
            // Accessibility setText may not open the IME. An unconditional Back would
            // instead leave the parent area, concealing the create/submit control.
            if(device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) device.pressBack()
        }
        fun protectedFields():Int {
            val queue=java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
            instrumentation.uiAutomation.rootInActiveWindow?.let {queue.add(it)}
            var count=0
            while(queue.isNotEmpty()) {
                val node=queue.removeFirst()
                if(node.className=="android.widget.EditText" && node.isPassword) count++
                for(index in 0 until node.childCount) node.getChild(index)?.let {queue.add(it)}
                @Suppress("DEPRECATION") node.recycle()
            }
            return count
        }
        fun unlock() {
            tap("Parent area");waitFor("Unlock")
            val field=device.findObject(By.clazz("android.widget.EditText"))
            assertEquals(1,protectedFields());field.text="764829";hideKeyboard();tap("Unlock")
            waitFor("A little practice. A clearer picture.")
        }
        val folder=File(host.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        val checks=JSONArray()
        try {
            device.wakeUp()
            device.executeShellCommand("am start -W -n com.skillsprout.arcade/.MainActivity")
            waitFor("Save PIN")
            val fields=device.findObjects(By.clazz("android.widget.EditText"))
            assertEquals(2,fields.size);assertEquals(2,protectedFields())
            fields[0].text="764829";fields[1].text="764829";hideKeyboard();tap("Save PIN")
            waitFor("A little practice. A clearer picture.")
            tap("Explorers");waitFor("Explorer nickname")
            device.findObject(By.clazz("android.widget.EditText")).text="Release QA"
            hideKeyboard();tap("Create explorer");waitFor("Explorer created.")
            // The real default bedtime is 20:00. Keep the actual device clock intact
            // and configure only our made-up explorer for an evening test session.
            tap("Controls")
            find("Bedtime starts (hour)")
            // A visible label can precede a control just below the clipped viewport.
            // Bring the whole row into view; the inspected form orders allowance first,
            // bedtime second. Native accessibility also normalises resource whitespace.
            device.swipe(device.displayWidth*4/5,device.displayHeight*3/4,device.displayWidth*4/5,device.displayHeight/2,20);Thread.sleep(600)
            val bedtimeChoice=device.findObjects(By.textStartsWith("20"))
                .lastOrNull()
                ?: throw AssertionError("Missing bedtime control for synthetic explorer")
            bedtimeChoice.parent.click();Thread.sleep(600);device.dumpWindowHierarchy(File(folder,"bedtime-popup.xml"));tap("0");tap("Save controls");find("Controls saved.")
            device.pressBack();waitFor("Parent area")
            val all=ContentPacks(host).starter()
            val free=FamilyAccess(false,0,System.currentTimeMillis()).activities(all,all.map {it.id}.toSet())
            for(game in Game.entries) {
                tap(game.title)
                find("A little hint")
                val pool=free.filter {it.game==game}
                val activity=pool.firstOrNull {visible(it.prompt)} ?: run {
                    val selected=LearningEngine.select(pool,1,emptyMap(),emptyList())!!
                    find(selected.prompt);selected
                }
                tap("A little hint");find(activity.hints.first())
                if(activity.type in listOf("letters","sequence")) {
                    val tiles=if(activity.type=="letters") activity.answer.map {it.toString()} else activity.answer.split(' ')
                    for(tile in tiles) {
                        val item=device.findObjects(By.text(tile)).firstOrNull {it.isEnabled && (it.parent?.isEnabled!=false)}
                            ?: find(tile)
                        item.click()
                    }
                } else tap(activity.answer)
                tap("Check my answer");find("Next challenge")
                checks.put(JSONObject().put("game",game.name).put("question",true).put("hint",true).put("correctAnswerAndFeedback",true))
                device.takeScreenshot(File(folder,"production-${game.name.lowercase()}.png"))
                tap("Next challenge");find("A little hint")
                tap("Pause");find("Continue");tap("Continue")
                device.pressBack();waitFor("Parent area")
            }
            unlock();tap("Premium");find("Restore purchases");find("Refresh store")
            assertFalse("No sandbox-only verified subscription in production",visible("Your Amazon receipt has been verified."))
            device.pressHome();assertTrue("Release activity must actually leave foreground",device.wait(Until.gone(By.pkg("com.skillsprout.arcade")),10000));Thread.sleep(1000)
            device.executeShellCommand("am start -W -n com.skillsprout.arcade/.MainActivity")
            tap("Parent area");waitFor("Unlock")
            assertFalse(visible("Restore purchases"))
            File(folder,"production-candidate.json").writeText(JSONObject().put("apkSha256",actual)
                .put("deviceModel",android.os.Build.MODEL).put("androidApi",android.os.Build.VERSION.SDK_INT)
                .put("nonDebuggable",true).put("freshSyntheticPinAndExplorer",true).put("starterGames",checks)
                .put("parentBillingControlsReachable",true).put("backgroundRelock",true)
                .put("livePurchaseAttempted",false).put("spokenOutputVerified",false).put("productionApproval",false).toString(2))
        } finally {device.dumpWindowHierarchy(File(folder,"production-final-state.xml"));Configurator.getInstance().waitForIdleTimeout=originalIdle}
    }
}
