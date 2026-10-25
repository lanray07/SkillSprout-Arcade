package com.skillsprout.arcade

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class GameCheckpointTest {
    private val run="c362b4bc-5703-470d-b367-81c56a14059f"
    private val state=GameCheckpoint(run,2,"c",listOf("a","b"))
    private val allowed=setOf("a","b","c")
    private fun json()=JSONObject(GameCheckpoints.encode("scope",state))
    private fun restore(raw:String?)=GameCheckpoints.restore(raw,"scope",5,allowed)
    private fun fresh(raw:String?) {
        val result=restore(raw);assertEquals(0,result.completed);assertEquals("",result.activity)
        assertTrue(result.seen.isEmpty());assertNotEquals(run,result.runId)
        assertEquals(result.runId,java.util.UUID.fromString(result.runId).toString())
    }
    @Test fun validCheckpointRoundTripsExactly() {assertEquals(state,restore(json().toString()))}
    @Test fun absentMalformedAndOversizedDataStartFresh() {listOf(null,"{","[]","x".repeat(131_073)).forEach {fresh(it)}}
    @Test fun legacyAndUnknownFormatsStartFresh() {
        fresh(json().remove("format").let {JSONObject().put("runId",run).put("completed",2).toString()})
        fresh(json().put("format",2).toString());fresh(json().put("format","1").toString())
    }
    @Test fun negativeFinishedAndOverflowCountersStartFresh() {
        listOf(-1,5,Int.MAX_VALUE,Long.MAX_VALUE,"2",2.5).forEach {fresh(json().put("completed",it).toString())}
    }
    @Test fun invalidOrMissingRunIdentifiersStartFresh() {
        listOf("","other","1-1-1-1-1",JSONObject.NULL).forEach {fresh(json().put("runId",it).toString())}
        val j=json();j.remove("runId");fresh(j.toString())
    }
    @Test fun removedActivitiesAndMalformedSeenListsStartFresh() {
        fresh(json().put("activity","removed").toString())
        fresh(json().put("seen",JSONArray(listOf("a","removed"))).toString())
        fresh(json().put("seen",JSONArray(listOf("a"))).toString())
        fresh(json().put("seen",JSONArray(listOf(1,"b"))).toString())
    }
    @Test fun pendingSelectionAndRepeatedActivitiesAreValid() {
        val repeated=state.copy(activity="",seen=listOf("a","a"))
        assertEquals(repeated,restore(GameCheckpoints.encode("scope",repeated)))
    }
    @Test fun identifiersWithCommasAndUnicodeRoundTrip() {
        val value=GameCheckpoint(run,1,"é,c",listOf("é,c"))
        assertEquals(value,GameCheckpoints.restore(GameCheckpoints.encode("scope",value),"scope",5,setOf("é,c")))
    }
    @Test fun changedScopeRejectsOtherwiseValidProgress() {fresh(json().put("scope","old").toString())}
    @Test fun scopeIsOrderIndependentButTracksContentAndAssignmentSettings() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val child=Child("checkpoint","QA",year=2);val pool=ContentPacks(context).starter().take(3)
        val assignment=Assignment("task",child.id,"MATHS","",2,5,1,"2026-10-25")
        fun scope(c:Child=child,p:List<Activity> = pool,a:Assignment=assignment)=GameCheckpoints.scope(c,Game.MATHS,p,a)
        val original=scope();assertEquals(original,scope(p=pool.reversed()))
        assertNotEquals(original,scope(p=listOf(pool.first().copy(answer="changed"))+pool.drop(1)))
        assertNotEquals(original,scope(p=listOf(pool.first().copy(hints=listOf("Changed hint")))+pool.drop(1)))
        assertNotEquals(original,scope(a=assignment.copy(count=10)))
        assertNotEquals(original,scope(a=assignment.copy(difficulty=2)))
        assertNotEquals(original,scope(c=child.copy(year=3)))
        assertEquals(original,scope(a=assignment.copy(completed=1,due="2026-10-26")))
    }
    @Test fun tileRestoreRejectsInvalidDuplicateAndOutOfRangeIndices() {
        for(raw in listOf("-1","3","0,0","0,x","2147483648","0,1,2,3")) assertEquals(emptyList<Int>(),GameCheckpoints.tileIndices(raw,3))
        assertEquals(listOf(2,0,1),GameCheckpoints.tileIndices("2,0,1",3))
        assertEquals(emptyList<Int>(),GameCheckpoints.tileIndices("",3))
    }
}
