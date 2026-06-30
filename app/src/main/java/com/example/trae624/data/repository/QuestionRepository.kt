package com.example.trae624.data.repository

import com.example.trae624.data.db.PracticeRecordDao
import com.example.trae624.data.db.QuestionDao
import com.example.trae624.data.model.PracticeRecord
import com.example.trae624.data.model.Question
import com.example.trae624.data.sample.SampleData

class QuestionRepository(
    private val questionDao: QuestionDao,
    private val recordDao: PracticeRecordDao
) {
    private var dataLoaded = false

    suspend fun ensureDataLoaded() {
        if (dataLoaded) return
        if (questionDao.getCount() == 0) {
            questionDao.insertAll(SampleData.getAnalogyQuestions())
        }
        dataLoaded = true
    }

    suspend fun getQuestionsByCategory(categoryId: Int): List<Question> {
        ensureDataLoaded()
        return questionDao.getByCategoryOrdered(categoryId)
    }

    suspend fun getRecord(questionId: Int): PracticeRecord? {
        return recordDao.getRecord(questionId)
    }

    suspend fun saveRecord(record: PracticeRecord) {
        val existing = recordDao.getRecord(record.questionId)
        if (existing != null) {
            recordDao.update(record)
        } else {
            recordDao.insertIfNotExists(record)
        }
    }

    suspend fun toggleFavorite(questionId: Int, favorite: Boolean) {
        recordDao.insertIfNotExists(PracticeRecord(questionId = questionId))
        recordDao.updateFavorite(questionId, favorite)
    }

    suspend fun toggleConquered(questionId: Int, conquered: Boolean) {
        recordDao.insertIfNotExists(PracticeRecord(questionId = questionId))
        recordDao.updateConquered(questionId, conquered)
    }

    suspend fun getFavorites(): List<PracticeRecord> = recordDao.getFavorites()
    suspend fun getWrongRecords(): List<PracticeRecord> = recordDao.getWrongRecords()
    suspend fun getConqueredRecords(): List<PracticeRecord> = recordDao.getConqueredRecords()
    suspend fun getAnsweredRecords(): List<PracticeRecord> = recordDao.getAnsweredRecords()
    suspend fun getCorrectCount(): Int = recordDao.getCorrectCount()
    suspend fun getWrongCount(): Int = recordDao.getWrongCount()
    suspend fun getAnsweredCount(): Int = recordDao.getAnsweredCount()
    suspend fun deleteAllRecords() = recordDao.deleteAll()
}
