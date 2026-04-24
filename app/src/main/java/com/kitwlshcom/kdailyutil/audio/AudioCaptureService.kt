package com.kitwlshcom.kdailyutil.audio

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.kitwlshcom.kdailyutil.MainActivity
import com.kitwlshcom.kdailyutil.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.IOException
import android.media.session.MediaSession
import kotlin.concurrent.thread

class AudioCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var mediaPlayer: MediaPlayer? = null
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSession? = null
    private var isForeground = false
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val CHANNEL_ID = "AudioCaptureChannel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_PREPARE = "ACTION_PREPARE"
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_SEEK = "ACTION_SEEK"
        const val ACTION_STOP_PLAYBACK = "ACTION_STOP_PLAYBACK"
        const val ACTION_SHOW_FLOATING = "ACTION_SHOW_FLOATING"
        const val ACTION_HIDE_FLOATING = "ACTION_HIDE_FLOATING"
        const val ACTION_DISMISS_PREPARE = "ACTION_DISMISS_PREPARE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
        const val EXTRA_SEEK_POSITION = "EXTRA_SEEK_POSITION"
        const val EXTRA_RECORDING_SOURCE = "EXTRA_RECORDING_SOURCE"

        private val _currentPosition = MutableStateFlow(0L)
        val currentPosition = _currentPosition.asStateFlow()

        private val _playbackDuration = MutableStateFlow(0L)
        val playbackDuration = _playbackDuration.asStateFlow()

        private val _isPlaybackPaused = MutableStateFlow(false)
        val isPlaybackPaused = _isPlaybackPaused.asStateFlow()

        private val _currentlyPlaying = MutableStateFlow<com.kitwlshcom.kdailyutil.data.model.AudioItem?>(null)
        val currentlyPlaying = _currentlyPlaying.asStateFlow()

        private val _playbackCompleted = MutableSharedFlow<Unit>()
        val playbackCompleted = _playbackCompleted.asSharedFlow()

        private val _isPrepared = MutableStateFlow(false)
        val isPrepared = _isPrepared.asStateFlow()

        private var savedResultData: Intent? = null
            set(value) {
                field = value
                _isPrepared.value = value != null
            }
        private var savedFilePath: String? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    _currentPosition.value = it.currentPosition.toLong()
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val noisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                pauseAudio()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val filter = android.content.IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
        
        mediaSession = MediaSession(this, "AudioCaptureService").apply {
            isActive = true
        }
        
        createNotificationChannel()
        // WakeLock 초기화
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KDailyUtil::AudioWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREPARE -> {
                savedResultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                savedFilePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: savedFilePath
                _isPrepared.value = true
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification("녹음 준비됨. 시작 버튼을 누르세요."),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
                )
                // showFloatingControl() // 앱 내부에서는 Lifecycle에서 표시/제어함
            }
            ACTION_DISMISS_PREPARE -> {
                savedResultData = null
                _isPrepared.value = false
                hideFloatingControl()
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            ACTION_START_RECORDING -> {
                val resultData = savedResultData
                val filePath = savedFilePath ?: intent.getStringExtra(EXTRA_FILE_PATH)
                val source = intent.getStringExtra(EXTRA_RECORDING_SOURCE) ?: "INTERNAL"
                
                if (filePath != null) {
                    val serviceType = if (source == "MIC") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
                    }
                    
                    startForeground(
                        NOTIFICATION_ID, 
                        createNotification(if (source == "MIC") "마이크 녹음 중..." else "시스템 소리 녹음 중..."),
                        serviceType
                    )
                    startRecording(resultData, filePath, source)
                    updateFloatingButtonIcon()
                }
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
                updateFloatingButtonIcon()
                updateNotification("녹음 중지됨. 저장되었습니다.")
            }
            ACTION_PLAY -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                if (filePath != null) {
                    playOrResumeAudio(filePath)
                }
            }
            ACTION_PAUSE -> {
                pauseAudio()
            }
            ACTION_SEEK -> {
                val position = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                seekTo(position)
            }
            ACTION_STOP_PLAYBACK -> {
                stopAudio()
            }
            ACTION_SHOW_FLOATING -> {
                showFloatingControl()
            }
            ACTION_HIDE_FLOATING -> {
                hideFloatingControl()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(resultData: Intent?, filePath: String, source: String = "INTERNAL") {
        if (isRecording) return
        acquireWakeLock()

        if (source == "INTERNAL" && resultData != null) {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpManager.getMediaProjection(Activity.RESULT_OK, resultData)
        }

        val sampleRate = 44100
        val channelConfig = if (source == "INTERNAL") AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val audioSource = if (source == "INTERNAL") {
            // Internal audio requires specific config and MediaProjection
            -1 // We'll handle this separately
        } else {
            MediaRecorder.AudioSource.MIC
        }

        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)

        audioRecord = if (source == "INTERNAL" && mediaProjection != null) {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AudioRecord.Builder()
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()
            } else null
        } else if (source == "MIC") {
            AudioRecord(audioSource, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        } else null

        if (audioRecord == null) {
            Log.e(TAG, "Failed to initialize AudioRecord")
            stopForeground(STOP_FOREGROUND_REMOVE)
            releaseWakeLock()
            return
        }

        isRecording = true
        audioRecord?.startRecording()

        thread {
            try {
                val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, if (channelConfig == AudioFormat.CHANNEL_IN_STEREO) 2 else 1)
                format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
                
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val muxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var trackIndex = -1
                
                encoder.start()
                val bufferInfo = MediaCodec.BufferInfo()
                val pcmData = ByteArray(bufferSize)
                
                while (isRecording) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        val read = audioRecord?.read(pcmData, 0, bufferSize) ?: 0
                        if (read > 0) {
                            inputBuffer?.put(pcmData, 0, read)
                            encoder.queueInputBuffer(inputBufferIndex, 0, read, System.nanoTime() / 1000, 0)
                        }
                    }
                    
                    var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    while (outputBufferIndex >= 0) {
                        if (trackIndex == -1) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                        }
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        outputBuffer?.let { muxer.writeSampleData(trackIndex, it, bufferInfo) }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    }
                }
                encoder.stop(); encoder.release()
                muxer.stop(); muxer.release()
            } catch (e: Exception) { 
                Log.e(TAG, "Recording error", e) 
            } finally {
                handler.post { releaseWakeLock() }
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop(); audioRecord?.release(); audioRecord = null
        releaseWakeLock()
        
        updateFloatingButtonIcon()
        updateNotification("녹음 대기 중... 언제든 다시 시작하세요.")
        
        val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "KDailyUtil")
        if (!dir.exists()) dir.mkdirs()
        savedFilePath = File(dir, "capture_${System.currentTimeMillis()}.m4a").absolutePath
    }

    private fun showFloatingControl() {
        if (!Settings.canDrawOverlays(this) || floatingView != null) return
        // 사용자가 마이크 버튼을 눌러서 'savedResultData'가 있는 경우에만 표시
        if (savedResultData == null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val frame = FrameLayout(this)
        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setBackgroundResource(android.R.drawable.presence_online)
            setPadding(20, 20, 20, 20)
        }
        frame.addView(icon)
        floatingView = frame
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 100; y = 100 }

        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0; private var initialY = 0
            private var initialTouchX = 0f; private var initialTouchY = 0f
            private var startTime = 0L
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params!!.x; initialY = params!!.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        startTime = System.currentTimeMillis(); return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params); return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (System.currentTimeMillis() - startTime < 200) toggleRecording()
                        return true
                    }
                }
                return false
            }
        })
        windowManager?.addView(floatingView, params)
    }

    private fun toggleRecording() {
        val action = if (isRecording) ACTION_STOP_RECORDING else ACTION_START_RECORDING
        startService(Intent(this, AudioCaptureService::class.java).apply { this.action = action })
    }

    private fun updateFloatingButtonIcon() {
        val icon = (floatingView as? FrameLayout)?.getChildAt(0) as? ImageView
        icon?.let {
            if (isRecording) {
                it.setImageResource(android.R.drawable.ic_media_pause)
                it.setBackgroundResource(android.R.drawable.presence_busy)
            } else {
                it.setImageResource(android.R.drawable.ic_btn_speak_now)
                it.setBackgroundResource(android.R.drawable.presence_online)
            }
        }
    }

    private fun hideFloatingControl() {
        floatingView?.let { windowManager?.removeView(it); floatingView = null }
    }

    private var currentPlayingPath: String? = null

    private fun playOrResumeAudio(filePath: String) {
        if (mediaPlayer != null && currentPlayingPath == filePath) {
            mediaPlayer?.start()
            _isPlaybackPaused.value = false
            handler.post(progressRunnable)
            acquireWakeLock()
            val notification = createNotification("재생 중: ${File(filePath).name}")
            if (!isForeground) {
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else 0
                startForeground(NOTIFICATION_ID, notification, type)
                isForeground = true
            } else {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            
            // 재생 정보 업데이트
            val repository = com.kitwlshcom.kdailyutil.data.repository.AudioRepository(this)
            val item = repository.getRecordedFiles().find { it.path == filePath }
            _currentlyPlaying.value = item
            
            return
        }

        // stopAudio() 호출을 제거하여 포그라운드 상태를 유지함
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(this@AudioCaptureService, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            }
        } else {
            mediaPlayer?.reset()
        }
        
        currentPlayingPath = filePath
        mediaPlayer?.apply {
            try {
                setDataSource(filePath)
                prepare()
                _playbackDuration.value = duration.toLong()
                start()
                _isPlaybackPaused.value = false
                handler.post(progressRunnable)
                acquireWakeLock()
                
                val notification = createNotification("재생 중: ${File(filePath).name}")
                if (!isForeground) {
                    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    } else 0
                    startForeground(NOTIFICATION_ID, notification, type)
                    isForeground = true
                } else {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
                
                
                // 재생 정보 업데이트
                val repository = com.kitwlshcom.kdailyutil.data.repository.AudioRepository(this@AudioCaptureService)
                val item = repository.getRecordedFiles().find { it.path == filePath }
                _currentlyPlaying.value = item

                setOnCompletionListener {
                    handler.removeCallbacks(progressRunnable)
                    _currentPosition.value = 0
                    // _currentlyPlaying.value = null // ViewModel에서 다음 곡 계산을 위해 유지
                    updateNotification("재생 완료")
                    thread { kotlinx.coroutines.runBlocking { _playbackCompleted.emit(Unit) } }
                }
            } catch (e: IOException) { Log.e(TAG, "Playback error", e) }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes max */)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaybackPaused.value = true
                handler.removeCallbacks(progressRunnable)
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_DETACH)
                updateNotification("일시정지 중")
            }
        }
    }

    private fun stopAudio() {
        handler.removeCallbacks(progressRunnable)
        _currentPosition.value = 0
        currentPlayingPath = null
        _currentlyPlaying.value = null
        _isPlaybackPaused.value = false
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "오디오 서비스", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KDailyUtil 오디오")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {}
        releaseWakeLock()
        hideFloatingControl(); stopRecording(); stopAudio()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
