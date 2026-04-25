package com.kitwlshcom.kdailyutil.ui.screens

import kotlinx.coroutines.launch
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import com.kitwlshcom.kdailyutil.audio.AudioCaptureService
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioCaptureViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.AudioTab
import com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode
import com.kitwlshcom.kdailyutil.ui.viewmodel.RecordingSource
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
    val allFiles by viewModel.allRootFiles.collectAsState()
    val hiddenFiles by viewModel.hiddenRecordings.collectAsState()
    val trashFiles by viewModel.trashRecordings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPrepared by viewModel.isPrepared.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isEditLocked by viewModel.isEditLocked.collectAsState()
    val recordingSource by viewModel.recordingSource.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showRenameDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showPlaylistMoveDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showAddFilesDialog by remember { mutableStateOf(false) }
    var showPlaylistManager by remember { mutableStateOf(false) }
    var showRenamePlaylistDialog by remember { mutableStateOf<String?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var newPlaylistName by remember { mutableStateOf("") }
    var editedPlaylistName by remember { mutableStateOf("") }

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.prepareRecording(result.data!!)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
        }
    }

    val playlistPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importPlaylistFile(uri)
        }
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

    // 다이얼로그들
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

    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("새 재생목록") },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("목록 이름") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.addPlaylist(newPlaylistName)
                        val name = newPlaylistName
                        newPlaylistName = ""
                        showNewPlaylistDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("'${name}' 재생목록이 생성되었습니다.")
                        }
                    }
                }) { Text("생성") }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylistDialog = false }) { Text("취소") }
            }
        )
    }

    if (showPlaylistMoveDialog != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistMoveDialog = null },
            title = { Text("목록에 추가") },
            text = {
                LazyColumn {
                    items(playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist) },
                            modifier = Modifier.clickable {
                                viewModel.addItemToPlaylist(showPlaylistMoveDialog!!, playlist)
                                showPlaylistMoveDialog = null
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistMoveDialog = null }) { Text("취소") }
            }
        )
    }
    if (showAddFilesDialog && selectedPlaylist != null) {
        val currentPaths = recordings.map { it.path }
        var selectedPaths by remember { mutableStateOf(setOf<String>()) }
        
        AlertDialog(
            onDismissRequest = { showAddFilesDialog = false },
            title = { Text("'${selectedPlaylist}'에 곡 추가") },
            text = {
                Column {
                    Text("목록에 추가할 파일을 선택하세요.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(allFiles) { item ->
                            val isAlreadyIn = item.path in currentPaths
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyIn) {
                                        selectedPaths = if (item.path in selectedPaths) {
                                            selectedPaths - item.path
                                        } else {
                                            selectedPaths + item.path
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAlreadyIn || item.path in selectedPaths,
                                    onCheckedChange = null,
                                    enabled = !isAlreadyIn
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, color = if (isAlreadyIn) Color.Gray else Color.Unspecified)
                                    if (isAlreadyIn) Text("이미 목록에 있음", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val selectedItems = allFiles.filter { it.path in selectedPaths }
                    if (selectedItems.isNotEmpty()) {
                        viewModel.addItemsToPlaylist(selectedItems, selectedPlaylist!!)
                    }
                    showAddFilesDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("${selectedPaths.size}곡이 추가되었습니다.")
                    }
                }) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFilesDialog = false }) { Text("취소") }
            }
        )
    }

    if (showPlaylistManager) {
        AlertDialog(
            onDismissRequest = { showPlaylistManager = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("재생목록 관리")
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { playlistPickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "목록 파일 불러오기")
                    }
                }
            },
            text = {
                if (playlists.isEmpty()) {
                    Text("생성된 재생목록이 없습니다.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    playlist, 
                                    modifier = Modifier.weight(1f).clickable { 
                                        viewModel.selectPlaylist(playlist)
                                        showPlaylistManager = false 
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedPlaylist == playlist) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPlaylist == playlist) MaterialTheme.colorScheme.primary else Color.Unspecified
                                )
                                Row {
                                    IconButton(onClick = { 
                                        editedPlaylistName = playlist
                                        showRenamePlaylistDialog = playlist 
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "이름 변경", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { viewModel.deletePlaylist(playlist) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistManager = false }) { Text("닫기") }
            }
        )
    }

    if (showRenamePlaylistDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenamePlaylistDialog = null },
            title = { Text("재생목록 이름 변경") },
            text = {
                TextField(
                    value = editedPlaylistName,
                    onValueChange = { editedPlaylistName = it },
                    label = { Text("새 이름") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editedPlaylistName.isNotBlank() && editedPlaylistName != showRenamePlaylistDialog) {
                        viewModel.renamePlaylist(showRenamePlaylistDialog!!, editedPlaylistName)
                        showRenamePlaylistDialog = null
                    }
                }) { Text("변경") }
            },
            dismissButton = {
                TextButton(onClick = { showRenamePlaylistDialog = null }) { Text("취소") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 사이드 탭 (NavigationRail)
            NavigationRail(
                modifier = Modifier.width(80.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                NavigationRailItem(
                    selected = activeTab == AudioTab.CAPTURE,
                    onClick = { viewModel.setActiveTab(AudioTab.CAPTURE) },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "녹음") },
                    label = { Text("녹음", fontSize = 10.sp) }
                )
                
                NavigationRailItem(
                    selected = activeTab == AudioTab.PLAYER,
                    onClick = { viewModel.setActiveTab(AudioTab.PLAYER) },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = "재생") },
                    label = { Text("재생", fontSize = 10.sp) }
                )
                
                NavigationRailItem(
                    selected = activeTab == AudioTab.FILES,
                    onClick = { viewModel.setActiveTab(AudioTab.FILES) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "파일") },
                    label = { Text("파일", fontSize = 10.sp) }
                )
                
                NavigationRailItem(
                    selected = activeTab == AudioTab.PLAYLISTS,
                    onClick = { viewModel.setActiveTab(AudioTab.PLAYLISTS) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "목록") },
                    label = { Text("목록", fontSize = 10.sp) }
                )

                Spacer(modifier = Modifier.weight(1f))
                
                // 소스 전환 버튼을 사이드 하단에 배치 (녹음 탭일 때만 표시)
                if (activeTab == AudioTab.CAPTURE) {
                    IconButton(onClick = { viewModel.toggleRecordingSource() }) {
                        Icon(
                            if (recordingSource == RecordingSource.MIC) Icons.Default.Mic else Icons.Default.GraphicEq, 
                            contentDescription = "소스"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 메인 콘텐츠 영역
            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = activeTab, animationSpec = tween(300), label = "tabTransition") { tab ->
                    when (tab) {
                        AudioTab.CAPTURE -> CaptureTabContent(
                            viewModel = viewModel,
                            onStartInternalRecording = {
                                projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            }
                        )
                        AudioTab.PLAYER -> PlayerTabContent(viewModel)
                        AudioTab.FILES -> FileManagerTabContent(
                            viewModel = viewModel,
                            onRenameClick = { 
                                newFileName = it.name.substringBeforeLast(".")
                                showRenameDialog = it 
                            },
                            onDeleteClick = { showDeleteConfirmDialog = it },
                            onPlaylistClick = { showPlaylistMoveDialog = it }
                        )
                        AudioTab.PLAYLISTS -> PlaylistTabContent(
                            viewModel = viewModel,
                            onCreateClick = { showNewPlaylistDialog = true },
                            onManageClick = { showPlaylistManager = true },
                            onAddClick = { showAddFilesDialog = true }
                        )
                    }
                }
            }
        }
    }
}
