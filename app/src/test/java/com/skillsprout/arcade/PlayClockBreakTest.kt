package com.skillsprout.arcade

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class PlayClockBreakTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private var elapsed=0L
    private var date=LocalDate.of(2026,10,24)
    private fun clock()=PlayClock(context,{elapsed},{date},{12})
    private val child=Child("break-test","Test",breakMinutes=5,allowanceMinutes=10)
    private fun play(clock:PlayClock,id:String=child.id,seconds:Int=300) {
        clock.resume()
        repeat(seconds/5) {elapsed+=5000;clock.tick(id)}
        if(seconds%5>0) {elapsed+=(seconds%5)*1000;clock.tick(id)}
    }
    @Test fun intervalSurvivesRecreationAndContinuesAccruing() {
        val first=clock();play(first)
        val next=clock();assertTrue(next.breakDue(child));assertEquals(300_000L,next.breakUsed(child.id))
        elapsed+=1000;next.tick(child.id);assertEquals(301_000L,next.breakUsed(child.id))
    }
    @Test fun explorersHaveIndependentIntervals() {
        val clock=clock();play(clock);play(clock,"second",30)
        assertTrue(clock.breakDue(child));assertFalse(clock.breakDue(child.copy(id="second")))
        assertEquals(30_000L,clock.breakUsed("second"));assertEquals(300_000L,clock.breakUsed(child.id))
    }
    @Test fun acknowledgingBreakPreservesDailyLifetimeAndBonusAllowance() {
        val clock=clock();play(clock,seconds=600);clock.bonus(child.id)
        assertFalse(clock.blocked(child));clock.resetBreak(child.id)
        assertFalse(clock.breakDue(child));assertEquals(600_000L,clock.used(child.id));assertEquals(600_000L,clock.total(child.id))
        assertFalse(clock.blocked(child));play(clock,seconds=300);assertTrue(clock.blocked(child))
    }
    @Test fun disabledReminderDoesNotBlockAndChangingIntervalUsesStoredTime() {
        val clock=clock();play(clock)
        assertFalse(clock.breakDue(child.copy(breakMinutes=0)))
        assertFalse(clock.breakDue(child.copy(breakMinutes=10)))
        assertTrue(clock.breakDue(child))
    }
    @Test fun newDayResetsDailyIntervalAndBonusButPreservesLifetime() {
        val clock=clock();play(clock);clock.bonus(child.id);date=date.plusDays(1)
        assertEquals(0L,clock.breakUsed(child.id));assertEquals(0L,clock.used(child.id));assertEquals(300_000L,clock.total(child.id))
        play(clock,seconds=600);assertTrue(clock.blocked(child))
    }
    @Test fun backwardsDateCannotGrantFreshBreakOrDailyAllowance() {
        val clock=clock();play(clock,seconds=600);date=date.minusDays(1)
        assertTrue(clock.breakDue(child));assertTrue(clock.blocked(child));assertEquals(600_000L,clock.breakUsed(child.id))
    }
    @Test fun resumeExcludesBackgroundAndParentAreaTime() {
        val clock=clock();play(clock,seconds=10);elapsed+=3_600_000;clock.resume();elapsed+=1000;clock.tick(child.id)
        assertEquals(11_000L,clock.used(child.id));assertEquals(11_000L,clock.breakUsed(child.id))
    }
    @Test fun schedulerGapUsesSameBoundedElapsedTimeForBothCounters() {
        val clock=clock();elapsed+=60_000;clock.tick(child.id)
        assertEquals(5000L,clock.used(child.id));assertEquals(5000L,clock.breakUsed(child.id))
    }
    @Test fun clearErasesOnlyTargetExplorerIncludingBreakInterval() {
        val clock=clock();play(clock);play(clock,"second",30);clock.bonus(child.id);clock.clear(child.id)
        assertEquals(0L,clock.breakUsed(child.id));assertEquals(0L,clock.used(child.id));assertEquals(0L,clock.total(child.id))
        assertEquals(30_000L,clock.breakUsed("second"));assertEquals(30_000L,clock.total("second"))
    }
    @Test fun upgradePreservesExistingUsageAndStartsNewIntervalAtZero() {
        context.getSharedPreferences("play_clock",Context.MODE_PRIVATE).edit()
            .putString("${child.id}-day",date.toString()).putLong("${child.id}-used",600_000)
            .putLong("${child.id}-total",900_000).putLong("${child.id}-bonus",300_000).commit()
        val clock=clock();assertEquals(0L,clock.breakUsed(child.id));assertEquals(600_000L,clock.used(child.id))
        assertEquals(900_000L,clock.total(child.id));assertFalse(clock.blocked(child))
        elapsed+=1000;clock.tick(child.id);assertEquals(1000L,clock.breakUsed(child.id))
    }
    @Test fun acknowledgingBreakExcludesTimeSpentInBreakDialog() {
        val clock=clock();play(clock);elapsed+=600_000;clock.resetBreak(child.id);elapsed+=1000;clock.tick(child.id)
        assertEquals(1000L,clock.breakUsed(child.id));assertEquals(301_000L,clock.used(child.id))
    }
}
