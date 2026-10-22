package com.skillsprout.arcade

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File

/** Actual Android accessibility bridge with VoiceView enabled; no PIN entry or speech claim. */
@RunWith(AndroidJUnit4::class)
class FireAccessibilityTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Test fun voiceViewFocusAndProtectedPinNodes(){
        check(BuildConfig.DEBUG && BuildConfig.RVS_ENVIRONMENT=="sandbox")
        val instrument=InstrumentationRegistry.getInstrumentation()
        org.junit.Assume.assumeTrue("Use the settings-restoring VoiceView runner",InstrumentationRegistry.getArguments().getString("voiceViewExpected")=="true")
        val context=instrument.targetContext
        val automation=instrument.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        automation.serviceInfo=automation.serviceInfo.apply {flags=flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS}
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent 224")).use {it.readBytes()}
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("wm dismiss-keyguard")).use {it.readBytes()}
        compose.activityRule.scenario.onActivity {it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
        val manager=context.getSystemService(AccessibilityManager::class.java)
        val enabled=manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
            .any {it.id.startsWith("com.amazon.logan/")}
        assertTrue("VoiceView must be enabled; default automation must not suppress it",enabled)
        // Traverse native nodes: Compose's virtual node provider need not implement find-by-text.
        fun nodes(node:AccessibilityNodeInfo):List<AccessibilityNodeInfo> = listOf(node)+(0 until node.childCount).flatMap {i->node.getChild(i)?.let {nodes(it)} ?: emptyList()}
        fun named(node:AccessibilityNodeInfo,text:String)=node.text?.toString()?.contains(text)==true || node.contentDescription?.toString()?.contains(text)==true || node.hintText?.toString()?.contains(text)==true
        fun control(root:AccessibilityNodeInfo,text:String):AccessibilityNodeInfo {
            val labels=nodes(root).filter {named(it,text)}
            return labels.asSequence().flatMap {node->generateSequence(node) {it.parent}}
                .firstOrNull {it.isVisibleToUser && (it.isClickable || it.actionList.any {action->action.id==AccessibilityNodeInfo.ACTION_CLICK})}
                ?: throw AssertionError("No visible accessibility click target for $text; matching labels=${labels.size}")
        }
        fun rootWith(text:String):AccessibilityNodeInfo {
            val deadline=android.os.SystemClock.elapsedRealtime()+30000
            var lastPackage="none"
            var lastChildren=0
            while(android.os.SystemClock.elapsedRealtime()<deadline){
                val roots=(listOfNotNull(automation.rootInActiveWindow)+automation.windows.mapNotNull {it.root})
                for(root in roots){
                    lastPackage=root.packageName?.toString() ?: "none"
                    lastChildren=root.childCount
                    if(root.packageName?.toString()==context.packageName && nodes(root).any {named(it,text)})return root
                }
                Thread.sleep(200)
            }
            throw AssertionError("App accessibility root did not expose $text; root package=$lastPackage; children=$lastChildren")
        }
        val root=rootWith("Parent area")
        val parent=control(root,"Parent area")
        automation.executeAndWaitForEvent({
            parent.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
            parent.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        },{it.eventType==AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED && it.packageName==context.packageName},5000)
        assertTrue("Accessibility click must open the parent gate",parent.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        // Native accessibility actions do not drive Compose's controlled test clock.
        compose.waitForIdle()
        val pinRoot=rootWith("Parent PIN")
        val all=nodes(pinRoot)
        assertTrue("PIN input must expose password semantics",all.any {it.isPassword && it.isEditable})
        val unlock=control(pinRoot,"Unlock")
        automation.executeAndWaitForEvent({
            unlock.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
            unlock.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        },{it.eventType==AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED && it.packageName==context.packageName},5000)
        assertFalse("Private parent controls must remain behind the PIN gate",all.any {named(it,"Restore purchases")})
        assertTrue("VoiceView must remain enabled through the complete check",manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).any {it.id.startsWith("com.amazon.logan/")})
        val directory=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(directory,"voiceview-nodes.json").writeText(JSONObject()
            .put("deviceModel",android.os.Build.MODEL).put("androidApi",android.os.Build.VERSION.SDK_INT)
            .put("voiceViewServiceEnabled",enabled).put("parentControlFocusEvent",true)
            .put("accessibilityClickOpensPinGate",true).put("pinPasswordSemantics",true)
            .put("unlockControlFocusEvent",true).put("purchaseControlsBehindPin",true)
            .put("pinEntered",false).put("spokenOutputVerified",false)
            .put("gestureExplorationVerified",false).put("productionApproval",false).toString(2))
    }
}
