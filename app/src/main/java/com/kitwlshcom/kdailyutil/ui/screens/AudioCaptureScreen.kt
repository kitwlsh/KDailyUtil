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
    val isRecording by viewModel.isRecording.collectAsState()
    val isPrepared by viewModel.isPrepared.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isEditLocked by viewModel.isEditLocked.collectAsState()
    val recordingSource by viewModel.recordingSource.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
    var showPlaylistMoveDialog by remember { mutableStateOf<AudioItem?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showHiddenManager by remember { mutableStateOf(false) }
    var showTrashManager by remember { mutableStateOf(false) }
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

    if (showTrashManager) {
        val trashFiles by viewModel.trashRecordings.collectAsState()
        AlertDialog(
            onDismissRequest = { showTrashManager = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("휴지통")
                }
            },
            text = {
                if (trashFiles.isEmpty()) {
                    Text("휴지통이 비어 있습니다.")
                } else {
                    Column {
                        TextButton(
                            onClick = { viewModel.emptyTrash() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("비우기", color = MaterialTheme.colorScheme.error)
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(trashFiles) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                                    Row {
                                        IconButton(onClick = { viewModel.restoreFromTrash(item) }) {
                                            Icon(Icons.Default.Restore, contentDescription = "복구", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.permanentlyDelete(item) }) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "영구 삭제", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrashManager = false }) { Text("닫기") }
            }
        )
    }

    if (showAddFilesDialog && selectedPlaylist != null) {
        val allFiles by viewModel.allRootFiles.collectAsState()
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
                    selectedPaths.forEach { path ->
                        val item = allFiles.find { it.path == path }
                        if (item != null) viewModel.addItemToPlaylist(item, selectedPlaylist!!)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 헤더 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "오디오 캡처",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedPlaylist != null) {
                        Text(
                            text = "📌 ${selectedPlaylist!!}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "전체 목록",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                val micColor = when {
                    isRecording -> Color.Red
                    isPrepared -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else if (isPrepared) {
                                viewModel.dismissPreparation()
                            } else {
                                if (recordingSource == RecordingSource.MIC) {
                                    viewModel.startRecording(null)
                                } else {
                                    projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                                }
                            }
                        },
                        shape = CircleShape,
                        color = micColor,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "파일 추가", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { viewModel.toggleRecordingSource() }) {
                        Icon(if (recordingSource == RecordingSource.MIC) Icons.Default.Mic else Icons.Default.GraphicEq, contentDescription = "소스 전환")
                    }

                    IconButton(onClick = { showHiddenManager = true }) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "숨김")
                    }

                    IconButton(onClick = { showTrashManager = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "휴지통")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 재생목록 섹션
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showNewPlaylistDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "새 리스트", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showPlaylistManager = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "리스트 관리", tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedPlaylist == null,
                            onClick = { viewModel.selectPlaylist(null) },
                            label = { Text("전체") }
                        )
                    }
                    items(playlists) { playlist ->
                        FilterChip(
                            selected = selectedPlaylist == playlist,
                            onClick = { viewModel.selectPlaylist(playlist) },
                            label = { Text(playlist) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 파일 리스트 섹션
            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (selectedPlaylist == null) "파일이 없습니다." else "비어 있는 재생목록입니다.")
                        if (selectedPlaylist != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showAddFilesDialog = true }) {
                                Text("곡 추가하기")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
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
                            },
                            onAddToPlaylist = { showPlaylistMoveDialog = item },
                            onRemoveFromPlaylist = if (selectedPlaylist != null) {
                                { viewModel.removeItemFromPlaylist(item, selectedPlaylist!!) }
                            } else null,
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
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
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
                Text(dateFormat.format(Date(item.dateAdded)), style = MaterialTheme.typography.labelSmall)
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
