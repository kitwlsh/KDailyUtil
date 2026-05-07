package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.model.QuizQuestion
import com.kitwlshcom.kdailyutil.data.model.QuizType
import com.kitwlshcom.kdailyutil.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class QuizState {
    IDLE,
    PLAYING,
    ANSWER_CHECKED,
    FINISHED
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository()

    private val _questions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val questions: StateFlow<List<QuizQuestion>> = _questions.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _quizState = MutableStateFlow(QuizState.IDLE)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _isCorrect = MutableStateFlow(false)
    val isCorrect: StateFlow<Boolean> = _isCorrect.asStateFlow()

    // 힌트 관련 상태
    private val _hintLevel = MutableStateFlow(0)
    val hintLevel: StateFlow<Int> = _hintLevel.asStateFlow()

    private val _filteredOptions = MutableStateFlow<List<String>?>(null)
    val filteredOptions: StateFlow<List<String>?> = _filteredOptions.asStateFlow()

    private val _currentHintText = MutableStateFlow<String?>(null)
    val currentHintText: StateFlow<String?> = _currentHintText.asStateFlow()

    fun syncRemoteData() {
        viewModelScope.launch {
            repository.syncRemoteQuizzes(getApplication())
        }
    }

    fun startQuiz() {
        viewModelScope.launch {
            val allQuestions = repository.getQuizzes(getApplication())
            _questions.value = allQuestions.take(10)
            _currentIndex.value = 0
            _score.value = 0
            _quizState.value = QuizState.PLAYING
            _currentInput.value = ""
            _isCorrect.value = false
            resetHintState()
        }
    }

    private fun resetHintState() {
        _hintLevel.value = 0
        val currentQ = _questions.value.getOrNull(_currentIndex.value)
        _filteredOptions.value = currentQ?.options
        _currentHintText.value = null
    }

    fun requestHint() {
        val currentQ = _questions.value.getOrNull(_currentIndex.value) ?: return
        
        _hintLevel.value += 1
        val level = _hintLevel.value

        if (currentQ.type == QuizType.MULTIPLE_CHOICE) {
            // 객관식 힌트 로직
            val options = currentQ.options ?: return
            val answer = currentQ.answer
            val wrongOptions = options.filter { it != answer }.shuffled()
            
            if (level == 1) {
                // 오답 1개 제거 (3개 남음)
                if (wrongOptions.isNotEmpty()) {
                    _filteredOptions.value = options.filter { it != wrongOptions[0] }
                }
                _currentHintText.value = currentQ.semanticHint
            } else if (level >= 2) {
                // 오답 2개 제거 (2개 남음 - 50:50)
                if (wrongOptions.size >= 2) {
                    val removed = wrongOptions.take(2)
                    _filteredOptions.value = options.filter { !removed.contains(it) }
                }
                _currentHintText.value = currentQ.semanticHint
            }
        } else {
            // 주관식 힌트 로직
            val answer = currentQ.answer
            when (level) {
                1 -> _currentHintText.value = "글자 수: ${answer.length}글자\n(의미: ${currentQ.semanticHint ?: ""})"
                2 -> {
                    val chosung = answer.map { getChosung(it) }.joinToString("")
                    _currentHintText.value = "초성: $chosung\n(의미: ${currentQ.semanticHint ?: ""})"
                }
                else -> {
                    val chosungList = answer.map { getChosung(it) }
                    val mixed = answer.mapIndexed { index, char ->
                        if (index % 2 == 0) char.toString() else chosungList[index].toString()
                    }.joinToString("")
                    _currentHintText.value = "일부 글자 공개: $mixed\n(의미: ${currentQ.semanticHint ?: ""})"
                }
            }
        }
    }

    private fun getChosung(char: Char): Char {
        val choList = listOf('ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')
        if (char in '가'..'힣') {
            val code = char.code - 0xAC00
            val choIndex = code / (21 * 28)
            return choList[choIndex]
        }
        return char
    }

    fun updateInput(input: String) {
        if (_quizState.value == QuizState.PLAYING) {
            _currentInput.value = input
        }
    }

    fun checkAnswer() {
        if (_quizState.value != QuizState.PLAYING) return

        val currentQuestion = _questions.value[_currentIndex.value]
        val userAnswer = _currentInput.value.trim()
        val correctAnswer = currentQuestion.answer.trim()

        val correct = userAnswer.equals(correctAnswer, ignoreCase = true)
        _isCorrect.value = correct
        
        if (correct) {
            _score.value += 1
        }
        
        _quizState.value = QuizState.ANSWER_CHECKED
    }

    fun nextQuestion() {
        if (_currentIndex.value < _questions.value.size - 1) {
            _currentIndex.value += 1
            _quizState.value = QuizState.PLAYING
            _currentInput.value = ""
            _isCorrect.value = false
            resetHintState()
        } else {
            _quizState.value = QuizState.FINISHED
        }
    }

    fun exitQuiz() {
        _quizState.value = QuizState.IDLE
    }
}
