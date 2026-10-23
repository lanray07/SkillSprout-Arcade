package com.skillsprout.arcade

import android.content.Intent
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Headless: uses real TTS, emits no speech and never opens a saved profile or PIN. */
@RunWith(AndroidJUnit4::class)
class NarrationRecoveryDeviceTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private class ObservedEngine(val delegate:NarrationEngine):NarrationEngine by delegate {
        val initialized=CountDownLatch(1)
        var initializedSuccessfully=false
        var stops=0;var shutdowns=0
        var selected:NarrationVoice?=null
        private var observedVoices=emptyList<NarrationVoice>()
        override fun voices():List<NarrationVoice> = delegate.voices().also {observedVoices=it}
        override fun initialize(onResult:(Boolean)->Unit) {
            delegate.initialize {success->
                initializedSuccessfully=success
                onResult(success)
                initialized.countDown()
            }
        }
        override fun selectVoice(name:String):Boolean {
            val ok=delegate.selectVoice(name)
            if(ok) selected=observedVoices.first {it.name==name}
            return ok
        }
        override fun speak(text:String,volume:Float):Boolean=error("Physical recovery test must never emit speech")
        override fun stop() {stops++;delegate.stop()}
        override fun shutdown() {shutdowns++;delegate.shutdown()}
    }
    private fun restoreEngine(packageName:String) {
        val pipe=instrumentation.uiAutomation.executeShellCommand("pm default-state --user 0 "+packageName)
        val result=ParcelFileDescriptor.AutoCloseInputStream(pipe).bufferedReader().use {it.readText()}
        check("new state: default" in result) {"Known voice-engine restoration failed"}
    }
    @Test fun actualMissingEngineRecoversAndForegroundRebindsWithoutSpeech() {
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.voiceqa")
        check(context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
        check(android.os.Build.MODEL=="KFTRWI")
        check(InstrumentationRegistry.getArguments().getString("missingVoiceExpected")=="true")
        assertEquals(0,Settings.Secure.getInt(context.contentResolver,"accessibility_enabled"))
        assertEquals(0,Settings.Global.getInt(context.contentResolver,"wifi_on"))
        assertTrue(context.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),0).isEmpty())
        val engines=mutableListOf<ObservedEngine>()
        val narrator=OfflineNarration {ObservedEngine(AndroidNarrationEngine(context)).also {engines+=it}}
        fun start():ObservedEngine {
            instrumentation.runOnMainSync {narrator.start()}
            val engine=engines.last()
            assertTrue("Real TTS callback must complete",engine.initialized.await(15,TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            return engine
        }
        try {
            val missing=start()
            assertFalse(missing.initializedSuccessfully);assertFalse(narrator.available.value)
            assertEquals(1,missing.shutdowns)
            // The host helper journaled both original states as default before disabling them.
            restoreEngine("com.ivona.tts.oem")
            restoreEngine("com.svox.pico")
            assertEquals(2,context.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),0).size)
            val recovered=start()
            assertTrue(recovered.initializedSuccessfully);assertTrue(narrator.available.value)
            val selected=requireNotNull(recovered.selected)
            assertEquals("en",selected.language);assertFalse(selected.networkRequired);assertTrue(selected.installed)
            instrumentation.runOnMainSync {narrator.background()}
            assertFalse(narrator.available.value);assertEquals(1,recovered.shutdowns)
            val foreground=start()
            assertTrue(foreground.initializedSuccessfully);assertTrue(narrator.available.value)
            assertNotSame(recovered,foreground)
            instrumentation.runOnMainSync {narrator.close()}
            assertFalse(narrator.available.value);assertEquals(1,foreground.shutdowns)
            val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
            File(folder,"narration-recovery.json").writeText(JSONObject()
                .put("model",android.os.Build.MODEL).put("api",android.os.Build.VERSION.SDK_INT)
                .put("missingInitializationFailed",true).put("sameControllerRecoveredAfterEngineRestore",true)
                .put("backgroundReleasedEngine",true).put("foregroundInitializationAndSelectionSucceeded",true)
                .put("selectedLanguage",selected.language).put("selectedCountry",selected.country)
                .put("selectedVoiceOffline",true).put("spokenOutputTested",false)
                .put("profilesOrPinOpened",false).put("productionApproval",false).toString(2))
        } finally {instrumentation.runOnMainSync {narrator.close()}}
    }
}
