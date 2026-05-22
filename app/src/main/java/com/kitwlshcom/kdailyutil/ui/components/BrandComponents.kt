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
            painter = painterResource(id = R.drawable.ic_k_logo_3d),
            contentDescription = null,
            modifier = Modifier
                .size(360.dp)
                .alpha(0.28f), // 28% 투명도로 명품화
            contentScale = ContentScale.Fit
        )
    }
}
