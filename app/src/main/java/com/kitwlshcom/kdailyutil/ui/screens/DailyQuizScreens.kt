package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitwlshcom.kdailyutil.data.DailyRecord
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import com.kitwlshcom.kdailyutil.ui.viewmodel.QuizViewModel
import java.time.LocalDate

/**
 * 「매일 한 번은 들어와서 보게 하는」 화면 조각들. (2026-09-04 신설)
 *
 * 설계 배경은 `doc/RETENTION_PLAN.md`. 요약하면:
 *  - 기존 퀴즈는 랜덤 10문제라 **끝이 없었다.** 끝이 없으면 「언제 해도 되니까」 안 하게 되고,
 *    내일 다시 올 이유도 생기지 않는다 → 하루치(5문제)로 잘랐다.
 *  - 보상(포인트·현금)은 주지 않는다. 대신 **자기 기록**(연속·누적·배지)을 준다.
 *  - 자동 생성 로봇이 매일 문제를 넣고 있는데 앱이 그 사실을 안 알려서, 사용자에게는
 *    어제와 오늘의 앱이 똑같았다 → 「새 문제 N개」를 눈에 보이게 했다.
 */

// ──────────────────────────────────────────────────────────────────
// 오늘의 퀴즈 홈 카드
// ──────────────────────────────────────────────────────────────────

@Composable
fun DailyQuizHomeCard(viewModel: QuizViewModel) {
    val status by viewModel.dailyStatus.collectAsState()
    val newNotice by viewModel.newQuizNotice.collectAsState()
    val wrongCount by viewModel.wrongToReviewCount.collectAsState()

    // 화면이 떠 있는 동안 날짜가 바뀔 수 있다(자정을 넘겨 쓰는 사람이 있다).
    // remember로 굳혀 두면 자정 이후에도 「오늘 완료」가 그대로 남아 거짓말이 된다.
    val today = LocalDate.now()
    val doneToday = status.isDoneToday(today)
    val streak = status.displayStreak(today)

    var showRecord by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── 오늘의 퀴즈 ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (doneToday) Color(0xFF1B3A2A).copy(alpha = 0.55f)
                    else Gold24K.copy(alpha = 0.12f)
                )
                .border(
                    width = 1.dp,
                    color = if (doneToday) Color(0xFF4CAF50).copy(alpha = 0.5f) else Gold24K.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (doneToday) Icons.Default.CheckCircle else Icons.Default.Today,
                    contentDescription = null,
                    tint = if (doneToday) Color(0xFF7BD98F) else Gold24K,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (doneToday) "오늘 완료!" else "오늘의 퀴즈",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (doneToday) Color(0xFF7BD98F) else Gold24K
                )
                Spacer(Modifier.weight(1f))
                if (streak > 0) StreakChip(streak)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (doneToday) {
                    val todayScore = status.history.lastOrNull { it.date == today }
                    if (todayScore != null) {
                        "오늘 성적 ${todayScore.correct}/${todayScore.total} · 내일 새 문제가 기다립니다"
                    } else {
                        "내일 새 문제가 기다립니다"
                    }
                } else {
                    "하루 ${DailyRecord.DAILY_QUIZ_COUNT}문제. 오늘 것만 풀면 끝입니다."
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 20.sp
            )

            // 🔴 오래 비운 사용자에게는 숫자를 말하지 않는다(복귀 사면).
            // 「놓친 300개」는 초대가 아니라 청구서이고, 실제로 해야 하는 일은 어느 쪽이든 오늘 한 판이다.
            if (newNotice.amnesty || newNotice.hasNumber) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (newNotice.amnesty) {
                        "🌱 그동안 새 문제가 쌓였어요 — 오늘 한 판부터 다시 시작해요"
                    } else {
                        "🆕 새 문제 ${newNotice.text}가 들어왔어요"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold24K.copy(alpha = 0.9f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.markNewQuizzesSeen()
                    viewModel.startDailyQuiz()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (doneToday) Color.White.copy(alpha = 0.14f) else Gold24K,
                    contentColor = if (doneToday) Color.White else Color.Black
                )
            ) {
                Text(
                    text = if (doneToday) "오늘 문제 다시 보기" else "오늘의 ${DailyRecord.DAILY_QUIZ_COUNT}문제 풀기",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 오답 노트 · 내 기록 ────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Replay,
                title = "오답 노트",
                subtitle = if (wrongCount > 0) "복습할 ${wrongCount}개" else "아직 없음",
                enabled = wrongCount > 0,
                onClick = { viewModel.startReviewQuiz() }
            )
            Spacer(Modifier.width(12.dp))
            SmallActionTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MilitaryTech,
                title = "내 기록",
                subtitle = if (status.bestStreak > 0) "최고 ${status.bestStreak}일" else "기록 보기",
                enabled = true,
                onClick = { showRecord = true }
            )
        }
    }

    if (showRecord) {
        DailyRecordDialog(status = status, today = today, onDismiss = { showRecord = false })
    }
}

@Composable
private fun StreakChip(streak: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFF6D00).copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "${streak}일 연속",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFB74D)
        )
    }
}

@Composable
private fun SmallActionTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Gold24K.copy(alpha = alpha),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = alpha))
        Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f * alpha))
    }
}

// ──────────────────────────────────────────────────────────────────
// 내 기록 (연속 · 누적 · 최근 30일 · 배지)
// ──────────────────────────────────────────────────────────────────

@Composable
fun DailyRecordDialog(
    status: com.kitwlshcom.kdailyutil.data.repository.SettingsRepository.DailyStatus,
    today: LocalDate,
    onDismiss: () -> Unit
) {
    val accuracy = if (status.totalSolved > 0) status.totalCorrect * 100 / status.totalSolved else 0
    val badges = DailyRecord.badges(
        streak = status.streak,
        bestStreak = status.bestStreak,
        totalSolved = status.totalSolved,
        totalCorrect = status.totalCorrect
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기", color = Gold24K, fontWeight = FontWeight.Bold) }
        },
        title = {
            Text("내 기록", fontWeight = FontWeight.ExtraBold, color = Gold24K, fontSize = 22.sp)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell(Modifier.weight(1f), "현재 연속", "${status.displayStreak(today)}일")
                    StatCell(Modifier.weight(1f), "최고 기록", "${status.bestStreak}일")
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell(Modifier.weight(1f), "누적 문항", "${status.totalSolved}개")
                    StatCell(Modifier.weight(1f), "정답률", if (status.totalSolved > 0) "$accuracy%" else "-")
                }

                if (status.history.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("최근 기록", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(8.dp))
                    HistoryBars(status.history)
                }

                Spacer(Modifier.height(20.dp))
                Text("배지", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text(
                    "포인트나 현금 보상은 없습니다. 대신 기록이 남습니다.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )
                Spacer(Modifier.height(10.dp))
                badges.forEach { badge ->
                    BadgeRow(badge)
                    Spacer(Modifier.height(10.dp))
                }
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}

@Composable
private fun StatCell(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Gold24K)
    }
}

/**
 * 최근 기록 막대. 라이브러리 없이 Box 높이로만 그린다.
 * (차트 라이브러리를 하나 더 넣을 만큼의 화면이 아니다)
 */
@Composable
private fun HistoryBars(history: List<DailyRecord.DayScore>) {
    val recent = history.takeLast(14)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        recent.forEach { day ->
            val ratio = if (day.total > 0) day.correct.toFloat() / day.total else 0f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        // 0점인 날도 «왔다»는 것은 보여야 하므로 최소 높이를 준다
                        .height((6 + (40 * ratio)).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Gold24K.copy(alpha = 0.35f + 0.55f * ratio))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${day.date.dayOfMonth}",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
private fun BadgeRow(badge: DailyRecord.Badge) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (badge.achieved) "🏅" else "🔒",
            fontSize = 20.sp,
            modifier = Modifier.width(32.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = badge.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (badge.achieved) Gold24K else Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = badge.description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
            if (!badge.achieved) {
                Spacer(Modifier.height(5.dp))
                // 못 받은 배지도 «얼마나 왔는지»를 보여 준다 — 잠긴 자물쇠만 보이면 포기한다
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(badge.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Gold24K.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
// 결과 화면에 얹는 축하 배너
// ──────────────────────────────────────────────────────────────────

@Composable
fun DailyCelebrationBanner(celebration: QuizViewModel.DailyCelebration) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFF6D00).copy(alpha = 0.14f))
            .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "오늘의 퀴즈 완료 · ${celebration.streak}일 연속",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFB74D)
            )
        }

        if (celebration.newBadges.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            celebration.newBadges.forEach { badge ->
                Text(
                    text = "🏅 새 배지 — ${badge.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold24K,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "내일 이 시간에 새 문제가 준비됩니다.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
