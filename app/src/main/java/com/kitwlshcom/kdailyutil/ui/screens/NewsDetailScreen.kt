package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.BriefingViewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    onBack: () -> Unit,
    viewModel: BriefingViewModel = viewModel()
) {
    val selectedNewsItem by viewModel.selectedNewsItem.collectAsState()
    val isBriefingPlaying by viewModel.isBriefingPlaying.collectAsState()
    val isLoadingDetail by viewModel.isLoadingDetail.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(selectedNewsItem) {
        selectedNewsItem?.let { item ->
            // 무조건 본문을 다시 긁어오도록 유도 (개선된 알고리즘 적용을 위해)
            viewModel.loadFullContent(item)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedNewsItem?.source ?: "뉴스 본문", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedNewsItem?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.link))
                            context.startActivity(intent)
                        }
                    }) {
                        Icon(Icons.Default.Public, contentDescription = "브라우저에서 열기")
                    }
                    
                    Button(
                        onClick = { 
                            selectedNewsItem?.let { viewModel.startSingleNewsBriefing(it) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBriefingPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            if (isBriefingPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isBriefingPlaying) "중지" else "브리핑", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        selectedNewsItem?.let { item ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.source,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.pubDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                if (isLoadingDetail) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("본문 내용을 분석 중입니다...", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    val content = item.fullContent.ifBlank { item.description }
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (item.fullContent.isBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "본문을 가져오는 데 실패했습니다. 원본 보기를 이용해 주세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("뉴스를 불러올 수 없습니다.")
        }
    }
}
