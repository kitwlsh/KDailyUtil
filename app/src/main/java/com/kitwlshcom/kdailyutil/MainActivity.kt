package com.kitwlshcom.kdailyutil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kitwlshcom.kdailyutil.ui.MainScreen
import com.kitwlshcom.kdailyutil.ui.theme.KDailyUtilTheme

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kitwlshcom.kdailyutil.audio.AudioCaptureService
import android.content.Intent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioCaptureViewModel

class MainActivity : ComponentActivity() {
    private lateinit var audioViewModel: AudioCaptureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        enableEdgeToEdge()
        setContent {
            audioViewModel = viewModel()
            KDailyUtilTheme {
                MainScreen(audioViewModel)
            }
        }

        // 전역 라이프사이클 관찰자 추가 (플로팅 아이콘 제어)
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (::audioViewModel.isInitialized) audioViewModel.loadRecordings()
                    startService(Intent(this, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_HIDE_FLOATING
                    })
                }
                Lifecycle.Event.ON_PAUSE -> {
                    startService(Intent(this, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_SHOW_FLOATING
                    })
                }
                else -> {}
            }
        })
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
        )
        
        // Android 10 (Q) 이하에서만 공용 저장소 쓰기 권한이 필수적일 수 있음
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        // Android 13 이상 미디어 권한
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