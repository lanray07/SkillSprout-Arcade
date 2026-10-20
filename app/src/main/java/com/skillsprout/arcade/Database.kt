package com.skillsprout.arcade

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity data class Child(@PrimaryKey val id: String, val nickname: String, val avatar: Int = 0, val year: Int = 1,
    val subjects: String = "MATHS,SPELLING,STORY,TABLES,LOGIC,WORDS", val allowanceMinutes: Int = 20,
    val bedtimeHour: Int = 20, val wakeHour: Int = 7, val voice: Boolean = true, val reducedMotion: Boolean = false,
    val highContrast: Boolean = false, val switchingAllowed: Boolean = true, val breakMinutes: Int = 10,
    @ColumnInfo(defaultValue="80") val voiceVolume: Int = 80, @ColumnInfo(defaultValue="'scarf'") val decoration: String = "scarf")
@Entity(indices=[Index("childId")]) data class Attempt(@PrimaryKey val id: String, val childId: String,
    val activityId: String, val skill: String, val game: String, val correct: Boolean, val hints: Int,
    val difficulty: Int, val durationMs: Long, val at: Long, val assignmentId: String? = null)
@Entity(indices=[Index("childId")]) data class Assignment(@PrimaryKey val id: String, val childId: String,
    val game: String, val skill: String, val year: Int, val count: Int, val difficulty: Int,
    val due: String, val repeatDays: Int = 0, val targetMinutes: Int = 0, val completed: Int = 0,
    val completedAt: Long = 0)
@Entity(indices=[Index("childId"),Index("assignmentId")]) data class MissionCompletion(
    @PrimaryKey val runId:String,val assignmentId:String,val childId:String,val at:Long)
@Dao interface ArcadeDao {
    @Query("SELECT * FROM Child ORDER BY nickname") fun children(): Flow<List<Child>>
    @Query("SELECT * FROM Attempt ORDER BY at") fun attempts(): Flow<List<Attempt>>
    @Query("SELECT * FROM MissionCompletion ORDER BY at") fun completions():Flow<List<MissionCompletion>>
    @Query("SELECT * FROM Assignment ORDER BY due") fun assignments(): Flow<List<Assignment>>
    @Query("SELECT * FROM Assignment WHERE id = :id") suspend fun assignment(id:String):Assignment?
    @Upsert suspend fun save(child: Child)
    @Query("SELECT EXISTS(SELECT 1 FROM Child WHERE id = :id)") suspend fun childExists(id:String):Boolean
    @Upsert suspend fun insertAssignment(assignment: Assignment)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertAttempt(attempt: Attempt)
    @Transaction suspend fun save(assignment:Assignment) {if(childExists(assignment.childId)) insertAssignment(assignment)}
    @Transaction suspend fun save(attempt:Attempt) {if(childExists(attempt.childId)) insertAttempt(attempt)}
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertCompletion(completion:MissionCompletion):Long
    @Query("UPDATE Assignment SET completed = completed + 1, completedAt = :at WHERE id = :id AND childId = :childId")
    suspend fun incrementCompletion(id:String,childId:String,at:Long)
    @Transaction suspend fun completeMission(assignment:Assignment,runId:String,at:Long=System.currentTimeMillis()) {
        require(runId.isNotBlank() && runId.length<=128)
        check(assignment(assignment.id)?.childId==assignment.childId) {"Mission no longer exists for this explorer"}
        if(insertCompletion(MissionCompletion(runId,assignment.id,assignment.childId,at))!=-1L)
            incrementCompletion(assignment.id,assignment.childId,at)
    }
    @Query("DELETE FROM MissionCompletion WHERE childId = :id") suspend fun deleteCompletions(id:String)
    @Query("DELETE FROM Attempt WHERE childId = :id") suspend fun deleteAttempts(id: String)
    @Query("DELETE FROM Assignment WHERE childId = :id") suspend fun deleteAssignments(id: String)
    @Query("DELETE FROM Child WHERE id = :id") suspend fun deleteChild(id: String)
    @Transaction suspend fun eraseChild(id: String) { deleteAttempts(id); deleteCompletions(id); deleteAssignments(id); deleteChild(id) }
}
@Database(entities=[Child::class,Attempt::class,Assignment::class,MissionCompletion::class], version=2, exportSchema=true)
abstract class ArcadeDatabase: RoomDatabase() {
    abstract fun dao(): ArcadeDao
    companion object {
        val MIGRATION_1_2=object:Migration(1,2) { override fun migrate(db:SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Child ADD COLUMN voiceVolume INTEGER NOT NULL DEFAULT 80")
            db.execSQL("ALTER TABLE Child ADD COLUMN decoration TEXT NOT NULL DEFAULT 'scarf'")
            db.execSQL("CREATE TABLE IF NOT EXISTS MissionCompletion (runId TEXT NOT NULL PRIMARY KEY, assignmentId TEXT NOT NULL, childId TEXT NOT NULL, at INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_MissionCompletion_childId ON MissionCompletion (childId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_MissionCompletion_assignmentId ON MissionCompletion (assignmentId)")
        } }
        fun open(context: Context) = Room.databaseBuilder(context,ArcadeDatabase::class.java,"arcade.db").addMigrations(MIGRATION_1_2).build()
    }
}
