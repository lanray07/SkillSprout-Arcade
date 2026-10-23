package com.skillsprout.arcade

import android.content.Context
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35]) @LooperMode(LooperMode.Mode.PAUSED)
class AndroidNarrationEngineTest {
    private lateinit var narrator:OfflineNarration
    private fun voice(name:String,locale:Locale=Locale.UK,network:Boolean=false,features:Set<String> = emptySet(),quality:Int=Voice.QUALITY_NORMAL)=
        Voice(name,locale,quality,Voice.LATENCY_NORMAL,network,features)
    private fun start() {
        narrator=OfflineNarration {AndroidNarrationEngine(ApplicationProvider.getApplicationContext<Context>())}
        narrator.start()
    }
    private fun finishInitialization(status:Int=TextToSpeech.SUCCESS) {
        shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()).onInitListener.onInit(status)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @After fun cleanUp() {if(::narrator.isInitialized) narrator.close();shadowOf(Looper.getMainLooper()).idle()}

    @Test fun platformAdapterSelectsInstalledOfflineEnglishAndPassesVolume() {
        ShadowTextToSpeech.addVoice(voice("uk"))
        ShadowTextToSpeech.addVoice(voice("network",network=true,quality=Voice.QUALITY_VERY_HIGH))
        ShadowTextToSpeech.addVoice(voice("download",features=setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED),quality=Voice.QUALITY_VERY_HIGH))
        ShadowTextToSpeech.addVoice(voice("foreign",locale=Locale.FRANCE,quality=Voice.QUALITY_VERY_HIGH))
        start()
        val tts=ShadowTextToSpeech.getLastTextToSpeechInstance()
        val platform=shadowOf(tts)
        platform.onInitListener.onInit(TextToSpeech.SUCCESS)
        assertFalse(narrator.available.value) // Callback is posted after constructor/driver setup.
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(narrator.available.value);assertEquals("uk",platform.currentVoice.name)
        narrator.speak("Test narration",0.3f)
        assertEquals("Test narration",platform.lastSpokenText)
        assertEquals(TextToSpeech.QUEUE_FLUSH,platform.queueMode)
        assertEquals(0.3f,requireNotNull(ShadowTextToSpeech.getLastParams()).getFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME),0.0001f)
        narrator.background()
        assertTrue(platform.isStopped);assertTrue(platform.isShutdown);assertFalse(narrator.available.value)
    }
    @Test fun initializationFailureAndLateCompletionNeverEnableShutdownEngine() {
        ShadowTextToSpeech.addVoice(voice("uk"));start()
        val platform=shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        finishInitialization(TextToSpeech.ERROR)
        assertFalse(narrator.available.value);assertTrue(platform.isShutdown)
        platform.onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(narrator.available.value)
    }
    @Test fun mainActivityRecreatesNarrationEngineAfterBackgrounding() {
        val activity=org.robolectric.Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        try {
            val old=ShadowTextToSpeech.getLastTextToSpeechInstance()
            assertNotNull(old)
            activity.pause().stop()
            assertTrue(shadowOf(old).isShutdown)
            activity.start().resume()
            assertNotSame(old,ShadowTextToSpeech.getLastTextToSpeechInstance())
        } finally {activity.pause().stop().destroy()}
    }

    @Test fun synthesisErrorDisablesVoiceAndNextForegroundEntryRecovers() {
        ShadowTextToSpeech.addVoice(voice("uk"));start();finishInitialization()
        val old=ShadowTextToSpeech.getLastTextToSpeechInstance()
        assertTrue(narrator.available.value)
        shadowOf(old).utteranceProgressListener.onError("pip",TextToSpeech.ERROR_SYNTHESIS)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(narrator.available.value);assertTrue(shadowOf(old).isShutdown)
        narrator.start();finishInitialization()
        assertNotSame(old,ShadowTextToSpeech.getLastTextToSpeechInstance())
        assertTrue(narrator.available.value)
    }
}
