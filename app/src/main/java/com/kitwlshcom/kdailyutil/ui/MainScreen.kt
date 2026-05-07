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
import com.kitwlshcom.kdailyutil.ui.screens.LearningHubScreen
import com.kitwlshcom.kdailyutil.ui.viewmodel.BriefingViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.ShadowingViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioCaptureViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioTab
import com.kitwlshcom.kdailyutil.ui.components.BottomPlayerBar

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    audioViewModel: AudioCaptureViewModel = viewModel(),
    startAutoBriefing: Boolean = false,
    onAutoBriefingHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 뷰모델 생성 (전달받은 것 사용)
    val briefingViewModel: BriefingViewModel = viewModel()
    val shadowingViewModel: ShadowingViewModel = viewModel()

    LaunchedEffect(startAutoBriefing) {
        if (startAutoBriefing) {
            navController.navigate(NavScreen.NewsBriefing.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            
            // 강제로 뉴스를 새로고침하도록 지시
            briefingViewModel.fetchNews()
            
            // fetchNews 내부의 코루틴이 시작되어 isRefreshing이 true가 될 때까지 약간 대기
            delay(300)
            
            // 로딩이 끝날 때까지 대기
            while (briefingViewModel.isRefreshing.value) {
                delay(200)
            }
            
            // 화면 렌더링 안정화를 위해 잠시 대기
            delay(500)
            
            // 로딩된 뉴스를 기반으로 브리핑 시작
            briefingViewModel.startLiveBriefing()
            onAutoBriefingHandled()
        }
    }

    // 재생 상태 구독
    val currentlyPlaying by audioViewModel.currentlyPlaying.collectAsState()
    val isPlaybackPaused by audioViewModel.isPlaybackPaused.collectAsState()
    val playbackMode by audioViewModel.playbackMode.collectAsState()
    val isEditLocked by audioViewModel.isEditLocked.collectAsState()
    val playbackProgress by audioViewModel.playbackProgress.collectAsState()
    val playbackDuration by audioViewModel.playbackDuration.collectAsState()
    val activeTab by audioViewModel.activeTab.collectAsState()

    Scaffold(
        bottomBar = {
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
            composable(NavScreen.AudioCapture.route) { AudioCaptureScreen(audioViewModel) }
            composable(NavScreen.MorningSettings.route) { 
                MorningBriefingSettingsScreen(viewModel = briefingViewModel) 
            }
            composable(NavScreen.LearningHub.route) { LearningHubScreen() }
            composable(NavScreen.NewsDetail.route) { 
                NewsDetailScreen(onBack = { navController.popBackStack() }, viewModel = briefingViewModel) 
            }
        }
    }
}
