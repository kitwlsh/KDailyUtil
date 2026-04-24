package com.kitwlshcom.kdailyutil.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kitwlshcom.kdailyutil.ui.navigation.NavScreen
import com.kitwlshcom.kdailyutil.ui.screens.AudioCaptureScreen
import com.kitwlshcom.kdailyutil.ui.screens.DrivingShadowingScreen
import com.kitwlshcom.kdailyutil.ui.screens.MorningBriefingSettingsScreen
import com.kitwlshcom.kdailyutil.ui.screens.NewsBriefingScreen
import com.kitwlshcom.kdailyutil.ui.screens.NewsDetailScreen
import com.kitwlshcom.kdailyutil.ui.screens.PlaceholderScreen
import com.kitwlshcom.kdailyutil.ui.viewmodel.BriefingViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.ShadowingViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioCaptureViewModel
import com.kitwlshcom.kdailyutil.ui.components.BottomPlayerBar

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 뷰모델 생성
    val briefingViewModel: BriefingViewModel = viewModel()
    val shadowingViewModel: ShadowingViewModel = viewModel()
    val audioViewModel: AudioCaptureViewModel = viewModel()

    // 재생 상태 구독
    val currentlyPlaying by audioViewModel.currentlyPlaying.collectAsState()
    val isPlaybackPaused by audioViewModel.isPlaybackPaused.collectAsState()
    val playbackMode by audioViewModel.playbackMode.collectAsState()
    val isEditLocked by audioViewModel.isEditLocked.collectAsState()
    val playbackProgress by audioViewModel.playbackProgress.collectAsState()
    val playbackDuration by audioViewModel.playbackDuration.collectAsState()

    Scaffold(
        bottomBar = {
            Column {
                // 오디오 캡처 화면에서만 재생 컨트롤 바 표시
                if (currentlyPlaying != null && currentDestination?.route == NavScreen.AudioCapture.route) {
                    BottomPlayerBar(
                        item = currentlyPlaying!!,
                        isPaused = isPlaybackPaused,
                        playbackMode = playbackMode,
                        isEditLocked = isEditLocked,
                        progress = playbackProgress,
                        totalDuration = playbackDuration,
                        onTogglePlay = { audioViewModel.playAudio(currentlyPlaying!!) },
                        onToggleMode = { audioViewModel.togglePlaybackMode() },
                        onToggleLock = { audioViewModel.toggleEditLock() },
                        onSeek = { audioViewModel.seekTo(it.toLong()) },
                        onPrev = { audioViewModel.playPreviousRecording() },
                        onNext = { audioViewModel.playNextRecording() }
                    )
                }
                
                // 하단 내비게이션 바
                NavigationBar {
                    NavScreen.items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavScreen.NewsBriefing.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavScreen.NewsBriefing.route) { 
                NewsBriefingScreen(
                    navController = navController, 
                    viewModel = briefingViewModel,
                    shadowingViewModel = shadowingViewModel
                ) 
            }
            composable(NavScreen.DrivingShadowing.route) { 
                DrivingShadowingScreen(viewModel = shadowingViewModel) 
            }
            composable(NavScreen.AudioCapture.route) { AudioCaptureScreen() }
            composable(NavScreen.MorningSettings.route) { 
                MorningBriefingSettingsScreen(viewModel = briefingViewModel) 
            }
            composable(NavScreen.Placeholder.route) { PlaceholderScreen() }
            composable(NavScreen.NewsDetail.route) { 
                NewsDetailScreen(onBack = { navController.popBackStack() }, viewModel = briefingViewModel) 
            }
        }
    }
}
