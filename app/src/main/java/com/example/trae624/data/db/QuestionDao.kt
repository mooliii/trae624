package com.example.trae624.data.db

import androidx.room.*
import com.example.trae624.data.model.Question

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(questions: List<Question>)

    @Query("SELECT * FROM questions WHERE categoryId = :categoryId")
    suspend fun getByCategory(categoryId: Int): List<Question>

    @Query("SELECT * FROM questions WHERE categoryId = :categoryId ORDER BY id")
    suspend fun getByCategoryOrdered(categoryId: Int): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Int): Question?

    @Query("SELECT COUNT(*) FROM questions WHERE categoryId = :categoryId")
    suspend fun getCountByCategory(categoryId: Int): Int

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getCount(): Int
}
