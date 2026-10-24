package com.skillsprout.arcade

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses generated preference keys only. No profiles, parent PIN or device clock changes. */
@RunWith(AndroidJUnit4::class)
class PlayClockDeviceTest {
    @Test fun durableIsolatedBreakIntervalsAndDailyAllowance() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(BuildConfig.DEBUG && BuildConfig.APPLICATION_ID=="com.skillsprout.arcade.voiceqa")
        check(context.packageName=="com.skillsprout.arcade.voiceqa" && !BuildConfig.PURCHASING_ALLOWED)
        val id="clock-qa-"+UUID.randomUUID();val other="clock-qa-"+UUID.randomUUID()
        var elapsed=0L;var date=LocalDate.of(2026,10,24)
        fun clock()=PlayClock(context,{elapsed},{date},{12})
        val first=clock();val child=Child(id,"Generated QA",breakMinutes=5,allowanceMinutes=5)
        try {
            repeat(60) {elapsed+=5000;first.tick(id)}
            val recreated=clock()
            assertTrue(recreated.breakDue(child));assertTrue(recreated.blocked(child))
            assertEquals(300_000L,recreated.breakUsed(id));assertEquals(0L,recreated.breakUsed(other))
            recreated.bonus(id);assertFalse(recreated.blocked(child))
            elapsed+=600_000;recreated.resetBreak(id);elapsed+=1000;recreated.tick(id)
            assertEquals(1000L,recreated.breakUsed(id));assertEquals(301_000L,recreated.used(id))
            assertEquals(301_000L,recreated.total(id));assertFalse(recreated.blocked(child))
            elapsed+=1000;recreated.tick(other);recreated.clear(other)
            assertEquals(0L,recreated.breakUsed(other));assertEquals(1000L,recreated.breakUsed(id))
            date=date.minusDays(1);assertEquals(301_000L,recreated.used(id))
            date=date.plusDays(2);assertEquals(0L,recreated.breakUsed(id));assertEquals(0L,recreated.used(id))
            assertEquals(301_000L,recreated.total(id))
        } finally {first.clear(id);first.clear(other)}
        val prefs=context.getSharedPreferences("play_clock",0)
        assertFalse(prefs.all.keys.any {it.startsWith("$id-") || it.startsWith("$other-")})
        val folder=File(context.getExternalFilesDir(null),"fire-qa").apply {mkdirs()}
        File(folder,"play-clock.json").writeText(JSONObject().put("model",android.os.Build.MODEL)
            .put("api",android.os.Build.VERSION.SDK_INT).put("durablePreferenceRecovery",true)
            .put("explorerIsolation",true).put("resetPreservesDailyTotalAndBonus",true)
            .put("backwardAndForwardDateChecks",true).put("generatedKeysRemoved",true)
            .put("injectedTimeSources",true).put("actualFiveMinuteWaitTested",false)
            .put("profilesOrPinOpened",false).put("productionApproval",false).toString(2))
    }
}
