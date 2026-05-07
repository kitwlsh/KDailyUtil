package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // 2.5초 대기
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E), // Deep Indigo
                        Color(0xFF0D47A1)  // Royal Blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value).scale(scaleAnim.value)
        ) {
            // 메탈릭 느낌의 K 이니셜 (폰트나 캔버스로 대체 가능)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE0E0E0), // Silver Light
                                Color(0xFF9E9E9E), // Silver Dark
                                Color(0xFFBDBDBD)  // Silver Medium
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A237E),
                    style = MaterialTheme.typography.displayLarge
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "K-DailyUtil",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Premium Utility Series",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 4.sp
            )
        }
    }
}
