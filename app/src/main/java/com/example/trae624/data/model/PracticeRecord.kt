package com.example.trae624.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_records")
data class PracticeRecord(
    @PrimaryKey val questionId: Int,
    val isCorrect: Boolean = false,
    val isAnswered: Boolean = false,
    val isFavorite: Boolean = false,
    val isConquered: Boolean = false,
    val userAnswer: String = ""
)
