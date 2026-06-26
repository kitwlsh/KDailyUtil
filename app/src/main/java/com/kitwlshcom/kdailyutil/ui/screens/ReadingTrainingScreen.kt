package com.kitwlshcom.kdailyutil.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import com.kitwlshcom.kdailyutil.ui.viewmodel.ComprehensionQuestion
import com.kitwlshcom.kdailyutil.ui.viewmodel.ReadingTrainingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── 연습 지문 (직접 작성한 원문 — 저작권 안전) ─────────────────────────
private val PRACTICE_PASSAGES = listOf(
    "아침 햇살이 창문 틈으로 천천히 스며들었다. 책상 위에 펼쳐진 책은 어제와 같은 자리에 그대로 있었지만, 오늘의 나는 어제와 조금 다른 사람이 되어 있었다. 한 줄을 읽는 동안에도 생각은 여러 갈래로 뻗어 나갔고, 그 생각들이 모여 문장의 의미를 더 깊고 빠르게 붙잡게 만들었다. 읽기란 결국 눈의 속도가 아니라 생각의 속도라는 말을 그제야 어렴풋이 이해할 수 있었다.",
    "처음에는 한 단어 한 단어가 또렷이 보이지 않아 답답했다. 그러나 시선을 한 점에 두고 주변을 넓게 받아들이는 연습을 반복하자, 어느 순간 문장이 덩어리째 눈에 들어오기 시작했다. 빠르게 읽는 것은 대충 읽는 것과 다르다. 오히려 집중이 흐트러질 틈이 없어 내용이 더 선명하게 머릿속에 남았다. 매일 조금씩, 짧게라도 꾸준히 하는 것이 가장 큰 비결이었다.",
    "좋은 독서는 속도와 이해가 함께 자라는 일이다. 너무 빨리 읽어 아무것도 남지 않는다면 그것은 진짜 읽기가 아니다. 반대로 한 글자씩 소리 내어 따라가느라 흐름을 놓친다면 그 또한 아쉬운 일이다. 시선을 부드럽게 미끄러뜨리되 핵심에서는 잠시 머무는 리듬을 익히면, 같은 시간에 더 많은 것을 얻을 수 있다. 오늘의 한 페이지가 내일의 한 권이 된다."
)

private enum class ReadingModule { HUB, WARMUP, PACER, RSVP, CHUNK, EYE, RESULT, COMPREHENSION }

@Composable
fun ReadingTrainingScreen(
    viewModel: ReadingTrainingViewModel = viewModel(),
    onShadow: (String) -> Unit = {}
) {
    var module by remember { mutableStateOf(ReadingModule.HUB) }
    var passage by remember { mutableStateOf(PRACTICE_PASSAGES.random()) }
    var lastWpm by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        when (module) {
            ReadingModule.HUB -> ReadingHub(
                viewModel = viewModel,
                passage = passage,
                onUseRandom = { passage = PRACTICE_PASSAGES.random() },
                onUseCustom = { passage = it },
                onSelect = { module = it },
                onShadow = onShadow
            )
            ReadingModule.WARMUP -> WarmupModule(
                onExit = { module = ReadingModule.HUB },
                onComplete = { viewModel.recordSession(0); module = ReadingModule.HUB }
            )
            ReadingModule.PACER -> PacerModule(
                passage = passage,
                onExit = { module = ReadingModule.HUB },
                onComplete = { wpm -> viewModel.recordSession(wpm); lastWpm = wpm; module = ReadingModule.RESULT }
            )
            ReadingModule.RSVP -> RsvpModule(
                passage = passage,
                onExit = { module = ReadingModule.HUB },
                onComplete = { wpm -> viewModel.recordSession(wpm); lastWpm = wpm; module = ReadingModule.RESULT }
            )
            ReadingModule.CHUNK -> ChunkModule(
                passage = passage,
                onExit = { module = ReadingModule.HUB },
                onComplete = { wpm -> viewModel.recordSession(wpm); lastWpm = wpm; module = ReadingModule.RESULT }
            )
            ReadingModule.EYE -> EyeTrackModule(
                onExit = { module = ReadingModule.HUB },
                onComplete = { viewModel.recordSession(0); module = ReadingModule.HUB }
            )
            ReadingModule.RESULT -> ResultModule(
                wpm = lastWpm,
                onQuiz = { module = ReadingModule.COMPREHENSION },
                onDone = { module = ReadingModule.HUB }
            )
            ReadingModule.COMPREHENSION -> ComprehensionModule(
                viewModel = viewModel,
                passage = passage,
                onDone = { module = ReadingModule.HUB }
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
    passage: String,
    onUseRandom: () -> Unit,
    onUseCustom: (String) -> Unit,
    onSelect: (ReadingModule) -> Unit,
    onShadow: (String) -> Unit = {}
) {
    val bestWpm by viewModel.bestWpm.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val total by viewModel.totalSessions.collectAsState()
    val bestComp by viewModel.bestComprehension.collectAsState()
    val savedPassages by viewModel.savedPassages.collectAsState()
    val wpmHistory by viewModel.wpmHistory.collectAsState()
    val trainedDates by viewModel.trainedDates.collectAsState()

    var customText by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    // 책 페이지 촬영/업로드 → OCR
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isExtracting by viewModel.isExtractingText.collectAsState()
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun handleImage(uri: Uri) {
        scope.launch {
            val bmp = loadAndCompressImageForReading(context, uri)
            if (bmp == null) {
                Toast.makeText(context, "이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            viewModel.extractTextFromImage(bmp) { text, err ->
                if (text != null) {
                    viewModel.savePassageFromImage(bmp, text) // 보관함에 썸네일과 함께 저장
                    onUseCustom(text)
                    Toast.makeText(context, "책 본문을 가져와 보관함에 저장했어요!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, err ?: "추출 실패", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) tempPhotoUri?.let { handleImage(it) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) handleImage(uri)
    }
    val cameraPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            tempPhotoUri = getTempImageUriForReading(context)
            tempPhotoUri?.let { takePicture.launch(it) }
        } else {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

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
                StatItem("최고 속도", if (bestWpm > 0) "$bestWpm" else "-", "WPM")
                StatItem("최고 이해도", if (bestComp > 0) "$bestComp%" else "-", "정답률")
                StatItem("연속", if (streak > 0) "$streak" else "-", "일")
                StatItem("누적", "$total", "회")
            }
        }

        // 기록: WPM 추이 + 21일 챌린지
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(0.85f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(0.15f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📈 기록", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold24K)
                if (wpmHistory.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        val data = wpmHistory
                        val maxV = data.max().toFloat()
                        val minV = data.min().toFloat()
                        val range = (maxV - minV).coerceAtLeast(1f)
                        val stepX = size.width / (data.size - 1)
                        val path = Path()
                        data.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = size.height - ((v - minV) / range) * (size.height - 8f) - 4f
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = Gold24K, style = Stroke(width = 2.dp.toPx()))
                    }
                    Text("WPM 추이 · 최근 ${wpmHistory.size}회 (최고 ${wpmHistory.max()})", fontSize = 11.sp, color = Color.White.copy(0.5f))
                } else {
                    Text("읽기 훈련을 완료하면 WPM 추이가 표시됩니다.", fontSize = 12.sp, color = Color.Gray)
                }

                val last21 = remember(trainedDates) {
                    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    (0 until 21).map {
                        val c = cal.clone() as Calendar
                        c.add(Calendar.DATE, -(20 - it))
                        sdf.format(c.time)
                    }
                }
                val doneCount = last21.count { it in trainedDates }
                Text("🔥 21일 챌린지 · $doneCount/21일", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold24K)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    last21.forEach { d ->
                        val done = d in trainedDates
                        Box(
                            modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(3.dp))
                                .background(if (done) Gold24K else Color.White.copy(0.10f))
                        )
                    }
                }
            }
        }

        // 연습 지문 선택
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(0.85f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(0.15f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("연습 지문", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold24K)
                Text(
                    passage.take(60) + if (passage.length > 60) "…" else "",
                    fontSize = 12.sp, color = Color.White.copy(0.7f), lineHeight = 18.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onUseRandom) { Text("랜덤 지문", color = Gold24K, fontSize = 12.sp) }
                    OutlinedButton(onClick = { showCustomInput = !showCustomInput }) {
                        Text(if (showCustomInput) "닫기" else "내 텍스트 붙여넣기", color = Gold24K, fontSize = 12.sp)
                    }
                }
                // 책 페이지 촬영 / 이미지에서 가져오기 (OCR)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            tempPhotoUri = getTempImageUriForReading(context)
                            tempPhotoUri?.let { takePicture.launch(it) }
                        } else {
                            cameraPerm.launch(android.Manifest.permission.CAMERA)
                        }
                    }) { Text("📷 책 페이지 촬영", color = Gold24K, fontSize = 12.sp) }
                    OutlinedButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("🖼 이미지에서", color = Gold24K, fontSize = 12.sp)
                    }
                }
                Text(
                    "ⓘ 촬영·붙여넣은 지문의 저작권 및 이용 권한 확인은 사용자 책임이며, 개인 학습 용도로만 사용하세요.",
                    fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp
                )
                if (isExtracting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Gold24K, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("책 페이지에서 글자를 읽는 중…", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }
                Text("※ 한 번에 1페이지, 글자가 선명하게 나오도록 촬영하세요.", fontSize = 10.sp, color = Color.Gray)
                if (showCustomInput) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("연습할 텍스트를 붙여넣으세요") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 220.dp)
                    )
                    Button(
                        onClick = {
                            if (customText.isNotBlank()) {
                                val t = customText.trim()
                                viewModel.savePassageText(t) // 보관함에 저장
                                onUseCustom(t)
                                showCustomInput = false
                            }
                        },
                        enabled = customText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
                    ) { Text("이 텍스트로 연습 (보관함 저장)", fontWeight = FontWeight.Bold) }
                }
            }
        }

        // 지문 보관함
        if (savedPassages.isNotEmpty()) {
            Text("📚 내 지문 보관함", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gold24K)
            savedPassages.forEach { p ->
                val selected = p.text.trim() == passage.trim()
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onUseCustom(p.text)
                        android.widget.Toast.makeText(context, "「${p.title}」 선택됨", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Gold24K.copy(0.14f) else DeepCharcoal.copy(0.85f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selected) 1.5.dp else 0.5.dp,
                        Gold24K.copy(if (selected) 0.7f else 0.12f)
                    )
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (p.imagePath != null) {
                            AsyncImage(
                                model = java.io.File(p.imagePath),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Gold24K.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Text("📄", fontSize = 18.sp) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (selected) Text("✓", color = Gold24K, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    p.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (selected) Gold24K else Color.White, maxLines = 1
                                )
                            }
                            Text(
                                if (selected) "사용 중" else (p.text.take(40) + if (p.text.length > 40) "…" else ""),
                                fontSize = 11.sp,
                                color = if (selected) Gold24K.copy(0.8f) else Color.White.copy(0.55f),
                                maxLines = 1
                            )
                        }
                        Text("✕", color = Color.White.copy(0.5f), fontSize = 16.sp,
                            modifier = Modifier.clip(CircleShape).clickable { viewModel.deletePassage(p.id) }.padding(8.dp))
                    }
                }
            }
        }

        // 훈련 모듈
        ModuleCard("🧘 집중 워밍업", "한 점을 응시하며 호흡으로 집중력을 끌어올려요.") { onSelect(ReadingModule.WARMUP) }
        ModuleCard("🎯 리듬 페이서", "하이라이트를 따라 줄 단위로 읽으며 묵독을 줄여요.") { onSelect(ReadingModule.PACER) }
        ModuleCard("⚡ 단어 점멸 (RSVP)", "한 곳에서 단어가 빠르게 바뀌어 안구 이동을 최소화해요.") { onSelect(ReadingModule.RSVP) }
        ModuleCard("🔭 묶어 읽기 (청크)", "여러 단어를 한 묶음으로 보며 시야 폭을 넓혀요.") { onSelect(ReadingModule.CHUNK) }
        ModuleCard("👀 안구 추적", "움직이는 점을 눈으로 따라가며 안구 근육을 풀어줘요.") { onSelect(ReadingModule.EYE) }
        ModuleCard("🗣️ 따라 말하기 (쉐도잉)", "선택한 지문을 한 문장씩 들려주고 따라 말하며 녹음해요.") { onShadow(passage) }

        Spacer(Modifier.height(4.dp))
        Text(
            "ⓘ 본 기능은 일반 속독 훈련 원리에 기반한 독자 구현이며 특정 도서·저자·프로그램과 무관합니다. 효과는 개인차가 있습니다.\n속독 원리에 관심이 있다면 김병완 「1시간에 1권 퀀텀 독서법」을 추천합니다.",
            fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Gold24K)
        Text(unit, fontSize = 9.sp, color = Color.White.copy(0.45f))
        Text(label, fontSize = 10.sp, color = Color.White.copy(0.6f))
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
// ① 집중 워밍업
// ────────────────────────────────────────────────────────────────
@Composable
private fun WarmupModule(onExit: () -> Unit, onComplete: () -> Unit) {
    var remaining by remember { mutableStateOf(60) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        while (running && remaining > 0) { delay(1000); remaining-- }
        if (remaining <= 0) onComplete()
    }

    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.55f, targetValue = 1.0f,
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
            // 호흡 위상(0=날숨, 1=들숨). 시선을 옮기지 않도록 '원 자체의 크기+밝기'로 신호를 주고,
            // 들숨/날숨 라벨은 응시점인 원 중앙에 작게 표시.
            val t = ((scale - 0.55f) / 0.45f).coerceIn(0f, 1f)
            val phaseText = if (t > 0.5f) "들숨" else "날숨"
            Box(
                modifier = Modifier.size(210.dp).scale(scale).clip(CircleShape)
                    .background(Gold24K.copy(alpha = 0.12f + 0.40f * t))
                    .border(2.dp, Gold24K.copy(alpha = 0.30f + 0.50f * t), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$remaining", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(phaseText, color = Color.White.copy(alpha = 0.55f + 0.35f * t), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("원이 커지고 밝아지면 들숨, 작아지고 옅어지면 날숨.\n눈은 옮기지 말고 원 중앙을 가만히 응시하세요.",
                color = Color.White.copy(0.7f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { running = !running }) { Text(if (running) "일시정지" else "계속", color = Gold24K) }
                Button(onClick = onComplete, colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)) {
                    Text("완료", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// ② 리듬 페이서
// ────────────────────────────────────────────────────────────────
@Composable
private fun PacerModule(passage: String, onExit: () -> Unit, onComplete: (Int) -> Unit) {
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

    val listState = rememberLazyListState()
    LaunchedEffect(currentLine) {
        if (currentLine in lines.indices) listState.animateScrollToItem(currentLine.coerceAtLeast(0))
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
                    line.trim(), fontSize = 17.sp, lineHeight = 26.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.Black else Color.White.copy(0.45f),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(if (active) Gold24K.copy(0.85f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        ControlBar(running, wpm, { running = !running }, { wpm = it }, { currentLine = 0; running = true })
    }
}

// ────────────────────────────────────────────────────────────────
// ③ 단어 점멸 (RSVP)
// ────────────────────────────────────────────────────────────────
@Composable
private fun RsvpModule(passage: String, onExit: () -> Unit, onComplete: (Int) -> Unit) {
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
            Text(words.getOrElse(index) { "" }, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        }
        Text("${(index + 1).coerceAtMost(words.size)} / ${words.size}", color = Color.White.copy(0.5f), fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Center)
        ControlBar(running, wpm, { running = !running }, { wpm = it }, { index = 0; running = true })
    }
}

// ────────────────────────────────────────────────────────────────
// ④ 묶어 읽기 (청크)
// ────────────────────────────────────────────────────────────────
@Composable
private fun ChunkModule(passage: String, onExit: () -> Unit, onComplete: (Int) -> Unit) {
    val words = remember(passage) { passage.trim().split(Regex("\\s+")).filter { it.isNotBlank() } }
    var chunkSize by remember { mutableStateOf(3) }
    val chunks = remember(words, chunkSize) { words.chunked(chunkSize) }
    var index by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var wpm by remember { mutableStateOf(300f) }

    // 묶음 크기 변경 시 처음부터
    LaunchedEffect(chunkSize) { index = 0; running = false }

    LaunchedEffect(running, index, wpm, chunkSize) {
        if (running && index <= chunks.lastIndex) {
            val perWord = 60000f / wpm
            val interval = (perWord * chunks[index].size).toLong().coerceAtLeast(180L)
            delay(interval)
            if (index < chunks.lastIndex) index++
            else { running = false; onComplete(wpm.toInt()) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ModuleTopBar("묶어 읽기 (청크)", onExit)
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                chunks.getOrElse(index) { emptyList() }.joinToString(" "),
                fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White,
                textAlign = TextAlign.Center, lineHeight = 40.sp
            )
        }
        Text("${(index + 1).coerceAtMost(chunks.size)} / ${chunks.size}", color = Color.White.copy(0.5f), fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Center)
        // 묶음 크기 선택
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("묶음 크기: ${chunkSize}단어", color = Gold24K, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = chunkSize.toFloat(), onValueChange = { chunkSize = it.toInt() },
                valueRange = 2f..5f, steps = 2,
                colors = SliderDefaults.colors(thumbColor = Gold24K, activeTrackColor = Gold24K)
            )
        }
        ControlBar(running, wpm, { running = !running }, { wpm = it }, { index = 0; running = true })
    }
}

@Composable
private fun ControlBar(running: Boolean, wpm: Float, onToggle: () -> Unit, onWpm: (Float) -> Unit, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.25f)).padding(16.dp).padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("속도: ${wpm.toInt()} WPM (분당 단어수)", color = Gold24K, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Slider(value = wpm, onValueChange = onWpm, valueRange = 150f..700f,
            colors = SliderDefaults.colors(thumbColor = Gold24K, activeTrackColor = Gold24K))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onToggle, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)) {
                Text(if (running) "일시정지" else "시작", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("처음부터", color = Gold24K) }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// 읽기 완료 결과
// ────────────────────────────────────────────────────────────────
@Composable
private fun ResultModule(wpm: Int, onQuiz: () -> Unit, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("훈련 완료! 🎉", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Gold24K)
        Spacer(Modifier.height(12.dp))
        Text("이번 속도", color = Color.White.copy(0.7f), fontSize = 13.sp)
        Text("$wpm WPM", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Text("얼마나 이해했는지 AI 퀴즈로 확인해볼까요?\n(속도만 빠른 건 의미가 없어요!)",
            color = Color.White.copy(0.7f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onQuiz, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)) {
            Text("📝 이해도 퀴즈 풀기", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("건너뛰고 완료", color = Gold24K) }
    }
}

// ────────────────────────────────────────────────────────────────
// AI 이해도 퀴즈
// ────────────────────────────────────────────────────────────────
@Composable
private fun ComprehensionModule(viewModel: ReadingTrainingViewModel, passage: String, onDone: () -> Unit) {
    val isLoading by viewModel.isGeneratingQuiz.collectAsState()
    var questions by remember { mutableStateOf<List<ComprehensionQuestion>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var answers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.generateComprehension(passage) { list, err ->
            questions = list; error = err
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ModuleTopBar("이해도 퀴즈", onDone)
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Gold24K)
                    Spacer(Modifier.height(12.dp))
                    Text("AI가 이해도 문제를 만드는 중…", color = Color.White.copy(0.7f), fontSize = 13.sp)
                }
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = Color.White.copy(0.8f), fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)) {
                        Text("완료", fontWeight = FontWeight.Bold)
                    }
                }
            }
            questions != null -> {
                val qs = questions!!
                val score = qs.indices.count { answers[it] == qs[it].answerIndex }
                val percent = if (qs.isNotEmpty()) score * 100 / qs.size else 0

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (submitted) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Gold24K.copy(0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Gold24K.copy(0.5f))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("이해도 $percent%", color = Gold24K, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("$score / ${qs.size} 정답", color = Color.White.copy(0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                    qs.forEachIndexed { qi, q ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${qi + 1}. ${q.question}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            q.options.forEachIndexed { oi, opt ->
                                val selected = answers[qi] == oi
                                val isAnswer = q.answerIndex == oi
                                val rowColor = when {
                                    submitted && isAnswer -> Color(0xFF2ECC71).copy(0.25f)
                                    submitted && selected && !isAnswer -> Color(0xFFFF4D4D).copy(0.25f)
                                    selected -> Gold24K.copy(0.2f)
                                    else -> Color.Transparent
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(rowColor)
                                        .clickable(enabled = !submitted) { answers = answers + (qi to oi) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selected, onClick = if (submitted) null else { { answers = answers + (qi to oi) } },
                                        colors = RadioButtonDefaults.colors(selectedColor = Gold24K))
                                    Text(opt, color = Color.White.copy(0.9f), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 80.dp)) {
                    if (!submitted) {
                        Button(
                            onClick = { submitted = true; viewModel.recordComprehension(percent) },
                            enabled = answers.size == qs.size,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
                        ) { Text(if (answers.size == qs.size) "채점하기" else "모든 문제에 답해주세요", fontWeight = FontWeight.Bold) }
                    } else {
                        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)) {
                            Text("완료", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// ⑤ 안구 추적 — 움직이는 점을 눈으로 따라가기
// ────────────────────────────────────────────────────────────────
@Composable
private fun EyeTrackModule(onExit: () -> Unit, onComplete: () -> Unit) {
    var remaining by remember { mutableStateOf(45) }
    var running by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }

    LaunchedEffect(running) {
        while (running && remaining > 0) { delay(1000); remaining-- }
        if (remaining <= 0) onComplete()
    }

    val transition = rememberInfiniteTransition(label = "eye")
    val durationMs = (1600f / speed).toInt().coerceIn(400, 4000)
    val fraction by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pos"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ModuleTopBar("안구 추적", onExit)
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.CenterStart) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val dot = 30.dp
                val maxOffset = maxWidth - dot
                Box(
                    modifier = Modifier
                        .offset(x = maxOffset * fraction)
                        .size(dot)
                        .clip(CircleShape)
                        .background(Gold24K)
                        .border(2.dp, Color.White.copy(0.5f), CircleShape)
                )
            }
        }
        Text("$remaining 초 · 점을 눈으로만 부드럽게 따라가세요(고개는 고정).",
            color = Color.White.copy(0.6f), fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
        Column(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.25f)).padding(16.dp).padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("속도", color = Gold24K, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2.5f,
                colors = SliderDefaults.colors(thumbColor = Gold24K, activeTrackColor = Gold24K))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { running = !running }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)) {
                    Text(if (running) "일시정지" else "계속", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onComplete, modifier = Modifier.weight(1f)) { Text("완료", color = Gold24K) }
            }
        }
    }
}

// ── 이미지 로드/압축 & 임시 URI (책 페이지 OCR용) ─────────────────────
private suspend fun loadAndCompressImageForReading(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        // 1) 크기만 먼저 측정
        var input = context.contentResolver.openInputStream(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(input, null, bounds)
        input?.close()

        val maxDim = 2048 // 글자 인식을 위해 충분한 해상도(두 쪽 펼침 대응)
        var sample = 1
        val larger = maxOf(bounds.outWidth, bounds.outHeight)
        if (larger > maxDim) sample = larger / maxDim

        input = context.contentResolver.openInputStream(uri)
        val loaded = BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = if (sample < 1) 1 else sample })
        input?.close()
        loaded ?: return@withContext null

        if (loaded.width > maxDim || loaded.height > maxDim) {
            val ratio = loaded.width.toFloat() / loaded.height.toFloat()
            val w = if (loaded.width > loaded.height) maxDim else (maxDim * ratio).toInt()
            val h = if (loaded.width > loaded.height) (maxDim / ratio).toInt() else maxDim
            val resized = Bitmap.createScaledBitmap(loaded, w.coerceAtLeast(1), h.coerceAtLeast(1), true)
            if (resized != loaded) loaded.recycle()
            return@withContext resized
        }
        return@withContext loaded
    } catch (e: Exception) {
        return@withContext null
    }
}

private fun getTempImageUriForReading(context: Context): Uri? = try {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val tempFile = File.createTempFile("READ_${stamp}_", ".jpg", context.cacheDir)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
} catch (e: Exception) {
    null
}
