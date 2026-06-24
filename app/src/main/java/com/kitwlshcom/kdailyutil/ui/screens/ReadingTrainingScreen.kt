package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import com.kitwlshcom.kdailyutil.ui.viewmodel.ReadingTrainingViewModel
import kotlinx.coroutines.delay

// ── 연습 지문 (직접 작성한 원문 — 저작권 안전) ─────────────────────────
private val PRACTICE_PASSAGES = listOf(
    "아침 햇살이 창문 틈으로 천천히 스며들었다. 책상 위에 펼쳐진 책은 어제와 같은 자리에 그대로 있었지만, 오늘의 나는 어제와 조금 다른 사람이 되어 있었다. 한 줄을 읽는 동안에도 생각은 여러 갈래로 뻗어 나갔고, 그 생각들이 모여 문장의 의미를 더 깊고 빠르게 붙잡게 만들었다. 읽기란 결국 눈의 속도가 아니라 생각의 속도라는 말을 그제야 어렴풋이 이해할 수 있었다.",
    "처음에는 한 단어 한 단어가 또렷이 보이지 않아 답답했다. 그러나 시선을 한 점에 두고 주변을 넓게 받아들이는 연습을 반복하자, 어느 순간 문장이 덩어리째 눈에 들어오기 시작했다. 빠르게 읽는 것은 대충 읽는 것과 다르다. 오히려 집중이 흐트러질 틈이 없어 내용이 더 선명하게 머릿속에 남았다. 매일 조금씩, 짧게라도 꾸준히 하는 것이 가장 큰 비결이었다.",
    "좋은 독서는 속도와 이해가 함께 자라는 일이다. 너무 빨리 읽어 아무것도 남지 않는다면 그것은 진짜 읽기가 아니다. 반대로 한 글자씩 소리 내어 따라가느라 흐름을 놓친다면 그 또한 아쉬운 일이다. 시선을 부드럽게 미끄러뜨리되 핵심에서는 잠시 머무는 리듬을 익히면, 같은 시간에 더 많은 것을 얻을 수 있다. 오늘의 한 페이지가 내일의 한 권이 된다."
)

private enum class ReadingModule { HUB, WARMUP, PACER, RSVP }

@Composable
fun ReadingTrainingScreen(
    viewModel: ReadingTrainingViewModel = viewModel()
) {
    var module by remember { mutableStateOf(ReadingModule.HUB) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        when (module) {
            ReadingModule.HUB -> ReadingHub(
                viewModel = viewModel,
                onSelect = { module = it }
            )
            ReadingModule.WARMUP -> WarmupModule(
                onExit = { module = ReadingModule.HUB },
                onComplete = { viewModel.recordSession(0); module = ReadingModule.HUB }
            )
            ReadingModule.PACER -> PacerModule(
                onExit = { module = ReadingModule.HUB },
                onComplete = { wpm -> viewModel.recordSession(wpm); module = ReadingModule.HUB }
            )
            ReadingModule.RSVP -> RsvpModule(
                onExit = { module = ReadingModule.HUB },
                onComplete = { wpm -> viewModel.recordSession(wpm); module = ReadingModule.HUB }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// 허브
// ────────────────────────────────────────────────────────────────
@Composable
private fun ReadingHub(
    viewModel: ReadingTrainingViewModel,
    onSelect: (ReadingModule) -> Unit
) {
    val bestWpm by viewModel.bestWpm.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val total by viewModel.totalSessions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("⚡ 빠른 독서 훈련", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Gold24K)
        Text(
            "매일 짧게 — 집중하고, 리듬을 타고, 시선을 빠르게. 속도와 이해를 함께 키워요.",
            fontSize = 13.sp, color = Color.White.copy(0.7f)
        )

        // 진척 통계
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(0.85f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(0.25f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("최고 속도", if (bestWpm > 0) "$bestWpm WPM" else "-")
                StatItem("연속", if (streak > 0) "${streak}일" else "-")
                StatItem("누적", "${total}회")
            }
        }

        // 훈련 모듈
        ModuleCard("🧘 집중 워밍업", "한 점을 응시하며 호흡으로 집중력을 끌어올려요.") { onSelect(ReadingModule.WARMUP) }
        ModuleCard("🎯 리듬 페이서", "하이라이트를 따라 줄 단위로 읽으며 묵독을 줄여요.") { onSelect(ReadingModule.PACER) }
        ModuleCard("⚡ 단어 점멸 (RSVP)", "한 곳에서 단어가 빠르게 바뀌어 안구 이동을 최소화해요.") { onSelect(ReadingModule.RSVP) }

        Spacer(Modifier.height(4.dp))
        // 면책 / 추천 크레딧
        Text(
            "ⓘ 본 기능은 일반 속독 훈련 원리에 기반한 독자 구현이며 특정 도서·저자·프로그램과 무관합니다. 효과는 개인차가 있습니다.\n속독 원리에 관심이 있다면 김병완 「1시간에 1권 퀀텀 독서법」을 추천합니다.",
            fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Gold24K)
        Text(label, fontSize = 11.sp, color = Color.White.copy(0.6f))
    }
}

@Composable
private fun ModuleCard(title: String, desc: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(0.85f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(desc, fontSize = 12.sp, color = Color.White.copy(0.6f))
            }
            Text("▶", color = Gold24K)
        }
    }
}

// 모듈 공통 상단바
@Composable
private fun ModuleTopBar(title: String, onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹ 돌아가기", color = Gold24K, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onExit))
        Spacer(Modifier.weight(1f))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(56.dp))
    }
}

// ────────────────────────────────────────────────────────────────
// ① 집중 워밍업 — 호흡 원 + 카운트다운
// ────────────────────────────────────────────────────────────────
@Composable
private fun WarmupModule(onExit: () -> Unit, onComplete: () -> Unit) {
    var remaining by remember { mutableStateOf(60) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1000)
            remaining--
        }
        if (remaining <= 0) onComplete()
    }

    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ModuleTopBar("집중 워밍업", onExit)
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (scale > 0.78f) "들숨 …" else "날숨 …", color = Gold24K, fontSize = 16.sp)
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Gold24K.copy(0.25f))
                    .border(2.dp, Gold24K.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$remaining", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Text("원의 리듬에 맞춰 천천히 호흡하며\n화면 중앙을 가만히 응시하세요.",
                color = Color.White.copy(0.7f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { running = !running }) {
                    Text(if (running) "일시정지" else "계속", color = Gold24K)
                }
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
                ) { Text("완료", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// ② 리듬 페이서 — 줄 단위 하이라이트 자동 이동
// ────────────────────────────────────────────────────────────────
@Composable
private fun PacerModule(onExit: () -> Unit, onComplete: (Int) -> Unit) {
    val passage = remember { PRACTICE_PASSAGES.random() }
    // 문장 단위로 줄 분할
    val lines = remember(passage) {
        passage.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
    }
    var currentLine by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var wpm by remember { mutableStateOf(300f) }

    LaunchedEffect(running, currentLine, wpm) {
        if (running && currentLine <= lines.lastIndex) {
            val words = lines[currentLine].trim().split(Regex("\\s+")).size.coerceAtLeast(1)
            val interval = (words / wpm * 60000f).toLong().coerceAtLeast(180L)
            delay(interval)
            if (currentLine < lines.lastIndex) currentLine++
            else { running = false; onComplete(wpm.toInt()) }
        }
    }

    // 현재 줄이 바뀔 때마다 그 줄이 화면에 보이도록 자동 스크롤
    val listState = rememberLazyListState()
    LaunchedEffect(currentLine) {
        if (currentLine in lines.indices) {
            listState.animateScrollToItem(currentLine.coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ModuleTopBar("리듬 페이서", onExit)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            itemsIndexed(lines) { i, line ->
                val active = i == currentLine
                Text(
                    line.trim(),
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.Black else Color.White.copy(0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Gold24K.copy(0.85f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        ControlBar(
            running = running,
            wpm = wpm,
            onToggle = { running = !running },
            onWpm = { wpm = it },
            onRestart = { currentLine = 0; running = true }
        )
    }
}

// ────────────────────────────────────────────────────────────────
// ③ 단어 점멸 (RSVP)
// ────────────────────────────────────────────────────────────────
@Composable
private fun RsvpModule(onExit: () -> Unit, onComplete: (Int) -> Unit) {
    val passage = remember { PRACTICE_PASSAGES.random() }
    val words = remember(passage) { passage.trim().split(Regex("\\s+")).filter { it.isNotBlank() } }
    var index by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var wpm by remember { mutableStateOf(300f) }

    LaunchedEffect(running, index, wpm) {
        if (running && index <= words.lastIndex) {
            val interval = (60000f / wpm).toLong().coerceAtLeast(120L)
            delay(interval)
            if (index < words.lastIndex) index++
            else { running = false; onComplete(wpm.toInt()) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ModuleTopBar("단어 점멸 (RSVP)", onExit)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                words.getOrElse(index) { "" },
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        Text(
            "${index + 1} / ${words.size}",
            color = Color.White.copy(0.5f),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            textAlign = TextAlign.Center
        )
        ControlBar(
            running = running,
            wpm = wpm,
            onToggle = { running = !running },
            onWpm = { wpm = it },
            onRestart = { index = 0; running = true }
        )
    }
}

// 페이서/RSVP 공통 컨트롤 바
@Composable
private fun ControlBar(
    running: Boolean,
    wpm: Float,
    onToggle: () -> Unit,
    onWpm: (Float) -> Unit,
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.25f))
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("속도: ${wpm.toInt()} WPM (분당 단어수)", color = Gold24K, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = wpm,
            onValueChange = onWpm,
            valueRange = 150f..700f,
            colors = SliderDefaults.colors(thumbColor = Gold24K, activeTrackColor = Gold24K)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onToggle,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
            ) { Text(if (running) "일시정지" else "시작", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) {
                Text("처음부터", color = Gold24K)
            }
        }
    }
}
