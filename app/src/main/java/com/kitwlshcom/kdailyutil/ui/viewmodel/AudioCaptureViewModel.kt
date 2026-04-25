package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.audio.AudioCaptureService
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import com.kitwlshcom.kdailyutil.data.repository.AudioRepository
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class PlaybackMode {
    SEQUENTIAL, LOOP_LIST, SHUFFLE, REPEAT_ONE
}

enum class RecordingSource {
    INTERNAL, MIC
}

enum class AudioTab {
    CAPTURE, FILES, PLAYLISTS
}

class AudioCaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)
    private val settingsRepository = SettingsRepository(application)
    
    private val _recordings = MutableStateFlow<List<AudioItem>>(emptyList())
    val recordings: StateFlow<List<AudioItem>> = _recordings.asStateFlow()

    private val _hiddenRecordings = MutableStateFlow<List<AudioItem>>(emptyList())
    val hiddenRecordings: StateFlow<List<AudioItem>> = _hiddenRecordings.asStateFlow()
    
    private val _allRootFiles = MutableStateFlow<List<AudioItem>>(emptyList())
    val allRootFiles: StateFlow<List<AudioItem>> = _allRootFiles.asStateFlow()

    private val _trashRecordings = MutableStateFlow<List<AudioItem>>(emptyList())
    val trashRecordings: StateFlow<List<AudioItem>> = _trashRecordings.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentlyPlaying = MutableStateFlow<AudioItem?>(null)
    val currentlyPlaying: StateFlow<AudioItem?> = _currentlyPlaying.asStateFlow()

    private val _isPlaybackPaused = MutableStateFlow(false)
    val isPlaybackPaused: StateFlow<Boolean> = _isPlaybackPaused.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.SEQUENTIAL)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private val _isEditLocked = MutableStateFlow(true) // 기본은 잠금 상태
    val isEditLocked: StateFlow<Boolean> = _isEditLocked.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private val _recordingSource = MutableStateFlow(RecordingSource.INTERNAL)
    val recordingSource: StateFlow<RecordingSource> = _recordingSource.asStateFlow()
    
    private val _playlists = MutableStateFlow<List<String>>(emptyList())
    val playlists: StateFlow<List<String>> = _playlists.asStateFlow()
    
    private val _selectedPlaylist = MutableStateFlow<String?>(null) // null = "전체"
    val selectedPlaylist: StateFlow<String?> = _selectedPlaylist.asStateFlow()

    private val _activeTab = MutableStateFlow(AudioTab.FILES) // 기본값은 파일 목록
    val activeTab: StateFlow<AudioTab> = _activeTab.asStateFlow()

    private val _filterMode = MutableStateFlow("ALL")
    val filterMode: StateFlow<String> = _filterMode.asStateFlow()

    val displayFiles: StateFlow<List<AudioItem>> = combine(
        allRootFiles, hiddenRecordings, trashRecordings, _filterMode
    ) { all, hidden, trash, mode ->
        when (mode) {
            "IMPORTS" -> all.filter { it.path.contains("/imports/") }
            "HIDDEN" -> hidden
            "TRASH" -> trash
            else -> all
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPrepared = AudioCaptureService.isPrepared

    init {
        loadRecordings()
        observePlaybackProgress()
        observePlaybackCompletion()
        observeServiceState()
        loadSettings()
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            AudioCaptureService.currentlyPlaying.collect { _currentlyPlaying.value = it }
        }
        viewModelScope.launch {
            AudioCaptureService.isPlaybackPaused.collect { _isPlaybackPaused.value = it }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.playbackModeFlow.collect { _playbackMode.value = it }
        }
        viewModelScope.launch {
            settingsRepository.isEditLockedFlow.collect { _isEditLocked.value = it }
        }
    }

    private fun observePlaybackCompletion() {
        viewModelScope.launch {
            AudioCaptureService.playbackCompleted.collect {
                playNextRecording()
            }
        }
    }

    private fun observePlaybackProgress() {
        viewModelScope.launch {
            AudioCaptureService.currentPosition.collect { _playbackProgress.value = it.toFloat() }
        }
        viewModelScope.launch {
            AudioCaptureService.playbackDuration.collect { _playbackDuration.value = it }
        }
    }

    fun loadRecordings() {
        viewModelScope.launch {
            repository.initializeStorage() // 권한 확인 후 저장소 초기화/이동 시도
            _recordings.value = repository.getRecordedFiles(_selectedPlaylist.value)
            _allRootFiles.value = repository.getRecordedFiles(null) // 항상 전체 파일 로드
            _playlists.value = repository.getPlaylists()
            _hiddenRecordings.value = repository.getHiddenFiles()
            _trashRecordings.value = repository.getTrashFiles()
            
            // 선택된 재생목록이 여전히 존재하는지 확인
            if (_selectedPlaylist.value != null && !_playlists.value.contains(_selectedPlaylist.value)) {
                _selectedPlaylist.value = null
            }
        }
    }

    fun loadHiddenRecordings() {
        viewModelScope.launch {
            _hiddenRecordings.value = repository.getHiddenFiles()
        }
    }

    fun loadTrashRecordings() {
        viewModelScope.launch {
            _trashRecordings.value = repository.getTrashFiles()
        }
    }

    fun hideRecording(item: AudioItem) {
        if (repository.hideFile(item)) {
            loadRecordings()
        }
    }

    fun restoreRecording(item: AudioItem) {
        if (repository.restoreFile(item)) {
            loadRecordings()
        }
    }

    fun playAudio(item: AudioItem) {
        if (_currentlyPlaying.value == item) {
            if (_isPlaybackPaused.value) {
                // Resume
                val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
                    action = AudioCaptureService.ACTION_PLAY
                    putExtra(AudioCaptureService.EXTRA_FILE_PATH, item.path)
                }
                getApplication<Application>().startService(intent)
                _isPlaybackPaused.value = false
            } else {
                // Pause
                val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
                    action = AudioCaptureService.ACTION_PAUSE
                }
                getApplication<Application>().startService(intent)
                _isPlaybackPaused.value = true
            }
        } else {
            // Start new playback
            val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
                action = AudioCaptureService.ACTION_PLAY
                putExtra(AudioCaptureService.EXTRA_FILE_PATH, item.path)
            }
            getApplication<Application>().startService(intent)
            _currentlyPlaying.value = item
            _isPlaybackPaused.value = false
        }
    }

    fun stopPlayback() {
        val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP_PLAYBACK
        }
        getApplication<Application>().startService(intent)
        _currentlyPlaying.value = null
        _isPlaybackPaused.value = false
    }

    fun renameRecording(item: AudioItem, newName: String) {
        val wasPlaying = _currentlyPlaying.value == item
        val wasPaused = wasPlaying && _isPlaybackPaused.value
        val savedPos = if (wasPlaying) _playbackProgress.value.toLong() else 0L

        if (wasPlaying) {
            stopPlayback()
        }
        
        if (repository.renameFile(item, newName)) {
            loadRecordings()
            
            // 이름 변경 후 다시 이어서 재생 시도
            if (wasPlaying && !wasPaused) {
                viewModelScope.launch {
                    // 리스트 갱신 시간을 기다림
                    kotlinx.coroutines.delay(200)
                    val newItem = recordings.value.find { it.name.startsWith(newName) }
                    newItem?.let {
                        playAudio(it)
                        seekTo(savedPos)
                    }
                }
            }
        }
    }

    fun seekTo(position: Long) {
        val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_SEEK
            putExtra(AudioCaptureService.EXTRA_SEEK_POSITION, position)
        }
        getApplication<Application>().startService(intent)
    }

    fun playNextRecording() {
        val currentList = recordings.value
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.path == _currentlyPlaying.value?.path }
        
        when (_playbackMode.value) {
            PlaybackMode.SEQUENTIAL -> {
                if (currentIndex != -1 && currentIndex < currentList.size - 1) {
                    playAudio(currentList[currentIndex + 1])
                } else {
                    _currentlyPlaying.value = null
                }
            }
            PlaybackMode.LOOP_LIST -> {
                val nextIndex = (currentIndex + 1) % currentList.size
                playAudio(currentList[nextIndex])
            }
            PlaybackMode.SHUFFLE -> {
                val nextIndex = Random.nextInt(currentList.size)
                playAudio(currentList[nextIndex])
            }
            PlaybackMode.REPEAT_ONE -> {
                _currentlyPlaying.value?.let { playAudio(it) }
            }
        }
    }

    fun playPreviousRecording() {
        val currentList = recordings.value
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.path == _currentlyPlaying.value?.path }
        if (currentIndex == -1) return

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentList.size - 1
        playAudio(currentList[prevIndex])
    }

    fun togglePlaybackMode() {
        val newMode = when (_playbackMode.value) {
            PlaybackMode.SEQUENTIAL -> PlaybackMode.LOOP_LIST
            PlaybackMode.LOOP_LIST -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SEQUENTIAL
        }
        _playbackMode.value = newMode
        viewModelScope.launch { settingsRepository.savePlaybackMode(newMode) }
    }

    fun toggleEditLock() {
        val newLocked = !_isEditLocked.value
        _isEditLocked.value = newLocked
        viewModelScope.launch { settingsRepository.saveEditLocked(newLocked) }
    }

    fun deleteRecording(item: AudioItem) {
        if (repository.deleteFile(item)) {
            loadRecordings()
        }
    }

    fun restoreFromTrash(item: AudioItem) {
        if (repository.restoreFromTrash(item)) {
            loadRecordings()
        }
    }

    fun permanentlyDelete(item: AudioItem) {
        if (repository.permanentlyDelete(item)) {
            loadRecordings()
        }
    }

    fun emptyTrash() {
        repository.emptyTrash()
        loadRecordings()
    }

    fun importFiles(uris: List<android.net.Uri>) {
        viewModelScope.launch {
            if (repository.importFiles(uris) > 0) {
                loadRecordings()
            }
        }
    }

    fun importPlaylistFile(uri: android.net.Uri) {
        viewModelScope.launch {
            val playlistName = repository.importPlaylist(uri)
            if (playlistName != null) {
                android.widget.Toast.makeText(getApplication(), "재생목록 '${playlistName}'을 불러왔습니다.", android.widget.Toast.LENGTH_SHORT).show()
                _selectedPlaylist.value = playlistName
                loadRecordings()
            } else {
                android.widget.Toast.makeText(getApplication(), "재생목록 가져오기 실패", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun moveItemUp(item: AudioItem) {
        val currentList = _recordings.value.toMutableList()
        val index = currentList.indexOf(item)
        if (index > 0) {
            currentList.removeAt(index)
            currentList.add(index - 1, item)
            _recordings.value = currentList
            repository.saveOrder(currentList)
        }
    }

    fun moveItemDown(item: AudioItem) {
        val currentList = _recordings.value.toMutableList()
        val index = currentList.indexOf(item)
        if (index != -1 && index < currentList.size - 1) {
            currentList.removeAt(index)
            currentList.add(index + 1, item)
            _recordings.value = currentList
            repository.saveOrder(currentList)
        }
    }

    fun selectPlaylist(name: String?) {
        _selectedPlaylist.value = name
        loadRecordings()
    }

    fun addPlaylist(name: String) {
        if (repository.createPlaylist(name)) {
            android.widget.Toast.makeText(getApplication(), "재생목록 '${name}' 생성됨", android.widget.Toast.LENGTH_SHORT).show()
            _selectedPlaylist.value = name // 새 재생목록 생성 시 자동 선택
            loadRecordings()
        }
    }

    fun deletePlaylist(name: String) {
        if (repository.deletePlaylist(name)) {
            if (_selectedPlaylist.value == name) {
                _selectedPlaylist.value = null
            }
            loadRecordings()
        }
    }

    fun renamePlaylist(oldName: String, newName: String) {
        if (repository.renamePlaylist(oldName, newName)) {
            if (_selectedPlaylist.value == oldName) {
                _selectedPlaylist.value = newName
            }
            loadRecordings()
        }
    }

    fun addItemToPlaylist(item: AudioItem, playlistName: String) {
        if (repository.addItemToPlaylist(item, playlistName)) {
            loadRecordings()
        }
    }

    fun addItemsToPlaylist(items: List<AudioItem>, playlistName: String) {
        var anySuccess = false
        items.forEach { item ->
            if (repository.addItemToPlaylist(item, playlistName)) {
                anySuccess = true
            }
        }
        if (anySuccess) {
            loadRecordings()
        }
    }

    fun removeItemFromPlaylist(item: AudioItem, playlistName: String) {
        if (repository.removeItemFromPlaylist(item, playlistName)) {
            loadRecordings()
        }
    }

    fun setActiveTab(tab: AudioTab) {
        _activeTab.value = tab
    }

    fun setFilterMode(mode: String) {
        _filterMode.value = mode
    }

    fun toggleRecordingSource() {
        val newSource = if (_recordingSource.value == RecordingSource.INTERNAL) RecordingSource.MIC else RecordingSource.INTERNAL
        _recordingSource.value = newSource
        if (newSource == RecordingSource.MIC) {
            dismissPrepare() // 마이크로 전환 시 이전 내부 오디오 준비 해제
        }
    }

    fun prepareRecording(resultData: Intent) {
        val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_PREPARE
            putExtra(AudioCaptureService.EXTRA_RESULT_DATA, resultData)
            putExtra(AudioCaptureService.EXTRA_FILE_PATH, repository.getNewFilePath())
        }
        getApplication<Application>().startService(intent)
    }

    fun startRecording(resultData: Intent?) {
        val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_START_RECORDING
            putExtra(AudioCaptureService.EXTRA_RECORDING_SOURCE, _recordingSource.value.name)
            if (resultData != null) putExtra(AudioCaptureService.EXTRA_RESULT_DATA, resultData)
            putExtra(AudioCaptureService.EXTRA_FILE_PATH, repository.getNewFilePath())
        }
        getApplication<Application>().startService(intent)
        _isRecording.value = true
    }

    fun stopRecording() {
        val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP_RECORDING
        }
        getApplication<Application>().startService(intent)
        _isRecording.value = false
        // 녹음 완료 후 목록 새로고침
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            loadRecordings()
        }
    }

    fun dismissPrepare() {
        val intent = Intent(getApplication(), AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_DISMISS_PREPARE
        }
        getApplication<Application>().startService(intent)
    }
}
