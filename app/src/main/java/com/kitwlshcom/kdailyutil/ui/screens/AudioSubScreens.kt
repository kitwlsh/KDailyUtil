package com.kitwlshcom.kdailyutil.ui.screens

import android.content.Intent
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.*
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioCaptureViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.RecordingSource
import com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode

@Composable
fun CaptureTabContent(
    viewModel: AudioCaptureViewModel,
    onStartInternalRecording: () -> Unit
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val isPrepared by viewModel.isPrepared.collectAsState()
    val recordingSource by viewModel.recordingSource.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                isRecording -> "녹음 중..."
                isPrepared -> "준비 완료 (시작을 누르세요)"
                else -> "녹음 대기"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Large Mic/Stop Button
        Surface(
            onClick = {
                if (isRecording) {
                    viewModel.stopRecording()
                } else {
                    if (recordingSource == RecordingSource.MIC) {
                        viewModel.startRecording(null)
                    } else {
                        if (isPrepared) {
                            viewModel.startRecording(null) // 이미 준비된 데이터 사용
                        } else {
                            if (android.provider.Settings.canDrawOverlays(context)) {
                                onStartInternalRecording() // 권한 요청 및 준비 시작
                            } else {
                                // 권한 요청 페이지로 이동
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            color = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Source Toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("녹음 소스: ", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (recordingSource == RecordingSource.MIC) "마이크" else "내부 오디오",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { viewModel.toggleRecordingSource() }) {
                Icon(Icons.Default.GraphicEq, contentDescription = "소스 전환")
            }
        }

        if (isPrepared && !isRecording) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { viewModel.dismissPrepare() }) {
                Text("준비 해제", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PlayerTabContent(viewModel: AudioCaptureViewModel) {
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPaused by viewModel.isPlaybackPaused.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val duration by viewModel.playbackDuration.collectAsState()
    val mode by viewModel.playbackMode.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (currentlyPlaying == null) {
            Text("재생 중인 곡이 없습니다.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text(currentlyPlaying!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Slider(
                value = progress,
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(progress.toLong()))
                Text(formatTime(duration))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = { viewModel.togglePlaybackMode() }) {
                    Icon(
                        imageVector = when(mode) {
                            com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode.SHUFFLE -> Icons.Default.Shuffle
                            com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne
                            com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode.LOOP_LIST -> Icons.Default.Repeat
                            else -> Icons.AutoMirrored.Filled.ArrowForward
                        },
                        contentDescription = "모드"
                    )
                }
                IconButton(onClick = { viewModel.playPreviousRecording() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = { viewModel.playAudio(currentlyPlaying!!) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { viewModel.playNextRecording() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { /* Queue view? */ }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun FileManagerTabContent(
    viewModel: AudioCaptureViewModel,
    onRenameClick: (AudioItem) -> Unit,
    onDeleteClick: (AudioItem) -> Unit,
    onPlaylistClick: (AudioItem) -> Unit
) {
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isEditLocked by viewModel.isEditLocked.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val displayFiles by viewModel.displayFiles.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("파일 관리", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.toggleEditLock() }) {
                    Icon(if (isEditLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = "잠금", tint = if (isEditLocked) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
            
            // 필터 칩
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { viewModel.setFilterMode("ALL") },
                        label = { Text("전체") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterMode == "IMPORTS",
                        onClick = { viewModel.setFilterMode("IMPORTS") },
                        label = { Text("가져온 파일") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterMode == "HIDDEN",
                        onClick = { viewModel.setFilterMode("HIDDEN") },
                        label = { Text("숨김") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterMode == "TRASH",
                        onClick = { viewModel.setFilterMode("TRASH") },
                        label = { Text("휴지통") }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (displayFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("파일이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = displayFiles, key = { it.path }) { item ->
                    AudioListItem(
                        item = item,
                        isPlaying = currentlyPlaying == item,
                        isEditLocked = isEditLocked,
                        onPlay = { viewModel.playAudio(item) },
                        onDelete = { 
                            if (filterMode == "TRASH") viewModel.permanentlyDelete(item)
                            else onDeleteClick(item) 
                        },
                        onHide = { 
                            if (filterMode == "HIDDEN") viewModel.restoreRecording(item)
                            else viewModel.hideRecording(item) 
                        },
                        onRename = { onRenameClick(item) },
                        onAddToPlaylist = { onPlaylistClick(item) },
                        onMoveUp = { viewModel.moveItemUp(item) },
                        onMoveDown = { viewModel.moveItemDown(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistTabContent(
    viewModel: AudioCaptureViewModel,
    onCreateClick: () -> Unit,
    onManageClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isEditLocked by viewModel.isEditLocked.collectAsState()
    val recordings by viewModel.recordings.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedPlaylist == null) {
            // 재생목록 목록 뷰
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("재생목록 관리", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "새 목록", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onManageClick) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "목록")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (playlists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("생성된 재생목록이 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists) { playlist ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectPlaylist(playlist) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(playlist, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        } else {
            // 선택된 재생목록 상세 뷰
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    text = selectedPlaylist!!,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "곡 추가")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("이 재생목록에 곡이 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = recordings, key = { it.path }) { item ->
                        AudioListItem(
                            item = item,
                            isPlaying = currentlyPlaying == item,
                            isEditLocked = isEditLocked,
                            onPlay = { viewModel.playAudio(item) },
                            onDelete = { viewModel.removeItemFromPlaylist(item, selectedPlaylist!!) },
                            onHide = { viewModel.hideRecording(item) },
                            onRename = { /* Use main rename dialog */ },
                            onAddToPlaylist = { /* Already in playlist */ },
                            onRemoveFromPlaylist = { viewModel.removeItemFromPlaylist(item, selectedPlaylist!!) },
                            onMoveUp = { viewModel.moveItemUp(item) },
                            onMoveDown = { viewModel.moveItemDown(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioListItem(
    item: AudioItem,
    isPlaying: Boolean,
    isEditLocked: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onRename: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPlay() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Audiotrack,
                contentDescription = null,
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium)
                Text(dateFormat.format(java.util.Date(item.dateAdded)), style = MaterialTheme.typography.labelSmall)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("목록에 추가") }, onClick = { showMenu = false; onAddToPlaylist() })
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(text = { Text("목록에서 제거") }, onClick = { showMenu = false; onRemoveFromPlaylist() })
                    }
                    DropdownMenuItem(text = { Text("이름 변경") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(text = { Text("숨기기") }, onClick = { showMenu = false; onHide() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("삭제") }, onClick = { showMenu = false; onDelete() })
                }
            }
            if (!isEditLocked) {
                Column {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
