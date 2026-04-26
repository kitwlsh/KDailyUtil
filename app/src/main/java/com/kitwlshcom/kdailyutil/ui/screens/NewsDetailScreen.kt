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
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb

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
                            if (isBriefingPlaying) {
                                viewModel.stopBriefing()
                            } else {
                                selectedNewsItem?.let { viewModel.startSingleNewsBriefing(it) }
                            }
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
                    val contentText = item.fullContent.ifBlank { item.description }
                    val contentHtml = item.fullContentHtml.ifBlank { "<div>$contentText</div>" }
                    
                    val isDarkMode = isSystemInDarkTheme()
                    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
                    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
                    
                    val hexBackgroundColor = String.format("#%06X", 0xFFFFFF and backgroundColor)
                    val hexTextColor = String.format("#%06X", 0xFFFFFF and textColor)
                    val hexPrimaryColor = String.format("#%06X", 0xFFFFFF and primaryColor)

                    val styledHtml = """
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;700&display=swap');
                                body {
                                    font-family: 'Noto Sans KR', sans-serif;
                                    line-height: 1.8;
                                    color: $hexTextColor;
                                    background-color: $hexBackgroundColor;
                                    margin: 0;
                                    padding: 0;
                                    font-size: 17px;
                                    word-break: break-all;
                                }
                                p { margin-bottom: 24px; }
                                img {
                                    max-width: 100%;
                                    height: auto;
                                    display: block;
                                    margin: 20px auto;
                                    border-radius: 8px;
                                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                                }
                                a { color: $hexPrimaryColor; text-decoration: none; font-weight: bold; }
                                blockquote {
                                    border-left: 4px solid $hexPrimaryColor;
                                    padding-left: 16px;
                                    margin-left: 0;
                                    color: #888;
                                    font-style: italic;
                                }
                                .caption {
                                    font-size: 14px;
                                    color: #888;
                                    text-align: center;
                                    margin-top: -15px;
                                    margin-bottom: 20px;
                                }
                                * { max-width: 100%; }
                            </style>
                        </head>
                        <body>
                            $contentHtml
                        </body>
                        </html>
                    """.trimIndent()

                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 2000.dp)) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.apply {
                                        javaScriptEnabled = false // 보안상 끄기
                                        domStorageEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        defaultFontSize = 17
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                            url?.let {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                                context.startActivity(intent)
                                            }
                                            return true
                                        }
                                    }
                                    setBackgroundColor(backgroundColor)
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(item.link, styledHtml, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (item.fullContent.isBlank() && item.fullContentHtml.isBlank()) {
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
