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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
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
import androidx.activity.compose.BackHandler
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

    // 시스템 뒤로가기: 퀴즈 내부 단계를 먼저 되짚는다(곧장 뉴스탭으로 튕기지 않도록).
    //  문제 풀이/생성/결과/크리에이터 → 분야 선택,  분야 선택 → 초기 화면,  초기 화면(IDLE) → 기본 동작(배움터 이탈).
    BackHandler(enabled = quizState != QuizState.IDLE) {
        when (quizState) {
            QuizState.CATEGORY_SELECTION -> viewModel.backToIdle()
            else -> viewModel.selectCategory(null)
        }
    }

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
    var showAiGuideDialog by remember { mutableStateOf(false) }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    if (showPasteImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            title = { Text("텍스트(JSON) 붙여넣기로 가져오기", fontWeight = FontWeight.Bold, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "AI가 파일 대신 텍스트(JSON)로 준 경우, 그 내용을 전부 복사해 아래에 붙여넣고 '가져오기'를 누르세요. 앞뒤에 코드블록(```)이 섞여 있어도 됩니다.",
                        fontSize = 12.sp, color = Color.White.copy(0.8f), lineHeight = 18.sp
                    )
                    OutlinedButton(
                        onClick = {
                            val fromClip = clipboard.getText()?.text
                            if (!fromClip.isNullOrBlank()) {
                                pasteText = fromClip
                                android.widget.Toast.makeText(context, "클립보드 내용을 붙여넣었어요.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "클립보드가 비어 있어요.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        border = BorderStroke(1.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("📋 클립보드에서 붙여넣기", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        label = { Text("여기에 JSON 붙여넣기") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteText.isNotBlank()) {
                            viewModel.importQuizFromText(pasteText)
                            pasteText = ""
                            showPasteImportDialog = false
                        }
                    },
                    enabled = pasteText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K, contentColor = Color.Black)
                ) { Text("가져오기", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPasteImportDialog = false }) { Text("취소") }
            }
        )
    }

    val saveGuideLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = com.kitwlshcom.kdailyutil.data.QuizAiGuide.writeGuideTo(context, uri)
            android.widget.Toast.makeText(
                context,
                if (ok) "가이드를 저장했어요. 파일 앱에서 확인하세요." else "저장에 실패했어요.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (showAiGuideDialog) {
        AiQuizGuideDialog(
            onDismiss = { showAiGuideDialog = false },
            onCopyPrompt = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(com.kitwlshcom.kdailyutil.data.QuizAiGuide.PROMPT_TEMPLATE))
                android.widget.Toast.makeText(context, "AI 프롬프트를 복사했어요. AI에 붙여넣어 사용하세요.", android.widget.Toast.LENGTH_SHORT).show()
            },
            onCopyGuide = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(com.kitwlshcom.kdailyutil.data.QuizAiGuide.GUIDE_MARKDOWN))
                android.widget.Toast.makeText(context, "가이드 전체를 복사했어요.", android.widget.Toast.LENGTH_SHORT).show()
            },
            onSaveGuide = { saveGuideLauncher.launch(com.kitwlshcom.kdailyutil.data.QuizAiGuide.GUIDE_SAVE_FILENAME) },
            onShareGuide = { com.kitwlshcom.kdailyutil.data.QuizAiGuide.shareGuide(context) }
        )
    }

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

        // JSON 텍스트 붙여넣기로 가져오기 (AI가 파일 대신 텍스트만 준 경우)
        OutlinedButton(
            onClick = { showPasteImportDialog = true },
            border = BorderStroke(1.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "📋 텍스트(JSON) 붙여넣기로 가져오기",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // AI로 개인 퀴즈 만들기 가이드
        OutlinedButton(
            onClick = { showAiGuideDialog = true },
            border = BorderStroke(1.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "🤖 AI로 나만의 퀴즈 만들기 (가이드)",
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

    // 개인이 만들었거나 외부에서 가져온 커스텀 문제는 개발자가 수정할 수 없으므로
    // 오류 신고(개발자 이메일)를 제공하지 않는다. 공식 내장/클라우드(앱 관리) 문제만 신고 가능.
    val customCategories by viewModel.customCategories.collectAsState()
    val isPersonalQuiz = customCategories.contains(currentQuestion.category)

    // 커스텀(개인) 문제 직접 편집 다이얼로그
    var editingQuestion by remember { mutableStateOf<com.kitwlshcom.kdailyutil.data.model.QuizQuestion?>(null) }
    editingQuestion?.let { q ->
        EditQuizDialog(
            question = q,
            onDismiss = { editingQuestion = null },
            onSave = { updated ->
                viewModel.updateCustomQuestion(updated)
                editingQuestion = null
            }
        )
    }

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

                    // 오류 신고 버튼 — 앱이 관리하는(공식/클라우드) 문제에서만 노출.
                    // 개인 제작·가져온 커스텀 문제는 개발자가 고칠 수 없어 신고 대상에서 제외한다.
                    if (!isPersonalQuiz) {
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
                    } else {
                        // 개인 제작·가져온 문제: 신고 대신 '직접 편집' 제공.
                        // 단, 편집 화면엔 정답이 보이므로 '정답 확인 후'에만 편집 허용(풀이 중엔 잠금).
                        if (isAnswerChecked) {
                            IconButton(
                                onClick = { editingQuestion = currentQuestion },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "문제 편집",
                                    tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    android.widget.Toast.makeText(
                                        context,
                                        "정답을 확인한 뒤에 편집할 수 있어요. (편집 화면에 정답이 표시되기 때문)",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "편집 잠금 (정답 확인 후 편집 가능)",
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                        }
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

/**
 * AI(ChatGPT·Gemini·Claude 등)로 개인 퀴즈(.kquiz)를 만드는 방법을 안내하고,
 * 프롬프트 복사 / 가이드 파일 내려받기를 제공하는 도움말 다이얼로그.
 */
@Composable
fun AiQuizGuideDialog(
    onDismiss: () -> Unit,
    onCopyPrompt: () -> Unit,
    onCopyGuide: () -> Unit,
    onSaveGuide: () -> Unit,
    onShareGuide: () -> Unit
) {
    val gold = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("AI로 나만의 퀴즈 만들기", fontWeight = FontWeight.Bold, color = gold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "ChatGPT·Gemini·Claude 같은 AI에게 아래 프롬프트를 주면, 앱에 바로 가져올 수 있는 퀴즈 파일을 만들 수 있어요.",
                    fontSize = 13.sp, color = Color.White.copy(0.85f), lineHeight = 19.sp
                )

                Text("사용 순서", fontWeight = FontWeight.Bold, color = gold, fontSize = 14.sp)
                Text(
                    "1. 아래 '프롬프트 복사' 또는 '가이드 파일 저장' 후 AI에 붙여넣기/첨부\n" +
                    "2. 주제만 말하기 — 예: \"주제는 주식 영어 용어\" (문항 수·난이도는 기본값 자동)\n" +
                    "3. 결과를 앱에 넣기 (둘 중 하나)\n" +
                    "   · 파일이 되면: .kquiz 파일 내려받아 '📥 …(.kquiz) 가져오기'\n" +
                    "   · 텍스트만 나오면: JSON 전체 복사 → '📋 텍스트(JSON) 붙여넣기로 가져오기'",
                    fontSize = 12.5.sp, color = Color.White.copy(0.8f), lineHeight = 20.sp
                )

                Text("이렇게 요청하세요 (예시)", fontWeight = FontWeight.Bold, color = gold, fontSize = 14.sp)
                Text(
                    "• \"이 형식대로 퀴즈 10개 만들어줘. 주제는 '세계사 로마', 난이도 중.\"\n" +
                    "• \"형식대로 문제 만들어줘. 주제는 '기초 경제 용어', 객관식만.\"\n" +
                    "• (사진 첨부) \"첨부한 교과서 사진 내용으로 위 형식대로 퀴즈를 만들어줘.\"",
                    fontSize = 12.5.sp, color = Color.White.copy(0.8f), lineHeight = 20.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = gold.copy(0.06f)),
                    border = BorderStroke(0.8.dp, gold.copy(0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "📷 교과서·문제집 사진으로도 만들 수 있어요. 사진을 볼 수 있는 AI에 이미지를 첨부하고 위 프롬프트를 함께 주세요. (촬영·입력 자료의 이용 권한 확인은 본인 책임, 개인 학습용)",
                        fontSize = 11.5.sp, color = Color.White.copy(0.75f), lineHeight = 17.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = onCopyPrompt,
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI 프롬프트 복사하기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(
                    "가장 간편해요: 위 버튼으로 복사한 뒤 AI 채팅창에 붙여넣기만 하면 됩니다.",
                    fontSize = 11.sp, color = Color.White.copy(0.6f), lineHeight = 15.sp
                )
                OutlinedButton(
                    onClick = onCopyGuide,
                    border = BorderStroke(1.dp, gold.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("가이드 전체 복사", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onSaveGuide,
                    border = BorderStroke(1.dp, gold.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("가이드 파일로 저장 (.md, 위치 선택)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onShareGuide,
                    border = BorderStroke(1.dp, gold.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("가이드 파일 공유 (카톡·메일 첨부)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(
                    "저장 = 원하는 폴더에 파일로 보관 · 공유 = 카톡/메일에 파일 첨부. (복사만 해도 AI엔 충분해요)",
                    fontSize = 10.5.sp, color = Color.White.copy(0.55f), lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기", color = gold) }
        }
    )
}

/**
 * 커스텀(개인) 문제를 칸별 폼으로 직접 편집하는 다이얼로그.
 * 저장 시 원래 문제의 id/카테고리를 유지한 채 내용만 갱신한다(같은 id 저장 = 갱신).
 */
@Composable
fun EditQuizDialog(
    question: com.kitwlshcom.kdailyutil.data.model.QuizQuestion,
    onDismiss: () -> Unit,
    onSave: (com.kitwlshcom.kdailyutil.data.model.QuizQuestion) -> Unit
) {
    val gold = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
    var type by remember { mutableStateOf(question.type) }
    var questionText by remember { mutableStateOf(question.question) }
    var explanationText by remember { mutableStateOf(question.explanation) }
    var hintText by remember { mutableStateOf(question.semanticHint ?: "") }
    val initOptions = question.options ?: emptyList()
    var opt1 by remember { mutableStateOf(initOptions.getOrElse(0) { "" }) }
    var opt2 by remember { mutableStateOf(initOptions.getOrElse(1) { "" }) }
    var opt3 by remember { mutableStateOf(initOptions.getOrElse(2) { "" }) }
    var opt4 by remember { mutableStateOf(initOptions.getOrElse(3) { "" }) }
    var correctIndex by remember { mutableStateOf(initOptions.indexOf(question.answer).coerceAtLeast(0)) }
    var answerText by remember { mutableStateOf(if (question.type == QuizType.SUBJECTIVE) question.answer else "") }

    val isMc = type == QuizType.MULTIPLE_CHOICE
    val opts = listOf(opt1, opt2, opt3, opt4)
    val canSave = questionText.isNotBlank() && explanationText.isNotBlank() &&
        if (isMc) opts.all { it.isNotBlank() } else answerText.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = gold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("문제 편집", fontWeight = FontWeight.Bold, color = gold)
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("유형:", fontSize = 13.sp, color = Color.White.copy(0.8f))
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = isMc, onClick = { type = QuizType.MULTIPLE_CHOICE })
                    Text("객관식", fontSize = 13.sp, color = Color.White.copy(0.8f))
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = !isMc, onClick = { type = QuizType.SUBJECTIVE })
                    Text("주관식", fontSize = 13.sp, color = Color.White.copy(0.8f))
                }
                OutlinedTextField(
                    value = questionText, onValueChange = { questionText = it },
                    label = { Text("질문") }, modifier = Modifier.fillMaxWidth()
                )
                if (isMc) {
                    Text("보기 (왼쪽 동그라미로 정답 선택)", fontSize = 12.sp, color = gold)
                    val setters = listOf<(String) -> Unit>({ opt1 = it }, { opt2 = it }, { opt3 = it }, { opt4 = it })
                    opts.forEachIndexed { idx, value ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = correctIndex == idx, onClick = { correctIndex = idx })
                            OutlinedTextField(
                                value = value, onValueChange = setters[idx],
                                label = { Text("보기 ${idx + 1}") },
                                singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = answerText, onValueChange = { answerText = it },
                        label = { Text("정답 (1~3단어)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = explanationText, onValueChange = { explanationText = it },
                    label = { Text("해설") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                )
                OutlinedTextField(
                    value = hintText, onValueChange = { hintText = it },
                    label = { Text("힌트 (선택)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val updated = question.copy(
                        type = type,
                        question = questionText.trim(),
                        options = if (isMc) opts.map { it.trim() } else null,
                        answer = if (isMc) opts[correctIndex].trim() else answerText.trim(),
                        explanation = explanationText.trim(),
                        semanticHint = hintText.trim().ifBlank { null }
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black)
            ) { Text("저장", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
