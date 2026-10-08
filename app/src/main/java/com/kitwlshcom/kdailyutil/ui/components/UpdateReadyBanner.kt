package com.kitwlshcom.kdailyutil.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K

/**
 * 「새 버전을 다 받았어요 — 다시 시작할까요?」 (2026-10-08).
 *
 * 🔴 **유연 업데이트의 마지막 한 칸이다.** 내려받기는 뒤에서 끝났는데 앱이 한 번 꺼졌다 켜지지
 * 않으면 **설치가 되지 않는다.** 그 사실을 아는 것은 앱뿐이라, 말해 주지 않으면 사용자는
 * 받아 놓고도 영영 옛 버전을 쓴다.
 *
 * ⚠️ **막지 않는다** — 위에 얹는 카드일 뿐이고 「나중에」로 내릴 수 있다. 내려도 받아 둔 것은
 * 그대로 남아 다음에 앱을 껐다 켤 때 저절로 깔린다. **지금 당장 해야 하는 일이 아니다.**
 */
@Composable
fun UpdateReadyBanner(
    onRestart: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.97f)),
        border = BorderStroke(1.dp, Gold24K.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                "🎉 새 버전을 다 받았어요",
                color = Gold24K, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "앱을 한 번 다시 시작하면 적용돼요. 지금 안 하셔도 괜찮아요 — 다음에 앱을 껐다 켜면 저절로 적용됩니다.",
                color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onLater) {
                    Text("나중에", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                Spacer(Modifier.height(0.dp))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
                ) { Text("지금 다시 시작", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }
    }
}
