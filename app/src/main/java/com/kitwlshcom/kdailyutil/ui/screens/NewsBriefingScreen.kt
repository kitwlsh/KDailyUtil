package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.kitwlshcom.kdailyutil.data.model.AiChatSession
import com.kitwlshcom.kdailyutil.data.model.ChatMessage
import com.kitwlshcom.kdailyutil.data.model.ChatRole
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import com.kitwlshcom.kdailyutil.ui.components.MarkdownText
import com.kitwlshcom.kdailyutil.ui.navigation.NavScreen
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import com.kitwlshcom.kdailyutil.ui.viewmodel.BriefingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsBriefingScreen(
    navController: NavController = rememberNavController(),
    viewModel: BriefingViewModel = viewModel()
) {
    val newsItems by viewModel.newsItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isPlaying by viewModel.isBriefingPlaying.collectAsState()
    val isPaused by viewModel.isBriefingPaused.collectAsState()
    val isAiLoading by viewModel.isAiAnalysisLoading.collectAsState()
    val aiCommands by viewModel.aiBriefingCommands.collectAsState()
    val selectedAiCommand by viewModel.selectedAiCommand.collectAsState()
    val stockKeywords by viewModel.stockKeywords.collectAsState()
    val selectedStockKeyword by viewModel.selectedStockKeyword.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 탭에서 바로 항목을 추가하는 '＋' 다이얼로그 (설정 화면과 동일한 저장 로직 재사용)
    var addTarget by remember { mutableStateOf<String?>(null) } // "category" | "stock" | "ai"
    var addText by remember { mutableStateOf("") }
    if (addTarget != null) {
        val (dlgTitle, dlgHint) = when (addTarget) {
            "category" -> "주제(카테고리) 추가" to "예: 스포츠"
            "stock" -> "관심 증시/종목 추가" to "예: 테슬라, 비트코인"
            else -> "AI 브리핑 명령 추가" to "예: 오늘 반도체 이슈 요약해줘"
        }
        AlertDialog(
            onDismissRequest = { addTarget = null; addText = "" },
            title = { Text(dlgTitle, fontWeight = FontWeight.Bold, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K) },
            text = {
                OutlinedTextField(
                    value = addText,
                    onValueChange = { addText = it },
                    label = { Text(dlgHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = addText.trim()
                        if (t.isNotBlank()) {
                            when (addTarget) {
                                "category" -> {
                                    val fixed = setOf("전체", "증시", "AI")
                                    if (t !in fixed && t !in categories) viewModel.updateCategories(categories + t)
                                }
                                "stock" -> viewModel.updateStockKeywords(stockKeywords + t)
                                "ai" -> viewModel.updateAiCommands(aiCommands + t)
                            }
                        }
                        addText = ""; addTarget = null
                    },
                    enabled = addText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K, contentColor = Color.Black)
                ) { Text("추가", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { addTarget = null; addText = "" }) { Text("취소") } }
        )
    }

    LaunchedEffect(Unit) {
        if (newsItems.isEmpty()) {
            viewModel.fetchNews()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            // AI 대화 탭에서는 브리핑 FAB를 숨긴다(대화 입력 바와 겹침 방지, 낭독은 말풍선 🔊로 제공).
            if (newsItems.isNotEmpty() && selectedCategory != "AI") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPlaying) {
                        // 다음 뉴스 건너뛰기
                        FloatingActionButton(
                            onClick = { viewModel.skipToNextNews() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "다음 뉴스 건너뛰기")
                        }
                        // 일시정지 / 재개 (멈춘 뉴스 항목부터 다시 재생)
                        FloatingActionButton(
                            onClick = { if (isPaused) viewModel.resumeBriefing() else viewModel.pauseBriefing() },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "브리핑 재개" else "브리핑 일시정지"
                            )
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.startLiveBriefing() },
                        icon = {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        },
                        text = { Text(if (isPlaying) "브리핑 중지" else "전체 브리핑 시작") },
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .width(84.dp) // 너비 살짝 확대
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.4f)) // 더 어두운 배경으로 텍스트 보호
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "주제", 
                    style = MaterialTheme.typography.labelSmall,
                    color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(categories.toList()) { category ->
                        CategoryTabItem(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = {
                                viewModel.selectCategory(category)
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                }
                            }
                        )
                    }
                    item {
                        IconButton(onClick = { addTarget = "category" }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "주제 추가",
                                tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 메인 뉴스 리스트
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedCategory, 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = { viewModel.fetchNews(forceRefresh = true) }) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "새로고침",
                            tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                        )
                    }
                }

                // 새로고침 로딩바 (골드 프리미엄 인디케이터)
                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(bottom = 4.dp),
                        color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // AI 카테고리 선택 시 서브 탭 표시
                if (selectedCategory == "AI" && aiCommands.isNotEmpty()) {
                    val commandsList = aiCommands.toList()
                    val selectedIndex = commandsList.indexOf(selectedAiCommand).takeIf { it >= 0 } ?: 0
                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = Color.Transparent,
                        contentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        edgePadding = 0.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        commandsList.forEachIndexed { index, command ->
                            val shortCommand = if (command.length > 10) command.take(10) + "..." else command
                            Tab(
                                selected = index == selectedIndex,
                                onClick = {
                                    viewModel.selectAiCommand(command)
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                },
                                text = { Text(shortCommand, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                        Tab(
                            selected = false,
                            onClick = { addTarget = "ai" },
                            text = { Text("＋", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // 증시 카테고리 선택 시 서브 탭 표시
                if (selectedCategory == "증시" && stockKeywords.isNotEmpty()) {
                    val keywordsList = stockKeywords.toList()
                    val selectedIndex = keywordsList.indexOf(selectedStockKeyword).takeIf { it >= 0 } ?: 0
                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = Color.Transparent,
                        contentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        edgePadding = 0.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        keywordsList.forEachIndexed { index, keyword ->
                            val shortKeyword = if (keyword.length > 10) keyword.take(10) + "..." else keyword
                            Tab(
                                selected = index == selectedIndex,
                                onClick = {
                                    viewModel.selectStockKeyword(keyword)
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                },
                                text = { Text(shortKeyword, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                        Tab(
                            selected = false,
                            onClick = { addTarget = "stock" },
                            text = { Text("＋", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (selectedCategory == "AI" && isAiLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Gemini가 뉴스를 분석하여 브리핑을 생성 중입니다...", 
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (isRefreshing && newsItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("뉴스를 불러오는 중입니다...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (newsItems.isEmpty() && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val emptyMsg = when (selectedCategory) {
                            "AI" -> "AI 명령어를 먼저 등록해 주세요."
                            "증시" -> "해당 키워드에 대한 증시 뉴스가 없습니다."
                            else -> "불러온 뉴스가 없습니다."
                        }
                        Text(emptyMsg, textAlign = TextAlign.Center)
                    }
                } else if (selectedCategory == "AI" && newsItems.any { it.source == "Gemini AI" }) {
                    // AI 탭: 맞춤 분석을 첫 답으로 두고 이어서 대화(멀티턴) — doc/FEATURE_AI_NEWS_CHAT.md
                    AiChatSection(
                        viewModel = viewModel,
                        analysisItem = newsItems.first { it.source == "Gemini AI" }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(newsItems) { item ->
                            NewsCard(
                                item = item,
                                onClick = {
                                    viewModel.setSelectedNewsItem(item)
                                    navController.navigate(NavScreen.NewsDetail.route)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTabItem(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isFixed = category in setOf("전체", "증시", "AI")
    val backgroundColor = when {
        isSelected -> com.kitwlshcom.kdailyutil.ui.theme.Gold24K
        isFixed -> com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.05f)
    }
    val contentColor = when {
        isSelected -> Color.Black
        isFixed -> com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = if (isSelected || isFixed) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsCard(
    item: NewsItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.2f)
        )
    ) {
        // 저작권 보호: 외부 기사 쉐도잉(말하기 연습) 진입점 제거. 쉐도잉은 배움터(사용자 입력/OCR)에서 제공.
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                )
                Text(
                    text = item.pubDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val displayContent = item.description
            val isAiItem = item.source == "Gemini AI"
            Text(
                text = displayContent,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (isAiItem) 10 else 3,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// ===== 뉴스 AI 대화 UI (doc/FEATURE_AI_NEWS_CHAT.md) =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSection(
    viewModel: BriefingViewModel,
    analysisItem: NewsItem
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isResponding by viewModel.isChatResponding.collectAsState()
    val isListening by viewModel.isChatListening.collectAsState()
    val sttPartial by viewModel.chatSttPartial.collectAsState()
    val sessions by viewModel.chatSessions.collectAsState()
    val viewing by viewModel.viewingSession.collectAsState()

    var input by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val chatListState = rememberLazyListState()

    // 세션이 아직 시딩되기 전이면 분석 결과를 임시로 첫 말풍선처럼 표시
    val displayMessages = if (messages.isEmpty())
        listOf(ChatMessage(ChatRole.AI, analysisItem.description)) else messages

    // 새 메시지가 오면 맨 아래로 스크롤
    LaunchedEffect(displayMessages.size, isResponding) {
        if (displayMessages.isNotEmpty()) chatListState.animateScrollToItem(displayMessages.size)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 헤더: 대화 기록 진입
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.loadChatHistory(); showHistory = true }) {
                Icon(Icons.Default.History, contentDescription = null, tint = Gold24K, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("대화 기록", color = Gold24K, fontSize = 12.sp)
            }
        }

        // 대화 목록
        LazyColumn(
            state = chatListState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "⚠️ 오늘 뉴스의 제목·요약을 바탕으로 답합니다. 세부 사실은 부정확할 수 있으니 원문을 확인하세요.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(displayMessages) { msg ->
                ChatBubble(msg = msg, onSpeak = { viewModel.speakChatMessage(msg.text) })
            }
            if (isResponding) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Gold24K)
                        Spacer(Modifier.width(8.dp))
                        Text("AI가 답변 중…", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // 입력 바
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = if (isListening) sttPartial else input,
                onValueChange = { if (!isListening) input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (isListening) "듣는 중…" else "이어서 물어보기", fontSize = 13.sp) },
                enabled = !isResponding,
                maxLines = 3,
                shape = RoundedCornerShape(20.dp)
            )
            IconButton(
                onClick = { if (isListening) viewModel.stopChatVoiceInput() else viewModel.startChatVoiceInput() },
                enabled = !isResponding
            ) {
                Icon(Icons.Default.Mic, contentDescription = "음성 입력", tint = if (isListening) Color.Red else Gold24K)
            }
            IconButton(
                onClick = { viewModel.sendChat(input); input = "" },
                enabled = !isResponding && input.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송", tint = if (input.isNotBlank()) Gold24K else Color.White.copy(alpha = 0.3f))
            }
        }

        // 신고 링크 (Google Play 생성형 AI 정책 대응)
        Text(
            "부적절한 AI 응답 신고",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.End)
                .clickable { uriHandler.openUri("mailto:kitwlsh@gmail.com?subject=AI%20%EC%9D%91%EB%8B%B5%20%EC%8B%A0%EA%B3%A0") }
                .padding(bottom = 4.dp)
        )
    }

    if (showHistory) {
        ChatHistoryDialog(
            sessions = sessions,
            onDismiss = { showHistory = false },
            onOpen = { viewModel.viewChatSession(it) },
            onDelete = { viewModel.deleteChatSession(it) },
            onClearAll = { viewModel.clearChatHistory() }
        )
    }

    viewing?.let { session ->
        SessionViewDialog(
            session = session,
            onDismiss = { viewModel.closeViewingSession() },
            onSpeak = { viewModel.speakChatMessage(it) }
        )
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, onSpeak: () -> Unit) {
    val isUser = msg.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isUser) Gold24K.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
                .padding(10.dp)
        ) {
            if (!isUser) {
                Text("🤖 AI", fontSize = 10.sp, color = Gold24K.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
            }
            if (isUser) {
                Text(msg.text, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
            } else {
                // AI 답변은 마크다운 서식(굵게·목록·제목)으로 렌더링
                MarkdownText(msg.text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            }
            if (!isUser) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onSpeak).padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "낭독", tint = Gold24K, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("낭독", fontSize = 11.sp, color = Gold24K)
                }
            }
        }
    }
}

@Composable
fun ChatHistoryDialog(
    sessions: List<AiChatSession>,
    onDismiss: () -> Unit,
    onOpen: (AiChatSession) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🕓 대화 기록", fontWeight = FontWeight.Bold, color = Gold24K) },
        text = {
            if (sessions.isEmpty()) {
                Text("저장된 대화가 없습니다.", color = Color.White.copy(alpha = 0.6f))
            } else {
                Column {
                    Text("최근 30일 대화만 보관됩니다. 탭하면 열람(읽기 전용), 🗑으로 삭제합니다.",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sessions) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { onOpen(s); onDismiss() }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.command.ifBlank { "(명령 없음)" }, fontSize = 13.sp, color = Color.White,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${s.date} · 메시지 ${s.messages.size}개", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { onDelete(s.key) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "삭제", tint = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기", color = Gold24K) } },
        dismissButton = {
            if (sessions.isNotEmpty()) TextButton(onClick = onClearAll) {
                Text("전체 지우기", color = Color.Red.copy(alpha = 0.8f))
            }
        }
    )
}

@Composable
fun SessionViewDialog(session: AiChatSession, onDismiss: () -> Unit, onSpeak: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(session.command.ifBlank { "지난 대화" }, fontWeight = FontWeight.Bold, color = Gold24K,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${session.date} · 읽기 전용", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("지난 대화는 읽기 전용입니다. 이어가려면 오늘 탭에서 다시 물어보세요.",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                }
                items(session.messages) { m -> ChatBubble(msg = m, onSpeak = { onSpeak(m.text) }) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기", color = Gold24K) } }
    )
}
