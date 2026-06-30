package com.example.trae624

import android.app.Application
import com.example.trae624.data.db.AppDatabase
import com.example.trae624.data.repository.QuestionRepository

class TraeApp : Application() {
    val repository: QuestionRepository by lazy {
        val db = AppDatabase.getInstance(this)
        QuestionRepository(db.questionDao(), db.practiceRecordDao())
    }
}
