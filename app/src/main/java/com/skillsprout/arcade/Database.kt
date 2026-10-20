package com.skillsprout.arcade

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Child(@PrimaryKey val id: String, val nickname: String, val avatar: Int = 0, val year: Int = 1,
    val subjects: String = "MATHS,SPELLING,STORY,TABLES,LOGIC,WORDS", val allowanceMinutes: Int = 20,
    val bedtimeHour: Int = 20, val wakeHour: Int = 7, val voice: Boolean = true, val reducedMotion: Boolean = false,
    val highContrast: Boolean = false, val switchingAllowed: Boolean = true, val breakMinutes: Int = 10)
@Entity(indices=[Index("childId")]) data class Attempt(@PrimaryKey val id: String, val childId: String,
    val activityId: String, val skill: String, val game: String, val correct: Boolean, val hints: Int,
    val difficulty: Int, val durationMs: Long, val at: Long, val assignmentId: String? = null)
@Entity(indices=[Index("childId")]) data class Assignment(@PrimaryKey val id: String, val childId: String,
    val game: String, val skill: String, val year: Int, val count: Int, val difficulty: Int,
    val due: String, val repeatDays: Int = 0, val targetMinutes: Int = 0, val completed: Int = 0,
    val completedAt: Long = 0)
@Dao interface ArcadeDao {
    @Query("SELECT * FROM Child ORDER BY nickname") fun children(): Flow<List<Child>>
    @Query("SELECT * FROM Attempt ORDER BY at") fun attempts(): Flow<List<Attempt>>
    @Query("SELECT * FROM Assignment ORDER BY due") fun assignments(): Flow<List<Assignment>>
    @Upsert suspend fun save(child: Child)
    @Upsert suspend fun save(assignment: Assignment)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun save(attempt: Attempt)
    @Query("DELETE FROM Attempt WHERE childId = :id") suspend fun deleteAttempts(id: String)
    @Query("DELETE FROM Assignment WHERE childId = :id") suspend fun deleteAssignments(id: String)
    @Query("DELETE FROM Child WHERE id = :id") suspend fun deleteChild(id: String)
    @Transaction suspend fun eraseChild(id: String) { deleteAttempts(id); deleteAssignments(id); deleteChild(id) }
}
@Database(entities=[Child::class,Attempt::class,Assignment::class], version=1, exportSchema=false)
abstract class ArcadeDatabase: RoomDatabase() {
    abstract fun dao(): ArcadeDao
    companion object { fun open(context: Context) = Room.databaseBuilder(context,ArcadeDatabase::class.java,"arcade.db").build() }
}
