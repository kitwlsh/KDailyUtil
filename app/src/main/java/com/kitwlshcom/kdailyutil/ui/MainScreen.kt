package com.kitwlshcom.kdailyutil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.kitwlshcom.kdailyutil.ui.navigation.NavScreen
import com.kitwlshcom.kdailyutil.ui.screens.*
import com.kitwlshcom.kdailyutil.ui.viewmodel.*
import com.kitwlshcom.kdailyutil.ui.components.BrandWatermark
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    audioViewModel: AudioCaptureViewModel = viewModel(),
    startAutoBriefing: Boolean = false,
    onAutoBriefingHandled: () -> Unit = {},
    navigateToStockSubTab: Int? = null,
    onStockNavHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val briefingViewModel: BriefingViewModel = viewModel()
    val shadowingViewModel: ShadowingViewModel = viewModel()
    // 증시 VM을 Activity 스코프로 보유 → 탭 이동 시에도 AI 분석이 취소되지 않고 백그라운드 유지
    val stockViewModel: StockViewModel = viewModel()

    LaunchedEffect(startAutoBriefing) {
        if (startAutoBriefing) {
            navController.navigate(NavScreen.NewsBriefing.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            briefingViewModel.fetchNews(forceRefresh = true)
            delay(300)
            while (briefingViewModel.isRefreshing.value) {
                delay(200)
            }
            delay(500)
            briefingViewModel.startLiveBriefing()
            onAutoBriefingHandled()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // 증시 서브탭으로 이동하는 공통 동작
    fun navigateToStock(subTab: Int) {
        stockViewModel.requestStockSubTab(subTab)
        navController.navigate(NavScreen.StockDashboard.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // 알림 탭으로 진입한 경우 해당 서브탭으로 이동
    LaunchedEffect(navigateToStockSubTab) {
        navigateToStockSubTab?.let {
            navigateToStock(it)
            onStockNavHandled()
        }
    }

    // 인앱 완료 배너 (앱이 떠 있고 다른 메뉴를 보고 있을 때)
    LaunchedEffect(Unit) {
        stockViewModel.analysisCompletedEvent.collect { (msg, subTab) ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "보기",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) navigateToStock(subTab)
        }
    }

    // 최상위 컨테이너에 다크 배경색 강제 적용
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        BrandWatermark()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = DeepCharcoal.copy(alpha = 0.92f),
                    tonalElevation = 8.dp
                ) {
                    NavScreen.items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    screen.icon, 
                                    contentDescription = screen.title,
                                    tint = if (selected) Gold24K else Color.White.copy(alpha = 0.5f)
                                ) 
                            },
                            label = { 
                                Text(
                                    screen.title,
                                    color = if (selected) Gold24K else Color.White.copy(alpha = 0.5f)
                                ) 
                            },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Gold24K.copy(alpha = 0.15f)
                            ),
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
                composable(NavScreen.StockDashboard.route) {
                    StockDashboardScreen(navController = navController, viewModel = stockViewModel)
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
                    NewsDetailScreen(
                        onBack = { navController.popBackStack() }, 
                        navController = navController,
                        viewModel = briefingViewModel,
                        shadowingViewModel = shadowingViewModel
                    ) 
                }
            }
        }
    }
}
