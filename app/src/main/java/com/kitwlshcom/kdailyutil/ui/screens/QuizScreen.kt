package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.data.model.QuizType
import com.kitwlshcom.kdailyutil.ui.viewmodel.QuizState
import com.kitwlshcom.kdailyutil.ui.viewmodel.QuizViewModel

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel = viewModel()
) {
    val quizState by viewModel.quizState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when (quizState) {
            QuizState.IDLE -> QuizIdleScreen(onStart = { viewModel.selectCategory(null) })
            QuizState.CATEGORY_SELECTION -> QuizCategorySelectionScreen(viewModel)
            QuizState.GENERATING -> QuizGeneratingScreen()
            QuizState.PLAYING, QuizState.ANSWER_CHECKED -> QuizPlayScreen(viewModel)
            QuizState.FINISHED -> QuizFinishedScreen(viewModel)
            QuizState.CREATOR -> 
            {
                QuizCreatorScreen(
                    viewModel = viewModel,
                    onBack = 
                    {
                        viewModel.selectCategory(null)
                    }
                )
            }
        }
    }
}

@Composable
fun QuizIdleScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "K-Quiz Hub",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "지식의 가치를 높이는 시간",
            fontSize = 16.sp,
            color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "우리말 겨루기부터 AI가 직접 출제하는\n커스텀 퀴즈까지 모두 즐겨보세요.",
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.6f),
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                contentColor = Color.Black
            )
        ) {
            Text("시작하기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuizCategorySelectionScreen(viewModel: QuizViewModel)
{
    val categories by viewModel.availableCategories.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    var showAiTopicDialog by remember { mutableStateOf(false) }
    var aiTopic by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null)
        {
            viewModel.importQuizFromUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(
            text = "퀴즈 분야 선택",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Premium Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.5.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "나만의 AI 퀴즈 제작소",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "도서 사진 스캔, 인터넷 링크 주소 및 본문 붙여넣기, 수동 입력 및 AI 보기 완성을 통해 나만의 멋진 퀴즈를 3초 만에 만들어보세요!",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = 
                    {
                        viewModel.enterCreator()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✨ KuizGenius 크리에이터 진입",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // SAF .kquiz Importer Button
        OutlinedButton(
            onClick = 
            {
                filePickerLauncher.launch("*/*")
            },
            border = BorderStroke(1.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "📥 외부 퀴즈 패키지 (.kquiz) 가져오기",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        {
            categories.forEach { category ->
                val isDefault = listOf("우리말 겨루기", "트렌드 말하기", "상식 백과", "세계 여행", "AI 자동 생성 (KuizGenius)").contains(category)
                val isCustom = customCategories.contains(category)
                val isRemote = !isDefault && !isCustom
                val icon = when (category) {
                    "우리말 겨루기" -> Icons.Default.Language
                    "트렌드 말하기" -> Icons.Default.Psychology
                    "상식 백과" -> Icons.Default.MenuBook
                    "세계 여행" -> Icons.Default.Public
                    else -> Icons.Default.AutoAwesome
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .clickable {
                            if (category == "AI 자동 생성 (KuizGenius)") {
                                showAiTopicDialog = true
                            } else {
                                viewModel.selectCategory(category)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal.copy(alpha = 0.8f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (category.contains("AI") || isCustom) com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.5f)
                        else if (isRemote) com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (category.contains("AI") || isCustom) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else if (isRemote) Color.White.copy(alpha = 0.7f) else Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isDefault) category else if (isCustom) "⭐ $category" else "☁️ $category",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (category.contains("AI")) {
                                Text(
                                    text = "Gemini AI가 즉석에서 퀴즈를 생성합니다",
                                    fontSize = 11.sp,
                                    color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.7f)
                                )
                            } else if (isCustom) {
                                Text(
                                    text = "내가 생성/가져온 맞춤형 퀴즈 패키지",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            } else if (isRemote) {
                                Text(
                                    text = "클라우드에서 동기화된 공식 퀴즈 패키지",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        if (isCustom) {
                            // Sharing Button (.kquiz export)
                            IconButton(
                                onClick = 
                                {
                                    scope.launch {
                                        val repo = com.kitwlshcom.kdailyutil.data.repository.QuizRepository()
                                        val quizzes = repo.getQuizzes(context, category)
                                        val fileUri = com.kitwlshcom.kdailyutil.data.QuizFileHandler.exportQuizzes(
                                            context = context,
                                            categoryName = category,
                                            creatorName = "나의 크리에이터",
                                            creatorId = "local",
                                            quizzes = quizzes
                                        )
                                        if (fileUri != null) {
                                            com.kitwlshcom.kdailyutil.data.QuizFileHandler.triggerShareSheet(context, fileUri, category)
                                        } else {
                                            android.widget.Toast.makeText(context, "공유 파일 작성 실패", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "공유",
                                    tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete Button
                            var showDeleteConfirmDialog by remember { mutableStateOf(false) }
                            
                            IconButton(
                                onClick = 
                                {
                                    showDeleteConfirmDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (showDeleteConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirmDialog = false },
                                    title = { Text("카테고리 삭제") },
                                    text = { Text("[$category] 카테고리와 여기에 포함된 모든 퀴즈 문항을 완전히 삭제하시겠습니까?") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.deleteCustomCategory(category)
                                                showDeleteConfirmDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("삭제", color = Color.White)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                            Text("취소")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAiTopicDialog) {
        AlertDialog(
            onDismissRequest = { showAiTopicDialog = false },
            title = { Text("AI 퀴즈 주제 입력") },
            text = {
                Column {
                    Text("어떤 주제로 퀴즈를 만들까요? (예: 파이썬 프로그래밍, 한국사, 오늘 읽은 뉴스 등)")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = aiTopic,
                        onValueChange = { aiTopic = it },
                        placeholder = { Text("주제를 입력하세요") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (aiTopic.isNotBlank()) {
                            viewModel.generateAiQuiz(aiTopic)
                            showAiTopicDialog = false
                        }
                    },
                    enabled = aiTopic.isNotBlank()
                ) {
                    Text("생성하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiTopicDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    val pendingImport by viewModel.pendingImport.collectAsState()
    if (pendingImport != null) {
        val pkg = pendingImport!!
        var separateCategoryName by remember(pkg.category) { mutableStateOf("${pkg.category} (새 패키지)") }
        var isSeparateNameMode by remember { mutableStateOf(false) }

        if (!isSeparateNameMode) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelImportConflict() },
                title = { Text("📥 중복 카테고리 발견") },
                text = {
                    Text(
                        "가져올 카테고리 [${pkg.category}]와 동일한 이름의 카테고리가 이미 앱 내에 존재합니다.\n\n" +
                        "어떻게 처리하시겠습니까?",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resolveImportConflict("MERGE") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K, contentColor = Color.Black)
                        ) {
                            Text("합치기 (기존 목록에 추가)", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { isSeparateNameMode = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White)
                        ) {
                            Text("별도로 유지 (새 이름으로 저장)", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.resolveImportConflict("OVERWRITE") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f), contentColor = Color.White)
                        ) {
                            Text("덮어쓰기 (기존 목록 교체)", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { viewModel.cancelImportConflict() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("가져오기 취소", color = Color.Gray)
                        }
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { isSeparateNameMode = false },
                title = { Text("새 카테고리 이름 입력") },
                text = {
                    Column {
                        Text("구분하여 저장할 새 카테고리 이름을 입력해 주세요.", color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = separateCategoryName,
                            onValueChange = { separateCategoryName = it },
                            placeholder = { Text("예: ${pkg.category} (2)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (separateCategoryName.isNotBlank()) {
                                viewModel.resolveImportConflict("SEPARATE", separateCategoryName.trim())
                                isSeparateNameMode = false
                            }
                        },
                        enabled = separateCategoryName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K, contentColor = Color.Black)
                    ) {
                        Text("저장", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isSeparateNameMode = false }) {
                        Text("이전으로")
                    }
                }
            )
        }
    }
}

@Composable
fun QuizGeneratingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "KuizGenius가 문제를 출제 중입니다...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                "잠시만 기다려 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun QuizPlayScreen(viewModel: QuizViewModel)
{
    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val quizState by viewModel.quizState.collectAsState()
    val currentInput by viewModel.currentInput.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val currentHintText by viewModel.currentHintText.collectAsState()
    val filteredOptions by viewModel.filteredOptions.collectAsState()
    val isCheckingAnswer by viewModel.isCheckingAnswer.collectAsState()

    val currentQuestion = questions.getOrNull(currentIndex) ?: return
    val isAnswerChecked = quizState == QuizState.ANSWER_CHECKED

    val context = androidx.compose.ui.platform.LocalContext.current
    val statsManager = remember { com.kitwlshcom.kdailyutil.data.QuizStatsManager.getInstance(context) }
    val currentQuestionStats = remember(currentIndex, currentQuestion, quizState)
    {
        statsManager.getQuestionStats(currentQuestion.category, currentQuestion.question)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    )
    {
        // 상단 진행률
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Text(
                text = "문제 ${currentIndex + 1} / ${questions.size}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            val score by viewModel.score.collectAsState()
            Text(
                text = "현재 점수: $score 점",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (currentIndex + 1) / questions.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 문제 텍스트
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        )
        {
            Column(modifier = Modifier.padding(16.dp))
            {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    // 카테고리 뱃지
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    {
                        Text(
                            text = "${currentQuestion.category} > ${currentQuestion.subCategory}",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // 오류 신고 버튼
                    val emailContext = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = 
                        {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:")
                                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("kitwlsh@gmail.com"))
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "[KDailyUtil 퀴즈 오류 신고] 문제 ID: ${currentQuestion.id}")
                                putExtra(android.content.Intent.EXTRA_TEXT, "문제 내용: ${currentQuestion.question}\n\n[오류 내용 및 수정 제안]\n여기에 어떤 점이 이상한지 적어주세요.\n")
                            }
                            try
                            {
                                emailContext.startActivity(intent)
                            }
                            catch (e: Exception)
                            {
                                android.widget.Toast.makeText(emailContext, "이메일 앱을 찾을 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "오류 신고",
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                }

                // 골드 성취도 배지 UI 바인딩
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    if (currentQuestionStats.attemptCount > 0)
                    {
                        val correctPercent = (currentQuestionStats.correctCount.toFloat() / currentQuestionStats.attemptCount.toFloat() * 100).toInt()
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "성취도",
                            tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🏆 ${currentQuestionStats.attemptCount}회 도전 / ${currentQuestionStats.correctCount}회 성공 (정답률 $correctPercent%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                        )
                    }
                    else
                    {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "첫 도전",
                            tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🆕 첫 도전!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentQuestion.question,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                )
                if (!currentQuestion.imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentQuestion.imageUrl,
                            contentDescription = "문제 이미지",
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 힌트 버튼 및 힌트 텍스트 영역
        if (!isAnswerChecked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { viewModel.requestHint() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "힌트", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("힌트 보기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (!currentHintText.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = currentHintText ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 입력 방식 (객관식 vs 주관식)
        if (currentQuestion.type == QuizType.MULTIPLE_CHOICE) {
            val displayOptions = filteredOptions ?: currentQuestion.options ?: emptyList()
            displayOptions.forEach { option ->
                val isSelected = currentInput == option
                val backgroundColor = when {
                    isAnswerChecked && option == currentQuestion.answer -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    isAnswerChecked && isSelected && !isCorrect -> Color(0xFFF44336).copy(alpha = 0.2f)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    isAnswerChecked && option == currentQuestion.answer -> Color(0xFF4CAF50)
                    isAnswerChecked && isSelected && !isCorrect -> Color(0xFFF44336)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(backgroundColor, RoundedCornerShape(10.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isAnswerChecked && !isCheckingAnswer) {
                            viewModel.updateInput(option)
                        }
                        .padding(12.dp)
                ) {
                    Text(text = option, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        } else {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("정답을 입력하세요") },
                enabled = !isAnswerChecked && !isCheckingAnswer,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (currentInput.isNotBlank() && !isCheckingAnswer) viewModel.checkAnswer()
                })
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 결과 및 해설 영역
        if (isAnswerChecked) {
            val resultColor = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
            val resultIcon = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel
            val resultText = if (isCorrect) "정답입니다!" else "오답입니다. 정답은 '${currentQuestion.answer}' 입니다."

            Surface(
                color = resultColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(resultIcon, contentDescription = null, tint = resultColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(resultText, color = resultColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "해설: ${currentQuestion.explanation}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 하단 버튼
        if (!isAnswerChecked) {
            Button(
                onClick = { viewModel.checkAnswer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = currentInput.isNotBlank() && !isCheckingAnswer,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isCheckingAnswer) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text("정답 확인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val isLast = currentIndex == questions.size - 1
            Button(
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isLast) "결과 보기" else "다음 문제", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuizFinishedScreen(viewModel: QuizViewModel) {
    val score by viewModel.score.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val total = questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "퀴즈 종료!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "최종 점수",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$score / $total",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val feedbackText = when {
            score == total -> "만점입니다! 완벽한 우리말 실력을 갖추셨네요!"
            score >= total * 0.7 -> "훌륭합니다! 조금만 더 다듬으면 완벽하겠어요."
            else -> "아쉽네요. 다음에는 더 잘할 수 있을 거예요!"
        }
        Text(
            text = feedbackText,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { viewModel.selectCategory(null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("다른 분야 도전하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.exitQuiz() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("처음으로 돌아가기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
