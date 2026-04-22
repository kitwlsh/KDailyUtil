package com.kitwlshcom.kdailyutil.ui.screens

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import com.kitwlshcom.kdailyutil.audio.AudioCaptureService
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioCaptureViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCaptureScreen(
    viewModel: AudioCaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPrepared by viewModel.isPrepared.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPlaybackPaused by viewModel.isPlaybackPaused.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()
    val isEditLocked by viewModel.isEditLocked.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val playbackDuration by viewModel.playbackDuration.collectAsState()

    // 앱으로 돌아올 때마다 목록 자동 새로고침
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.loadRecordings()
                    // 앱이 활성화되면 플로팅 버튼 숨김
                    context.startService(Intent(context, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_HIDE_FLOATING
                    })
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // 다른 앱으로 가면 플로팅 버튼 다시 표시 (녹음 준비/시작 상태일 때만)
                    context.startService(Intent(context, AudioCaptureService::class.java).apply {
                        action = AudioCaptureService.ACTION_SHOW_FLOATING
                    })
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showRenameDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showHiddenManager by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // 녹음 시작 대신 '준비' 모드로 진입 (알림창에서 시작 가능하게)
            viewModel.prepareRecording(result.data!!)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFile(it) }
    }

    val listState = rememberLazyListState()
    
    // 현재 재생 중인 곡이 바뀌면 해당 위치로 자동 스크롤
    LaunchedEffect(currentlyPlaying) {
        currentlyPlaying?.let { playingItem ->
            val index = recordings.indexOfFirst { it.path == playingItem.path }
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
        }
    }

    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("이름 변경") },
            text = {
                TextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("새 파일 이름") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameRecording(showRenameDialog!!, newFileName)
                    showRenameDialog = null
                }) { Text("변경") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("취소") }
            }
        )
    }

    if (showDeleteConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("파일 삭제") },
            text = { Text("'${showDeleteConfirmDialog?.name}' 파일을 정말 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(showDeleteConfirmDialog!!)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("취소") }
            }
        )
    }

    if (showHiddenManager) {
        val hiddenFiles by viewModel.hiddenRecordings.collectAsState()
        AlertDialog(
            onDismissRequest = { showHiddenManager = false },
            title = { Text("숨긴 파일 관리") },
            text = {
                if (hiddenFiles.isEmpty()) {
                    Text("숨긴 파일이 없습니다.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(hiddenFiles) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.restoreRecording(item) }) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "복구", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenManager = false }) { Text("닫기") }
            }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "오디오 캡처",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // 3단계 마이크 버튼 (Idle -> Ready(Green) -> Recording(Red))
                val micColor = when {
                    isRecording -> Color.Red
                    isPrepared -> Color(0xFF4CAF50) // 녹색
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPrepared && !isRecording) {
                        Text(
                            text = "준비됨",
                            style = MaterialTheme.typography.labelSmall,
                            color = micColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    Surface(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else if (isPrepared) {
                                viewModel.dismissPreparation()
                            } else {
                                projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        },
                        shape = CircleShape,
                        color = micColor,
                        modifier = Modifier.size(40.dp),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "파일 추가",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showHiddenManager = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = "숨긴 파일 관리",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val canDrawOverlays = remember { Settings.canDrawOverlays(context) }
            
            Text(
                text = if (isRecording) "내부 소리 녹음 중..." else "마이크를 눌러 '준비' 상태가 되면 알림창이나 플로팅 버튼으로 녹음이 가능합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!canDrawOverlays) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("플로팅 버튼 권한 설정하기", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("저장된 녹음 파일이 없습니다.")
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (currentlyPlaying != null) 140.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(recordings) { item ->
                            AudioListItem(
                                item = item,
                                isPlaying = currentlyPlaying == item,
                                isEditLocked = isEditLocked,
                                onPlay = { viewModel.playAudio(item) },
                                onDelete = { showDeleteConfirmDialog = item },
                                onHide = { viewModel.hideRecording(item) },
                                onRename = { 
                                    newFileName = item.name.substringBeforeLast(".")
                                    showRenameDialog = item 
                                }
                            )
                        }
                    }

                    // 하단 컨트롤 바 (미니 플레이어)
                    if (currentlyPlaying != null) {
                        BottomPlayerBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 0.dp), // 탭바와 밀착
                            item = currentlyPlaying!!,
                            isPaused = isPlaybackPaused,
                            playbackMode = playbackMode,
                            isEditLocked = isEditLocked,
                            progress = playbackProgress,
                            totalDuration = playbackDuration,
                            onTogglePlay = { viewModel.playAudio(currentlyPlaying!!) },
                            onToggleMode = { viewModel.togglePlaybackMode() },
                            onToggleLock = { viewModel.toggleEditLock() },
                            onSeek = { viewModel.seekTo(it) },
                            onPrev = { viewModel.playPreviousRecording() },
                            onNext = { viewModel.playNextRecording() }
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun AudioListItem(
    item: AudioItem,
    isPlaying: Boolean,
    isEditLocked: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onRename: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Audiotrack,
                contentDescription = null,
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormat.format(Date(item.dateAdded)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isEditLocked) {
                Row {
                    IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "이름 변경", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onHide, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "목록에서 제외", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BottomPlayerBar(
    modifier: Modifier = Modifier,
    item: AudioItem,
    isPaused: Boolean,
    playbackMode: PlaybackMode,
    isEditLocked: Boolean,
    progress: Float,
    totalDuration: Long,
    onTogglePlay: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLock: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(bottom = 0.dp)
        ) {
            // 재생 위치 슬라이더를 맨 위로 (선 형태로)
            val currentPos = progress.toLong()
            val total = totalDuration
            
            Slider(
                value = if (total > 0) currentPos.toFloat() / total else 0f,
                onValueChange = { onSeek((it * total).toLong()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // 현재 곡 제목 및 탐색 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row {
                            Text(formatTime(currentPos), style = MaterialTheme.typography.labelSmall)
                            Text(" / ", style = MaterialTheme.typography.labelSmall)
                            Text(formatTime(total), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    Row {
                        IconButton(onClick = onPrev) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "이전 곡", modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = "다음 곡", modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 컨트롤 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleMode) {
                        val (icon, label) = when (playbackMode) {
                            PlaybackMode.SEQUENTIAL -> Icons.AutoMirrored.Filled.TrendingFlat to "순차"
                            PlaybackMode.LOOP_LIST -> Icons.Default.Repeat to "전체"
                            PlaybackMode.SHUFFLE -> Icons.Default.Shuffle to "랜덤"
                            PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne to "1곡"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        }
                    }

                    // 메인 재생 버튼
                    Surface(
                        onClick = onTogglePlay,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(onClick = onToggleLock) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (isEditLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "편집 잠금",
                                tint = if (isEditLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(if (isEditLocked) "잠금" else "열림", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
