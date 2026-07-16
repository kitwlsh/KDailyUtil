package com.kitwlshcom.kdailyutil.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kitwlshcom.kdailyutil.R

@Composable
fun BrandWatermark(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            // 배경 워터마크는 '해당 앱의 고유 아이콘'을 사용(엠블럼+톱니/나침반).
            // KLotto645가 자기 풀아이콘(ic_k_emblem_balls)을 워터마크로 쓰는 것과 동일한 패밀리 규칙.
            // 크기 = 스플래시와 동일하게 화면폭 × 0.80 (정사각). 고정 dp가 아니라 상대 비율.
            painter = painterResource(id = R.drawable.ic_k_app_icon),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .alpha(0.28f), // 28% 투명도로 명품화
            contentScale = ContentScale.Fit
        )
    }
}
