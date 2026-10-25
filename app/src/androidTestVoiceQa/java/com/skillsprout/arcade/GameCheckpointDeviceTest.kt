package com.skillsprout.arcade

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Generated local checkpoint only; no Activity, Room profile or parent PIN is opened. */
@RunWith(AndroidJUnit4::class)
class GameCheckpointDeviceTest {
    @Test fun actualPreferencesRecoverValidProgressAndRejectInvalidOrChangedScope() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.voiceqa")
        check(context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
        val prefs=context.getSharedPreferences("sessions",0)
        val key="checkpoint-qa-"+UUID.randomUUID()
        val state=GameCheckpoint(completed=2,activity="c",seen=listOf("a","b"))
        val allowed=setOf("a","b","c")
        try {
            assertTrue(prefs.edit().putString(key,GameCheckpoints.encode("current",state)).commit())
            val reopened=context.getSharedPreferences("sessions",0)
            assertEquals(state,GameCheckpoints.restore(reopened.getString(key,null),"current",5,allowed))
            assertNotEquals(state.runId,GameCheckpoints.restore(reopened.getString(key,null),"changed",5,allowed).runId)
            val damaged=JSONObject(GameCheckpoints.encode("current",state)).put("completed",-1).toString()
            assertTrue(prefs.edit().putString(key,damaged).commit())
            val fresh=GameCheckpoints.restore(reopened.getString(key,null),"current",5,allowed)
            assertEquals(0,fresh.completed);assertTrue(fresh.seen.isEmpty());assertNotEquals(state.runId,fresh.runId)
            assertEquals(emptyList<Int>(),GameCheckpoints.tileIndices("0,99",3))
        } finally {check(prefs.edit().remove(key).commit())}
        assertFalse(prefs.contains(key))
        val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(folder,"game-checkpoint.json").writeText(JSONObject().put("model",android.os.Build.MODEL)
            .put("api",android.os.Build.VERSION.SDK_INT).put("actualPreferenceRoundTrip",true)
            .put("invalidCounterRejected",true).put("changedScopeRejected",true).put("invalidTilesRejected",true)
            .put("generatedKeyRemoved",true).put("profilesOrPinOpened",false)
            .put("processDeathOrFullAppNavigationTested",false).put("productionApproval",false).toString(2))
    }
}
