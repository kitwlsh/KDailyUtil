package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import com.kitwlshcom.kdailyutil.ui.viewmodel.BriefingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsBriefingScreen(
    viewModel: BriefingViewModel = viewModel()
) {
    val newsItems by viewModel.newsItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val keywords by viewModel.keywords.collectAsState()
    val isPlaying by viewModel.isBriefingPlaying.collectAsState()

    // 화면 진입 시 뉴스 자동 로딩 (키워드 유무와 관계없이 주요 뉴스 포함하여 가져옴)
    LaunchedEffect(Unit) {
        if (newsItems.isEmpty()) {
            viewModel.fetchNews()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (newsItems.isNotEmpty()) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("오늘의 뉴스 브리핑", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { viewModel.fetchNews() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                }
            }

            if (isRefreshing && newsItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("뉴스를 불러오는 중입니다...")
                    }
                }
            } else if (newsItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("불러온 뉴스가 없습니다. 키워드를 확인하거나 나중에 다시 시도해 주세요.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newsItems) { item ->
                        NewsCard(
                            item = item,
                            onClick = { viewModel.startSingleNewsBriefing(item) }
                        )
                    }
                }
            }
        }
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
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.pubDate,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val displayContent = item.fullContent.ifBlank { item.description }
            Text(
                text = displayContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
