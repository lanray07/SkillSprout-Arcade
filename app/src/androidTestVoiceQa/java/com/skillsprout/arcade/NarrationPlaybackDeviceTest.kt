package com.skillsprout.arcade

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.AudioManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Emits only public synthetic learning text; never opens a user profile or records microphone input. */
@RunWith(AndroidJUnit4::class)
class NarrationPlaybackDeviceTest {
    @Test fun realControllerProducesAudioAndCompletesMathsAndSpellingUtterances() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        val context=instrument.targetContext
        check(BuildConfig.DEBUG && context.packageName=="com.skillsprout.arcade.voiceqa")
        val audio=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        assertTrue("Media volume must be audible",audio.getStreamVolume(AudioManager.STREAM_MUSIC)>0)
        lateinit var adapter:AndroidNarrationEngine
        val narrator=OfflineNarration {AndroidNarrationEngine(context).also {adapter=it}}
        instrument.runOnMainSync {narrator.start()}
        val deadline=System.currentTimeMillis()+15000
        while(!narrator.available.value && System.currentTimeMillis()<deadline) Thread.sleep(100)
        assertTrue("An installed offline English voice must be selected",narrator.available.value)
        val field=AndroidNarrationEngine::class.java.getDeclaredField("tts").apply {isAccessible=true}
        val tts=field.get(adapter) as TextToSpeech
        val samples=JSONArray()
        try {
            for(text in listOf("1 + 1 = ?","cat")) {
                val done=CountDownLatch(1);val bytes=AtomicLong();var started=false;var failed=false
                tts.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
                    override fun onStart(id:String?) {started=true}
                    override fun onDone(id:String?) {done.countDown()}
                    override fun onError(id:String?) {failed=true;done.countDown()}
                    override fun onAudioAvailable(id:String?,data:ByteArray?) {bytes.addAndGet(data?.size?.toLong() ?: 0)}
                })
                instrument.runOnMainSync {narrator.speak(text,0.8f)}
                assertTrue("Speech must finish, not merely enqueue",done.await(20,TimeUnit.SECONDS))
                assertFalse("Platform synthesis/playback error",failed)
                assertTrue("Platform playback must start",started)
                samples.put(JSONObject().put("text",text).put("started",started).put("completed",true).put("audioCallbackBytes",bytes.get()))
            }
            val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
            File(folder,"narration-playback.json").writeText(JSONObject().put("deviceModel",android.os.Build.MODEL)
                .put("api",android.os.Build.VERSION.SDK_INT).put("voiceLanguage",tts.voice?.locale?.language)
                .put("voiceCountry",tts.voice?.locale?.country).put("networkRequired",tts.voice?.isNetworkConnectionRequired)
                .put("mediaVolume",audio.getStreamVolume(AudioManager.STREAM_MUSIC)).put("samples",samples)
                .put("humanListeningConfirmed",false).put("microphoneUsed",false).toString(2))
        } finally {instrument.runOnMainSync {narrator.close()}}
    }
}
