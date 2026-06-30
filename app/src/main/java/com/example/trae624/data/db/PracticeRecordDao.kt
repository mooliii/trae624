package com.example.trae624.data.db

import androidx.room.*
import com.example.trae624.data.model.PracticeRecord

@Dao
interface PracticeRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(record: PracticeRecord)

    @Update
    suspend fun update(record: PracticeRecord)

    @Query("SELECT * FROM practice_records WHERE questionId = :questionId")
    suspend fun getRecord(questionId: Int): PracticeRecord?

    @Query("SELECT * FROM practice_records WHERE isFavorite = 1")
    suspend fun getFavorites(): List<PracticeRecord>

    @Query("SELECT * FROM practice_records WHERE isAnswered = 1 AND isCorrect = 0")
    suspend fun getWrongRecords(): List<PracticeRecord>

    @Query("SELECT * FROM practice_records WHERE isConquered = 1")
    suspend fun getConqueredRecords(): List<PracticeRecord>

    @Query("SELECT * FROM practice_records WHERE isAnswered = 1")
    suspend fun getAnsweredRecords(): List<PracticeRecord>

    @Query("UPDATE practice_records SET isFavorite = :favorite WHERE questionId = :questionId")
    suspend fun updateFavorite(questionId: Int, favorite: Boolean)

    @Query("UPDATE practice_records SET isConquered = :conquered WHERE questionId = :questionId")
    suspend fun updateConquered(questionId: Int, conquered: Boolean)

    @Query("SELECT COUNT(*) FROM practice_records WHERE isAnswered = 1 AND isCorrect = 1")
    suspend fun getCorrectCount(): Int

    @Query("SELECT COUNT(*) FROM practice_records WHERE isAnswered = 1 AND isCorrect = 0")
    suspend fun getWrongCount(): Int

    @Query("SELECT COUNT(*) FROM practice_records WHERE isAnswered = 1")
    suspend fun getAnsweredCount(): Int

    @Query("DELETE FROM practice_records")
    suspend fun deleteAll()
}
