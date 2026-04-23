package com.kitwlshcom.kdailyutil.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object NewsBriefing : NavScreen("news_briefing", "뉴스", Icons.Default.Newspaper)

    object DrivingShadowing : NavScreen("driving_shadowing", "뉴스 쉐도잉", Icons.Default.DriveEta)
    object AudioCapture : NavScreen("audio_capture", "오디오 캡처", Icons.Default.Audiotrack)
    object MorningSettings : NavScreen("morning_settings", "설정", Icons.Default.Settings)
    object Placeholder : NavScreen("placeholder", "더보기", Icons.Default.MoreHoriz)
    object NewsDetail : NavScreen("news_detail", "뉴스 상세", Icons.Default.Newspaper)

    companion object {
        val items = listOf(
            NewsBriefing,
            DrivingShadowing,
            AudioCapture,
            MorningSettings,
            Placeholder
        )
    }
}
