package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.repository.ReadingTrainingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReadingTrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ReadingTrainingRepository(application)

    val bestWpm: StateFlow<Int> = repo.bestWpmFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val streak: StateFlow<Int> = repo.streakFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalSessions: StateFlow<Int> = repo.totalSessionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 한 세션 완료 기록 (wpm=0이면 워밍업 등 속도 무관 세션) */
    fun recordSession(wpm: Int) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val today = sdf.format(cal.time)
            cal.add(Calendar.DATE, -1)
            val yesterday = sdf.format(cal.time)
            repo.recordSession(wpm, today, yesterday)
        }
    }
}
