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
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val audioCopyrightAccepted by viewModel.audioCopyrightAccepted.collectAsState()

    var showPlayerSheet by remember { mutableStateOf(false) }
    var showCopyrightDialog by remember { mutableStateOf(false) }
    var copyrightDoNotShowAgain by remember { mutableStateOf(false) }
    var onConfirmRecordingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showRenameDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showPlaylistMoveDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showInfoDialog by remember { mutableStateOf<AudioItem?>(null) }
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

    // SAF 폴더 선택 → 옛 공용 폴더(예: Download/KDailyUtil)의 녹음을 권한 없이 복구
    val folderRecoverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.recoverFromFolder(uri)
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
    if (showCopyrightDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCopyrightDialog = false 
                onConfirmRecordingAction = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("저작권 및 법적 주의사항 고지", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "본 캡처 기능으로 녹음된 오디오 파일은 저작권법 제30조(사적이용을 위한 복제)에 따라 개인적인 용도로만 사용해야 합니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "저작권자의 동의 없이 녹음된 파일을 외부로 공유, 전송, 배포하거나 상업적으로 이용하는 행위는 저작권 침해로 처벌받을 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "또한, 타인의 대화를 무단으로 녹음하는 행위는 통신비밀보호법에 의거하여 형사 처벌을 받을 수 있으므로 주의하시기 바랍니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { copyrightDoNotShowAgain = !copyrightDoNotShowAgain }
                    ) {
                        Checkbox(
                            checked = copyrightDoNotShowAgain,
                            onCheckedChange = { copyrightDoNotShowAgain = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("동의하며, 다시 표시하지 않기", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (copyrightDoNotShowAgain) {
                            viewModel.acceptAudioCopyright()
                        }
                        showCopyrightDialog = false
                        onConfirmRecordingAction?.invoke()
                        onConfirmRecordingAction = null
                    }
                ) {
                    Text("동의 및 계속")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCopyrightDialog = false
                        onConfirmRecordingAction = null
                    }
                ) {
                    Text("취소")
                }
            }
        )
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

    if (showInfoDialog != null) {
        val item = showInfoDialog!!
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            title = { Text("상세 정보") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("파일명: ${item.name}", fontWeight = FontWeight.Bold)
                    Text("경로: ${item.path}", fontSize = 12.sp, color = Color.Gray)
                    Text("크기: ${String.format("%.2f MB", item.size / (1024f * 1024f))}")
                    Text("날짜: ${dateFormat.format(Date(item.dateAdded))}")
                    Text("길이: ${String.format("%02d:%02d", (item.duration/1000)/60, (item.duration/1000)%60)}")
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = null }) { Text("확인") }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 사이드 탭 (NavigationRail)
            NavigationRail(
                modifier = Modifier.width(84.dp),
                containerColor = Color.Black.copy(alpha = 0.4f) // 사이드 배경 확보
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                NavigationRailItem(
                    selected = activeTab == AudioTab.CAPTURE,
                    onClick = { viewModel.setActiveTab(AudioTab.CAPTURE) },
                    icon = { 
                        Icon(
                            Icons.Default.Mic, 
                            contentDescription = "녹음",
                            tint = if (activeTab == AudioTab.CAPTURE) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    label = { 
                        Text(
                            "녹음", 
                            fontSize = 11.sp,
                            color = if (activeTab == AudioTab.CAPTURE) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.15f)
                    )
                )
                
                NavigationRailItem(
                    selected = activeTab == AudioTab.FILES,
                    onClick = { viewModel.setActiveTab(AudioTab.FILES) },
                    icon = { 
                        Icon(
                            Icons.Default.Folder, 
                            contentDescription = "파일",
                            tint = if (activeTab == AudioTab.FILES) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    label = { 
                        Text(
                            "파일", 
                            fontSize = 11.sp,
                            color = if (activeTab == AudioTab.FILES) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.15f)
                    )
                )
                
                NavigationRailItem(
                    selected = activeTab == AudioTab.PLAYLISTS,
                    onClick = { viewModel.setActiveTab(AudioTab.PLAYLISTS) },
                    icon = { 
                        Icon(
                            Icons.AutoMirrored.Filled.List, 
                            contentDescription = "목록",
                            tint = if (activeTab == AudioTab.PLAYLISTS) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    label = { 
                        Text(
                            "목록", 
                            fontSize = 11.sp,
                            color = if (activeTab == AudioTab.PLAYLISTS) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.weight(1f))
                
                // 소스 전환 버튼을 사이드 하단에 배치 (녹음 탭일 때만 표시)
                if (activeTab == AudioTab.CAPTURE) {
                    IconButton(onClick = { viewModel.toggleRecordingSource() }) {
                        Icon(
                            if (recordingSource == RecordingSource.MIC) Icons.Default.Mic else Icons.Default.GraphicEq, 
                            contentDescription = "소스",
                            tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            VerticalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 메인 콘텐츠 영역
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 선택 모드 헤더
                    if (isSelectionMode) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                        Icon(Icons.Default.Close, contentDescription = "취소")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${selectedPaths.size}개 선택됨", fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(targetState = activeTab, animationSpec = tween(300), label = "tabTransition") { tab ->
                            when (tab) {
                                AudioTab.CAPTURE -> CaptureTabContent(
                                    viewModel = viewModel,
                                    audioCopyrightAccepted = audioCopyrightAccepted,
                                    onShowCopyrightDialog = { onStartRecord ->
                                        onConfirmRecordingAction = onStartRecord
                                        showCopyrightDialog = true
                                    },
                                    onStartInternalRecording = {
                                        projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                                    }
                                )
                                AudioTab.FILES -> FileManagerTabContent(
                                    viewModel = viewModel,
                                    onRenameClick = {
                                        newFileName = it.name.substringBeforeLast(".")
                                        showRenameDialog = it
                                    },
                                    onDeleteClick = { showDeleteConfirmDialog = it },
                                    onPlaylistClick = { showPlaylistMoveDialog = it },
                                    onInfoClick = { showInfoDialog = it },
                                    onImportFiles = { filePickerLauncher.launch("audio/*") },
                                    onRecoverFolder = { folderRecoverLauncher.launch(null) }
                                )
                                AudioTab.PLAYLISTS -> PlaylistTabContent(
                                    viewModel = viewModel,
                                    onCreateClick = { showNewPlaylistDialog = true },
                                    onManageClick = { showPlaylistManager = true },
                                    onAddClick = { showAddFilesDialog = true },
                                    onInfoClick = { showInfoDialog = it }
                                )
                            }
                        }
                    }
                    
                    // 하단 미니 플레이어
                    MiniPlayerContent(
                        viewModel = viewModel,
                        onClick = { showPlayerSheet = true }
                    )
                }
            }
        }
    }

    // 재생 상세 바텀 시트
    if (showPlayerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FullPlayerSheetContent(viewModel = viewModel)
        }
    }
}
