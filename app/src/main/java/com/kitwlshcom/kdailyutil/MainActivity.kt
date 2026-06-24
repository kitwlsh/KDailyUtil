package com.kitwlshcom.kdailyutil

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.audio.AudioCaptureService
import com.kitwlshcom.kdailyutil.ui.MainScreen
import com.kitwlshcom.kdailyutil.ui.screens.SplashScreen
import com.kitwlshcom.kdailyutil.ui.theme.KDailyUtilTheme
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioCaptureViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.StockViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    private lateinit var audioViewModel: AudioCaptureViewModel
    private var startAutoBriefing by mutableStateOf(false)
    private var showSplash by mutableStateOf(true)
    // 알림 탭으로 요청된 증시 서브탭 (null = 요청 없음)
    private var navigateToStockSubTab by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        startAutoBriefing = intent.getBooleanExtra("START_AUTO_BRIEFING", false)
        navigateToStockSubTab = if (intent.getStringExtra("NAVIGATE_TO") == "stock")
            intent.getIntExtra("STOCK_SUBTAB", 1) else null
        
        enableEdgeToEdge()
        setContent {
            audioViewModel = viewModel()
            KDailyUtilTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val settingsRepository = remember { com.kitwlshcom.kdailyutil.data.repository.SettingsRepository(context) }
                val splashTheme by settingsRepository.splashThemeFlow.collectAsState(initial = "shimmer")

                // Surface를 사용하여 테마 배경색(DeepCharcoal)을 전체 화면에 적용
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        SplashScreen(theme = splashTheme, onFinished = { showSplash = false })
                    } else {
                        MainScreen(
                            audioViewModel = audioViewModel,
                            startAutoBriefing = startAutoBriefing,
                            onAutoBriefingHandled = { startAutoBriefing = false },
                            navigateToStockSubTab = navigateToStockSubTab,
                            onStockNavHandled = { navigateToStockSubTab = null }
                        )
                    }
                }
            }
        }

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (::audioViewModel.isInitialized) audioViewModel.loadRecordings()
                    startService(Intent(this, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_HIDE_FLOATING
                    })
                    // 포그라운드 진입 → 인앱 배너로 안내 (시스템 알림 대신)
                    runCatching { stockViewModelRef().setAppForeground(true) }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    startService(Intent(this, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_SHOW_FLOATING
                    })
                    // 백그라운드 이탈 → 시스템 알림으로 안내
                    runCatching { stockViewModelRef().setAppForeground(false) }
                }
                else -> {}
            }
        })
    }

    // MainScreen이 viewModel()로 얻는 것과 동일한(Activity 스코프) StockViewModel 인스턴스
    private fun stockViewModelRef(): StockViewModel =
        ViewModelProvider(this)[StockViewModel::class.java]

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startAutoBriefing = intent.getBooleanExtra("START_AUTO_BRIEFING", false)
        navigateToStockSubTab = if (intent.getStringExtra("NAVIGATE_TO") == "stock")
            intent.getIntExtra("STOCK_SUBTAB", 1) else null
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        }
        val needed = permissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }
}