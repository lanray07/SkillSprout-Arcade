package com.skillsprout.arcade

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class NarrationGameDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun mathsAndSpellingReadAloudTapProducesRealOfflineAudio() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        val context=instrument.targetContext
        check(BuildConfig.DEBUG && context.packageName=="com.skillsprout.arcade.voiceqa")
        androidx.test.uiautomator.UiDevice.getInstance(instrument).wakeUp()
        val lock=context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        check(!lock.isKeyguardLocked) {"Unlock the Fire tablet privately before the on-screen playback test"}
        val child=Child("narration-"+UUID.randomUUID(),"Voice test",year=1,reducedMotion=true)
        lateinit var engine:AndroidNarrationEngine
        val narrator=OfflineNarration {AndroidNarrationEngine(context).also {engine=it}}
        compose.runOnUiThread {
            compose.activity.setTurnScreenOn(true)
            compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            narrator.start()
        }
        compose.waitUntil(15000) {narrator.available.value}
        val field=AndroidNarrationEngine::class.java.getDeclaredField("tts").apply {isAccessible=true}
        val tts=field.get(engine) as TextToSpeech
        val content=ContentPacks(context).starter()
        var selected by mutableStateOf(Game.MATHS)
        val requests=mutableListOf<String>()
        var explicitPlayback=false
        val result=JSONArray()
        compose.setContent {
            MaterialTheme {key(selected) {
                GameScreen(child,selected,listOf(content.first {it.year==1 && it.game==selected}),emptyList(),null,
                    speak={requests+=it;if(explicitPlayback) narrator.speak(it,0.8f)},silence=narrator::stop,paused=false,
                    onPause={},onHome={},onAttempt={},onComplete={},speechAvailable=narrator.available.value)
            }}
        }
        try {
            for(game in listOf(Game.MATHS,Game.SPELLING)) {
                compose.runOnIdle {explicitPlayback=false;selected=game}
                compose.waitForIdle()
                // Isolate the explicit tap: the automatic question callback is recorded
                // but not played, so its completion cannot satisfy this button assertion.
                val done=CountDownLatch(1);val bytes=AtomicLong();var started=false;var failed=false
                tts.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
                    override fun onStart(id:String?) {started=true}
                    override fun onDone(id:String?) {done.countDown()}
                    override fun onError(id:String?) {failed=true;done.countDown()}
                    override fun onAudioAvailable(id:String?,data:ByteArray?) {bytes.addAndGet(data?.size?.toLong() ?: 0)}
                })
                val before=requests.size
                compose.runOnIdle {explicitPlayback=true}
                compose.onNodeWithText("Read aloud").performScrollTo().assertIsEnabled().performClick()
                assertTrue("Read aloud must complete actual playback",done.await(20,TimeUnit.SECONDS))
                assertTrue(started);assertFalse(failed);assertTrue("Real audio buffers required",bytes.get()>0)
                val item=content.first {it.year==1 && it.game==game}
                assertEquals(before+1,requests.size)
                assertEquals(if(game==Game.SPELLING) item.answer else item.prompt,requests.last())
                result.put(JSONObject().put("game",game.name).put("buttonTapped",true)
                    .put("correctLearningTextRequested",true).put("playbackStartedAndCompleted",true).put("audioBytes",bytes.get()))
            }
            File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
                .resolve("read-aloud-games.json").writeText(JSONObject().put("deviceModel",android.os.Build.MODEL)
                    .put("api",android.os.Build.VERSION.SDK_INT).put("games",result)
                    .put("offlineVoice",tts.voice?.isNetworkConnectionRequired==false)
                    .put("humanListeningConfirmed",false).put("personalProfileOrPinOpened",false).toString(2))
        } catch(error:Throwable) {
            val device=androidx.test.uiautomator.UiDevice.getInstance(instrument)
            val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
            File(folder,"read-aloud-failure.json").writeText(JSONObject().put("lifecycle",compose.activity.lifecycle.currentState.name)
                .put("finishing",compose.activity.isFinishing).put("destroyed",compose.activity.isDestroyed)
                .put("foregroundPackage",device.currentPackageName).put("requests",requests.toString()).toString(2))
            if(device.currentPackageName==context.packageName) {device.dumpWindowHierarchy(File(folder,"read-aloud-failure.xml"));device.takeScreenshot(File(folder,"read-aloud-failure.png"))}
            throw error
        } finally {
            compose.runOnUiThread {compose.activity.setContent {};narrator.close()}
            val prefs=context.getSharedPreferences("sessions",0)
            val edit=prefs.edit();prefs.all.keys.filter {it.startsWith(child.id+"-")}.forEach {edit.remove(it)};edit.commit()
        }
    }
}
