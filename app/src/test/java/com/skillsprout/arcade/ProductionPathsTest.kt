package com.skillsprout.arcade

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ProductionPathsTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val key by lazy {KeyPairGenerator.getInstance("EC").apply {initialize(256)}.generateKeyPair()}
    @Before fun resetOptionalPacks() {
        ContentPacks(context,key.public).deleteOptional()
        context.getSharedPreferences("pack_versions",0).edit().clear().commit()
    }
    private fun signed(version:Int=1,answerSuffix:String=""):ByteArray {
        val source=JSONObject(context.assets.open("packs/starter.json").bufferedReader().readText())
        val activity=source.getJSONArray("activities").getJSONObject(0).put("id","test-pack:one")
        val payload=source.put("activities",JSONArray().put(activity)).toString().toByteArray()
        val digest=ContentPacks.sha(payload)
        val message="SkillSprout-Pack-v1\ntest-pack\n$version\nen-GB\n$digest".toByteArray()
        val signature=Signature.getInstance("SHA256withECDSA").run {initSign(key.private);update(message);sign()}
        return JSONObject().put("format",1).put("manifest",JSONObject().put("packId","test-pack").put("version",version).put("locale","en-GB").put("sha256",digest))
            .put("payload",Base64.getEncoder().encodeToString(payload)+answerSuffix).put("signature",Base64.getEncoder().encodeToString(signature)).toString().toByteArray()
    }
    @Test fun signedPackUpdatesAtomicallyAndBlocksRollbackAfterRemoval() {
        val packs=ContentPacks(context,key.public)
        assertEquals(1,packs.importSigned(signed()))
        assertEquals(1,packs.installed().size)
        assertThrows(IllegalArgumentException::class.java) {packs.importSigned(signed())}
        assertEquals(1,packs.importSigned(signed(2)))
        packs.deleteOptional()
        assertThrows(IllegalArgumentException::class.java) {packs.importSigned(signed())}
        assertEquals(1,packs.importSigned(signed(3)))
    }
    @Test fun tamperingAndAnotherPublisherCannotReplaceInstalledContent() {
        val packs=ContentPacks(context,key.public);packs.importSigned(signed())
        val changed=JSONObject(signed(2).toString(Charsets.UTF_8));changed.getJSONObject("manifest").put("version",3)
        assertThrows(IllegalArgumentException::class.java) {packs.importSigned(changed.toString().toByteArray())}
        val otherKey=KeyPairGenerator.getInstance("EC").apply {initialize(256)}.generateKeyPair()
        assertThrows(IllegalArgumentException::class.java) {ContentPacks(context,otherKey.public).importSigned(signed(2))}
        assertEquals("test-pack:one",packs.installed().single().id)
    }
    @Test fun interruptedAtomicWriteRecoversItsBackup() {
        val packs=ContentPacks(context,key.public);packs.importSigned(signed())
        val file=context.filesDir.resolve("packs/test-pack.sproutpack")
        assertTrue(file.renameTo(java.io.File(file.path+".bak")))
        assertEquals("test-pack:one",packs.installed().single().id)
        assertTrue(file.exists())
    }
    @Test fun missionCompletionIsDurableAndIdempotent()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ArcadeDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao=db.dao();val assignment=Assignment("a","child","MATHS","",1,3,1,"2026-10-20")
            dao.save(Child("child","Explorer"));dao.save(assignment)
            dao.completeMission(assignment,"run-one",100);dao.completeMission(assignment,"run-one",101)
            assertEquals(1,dao.assignment("a")!!.completed)
            dao.completeMission(assignment,"run-two",200)
            assertEquals(2,dao.assignment("a")!!.completed)
            assertEquals(2,dao.completions().first().size)
            dao.eraseChild("child");assertTrue(dao.completions().first().isEmpty())
            // Delayed coroutine results cannot recreate records for an erased profile.
            dao.save(Attempt("late","child","q","addition","MATHS",true,0,1,1000,300))
            dao.save(assignment)
            assertTrue(dao.attempts().first().isEmpty());assertTrue(dao.assignments().first().isEmpty())
        } finally {db.close()}
    }
    @Test fun versionOneDatabaseMigratesWithoutLosingProfilesOrProgress()=runBlocking {
        val name="migration-${System.nanoTime()}.db"
        val old=context.openOrCreateDatabase(name,0,null)
        old.execSQL("CREATE TABLE Child (id TEXT NOT NULL PRIMARY KEY, nickname TEXT NOT NULL, avatar INTEGER NOT NULL, year INTEGER NOT NULL, subjects TEXT NOT NULL, allowanceMinutes INTEGER NOT NULL, bedtimeHour INTEGER NOT NULL, wakeHour INTEGER NOT NULL, voice INTEGER NOT NULL, reducedMotion INTEGER NOT NULL, highContrast INTEGER NOT NULL, switchingAllowed INTEGER NOT NULL, breakMinutes INTEGER NOT NULL)")
        old.execSQL("CREATE TABLE Attempt (id TEXT NOT NULL PRIMARY KEY, childId TEXT NOT NULL, activityId TEXT NOT NULL, skill TEXT NOT NULL, game TEXT NOT NULL, correct INTEGER NOT NULL, hints INTEGER NOT NULL, difficulty INTEGER NOT NULL, durationMs INTEGER NOT NULL, at INTEGER NOT NULL, assignmentId TEXT)")
        old.execSQL("CREATE INDEX index_Attempt_childId ON Attempt(childId)")
        old.execSQL("CREATE TABLE Assignment (id TEXT NOT NULL PRIMARY KEY, childId TEXT NOT NULL, game TEXT NOT NULL, skill TEXT NOT NULL, year INTEGER NOT NULL, count INTEGER NOT NULL, difficulty INTEGER NOT NULL, due TEXT NOT NULL, repeatDays INTEGER NOT NULL, targetMinutes INTEGER NOT NULL, completed INTEGER NOT NULL, completedAt INTEGER NOT NULL)")
        old.execSQL("CREATE INDEX index_Assignment_childId ON Assignment(childId)")
        old.execSQL("INSERT INTO Child VALUES ('child','Explorer',2,3,'MATHS',20,20,7,1,0,0,1,10)")
        old.execSQL("INSERT INTO Attempt VALUES ('answer','child','question','addition','MATHS',1,0,1,1000,1,NULL)")
        old.version=1;old.close()
        val db=Room.databaseBuilder(context,ArcadeDatabase::class.java,name).addMigrations(ArcadeDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {val child=db.dao().children().first().single();assertEquals(80,child.voiceVolume);assertEquals("scarf",child.decoration);assertEquals("answer",db.dao().attempts().first().single().id)}
        finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun cosmeticsRequirePracticeAndInvalidValuesFallBackSafely() {
        assertEquals(Decoration.SCARF,Decoration.available("compass",29))
        assertEquals(Decoration.COMPASS,Decoration.available("compass",30))
        assertEquals(Decoration.SCARF,Decoration.available("unknown",100))
    }
}
