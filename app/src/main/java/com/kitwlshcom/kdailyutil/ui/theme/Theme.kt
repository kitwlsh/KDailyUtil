package com.kitwlshcom.kdailyutil.ui.theme

import android.app.Activity
import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KitwLshColorScheme = darkColorScheme(
    primary = Gold24K,
    onPrimary = DeepCharcoal,
    secondary = TaegeukBlue,
    onSecondary = Gold24K,
    tertiary = TaegeukRed,
    background = DeepCharcoal,
    surface = DarkSurface,
    onBackground = Gold24K,
    onSurface = Gold24K.copy(alpha = 0.8f)
)

@Composable
fun KDailyUtilTheme(
    darkTheme: Boolean = true, // 브랜드 가이드에 따라 다크 모드 강제 또는 권장
    content: @Composable () -> Unit
) {
    val colorScheme = KitwLshColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-Edge(SDK 35+): 상태바/내비바 색상 API는 deprecated·무시됨 → 색을 칠하지 않고
            // 아이콘 대비만 지정(다크 배경이므로 밝은 아이콘). 배경은 전체화면 DeepCharcoal Surface가 채운다.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}