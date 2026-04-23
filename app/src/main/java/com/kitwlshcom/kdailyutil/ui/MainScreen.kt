package com.kitwlshcom.kdailyutil.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 뉴스 브리핑 관련 ViewModel을 공유하기 위해 상위에서 생성
    val briefingViewModel: BriefingViewModel = viewModel()

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
                NewsBriefingScreen(navController = navController, viewModel = briefingViewModel) 
            }
            composable(NavScreen.DrivingShadowing.route) { DrivingShadowingScreen() }
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
