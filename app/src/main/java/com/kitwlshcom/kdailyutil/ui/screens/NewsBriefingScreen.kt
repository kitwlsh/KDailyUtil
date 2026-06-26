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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import com.kitwlshcom.kdailyutil.ui.navigation.NavScreen
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

    LaunchedEffect(Unit) {
        if (newsItems.isEmpty()) {
            viewModel.fetchNews()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (newsItems.isNotEmpty()) {
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
