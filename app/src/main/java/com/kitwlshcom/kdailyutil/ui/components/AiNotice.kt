package com.kitwlshcom.kdailyutil.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K

/**
 * 무료 등급 Gemini 키의 데이터 취급 고지.
 *
 * 🔴 **왜 필요한가** — Gemini API 약관 원문:
 * - 무료(Unpaid): *"Google uses the content you submit… to provide, improve, and develop Google products"*,
 *   *"human reviewers may read, annotate, and process your API input and output"*,
 *   *"Do not submit sensitive, confidential, or personal information to the Unpaid Services."*
 * - 유료(Paid): *"Google doesn't use your prompts… to improve our products"*
 *
 * 사용자가 직접 발급하는 키도 **대부분 무료 등급**이라 이 조건이 그대로 적용된다.
 * 즉 체험 키를 넣기 전인 **현재 배포본에도 이미 해당**된다 → 고지가 먼저다.
 *
 * 방침 본문: `doc/privacy-kdailyutil.html` · 정책 근거: `KJangbu/doc/AI_KEY_POLICY.md` §9
 */
@Composable
fun GeminiDataNotice(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(0.5.dp, Gold24K.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "ℹ️ 무료 등급 키의 데이터 취급",
                fontWeight = FontWeight.Bold,
                color = Gold24K,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Google 약관상 무료 등급에서는 전송한 텍스트·이미지가 Google 제품 개선에 사용될 수 있고, " +
                    "품질 확인을 위해 사람이 검토할 수 있습니다. (유료 등급은 해당하지 않습니다)",
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "· 개인정보·기밀이 담긴 사진이나 문서는 AI 기능에 올리지 마세요\n" +
                    "· AI를 쓰지 않는 기능은 전송이 없습니다 — 뉴스 읽기·시세·오디오·퀴즈 풀기·독서 훈련 드릴",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

/**
 * 무료 키 발급 가이드 다이얼로그.
 *
 * 설정 화면의 ⓘ 도움말과 같은 내용을, **설정에 들어가지 않고도** 볼 수 있게 한 것이다
 * (온보딩 배너의 [발급 방법 보기]에서 열린다).
 *
 * ⚠️ **키 형식을 단정하지 않는다.** 새로 발급되는 키는 예전 `AIzaSy…`(39자)가 아니라
 * `AQ.Ab8…`(53자) 형태다. 형식을 적어두면 멀쩡한 키를 잘못된 키로 오해하게 만든다.
 */
@Composable
fun AiKeyGuideDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.ui.graphics.Color(0xFF17171B),
        title = {
            Text("🔑 Gemini API Key 무료 발급 가이드", fontWeight = FontWeight.Bold, color = Gold24K)
        },
        text = {
            Column {
                Text(
                    "AI 기능은 구글의 공식 Gemini AI를 사용합니다. 키를 등록하면 무료 한도 안에서 이용할 수 있습니다.",
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(10.dp))
                Text("💡 3단계 발급 방법", fontWeight = FontWeight.Bold, color = Gold24K, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("1. 아래 [무료 발급 사이트 이동]을 누릅니다.", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.8f))
                Text("2. Google AI Studio에서 [Create API Key]를 누릅니다.", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.8f))
                Text("3. 생성된 키를 복사해 설정 > AI·키 에 붙여넣고 [연결 테스트]를 누릅니다.", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text(
                    "※ 키 모양은 발급 시기에 따라 다릅니다. 복사한 키 전체를 그대로 붙여넣으면 됩니다.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(12.dp))
                GeminiDataNotice()
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://aistudio.google.com/app/apikey")
                        )
                    )
                }
            }) { Text("무료 발급 사이트 이동", color = Gold24K, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("닫기", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

/**
 * 키가 없을 때 첫 화면 최상단에 띄우는 안내 배너.
 *
 * 지금까지는 **설정에 들어가 보거나 AI 기능을 한 번 실패해봐야** 키가 필요하다는 걸 알 수 있었다.
 * 최초 설치 사용자에게는 그게 "기능이 고장났다"로 읽힌다.
 *
 * @param onShowGuide 발급 방법(도움말) 열기
 * @param onOpenSettings 설정 > AI·키 로 이동
 */
@Composable
fun AiKeyOnboardingBanner(
    onShowGuide: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Gold24K.copy(alpha = 0.10f)),
        border = BorderStroke(0.8.dp, Gold24K.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "🤖 AI 기능을 쓰려면 무료 키가 필요합니다",
                fontWeight = FontWeight.Bold,
                color = Gold24K,
                fontSize = 13.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "뉴스 AI 분석·대화, 책 페이지 인식, AI 퀴즈, 증시 AI 리포트에 쓰입니다. " +
                    "Google AI Studio에서 3분이면 무료로 발급받을 수 있습니다.",
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "키가 없어도 뉴스 읽기·시세·오디오 캡처·퀴즈 풀기·독서 훈련은 그대로 쓸 수 있습니다.",
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row {
                androidx.compose.material3.TextButton(onClick = onShowGuide) {
                    Text("발급 방법 보기", color = Gold24K, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.padding(horizontal = 2.dp))
                androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                    Text("설정으로 이동", color = Color.White.copy(alpha = 0.75f), fontSize = 12.5.sp)
                }
            }
        }
    }
}
