package com.skillsprout.arcade

import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Physical component checks; never opens a stored profile or enters/reads its PIN. */
@RunWith(AndroidJUnit4::class)
class FireMissingVoiceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val instrument get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrument.targetContext
    private val syntheticId = "voice-device-test-" + UUID.randomUUID()
    private var realSpeechAvailable = true

    @Before fun requireActualMissingEngines() {
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID == "com.skillsprout.arcade.voiceqa")
        check(context.packageName == "com.skillsprout.arcade.voiceqa")
        check(!BuildConfig.PURCHASING_ALLOWED)
        check(InstrumentationRegistry.getArguments().getString("missingVoiceExpected") == "true")
        // The synthetic test host may render over the device lock screen; it never
        // unlocks the tablet, opens saved profiles or changes device credentials.
        compose.runOnUiThread {
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
            compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        assertEquals(0, Settings.Global.getInt(context.contentResolver, "wifi_on"))
        assertTrue("No enabled TTS service may remain", context.packageManager.queryIntentServices(
            Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).isEmpty())
        val status = AtomicInteger(999)
        val initialized = CountDownLatch(1)
        var probe: TextToSpeech? = null
        try {
            instrument.runOnMainSync {
                probe = TextToSpeech(context) { result -> status.set(result); initialized.countDown() }
            }
            assertTrue("Real Android TTS initialization must return", initialized.await(10, TimeUnit.SECONDS))
            assertEquals("Missing engines must report ERROR", TextToSpeech.ERROR, status.get())
        } finally {
            instrument.runOnMainSync { probe?.shutdown() }
        }
        realSpeechAvailable = status.get() == TextToSpeech.SUCCESS
        assertFalse("Actual TTS initialization must leave narration unavailable", realSpeechAvailable)
    }

    @After fun removeSyntheticSessionOnly() {
        compose.runOnUiThread { compose.activity.setContent {} }
        val prefs = context.getSharedPreferences("sessions", 0)
        val edit = prefs.edit()
        prefs.all.keys.filter { it.startsWith(syntheticId + "-") }.forEach { edit.remove(it) }
        edit.commit()
    }

    private fun record(name: String, result: JSONObject) {
        val folder = File(context.getExternalFilesDir(null), "fire-qa").apply { mkdirs() }
        File(folder, name + ".json").writeText(result
            .put("model", android.os.Build.MODEL).put("api", android.os.Build.VERSION.SDK_INT)
            .put("actualTtsEngineCount", 0).put("actualTtsInitializationSucceeded", realSpeechAvailable)
            .put("pinEnteredOrRead", false).put("productionApproval", false).toString(2))
    }

    @Test fun missingVoiceDoesNotBlockParentControls() {
        var saved = false
        var previewCalled = false
        val explorer = Child(syntheticId, "Voice test", year = 1, voice = true)
        compose.setContent {
                MaterialTheme {
                    ParentDashboard(listOf(explorer), explorer, emptyList(), emptyList(),
                        ContentPacks(context).starter(), PlayClock(context), ContentPacks(context),
                        {}, { saved = true }, {}, {}, {}, {}, {},
                        previewVoice = { _, _ -> previewCalled = true },
                        speechAvailable = realSpeechAvailable)
                }
        }
        compose.onNodeWithText("Controls", substring = false).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.voice_unavailable)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Preview narration").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Save controls").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(saved); assertFalse(previewCalled) }
        record("missing-voice-parent", JSONObject().put("warningVisible", true)
            .put("previewDisabled", true).put("saveCallbackWorks", true))
    }

    @Test fun allSixGameComponentsRemainUsableWithoutVoice() {
        val results = JSONArray()
        val content = ContentPacks(context).starter()
        val explorer = Child(syntheticId, "Voice test", year = 1, voice = true, reducedMotion = true)
        var selectedGame by mutableStateOf(Game.entries.first())
        var attempts = 0
        compose.setContent {
            val current = content.first { it.game == selectedGame && it.year == 1 }
            MaterialTheme {
                key(selectedGame) {
                    GameScreen(explorer, selectedGame, listOf(current), emptyList(), null,
                        speak = {}, silence = {}, paused = false, onPause = {}, onHome = {},
                        onAttempt = { attempts++ }, onComplete = {})
                }
            }
        }
        for (game in Game.entries) {
            val activity = content.first { it.game == game && it.year == 1 }
            compose.runOnIdle { attempts = 0; selectedGame = game }
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Check my answer").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(context.getString(R.string.hint), substring = false).performScrollTo().performClick()
            compose.onNodeWithText(activity.hints.first(), substring = false).assertExists()
            if (activity.type in listOf("letters", "sequence")) {
                val tokens = if (activity.type == "letters") activity.answer.map { it.toString() }
                    else activity.answer.split(" ")
                val used = mutableMapOf<String, Int>()
                for (token in tokens) {
                    val index = used.getOrDefault(token, 0)
                    compose.onAllNodes(hasText(token, substring = false) and hasClickAction())[index]
                        .performScrollTo().performClick()
                    used[token] = index + 1
                }
            } else {
                val wrong = activity.choices.first { it != activity.answer }
                compose.onNodeWithText(wrong, substring = false).performScrollTo().performClick()
                compose.onNodeWithText("Check my answer").performScrollTo().performClick()
                compose.onNodeWithText(context.getString(R.string.ui_046a7ded41, activity.explanation)).assertExists()
                compose.onNodeWithText(activity.answer, substring = false).performScrollTo().performClick()
            }
            compose.onNodeWithText("Check my answer").performScrollTo().performClick()
            compose.onNodeWithText("Next challenge").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("Check my answer").performScrollTo().assertIsDisplayed()
            compose.runOnIdle { assertEquals(1, attempts) }
            results.put(JSONObject().put("game", game.name).put("activityId", activity.id)
                .put("hintAnswerFeedbackAndNextUsable", true))
        }
        record("missing-voice-games", JSONObject().put("games", results)
            .put("scope", "Real game components with speech suppressed after actual TTS initialization failure; no database attempt writes or full navigation claim."))
    }
}
