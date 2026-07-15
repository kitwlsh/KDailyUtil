package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitwlshcom.kdailyutil.R
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

class HexagonShape(private val insetPercent: Float = 0.005f) : Shape { 
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = (minOf(size.width, size.height) / 2f) * (1f - insetPercent)
            
            for (i in 0..5) {
                val angle = Math.toRadians(i * 60.0 - 90.0)
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

data class ShimmerState(
    val translationX: Float,
    val rotationZ: Float,
    val alpha: Float,
    val color: Color
)

@Composable
fun SplashScreen(
    theme: String = "shimmer",
    onFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.7f,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f, 
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    val shimmerParams = remember(shimmerProgress) {
        val p = shimmerProgress
        when {
            // Phase 1 (0.0 ~ 0.4): '>' 방향 사선 반사광 (45도), 밝고 선명하게 (White, alpha=0.22)
            p in 0.0f..0.4f -> {
                val phaseProgress = p / 0.4f
                val transX = -1000f + (phaseProgress * 2000f)
                ShimmerState(transX, 45f, 0.22f, Color.White)
            }
            // Phase 2 (0.5 ~ 0.9): '<' 방향 사선 반사광 (-45도), 어둡고 은은하게 (Gold24K, alpha=0.10)
            p in 0.5f..0.9f -> {
                val phaseProgress = (p - 0.5f) / 0.4f
                val transX = -1000f + (phaseProgress * 2000f)
                ShimmerState(transX, -45f, 0.10f, Gold24K)
            }
            // 쉬는 시간
            else -> {
                ShimmerState(-1000f, 45f, 0f, Color.White)
            }
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2C2C2E), DeepCharcoal)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alphaAnim.value)
        ) {
            // 로고 상단에 여백을 추가하여 잘림 방지
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(scaleAnim.value * pulseScale)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                        // 아이콘(엠블럼+톱니바퀴) 자체를 표시. shimmer는 아이콘 불투명 영역에만 얹어
                        // 아이콘 안에서만 빛이 흐르도록 SrcAtop 마스킹 (KLotto645와 동일 컨셉)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            if (theme == "shimmer") {
                                rotate(degrees = shimmerParams.rotationZ) {
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                shimmerParams.color.copy(alpha = shimmerParams.alpha),
                                                Color.Transparent
                                            ),
                                            start = Offset(center.x + shimmerParams.translationX - size.width / 2f, center.y),
                                            end = Offset(center.x + shimmerParams.translationX + size.width / 2f, center.y)
                                        ),
                                        blendMode = BlendMode.SrcAtop
                                    )
                                }
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_k_app_icon),
                        contentDescription = "KDailyUtil Icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                if (theme != "shimmer") {
                    // 신규 유성 스파이럴 궤도 및 반짝임 효과 (Meteor Orbit Canvas Overlay)
                    val p = shimmerProgress
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                    ) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val baseRadius = minOf(size.width, size.height) * 0.42f
                        val perspectiveY = 0.38f

                        // 1. 은은한 골드 백그라운드 아우라 (Twinkling ambient glow)
                        val auraAlpha = 0.08f + 0.05f * sin(p * 2f * Math.PI.toFloat())
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Gold24K.copy(alpha = auraAlpha), Color.Transparent),
                                center = Offset(centerX, centerY),
                                radius = baseRadius * 1.1f
                            ),
                            radius = baseRadius * 1.1f,
                            center = Offset(centerX, centerY)
                        )

                        // 2. 유성 꼬리 및 유성 머리 그리기 (Meteor Tail & Head)
                        // 꼬리 개수: 35개 골드 파티클 점진적 페이드
                        val tailPointsCount = 35
                        for (i in 0..tailPointsCount) {
                            val tailFraction = i.toFloat() / tailPointsCount // 0(꼬리 끝) ~ 1(유성 머리)
                            // 시간 오프셋을 두어 과거 위치 계산
                            val pTail = p - (1f - tailFraction) * 0.07f
                            if (pTail < 0f) continue

                            val tailAngle = pTail * 4.5f * 2f * Math.PI.toFloat()
                            
                            // 솟구치는 오프셋: 아래에서 위로 (1.0에서 -1.0로 점진 이동)
                            val yOffset = (1f - 2f * pTail) * baseRadius * 0.65f
                            // 나선 반지름: 바깥에서 중심으로 수렴
                            val tailRadius = baseRadius * (1.15f - pTail * 0.5f)

                            val tx = centerX + cos(tailAngle) * tailRadius
                            val ty = centerY + sin(tailAngle) * tailRadius * perspectiveY + yOffset

                            // 3D 입체감 (뒤로 갈 때는 어둡고 작게, 앞으로 올 때는 크고 밝게)
                            val tZFactor = 0.62f + 0.38f * sin(tailAngle)
                            val tailOpacity = tailFraction * tailFraction * 0.8f * tZFactor
                            val tailSize = (8f * (0.2f + 0.8f * tailFraction) * tZFactor).dp.toPx()

                            if (tailOpacity > 0f) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Gold24K.copy(alpha = tailOpacity),
                                            Gold24K.copy(alpha = tailOpacity * 0.3f),
                                            Color.Transparent
                                        ),
                                        center = Offset(tx, ty),
                                        radius = tailSize
                                    ),
                                    radius = tailSize,
                                    center = Offset(tx, ty)
                                )
                            }
                        }

                        // 3. 유성 머리 코어 플레어 (Meteor Head core flare)
                        val headAngle = p * 4.5f * 2f * Math.PI.toFloat()
                        val headRadius = baseRadius * (1.15f - p * 0.5f)
                        val headYOffset = (1f - 2f * p) * baseRadius * 0.65f
                        val hx = centerX + cos(headAngle) * headRadius
                        val hy = centerY + sin(headAngle) * headRadius * perspectiveY + headYOffset
                        val hZFactor = 0.62f + 0.38f * sin(headAngle)

                        val headSize = (14f * hZFactor).dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f * hZFactor),
                                    Gold24K.copy(alpha = 0.7f * hZFactor),
                                    Color.Transparent
                                ),
                                center = Offset(hx, hy),
                                radius = headSize
                            ),
                            radius = headSize,
                            center = Offset(hx, hy)
                        )

                        // 4. 클라이맥스 시점 (0.72 ~ 0.92) 사방 골드 별빛 플레어 폭발 (Central Sparkle Burst)
                        if (p in 0.72f..0.92f) {
                            val burstP = (p - 0.72f) / 0.20f // 0f ~ 1f
                            val flareRadius = baseRadius * 0.9f * sin(burstP * Math.PI.toFloat()).toFloat()
                            val flareAlpha = (1f - burstP) * 0.55f

                            // 중앙 플레어 폭발 광원
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = flareAlpha),
                                        Gold24K.copy(alpha = flareAlpha * 0.7f),
                                        Color.Transparent
                                    ),
                                    center = Offset(centerX, centerY),
                                    radius = flareRadius
                                ),
                                radius = flareRadius,
                                center = Offset(centerX, centerY)
                            )

                            // 사방으로 뻗어 나가는 별빛 입자 (Star sparkles) - 8방향
                            val starRayCount = 8
                            val maxRayDistance = baseRadius * 0.8f
                            val rayAlpha = (1f - burstP) * 0.9f
                            val baseRaySize = (10f * (1f - burstP)).dp.toPx()

                            for (j in 0 until starRayCount) {
                                val angleRad = (j * (360f / starRayCount)) * Math.PI.toFloat() / 180f
                                val distance = maxRayDistance * burstP
                                val rx = centerX + cos(angleRad) * distance
                                val ry = centerY + sin(angleRad) * distance * perspectiveY

                                // 4-Point Star (다이아몬드 교차) 드로잉
                                val path = Path().apply {
                                    moveTo(rx, ry - baseRaySize)
                                    lineTo(rx + baseRaySize / 3f, ry)
                                    lineTo(rx, ry + baseRaySize)
                                    lineTo(rx - baseRaySize / 3f, ry)
                                    close()
                                    moveTo(rx - baseRaySize, ry)
                                    lineTo(rx, ry - baseRaySize / 3f)
                                    lineTo(rx + baseRaySize, ry)
                                    lineTo(rx, ry + baseRaySize / 3f)
                                    close()
                                }
                                drawPath(path, color = Gold24K.copy(alpha = rayAlpha))
                            }
                        }

                        // 5. 배경 미세 twinkling 아우라 스타즈 (Twinkling ambient background stars)
                        val twinklingStars = listOf(
                            Offset(centerX - baseRadius * 0.6f, centerY - baseRadius * 0.5f) to 0.0f,
                            Offset(centerX + baseRadius * 0.7f, centerY - baseRadius * 0.3f) to 1.5f,
                            Offset(centerX - baseRadius * 0.5f, centerY + baseRadius * 0.4f) to 3.0f,
                            Offset(centerX + baseRadius * 0.6f, centerY + baseRadius * 0.6f) to 4.5f,
                            Offset(centerX, centerY - baseRadius * 0.9f) to 2.2f
                        )

                        twinklingStars.forEach { (pos, offsetPhase) ->
                            val twinkleAlpha = 0.1f + 0.3f * (0.5f + 0.5f * sin(p * 2f * Math.PI.toFloat() * 2f + offsetPhase))
                            val starSize = (4f * twinkleAlpha).dp.toPx()
                            
                            // 십자 별빛 드로잉
                            val path = Path().apply {
                                moveTo(pos.x, pos.y - starSize)
                                lineTo(pos.x + 0.5f, pos.y)
                                lineTo(pos.x, pos.y + starSize)
                                lineTo(pos.x - 0.5f, pos.y)
                                close()
                                moveTo(pos.x - starSize, pos.y)
                                lineTo(pos.x, pos.y - 0.5f)
                                lineTo(pos.x + starSize, pos.y)
                                lineTo(pos.x, pos.y + 0.5f)
                                close()
                            }
                            drawPath(path, color = Gold24K.copy(alpha = twinkleAlpha))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(42.dp))
            
            Text(
                text = "K-DailyUtil",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Gold24K,
                letterSpacing = 4.sp
            )
            
            Text(
                text = "Premium Utility Series",
                fontSize = 15.sp,
                color = Gold24K.copy(alpha = 0.6f),
                letterSpacing = 6.sp
            )
        }
        
        Text(
            text = "by KitwLSH",
            fontSize = 14.sp,
            color = Gold24K.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            letterSpacing = 3.sp
        )
    }
}
