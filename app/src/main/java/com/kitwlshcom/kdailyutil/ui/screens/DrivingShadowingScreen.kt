package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.ShadowingViewModel
import kotlinx.coroutines.launch

@Composable
fun DrivingShadowingScreen(
    viewModel: ShadowingViewModel = viewModel()
) {
    val currentSentences by viewModel.currentSentences.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isActive by viewModel.isShadowingActive.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 현재 인덱스가 변할 때마다 해당 문장으로 자동 스크롤
    LaunchedEffect(currentIndex) {
        if (currentSentences.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    // 마이크 권한 런처
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startShadowing()
    }

    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadEditorials()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // 눈부심 방지 딥 블랙
    ) {
        // 1. 상단 고정 제목 영역
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "학습 중인 기사",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = currentTitle.ifBlank { "기사를 선택해 주세요" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. 중앙 문장 리스트 영역
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(currentSentences) { index, sentence ->
                    val isCurrent = index == currentIndex
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable { /* 클릭 시 해당 문장으로 이동 가능하게 추후 확장 가능 */ },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = sentence,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = if (isCurrent) 28.sp else 20.sp,
                                lineHeight = if (isCurrent) 38.sp else 28.sp
                            ),
                            color = when {
                                isCurrent && isRecording -> Color.Yellow
                                isCurrent -> Color.White
                                else -> Color.DarkGray
                            },
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 하단 그라데이션 오버레이 (리스트가 사라지듯 보이게)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF121212))
                        )
                    )
            )
        }

        // 3. 상태 알림 (따라 읽기 표시)
        if (isRecording) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFFFF5252),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("지금 따라 읽으세요!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. 하단 거대 제어바
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 이전 문장
                IconButton(
                    onClick = { viewModel.skipToPrevious() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "이전", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // 일시정지 / 재생
                IconButton(
                    onClick = {
                        if (isPaused) viewModel.resumeShadowing()
                        else if (isActive) viewModel.pauseShadowing()
                        else permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .background(if (isPaused || !isActive) Color(0xFF4CAF50) else Color(0xFFFFC107), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPaused || !isActive) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "재생제어",
                        tint = Color.Black,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // 중지
                IconButton(
                    onClick = { viewModel.stopShadowing() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.DarkGray, CircleShape)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "중지", tint = Color.White)
                }

                // 다음 문장
                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "다음", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // 기사 새로고침
                IconButton(
                    onClick = { viewModel.loadEditorials() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "기사변경", tint = Color.Gray)
                }
            }
        }
    }
}
