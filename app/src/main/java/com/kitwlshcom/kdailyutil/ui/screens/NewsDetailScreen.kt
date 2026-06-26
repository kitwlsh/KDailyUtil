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
import android.webkit.WebResourceRequest
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info

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
    var showHelpDialog by remember { mutableStateOf(false) }
    // 'AI 이용 금지' 고지가 감지된 매체: AI 요약·낭독·쉐도잉을 비활성화하고 원문 보기만 제공
    val isRestricted = selectedNewsItem?.aiRestricted == true

    LaunchedEffect(selectedNewsItem) {
        selectedNewsItem?.let { item ->
            // 무조건 본문을 다시 긁어오도록 유도 (개선된 알고리즘 적용을 위해)
            viewModel.loadFullContent(item)
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("💡 기능 안내 (브리핑 & 쉐도잉)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column {
                    Text(
                        text = "1. AI 뉴스 브리핑 🎧",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "기사 본문을 음성(TTS)으로 친절하게 읽어주는 자동 낭독 서비스입니다. 출근길이나 이동 시 눈을 쓰지 않고 귀로 편리하게 기사를 들을 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "2. 뉴스 쉐도잉 (말하기 연습) 🗣️",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "AI 낭독 한 문장을 듣고 마이크에 큰 소리로 따라 읽는 스피킹 연습 기능입니다. 운전 중이나 이동 중에도 조작 없이 자동으로 음성 재생과 녹음이 전환되어 편리하게 따라 읽기 훈련을 할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(selectedNewsItem?.source ?: "뉴스 본문", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    val isWebUrl = selectedNewsItem?.link?.startsWith("http") == true
                    if (isWebUrl) {
                        IconButton(onClick = {
                            selectedNewsItem?.let {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.link))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("NewsDetailScreen", "Failed to open link: ${it.link}", e)
                                    Toast.makeText(context, "링크를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Public, contentDescription = "브라우저에서 열기")
                        }
                    }

                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "도움말")
                    }

                    // 'AI 이용 금지' 매체는 AI 낭독(브리핑) 버튼을 노출하지 않는다.
                    if (!isRestricted) {
                        Button(
                            onClick = {
                                if (isLoadingDetail) {
                                    Toast.makeText(context, "브리핑 데이터를 준비 중입니다. 잠시만 기다려 주세요.", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (isBriefingPlaying) {
                                        viewModel.stopBriefing()
                                    } else {
                                        selectedNewsItem?.let { viewModel.startSingleNewsBriefing(it) }
                                    }
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
                }
            )
        }
        // 저작권 보호: 외부 기사 쉐도잉 진입(FAB) 제거. 쉐도잉은 배움터(사용자 입력/OCR)에서 제공.
    ) { innerPadding ->
        selectedNewsItem?.let { item ->
            // 실제 기사 원본 URL이 있는 항목만 WebView로 로딩.
            // AI 맞춤 분석/시스템 메시지/오류 항목은 link가 http가 아니므로 본문 텍스트를 직접 표시한다.
            val targetUrl = item.resolvedUrl.ifBlank { item.link }
            val isWebUrl = targetUrl.startsWith("http")

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWebUrl) {
                    // 실제 뉴스 기사 웹페이지 로딩 (Outlink 인앱 브라우저로 저작권 분쟁 소지 제거)
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = true // 일반 기사 웹페이지이므로 JS 활성화 필수
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    databaseEnabled = true
                                    // 보안: 외부 웹페이지가 단말 내부 파일(file://, content://)에 접근하지 못하도록 차단
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    @Suppress("DEPRECATION")
                                    allowFileAccessFromFileURLs = false
                                    @Suppress("DEPRECATION")
                                    allowUniversalAccessFromFileURLs = false
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        // 웹뷰 내부에서 링크 클릭 시 해당 웹뷰에서 계속 탐색하도록 처리
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // 이미 표시 중인 원문 페이지(JS 렌더 포함)의 텍스트에서 'AI 학습·이용 금지' 고지를 점검.
                                        // 별도 스크랩이 아니라 '이미 띄운 페이지' 점검이므로 정책에 부합하며, 어느 매체든 자동 차단된다.
                                        view?.evaluateJavascript(
                                            "(function(){return document.body ? document.body.innerText : '';})();"
                                        ) { result ->
                                            if (com.kitwlshcom.kdailyutil.data.repository.NewsRepository
                                                    .detectAiRestrictionNotice(result)
                                            ) {
                                                viewModel.markSelectedAsRestricted()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        update = { webView ->
                            if (webView.url != targetUrl) {
                                webView.loadUrl(targetUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 'AI 이용 금지' 고지가 감지된 매체 안내 (AI 요약·낭독·쉐도잉 비활성화됨)
                    if (isRestricted) {
                        Surface(
                            color = Color(0xCC121212),
                            border = BorderStroke(1.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "ⓘ 이 매체는 AI 학습·이용을 제한하여 원문 보기만 제공됩니다.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    // AI 맞춤 분석 등 웹 링크가 아닌 항목: 본문 전체를 스크롤 텍스트로 표시
                    val bodyText = item.fullContent.ifBlank { item.description }
                        .ifBlank { "표시할 본문이 없습니다." }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                        )
                        if (item.pubDate.isNotBlank() && item.pubDate != "-") {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.pubDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = bodyText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.92f),
                            lineHeight = 26.sp
                        )
                        // 하단 플로팅 버튼(쉐도잉)에 본문이 가려지지 않도록 여백 확보
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("뉴스를 불러올 수 없습니다.")
        }
    }
}
