package com.example.trae624.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.trae624.TraeApp
import com.example.trae624.data.model.PracticeRecord
import com.example.trae624.data.model.Question
import com.example.trae624.data.sample.SampleData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PracticeUiState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val showResult: Boolean = false,
    val isMemorizeMode: Boolean = false,
    val isFavorite: Boolean = false,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val answeredCount: Int = 0,
    val autoAdvance: Boolean = true
)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as TraeApp).repository

    private val _uiState = MutableLiveData<PracticeUiState>()
    val uiState: LiveData<PracticeUiState> = _uiState

    private var sessionAnswers = mutableMapOf<Int, String>()

    fun loadQuestions(questions: List<Question>, startIndex: Int = 0) {
        _uiState.value = PracticeUiState(questions = questions, currentIndex = startIndex)
        sessionAnswers.clear()
        updateCounts()
        loadSessionState(startIndex)
    }

    fun selectAnswer(answer: String) {
        val state = _uiState.value ?: return
        if (state.showResult && !state.isMemorizeMode) return
        val question = state.questions[state.currentIndex]
        val isCorrect = answer == question.correctAnswer

        sessionAnswers[state.currentIndex] = answer

        _uiState.value = state.copy(
            selectedAnswer = answer,
            showResult = true
        )

        saveRecord(question.id, isCorrect, answer)
    }

    fun nextQuestion() {
        val state = _uiState.value ?: return
        if (state.currentIndex < state.questions.size - 1) {
            val newIndex = state.currentIndex + 1
            _uiState.value = state.copy(
                currentIndex = newIndex,
                selectedAnswer = null,
                showResult = false
            )
            loadSessionState(newIndex)
        }
    }

    fun prevQuestion() {
        val state = _uiState.value ?: return
        if (state.currentIndex > 0) {
            val newIndex = state.currentIndex - 1
            _uiState.value = state.copy(
                currentIndex = newIndex,
                selectedAnswer = null,
                showResult = false
            )
            loadSessionState(newIndex)
        }
    }

    fun jumpToQuestion(index: Int) {
        val state = _uiState.value ?: return
        if (index in state.questions.indices) {
            _uiState.value = state.copy(
                currentIndex = index,
                selectedAnswer = null,
                showResult = false
            )
            loadSessionState(index)
        }
    }

    fun toggleMemorizeMode() {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(isMemorizeMode = !state.isMemorizeMode)
        loadSessionState(state.currentIndex)
    }

    fun toggleFavorite() {
        val state = _uiState.value ?: return
        val question = state.questions[state.currentIndex]
        val newFavorite = !state.isFavorite
        _uiState.value = state.copy(isFavorite = newFavorite)

        viewModelScope.launch {
            repo.toggleFavorite(question.id, newFavorite)
        }
    }

    fun setAutoAdvance(enabled: Boolean) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(autoAdvance = enabled)
    }

    private fun loadSessionState(index: Int) {
        val state = _uiState.value ?: return
        val question = state.questions[index]

        val savedAnswer = sessionAnswers[index]
        if (savedAnswer != null) {
            _uiState.value = _uiState.value?.copy(
                selectedAnswer = savedAnswer,
                showResult = true
            )
        }

        viewModelScope.launch {
            val record = repo.getRecord(question.id)
            _uiState.value = _uiState.value?.copy(
                isFavorite = record?.isFavorite ?: false
            )
        }
    }

    private fun saveRecord(questionId: Int, isCorrect: Boolean, userAnswer: String) {
        viewModelScope.launch {
            val existing = repo.getRecord(questionId)
            val record = PracticeRecord(
                questionId = questionId,
                isCorrect = isCorrect,
                isAnswered = true,
                isFavorite = existing?.isFavorite ?: false,
                isConquered = true,
                userAnswer = userAnswer
            )
            repo.saveRecord(record)
            updateCounts()
        }
    }

    private fun updateCounts() {
        viewModelScope.launch {
            val correct = repo.getCorrectCount()
            val wrong = repo.getWrongCount()
            val answered = repo.getAnsweredCount()
            _uiState.value = _uiState.value?.copy(
                correctCount = correct,
                wrongCount = wrong,
                answeredCount = answered
            )
        }
    }
}
