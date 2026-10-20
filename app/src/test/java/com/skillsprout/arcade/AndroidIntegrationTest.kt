package com.skillsprout.arcade

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import androidx.room.Room
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class AndroidIntegrationTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun entireBundledPackPassesIntegrityAndSchemaChecks() {val pack=ContentPacks(context).starter();assertEquals(396,pack.size);assertEquals(6,pack.map {it.game}.distinct().size);assertEquals((1..6).toList(),pack.map {it.year}.distinct().sorted());assertTrue(pack.all {it.reviewStatus=="Awaiting Teacher Review"})}
    @Test fun corruptPackIsRejected() { assertThrows(IllegalArgumentException::class.java) {ContentPacks(context).import("corrupt".toByteArray(),"invalid")} }
    @Test fun profileDeletionRemovesProgressAndAssignments()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ArcadeDatabase::class.java).allowMainThreadQueries().build()
        val dao=db.dao();dao.save(Child("one","Pip"));dao.save(Child("two","Ada"));dao.save(Attempt("a","one","x","add","MATHS",true,0,1,1000,0));dao.save(Assignment("h","one","MATHS","",1,5,1,"2026-10-20"));dao.eraseChild("one")
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM Attempt").use {it.moveToFirst();assertEquals(0,it.getInt(0))}
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM Assignment").use {it.moveToFirst();assertEquals(0,it.getInt(0))}
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM Child").use {it.moveToFirst();assertEquals(1,it.getInt(0))};db.close()
    }
    @Test fun elapsedPlaySurvivesClockObjectRecreation() {
        val clock=PlayClock(context);clock.clear("timer");clock.resume();ShadowSystemClock.advanceBy(Duration.ofSeconds(3));clock.tick("timer")
        assertTrue(PlayClock(context).used("timer")>=3000)
    }
    @Test fun allowanceCannotBeResetByRecreatingTheClock() {
        val clock=PlayClock(context);clock.clear("limit");clock.resume()
        repeat(60) {ShadowSystemClock.advanceBy(Duration.ofSeconds(5));clock.tick("limit")}
        assertTrue(PlayClock(context).blocked(Child("limit","Explorer",allowanceMinutes=5)))
        assertTrue(PlayClock(context).total("limit")>=300_000)
        assertEquals(0,PlayClock(context).used("other"))
    }
    @Test fun duplicateAndUnsupportedContentFailsClosed() {
        val bytes=context.assets.open("packs/starter.json").readBytes();val root=org.json.JSONObject(bytes.toString(Charsets.UTF_8))
        root.getJSONArray("activities").put(root.getJSONArray("activities").getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) {ContentPacks(context).parse(root.toString().toByteArray())}
        val other=org.json.JSONObject(bytes.toString(Charsets.UTF_8)).put("locale","fr")
        assertThrows(IllegalArgumentException::class.java) {ContentPacks(context).parse(other.toString().toByteArray())}
    }
}
