package com.kitwlshcom.kdailyutil.audio

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
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
import android.media.session.PlaybackState
import kotlin.concurrent.thread

class AudioCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    /** 지금 무엇을 녹음 중인지(알림 문구). 재생을 멈출 때 녹음 알림을 되돌리는 데 쓴다. */
    private var recordingLabel = "녹음 중..."
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

        // 블루투스/이어폰 미디어 버튼의 다음/이전 곡 요청을 ViewModel로 전달
        private val _skipToNext = MutableSharedFlow<Unit>()
        val skipToNext = _skipToNext.asSharedFlow()

        private val _skipToPrevious = MutableSharedFlow<Unit>()
        val skipToPrevious = _skipToPrevious.asSharedFlow()

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
                // 이어폰이 빠진 것이다 — 포커스가 돌아와도 스피커로 되살리면 안 된다.
                pauseAudio(fromUser = true)
            }
        }
    }

    // 블루투스 헤드셋/이어폰 및 잠금화면의 미디어 버튼 처리
    private val mediaSessionCallback = object : MediaSession.Callback() {
        /**
         * 🔴 **버튼이 실제로 눌리는 상태를 [MediaPlayer]에게 직접 묻는다**(2026-09-23).
         *
         * 기본 구현은 [PlaybackState]를 보고 재생/일시정지를 가른다. 그 값이 한 번이라도
         * 실제 상태와 어긋나면 **버튼이 «아무 일도 안 하는 것처럼»** 보인다 —
         * 전화가 끝난 뒤 이어폰 버튼이 먹통이던 증상이 정확히 그 모양이었다.
         * 플레이어에게 직접 물으면 어긋날 여지가 없다.
         */
        override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
            val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            }
            // 🔴 `repeatCount`를 본다 — 이어폰 버튼을 **길게 누르면** ACTION_DOWN이 초당 여러 번
            //    쏟아진다. 안 거르면 누르고 있는 동안 재생/일시정지가 깜빡거린다.
            if (event == null || event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) {
                return super.onMediaButtonEvent(mediaButtonIntent)
            }
            Log.d(TAG, "미디어 버튼: keyCode=${event.keyCode} playing=${mediaPlayer?.isPlaying}")
            return when (event.keyCode) {
                // 🔴 이어폰 한 개짜리 버튼은 기기마다 HEADSETHOOK / PLAY_PAUSE 둘 중 하나로 온다.
                //    둘 다 잡는다 — 한쪽만 잡으면 «내 이어폰에서만 안 되는» 앱이 된다.
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (mediaPlayer?.isPlaying == true) onPause() else onPlay()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> { onPlay(); true }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> { onPause(); true }
                else -> super.onMediaButtonEvent(mediaButtonIntent)
            }
        }

        override fun onPlay() {
            // 일시정지 상태에서 재생 버튼 → 현재 곡 이어재생.
            // 🔴 `lastPlayedPath`로 떨어진다 — 서비스가 한 번 죽었다 살아나면
            //    `currentPlayingPath`는 null이고, 그때 버튼은 «눌러도 반응 없는 버튼»이 된다.
            (currentPlayingPath ?: lastPlayedPath)?.let { playOrResumeAudio(it) }
                ?: Log.w(TAG, "재생 버튼을 받았지만 틀 것이 없다")
        }
        override fun onPause() { pauseAudio(fromUser = true) }
        override fun onStop() { stopAudio() }
        override fun onSkipToNext() {
            thread { kotlinx.coroutines.runBlocking { _skipToNext.emit(Unit) } }
        }
        override fun onSkipToPrevious() {
            thread { kotlinx.coroutines.runBlocking { _skipToPrevious.emit(Unit) } }
        }
        override fun onSeekTo(pos: Long) { seekTo(pos) }
    }

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    /**
     * 🔴 **요청 객체를 하나만 만들어 재사용한다**(2026-09-23에 고쳤다).
     *
     * 예전에는 재생할 때마다 `AudioFocusRequest`를 **새로 만들어** 필드를 갈아치웠다.
     * 그러면 `abandonAudioFocusRequest`가 **마지막 것만** 반납하게 되고, 앞서 등록된 요청이
     * 시스템 쪽에 남아 «누가 포커스를 쥐고 있는지»가 어긋난다. 요청과 반납은 **같은 객체**여야 한다.
     *
     * 🔴 **`setAcceptsDelayedFocusGain`이 이 기능의 핵심이다.**
     * 통화 중에 포커스를 달라고 하면 **거절**당한다(`AUDIOFOCUS_REQUEST_FAILED`). 거절은 «줄 서 있다»가
     * 아니라 «안 준다»이므로, 그대로 두면 통화가 끝나도 **아무도 우리를 깨워 주지 않는다.**
     * 이 줄이 있어야 거절 대신 `AUDIOFOCUS_REQUEST_DELAYED`(접수됨)가 돌아오고,
     * **통화가 끝나는 순간 [AudioManager.AUDIOFOCUS_GAIN]이 실제로 날아온다.**
     */
    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(afChangeListener, handler)
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    /**
     * 인터럽트가 끝나면 **자동으로 다시 틀어야 하는가**.
     *
     * 🔴 **이 깃발을 «지우는» 자리는 셋뿐이다** — 사용자가 직접 멈췄을 때 · 실제로 재개할 때 ·
     * 정지할 때. 2026-09-23 이전에는 포커스를 잃을 때마다 `= (재생 중인가)`로 **덮어썼고**,
     * 그것이 「전화 끊어도 자동 재생이 안 된다」의 원인이었다(아래 [afChangeListener] 주석).
     */
    private var resumeOnFocusGain = false
    private var isDucking = false

    /** 영구 상실 뒤 «지연 포커스»를 이미 한 번 걸어 뒀는가. 상실이 반복돼도 한 번만 건다. */
    private var delayedFocusPending = false

    /**
     * 지금 통화 중인가(벨 울리는 중 포함). 권한이 필요 없는 [AudioManager.getMode]로 본다.
     *
     * 🔴 **왜 보는가** — 표준대로면 통화는 «일시적 상실»을 주지만, **기기·통신 앱에 따라
     * «영구 상실»을 주는 경우가 있다.** 그것을 곧이곧대로 «다른 음악앱이 재생을 가져갔다»로
     * 읽으면 통화가 끝나도 영영 재개하지 않는다.
     *
     * ⚠️ **완전하지는 않다** — 포커스를 빼앗기는 시점과 오디오 모드가 통화로 바뀌는 시점의
     * 앞뒤가 기기마다 다를 수 있다. 아직 `MODE_NORMAL`인 채로 상실이 먼저 오면 이 판정이 빗나간다.
     * 그래서 이것만 믿지 않고 **일시적 상실 쪽(정상 경로)을 먼저 튼튼하게** 고쳤다.
     */
    private fun isInCall(): Boolean = when (audioManager.mode) {
        AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION, AudioManager.MODE_RINGTONE -> true
        else -> false
    }

    /** 덕킹(볼륨 낮춤)을 풀어 원래 크기로. 🔴 풀지 않으면 «소리가 작아진 채 돌아온다». */
    private fun unduck() {
        if (isDucking) {
            isDucking = false
            try { mediaPlayer?.setVolume(1.0f, 1.0f) } catch (e: Exception) { }
        }
    }

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "오디오 포커스 변화: $focusChange (통화중=${isInCall()} 예약=$resumeOnFocusGain)")
        when (focusChange) {
            // 영구 상실(예: 다른 음악앱 재생) → 정지하고 자동 재개하지 않음.
            //
            // 🔴 **영구 상실 뒤에는 AUDIOFOCUS_GAIN이 오지 않는다** — 우리 요청이 포커스 스택에서
            //    아예 빠지기 때문이다. 그래서 «예약해 두고 기다리기»는 여기서 통하지 않는다.
            //    통화 때문에 잃은 것이라면 **다시 요청해서 줄을 서야** 한다(지연 포커스).
            AudioManager.AUDIOFOCUS_LOSS -> {
                val inCall = isInCall()
                val wasPlaying = mediaPlayer?.isPlaying == true
                unduck()
                pauseAudio(fromUser = false)
                if (inCall && wasPlaying && !delayedFocusPending) {
                    delayedFocusPending = true
                    resumeOnFocusGain = true
                    callRetryTicks = 0
                    // 콜백 안에서 곧장 다시 요청하지 않는다 — 한 박자 미뤄 재진입을 피한다.
                    handler.post(delayedFocusRunnable)
                } else {
                    // 🔴 **여기서도 `delayedFocusPending`을 반드시 내린다**(재검토에서 잡혔다).
                    //    안 내리면 이렇게 된다: 통화 중 상실 → 줄 섬(pending=true) → 통화 중에
                    //    그 대기가 취소되면 **또 LOSS가 온다** → 이번엔 «재생 중»이 아니라
                    //    이 else로 떨어지고, pending이 true로 **영영 남는다.**
                    //    그러면 다음 전화부터는 위 분기가 아예 열리지 않아
                    //    «자동 재개가 한 번은 되더니 그 뒤로 안 된다»가 된다.
                    giveUpAutoResume()
                }
            }
            // 일시적 상실(예: 수신 통화, 음성 안내) → 일시정지 후 복귀 시 재개.
            //
            // 🔴 **여기가 「전화 끊어도 자동 재생이 안 된다」의 주된 원인이었다**(2026-09-23에 고쳤다).
            //    전화는 포커스를 **두 번** 가져간다 — ① 벨이 울릴 때 ② 통화가 연결될 때.
            //    예전 코드는 `resumeOnFocusGain = (재생 중인가)`로 **대입**했기 때문에,
            //    ①에서 이미 멈춰 «재생 중 아님»이 된 상태로 ②가 오면 **예약이 false로 지워졌다.**
            //    그래서 통화가 끝나 AUDIOFOCUS_GAIN이 와도 되살릴 근거가 남아 있지 않았다.
            //    → **켜기만 하고 끄지 않는다.** 끄는 것은 «사용자가 직접 멈췄을 때»뿐이다.
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (mediaPlayer?.isPlaying == true) resumeOnFocusGain = true
                unduck()
                pauseAudio(fromUser = false)
            }
            // 덕킹 가능(예: 내비 안내음) → 정지 대신 볼륨만 낮춤
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer?.isPlaying == true) {
                    isDucking = true
                    try { mediaPlayer?.setVolume(0.2f, 0.2f) } catch (e: Exception) { }
                }
            }
            // 포커스 복귀(일시적 상실의 끝 · 지연 포커스 승인) → 볼륨 복원 + 자동 재개
            AudioManager.AUDIOFOCUS_GAIN -> {
                delayedFocusPending = false
                callRetryTicks = 0
                handler.removeCallbacks(delayedFocusRunnable)
                unduck()
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    val path = currentPlayingPath ?: lastPlayedPath
                    if (path != null) {
                        Log.d(TAG, "인터럽트가 끝났다 — 자동으로 이어 재생한다")
                        playOrResumeAudio(path)
                    }
                }
            }
        }
    }

    /** 통화가 끝나기를 기다리며 다시 줄 서는 간격·횟수(2초 × 150 ≈ 5분). 통화가 그보다 길면 포기한다. */
    private var callRetryTicks = 0
    private val callRetryIntervalMs = 2_000L
    private val callRetryMaxTicks = 150

    private val delayedFocusRunnable = Runnable { requestDelayedFocusAfterCall() }

    /** 자동 재개를 접는다. 🔴 **깃발 셋을 한자리에서 내린다** — 흩어 두면 하나가 남는다(위 else 주석). */
    private fun giveUpAutoResume() {
        resumeOnFocusGain = false
        delayedFocusPending = false
        callRetryTicks = 0
        handler.removeCallbacks(delayedFocusRunnable)
        abandonAudioFocus()
    }

    /**
     * 영구 상실로 스택에서 빠진 요청을 **다시 줄 세운다**.
     *
     * 통화 중이면 [AudioManager.AUDIOFOCUS_REQUEST_DELAYED]로 접수되고, 통화가 끝나는 순간
     * [AudioManager.AUDIOFOCUS_GAIN]이 온다. 🔴 **그런데 승인(`GRANTED`)이 곧 «통화가 끝났다»는
     * 뜻은 아니다** — 통화 앱이 오디오 포커스 다툼에 아예 끼지 않는 기기가 있고, 그런 기기에서는
     * 통화 도중에도 포커스가 덜컥 승인된다. 그대로 틀면 **통화 위에 녹음이 얹혀 나간다.**
     * 그래서 승인을 받아도 [isInCall]을 한 번 더 보고, 아직 통화 중이면 **반납하고 다시 기다린다.**
     */
    private fun requestDelayedFocusAfterCall() {
        // 기다리는 사이에 사용자가 정지했거나 다른 것을 틀었을 수 있다.
        if (!resumeOnFocusGain || mediaPlayer == null) {
            delayedFocusPending = false
            callRetryTicks = 0
            return
        }
        abandonAudioFocus()
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        Log.d(TAG, "통화 뒤 포커스 재요청 결과=$result (통화중=${isInCall()} 시도=$callRetryTicks)")
        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                if (isInCall()) {
                    // 받았지만 **아직 통화 중이다.** 반납하고 조금 뒤 다시 본다.
                    abandonAudioFocus()
                    if (++callRetryTicks <= callRetryMaxTicks) {
                        handler.postDelayed(delayedFocusRunnable, callRetryIntervalMs)
                    } else {
                        Log.w(TAG, "통화가 너무 길다 — 자동 재개를 접는다(버튼은 그대로 동작한다)")
                        giveUpAutoResume()
                    }
                    return
                }
                // 통화가 끝나 있었다 → GAIN 콜백을 기다릴 것이 없다. 여기서 직접 잇는다.
                delayedFocusPending = false
                callRetryTicks = 0
                resumeOnFocusGain = false
                (currentPlayingPath ?: lastPlayedPath)?.let { playOrResumeAudio(it) }
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                // 줄을 섰다. 통화가 끝나면 GAIN이 온다.
            }
            else -> {
                // 줄조차 못 섰다 — 자동 재개는 접는다. 이어폰 버튼은 그대로 살아 있다.
                Log.w(TAG, "지연 포커스도 거절됐다 — 자동 재개를 접는다(버튼은 동작한다)")
                giveUpAutoResume()
            }
        }
    }

    /** @return [AudioManager.AUDIOFOCUS_REQUEST_GRANTED] · `_DELAYED` · `_FAILED` */
    private fun requestAudioFocus(): Int = audioManager.requestAudioFocus(audioFocusRequest)

    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    /** 미디어 세션 재생 상태 갱신 — 이게 있어야 시스템이 미디어 버튼을 세션으로 라우팅한다. */
    private fun updatePlaybackState(state: Int) {
        val position = try { mediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Exception) { 0L }
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_STOP or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            )
            .setState(state, position, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
        if (!(mediaSession?.isActive ?: false)) mediaSession?.isActive = true
    }

    private fun updateMediaMetadata(filePath: String) {
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, File(filePath).nameWithoutExtension)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "KDailyUtil 오디오")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, _playbackDuration.value)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val filter = android.content.IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
        
        mediaSession = MediaSession(this, "AudioCaptureService").apply {
            setCallback(mediaSessionCallback, handler)
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
                // 🔴 깃발을 세워야 재생이 포그라운드 유형을 «재생»만으로 덮어쓰지 않는다.
                isForeground = true
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
                        createNotification(
                            (if (source == "MIC") "마이크 녹음 중..." else "시스템 소리 녹음 중...")
                                .also { recordingLabel = it }
                        ),
                        serviceType
                    )
                    isForeground = true
                    startRecording(resultData, filePath, source)
                    updateFloatingButtonIcon()
                }
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
                updateFloatingButtonIcon()
                // 상주 알림은 stopRecording()에서 제거됨 (유휴 상태 알림 미표시)
            }
            ACTION_PLAY -> {
                // 🔴 알림·이어폰의 «재생»에는 경로가 붙어 있지 않다 — 듣던 것으로 떨어진다.
                //    이 폴백이 없으면 그 버튼들은 «눌러도 아무 일도 안 하는 버튼»이 된다.
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                    ?: currentPlayingPath ?: lastPlayedPath
                if (filePath != null) {
                    playOrResumeAudio(filePath)
                } else {
                    Log.w(TAG, "ACTION_PLAY를 받았지만 틀 것이 없다")
                }
            }
            ACTION_PAUSE -> {
                pauseAudio(fromUser = true)
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
        // 녹음 종료 → 유휴 상태이므로 상주 포그라운드 알림 제거
        // (백그라운드에서 '녹음 대기 중...' 알림이 계속 노출되지 않도록. 알림은 브리핑 시간에만 표시)
        //
        // 🔴 **단, 재생이 살아 있으면 내려놓지 않는다**(2026-09-23). 내려놓으면 재생 중인데도
        //    평범한 백그라운드 서비스가 되어 ① 시스템이 죽일 수 있고 ② 다음에 포그라운드로
        //    올리려다 백그라운드 제한에 걸린다. 재생용 알림으로 **갈아 끼운다**.
        if (mediaPlayer != null) {
            val name = currentPlayingPath?.let { File(it).name } ?: "오디오"
            goForeground(createPlaybackNotification(name, playing = mediaPlayer?.isPlaying == true))
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }

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

    /**
     * 마지막으로 튼 파일. [currentPlayingPath]와 달리 [stopAudio]에서 지우지 않는다.
     *
     * 🔴 **이어폰 버튼·알림 버튼이 «틀 것»을 잃지 않게 하는 안전망이다.** 정지한 뒤에는
     * `currentPlayingPath`가 null이고, 알림의 재생 버튼에는 애초에 경로가 붙어 있지 않다.
     * 그 상태에서 버튼은 **눌러도 아무 일도 안 하는 버튼**이 된다.
     *
     * ⚠️ **인스턴스 필드다** — 서비스가 죽으면 같이 사라진다(그때는 미디어 세션도 없어져
     * 버튼이 우리에게 오지도 않으므로 아쉬울 것이 없다). 파일이 지워져 있으면 재생 시도가
     * 실패하는데, 그 경로에서 이 값을 비운다([playOrResumeAudio]의 catch).
     */
    private var lastPlayedPath: String? = null

    /**
     * 포커스를 못 얻어 못 튼 상태를 **화면·세션·알림에 그대로 반영한다**.
     *
     * 🔴 **이게 없으면 「재생 버튼이 죽었다」가 된다.** 화면의 재생/일시정지 버튼은
     * `isPlaybackPaused`를 보고 «다음에 무엇을 보낼지»를 정한다. 서비스가 못 틀었는데 그 값을
     * 그대로 두면, 화면은 «재생 중»으로 믿고 다음 누름에 **일시정지를 보낸다** —
     * 소리는 안 나는데 버튼만 두 번 헛도는 정확히 그 증상이다.
     */
    private fun reserveResume(filePath: String, delayed: Boolean) {
        // 🔴 **줄을 선 경우(DELAYED)에만 예약한다.** 거절(FAILED)은 «줄도 못 섰다»는 뜻이라
        //    깨워 줄 GAIN이 오지 않는다. 그런데도 예약을 켜 두면, 한참 뒤 엉뚱한 이유로 온 GAIN에
        //    **사용자가 시키지도 않은 재생**이 시작된다.
        resumeOnFocusGain = delayed
        lastPlayedPath = filePath
        _isPlaybackPaused.value = true
        updatePlaybackState(PlaybackState.STATE_PAUSED)
        goForeground(
            createPlaybackNotification(
                File(filePath).name, playing = false,
                // 🔴 «왜 안 나오는지»를 말해 준다 — 말 없이 멈춰 있으면 고장으로 읽힌다.
                statusText = if (delayed) "통화가 끝나면 이어서 재생돼요" else "지금은 재생할 수 없어요"
            )
        )
        Log.w(TAG, if (delayed) "통화 중이라 줄을 섰다 — 끝나면 저절로 이어진다" else "포커스를 못 받았다 — 버튼으로 다시 눌러야 한다")
    }

    private fun playOrResumeAudio(filePath: String) {
        if (mediaPlayer != null && currentPlayingPath == filePath) {
            // 🔴 **포커스를 못 얻으면 틀지 않는다**(2026-09-23). 통화 중에 이어폰 버튼을 누르면
            //    거절당하는데, 그대로 start()하면 **통화 위에 녹음이 얹혀 나간다.**
            val focus = requestAudioFocus()
            if (focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                reserveResume(filePath, delayed = focus == AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
                return
            }
            mediaPlayer?.start()
            resumeOnFocusGain = false
            delayedFocusPending = false
            callRetryTicks = 0
            handler.removeCallbacks(delayedFocusRunnable)
            _isPlaybackPaused.value = false
            handler.removeCallbacks(progressRunnable)   // 진행 추적이 두 줄로 겹치지 않게
            handler.post(progressRunnable)
            acquireWakeLock()
            updatePlaybackState(PlaybackState.STATE_PLAYING)
            lastPlayedPath = filePath
            goForeground(createPlaybackNotification(File(filePath).name, playing = true))

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
                updateMediaMetadata(filePath)

                // 재생 정보 업데이트
                val repository = com.kitwlshcom.kdailyutil.data.repository.AudioRepository(this@AudioCaptureService)
                _currentlyPlaying.value = repository.getRecordedFiles().find { it.path == filePath }

                // 🔴 **완료 리스너를 포커스 요청보다 먼저 건다**(2026-09-23).
                //    통화 중이라 거절당하면 아래에서 곧장 빠져나가는데, 그 뒤에 걸어 두면
                //    **이 곡은 끝나도 «끝났다»는 신호를 내지 않는다** — 다음 곡 자동 넘김이 죽는다.
                setOnCompletionListener {
                    handler.removeCallbacks(progressRunnable)
                    _currentPosition.value = 0
                    // _currentlyPlaying.value = null // ViewModel에서 다음 곡 계산을 위해 유지
                    // 🔴 **다 들었으면 «멈췄다»고 분명히 말한다**(2026-09-23 · 재검토에서 잡혔다).
                    //    ① 세션 상태를 «재생 중»으로 두면 이어폰 버튼이 «일시정지»로 해석돼
                    //       아무 일도 일어나지 않는다.
                    //    ② `_isPlaybackPaused`도 올려야 한다. 곡 넘김(ViewModel)이 이 값으로
                    //       «이어재생/일시정지»를 가르는데, false로 남겨 두면 곡이 끝난 그 줄은
                    //       **몇 번을 눌러도 «일시정지»만 보내는 죽은 줄**이 된다.
                    //    ⚠️ 예전에는 화면이 이 값을 스스로 미리 바꿔 놓아 경합이 생길까 봐
                    //       손대지 않았는데, 그 «미리 바꾸기»를 이번에 걷어내서 이제 안전하다.
                    _isPlaybackPaused.value = true
                    updatePlaybackState(PlaybackState.STATE_PAUSED)
                    releaseWakeLock()
                    val name = currentPlayingPath?.let { p -> File(p).name } ?: "오디오"
                    goForeground(createPlaybackNotification(name, playing = false))
                    thread { kotlinx.coroutines.runBlocking { _playbackCompleted.emit(Unit) } }
                }

                val focus = requestAudioFocus()
                if (focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    reserveResume(filePath, delayed = focus == AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
                    return
                }
                start()
                resumeOnFocusGain = false
                delayedFocusPending = false
                callRetryTicks = 0
                handler.removeCallbacks(delayedFocusRunnable)
                _isPlaybackPaused.value = false
                handler.removeCallbacks(progressRunnable)
                handler.post(progressRunnable)
                acquireWakeLock()
                updatePlaybackState(PlaybackState.STATE_PLAYING)
                lastPlayedPath = filePath
                goForeground(createPlaybackNotification(File(filePath).name, playing = true))
            } catch (e: Exception) {
                // 🔴 파일이 지워졌거나 깨졌다. **플레이어를 버린다** — 오류 상태로 남겨 두면
                //    다음 stop()이 IllegalStateException으로 앱을 죽인다.
                Log.e(TAG, "재생 실패($filePath): ${e.message}")
                releasePlayerQuietly()
                currentPlayingPath = null
                if (lastPlayedPath == filePath) lastPlayedPath = null
                _isPlaybackPaused.value = false
                _currentlyPlaying.value = null
                updatePlaybackState(PlaybackState.STATE_STOPPED)
            }
        }
    }

    /** 플레이어를 조용히 버린다 — 오류/완료 등 어느 상태에서 불려도 터지지 않아야 한다. */
    private fun releasePlayerQuietly() {
        try { mediaPlayer?.stop() } catch (e: Exception) { }
        try { mediaPlayer?.release() } catch (e: Exception) { }
        mediaPlayer = null
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

    /**
     * 재생을 멈춘다.
     *
     * @param fromUser **사용자가 직접 멈췄는가**(버튼·알림·이어폰 뽑힘).
     *   true면 «인터럽트가 끝나면 자동 재개» 예약을 **지운다** — 사람이 끈 것을 앱이 멋대로
     *   되살리면 안 된다. 전화·안내음 때문에 멈추는 경우는 false로 부른다.
     *
     * 🔴 **여기서 포그라운드를 내려놓지 않는다**(2026-09-23에 걷어냈다).
     *    예전에는 `stopForeground(STOP_FOREGROUND_DETACH)`를 불러 **일시정지하는 순간 평범한
     *    백그라운드 서비스**가 됐다. 결과가 둘이었다:
     *    ① 시스템이 언제든 이 서비스를 죽일 수 있고, 죽으면 미디어 세션도 같이 사라져
     *       **이어폰 버튼을 받을 상대가 없어진다**(사용자가 겪은 «버튼 무반응»이 이것이다)
     *    ② `isForeground`를 false로 되돌리지 않아, 다시 재생해도 `startForeground()`를
     *       건너뛰고 **백그라운드인 채로** 소리를 냈다
     *    → 틀 것이 남아 있는 동안에는 포그라운드를 유지한다. 음악 앱이 다 그렇게 한다.
     *    알림은 [stopAudio]에서만 걷는다.
     */
    private fun pauseAudio(fromUser: Boolean) {
        // 사람이 끈 것을 앱이 멋대로 되살리지 않는다 — 기다리던 재요청도 같이 접는다.
        if (fromUser) giveUpAutoResume()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaybackPaused.value = true
                handler.removeCallbacks(progressRunnable)
                releaseWakeLock()
                updatePlaybackState(PlaybackState.STATE_PAUSED)
                val name = currentPlayingPath?.let { p -> File(p).name } ?: "오디오"
                goForeground(createPlaybackNotification(name, playing = false))
            }
        }
    }

    private fun stopAudio() {
        handler.removeCallbacks(progressRunnable)
        _currentPosition.value = 0
        currentPlayingPath = null
        _currentlyPlaying.value = null
        _isPlaybackPaused.value = false
        // 사용자가 «그만»이라고 한 것이다 — 포커스가 돌아와도 되살리지 않는다.
        giveUpAutoResume()
        isDucking = false
        releasePlayerQuietly()
        releaseWakeLock()
        updatePlaybackState(PlaybackState.STATE_STOPPED)
        abandonAudioFocus()
        // 🔴 **녹음 중이면 포그라운드를 내려놓지 않는다**(재검토에서 잡혔다).
        //    내려놓으면 마이크·화면 녹화 유형까지 같이 사라져 **녹음이 조용히 끊긴다.**
        //    이 함수는 목록 끝에서 자동으로도 불리므로(ViewModel) 실제로 겹칠 수 있다.
        if (isRecording) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(recordingLabel))
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    /**
     * 포그라운드로 올리거나(처음) 알림만 갈아 끼운다(이미 올라가 있으면).
     *
     * 🔴 **녹음 중에는 아무것도 하지 않는다.** 알림 id가 같아서 «마이크 녹음 중…» 카드를
     * 덮어쓰고, 더 나쁜 것은 `startForeground`를 **재생 유형만** 달고 다시 부르면
     * 서비스의 포그라운드 유형에서 **마이크·화면 녹화가 빠져 녹음이 조용히 끊긴다**는 점이다.
     * 녹음 중에는 이미 포그라운드이므로 아무것도 안 해도 잃는 것이 없다.
     *
     * 🔴 **`startForeground`는 터질 수 있다** — Android 12+에서 앱이 백그라운드일 때 부르면
     * `ForegroundServiceStartNotAllowedException`이 난다. 여기는 **오디오 포커스 콜백**에서도
     * 불리는 자리라 그 예외가 곧 앱 크래시가 된다. 알림을 못 띄우는 것이 죽는 것보다 낫다.
     */
    private fun goForeground(notification: Notification) {
        if (isRecording) return
        if (!isForeground) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else 0
            try {
                startForeground(NOTIFICATION_ID, notification, type)
                isForeground = true
            } catch (e: Exception) {
                Log.w(TAG, "포그라운드로 올리지 못했다(백그라운드 제한?): ${e.message}")
                try { notificationManager.notify(NOTIFICATION_ID, notification) } catch (e2: Exception) { }
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
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

    /**
     * 재생용 알림 — **미디어 알림**(`MediaStyle`)으로 만든다 (2026-09-23).
     *
     * 🔴 **왜 평범한 알림으로는 안 되는가** — 예전 알림은 미디어 세션과 **아무 관계가 없었다.**
     * 시스템은 «이 알림 = 이 세션»을 모르므로 ① 잠금화면·알림 그늘에 재생 버튼이 안 생기고
     * ② **이어폰 버튼을 어느 앱에 보낼지 고를 때 우리를 뒤로 민다.** 전화를 받은 뒤에는
     * 통화 앱이 그 자리를 가져가므로, 링크가 없으면 되찾아 올 근거가 없다.
     * `setMediaSession(token)` 한 줄이 그 연결을 만든다.
     *
     * ⚠️ 녹음용 알림([createNotification])과 **일부러 나눠 두었다** — 녹음 중에 미디어 컨트롤이
     * 뜨면 «녹음을 일시정지하는 버튼»으로 오해하게 된다.
     */
    private fun createPlaybackNotification(
        title: String,
        playing: Boolean,
        /** 상태 한 줄을 갈아 끼울 때. 기본은 «재생 중 / 일시정지 중». */
        statusText: String? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        fun serviceIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
            this, requestCode,
            Intent(this, AudioCaptureService::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggle = if (playing) {
            Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                "일시정지", serviceIntent(ACTION_PAUSE, 11)
            ).build()
        } else {
            Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_media_play),
                "재생", serviceIntent(ACTION_PLAY, 12)
            ).build()
        }
        val stop = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "정지", serviceIntent(ACTION_STOP_PLAYBACK, 13)
        ).build()

        val style = Notification.MediaStyle().setShowActionsInCompactView(0)
        mediaSession?.sessionToken?.let { style.setMediaSession(it) }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText ?: if (playing) "재생 중" else "일시정지 중")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(toggle)
            .addAction(stop)
            .setStyle(style)
            .build()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {}
        handler.removeCallbacks(delayedFocusRunnable)
        releaseWakeLock()
        abandonAudioFocus()
        hideFloatingControl(); stopRecording(); stopAudio()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
