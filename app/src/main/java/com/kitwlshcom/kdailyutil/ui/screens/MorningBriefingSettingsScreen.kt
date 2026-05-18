package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.BriefingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningBriefingSettingsScreen(
    viewModel: BriefingViewModel = viewModel()
) {
    val keywords by viewModel.keywords.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val briefingTime by viewModel.briefingTime.collectAsState()
    val isEnabled by viewModel.isBriefingEnabled.collectAsState()
    val apiKey by viewModel.geminiApiKey.collectAsState()
    val aiCommands by viewModel.aiBriefingCommands.collectAsState()
    val stockKeywords by viewModel.stockKeywords.collectAsState()
    val aiAudioPath by viewModel.aiCommandAudioPath.collectAsState()
    val isRecording by viewModel.isRecordingCommand.collectAsState()
    val sttPartialText by viewModel.sttPartialText.collectAsState()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsState()
    
    val apiKeyStatusColor = when (apiKeyStatus) {
        is com.kitwlshcom.kdailyutil.ui.viewmodel.ApiKeyStatus.Valid -> MaterialTheme.colorScheme.primary
        is com.kitwlshcom.kdailyutil.ui.viewmodel.ApiKeyStatus.Invalid -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    var localApiKey by remember { mutableStateOf(apiKey ?: "") }
    
    // 외부(DataStore)에서 값이 변경되었을 때만 로컬 동기화
    LaunchedEffect(apiKey) {
        if (localApiKey != (apiKey ?: "")) {
            localApiKey = apiKey ?: ""
        }
    }

    var newKeyword by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newStockKeyword by remember { mutableStateOf("") }
    var newAiCommand by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp) // 네비게이션 바 고려
            .verticalScroll(rememberScrollState())
    ) {
        Text("오전 브리핑 설정", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // 브리핑 활성화 스위치
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("자동 브리핑 사용", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = isEnabled, onCheckedChange = { viewModel.toggleBriefing(it) })
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // 시간 설정
        Text("브리핑 시간", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { showTimePicker = true }) {
            Text(
                text = String.format("%02d:%02d", briefingTime.first, briefingTime.second),
                style = MaterialTheme.typography.displaySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 뉴스 카테고리 관리
        Text("뉴스 카테고리 (사이드 탭)", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCategory,
                onValueChange = { newCategory = it },
                label = { Text("카테고리 추가 (예: 스포츠)") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val cleanedCategory = newCategory.trim()
                val fixedCategories = setOf("전체", "증시", "AI")
                if (cleanedCategory.isNotBlank() && cleanedCategory !in fixedCategories && cleanedCategory !in categories) {
                    viewModel.updateCategories(categories + cleanedCategory)
                    newCategory = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "추가")
            }
        }

        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(categories.toList()) { category ->
                InputChip(
                    selected = true,
                    onClick = { },
                    label = { Text(category) },
                    trailingIcon = {
                        if (category !in setOf("전체", "증시", "AI")) {
                            IconButton(
                                onClick = { viewModel.updateCategories(categories - category) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "삭제",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 키워드 관리
        Text("관심 키워드 (브리핑용)", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newKeyword,
                onValueChange = { newKeyword = it },
                label = { Text("키워드 추가") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (newKeyword.isNotBlank()) {
                    viewModel.updateKeywords(keywords + newKeyword)
                    newKeyword = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "추가")
            }
        }

        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(keywords.toList()) { keyword ->
                InputChip(
                    selected = true,
                    onClick = { },
                    label = { Text(keyword) },
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.updateKeywords(keywords - keyword) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "삭제",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 증시 키워드 관리
        Text("관심 증시/종목 (증시 서브 탭)", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newStockKeyword,
                onValueChange = { newStockKeyword = it },
                label = { Text("키워드 추가 (예: 테슬라, 비트코인)") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (newStockKeyword.isNotBlank()) {
                    viewModel.updateStockKeywords(stockKeywords + newStockKeyword)
                    newStockKeyword = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "추가")
            }
        }

        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(stockKeywords.toList()) { keyword ->
                InputChip(
                    selected = true,
                    onClick = { },
                    label = { Text(keyword) },
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.updateStockKeywords(stockKeywords - keyword) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "삭제",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Gemini API Key
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gemini API Key", style = MaterialTheme.typography.titleMedium)
            if (apiKeyStatus is com.kitwlshcom.kdailyutil.ui.viewmodel.ApiKeyStatus.Validating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = { viewModel.validateApiKey() }) {
                    Text("연결 테스트")
                }
            }
        }
        
        var isKeyVisible by remember { mutableStateOf(false) }
        
        OutlinedTextField(
            value = localApiKey,
            onValueChange = { 
                localApiKey = it
                viewModel.updateApiKey(it) 
            },
            label = { Text("API Key를 입력하세요") },
            visualTransformation = if (isKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = apiKeyStatus is com.kitwlshcom.kdailyutil.ui.viewmodel.ApiKeyStatus.Invalid,
            trailingIcon = {
                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                    Icon(
                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isKeyVisible) "비밀번호 숨기기" else "비밀번호 보기"
                    )
                }
            }
        )

        // 검증 상태 표시
        val statusText = when (val status = apiKeyStatus) {
            is com.kitwlshcom.kdailyutil.ui.viewmodel.ApiKeyStatus.Valid -> status.message
            is com.kitwlshcom.kdailyutil.ui.viewmodel.ApiKeyStatus.Invalid -> status.error
            else -> ""
        }
        
        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = apiKeyStatusColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // AI 커스텀 브리핑 명령어 설정
        Text("나만의 AI 브리핑 명령어", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "AI가 뉴스를 분석할 때 참고할 특별한 요청사항을 입력하거나 음성으로 등록하세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newAiCommand,
                onValueChange = { newAiCommand = it },
                label = { Text("예: 나스닥 시황 알려줘") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (newAiCommand.isNotBlank()) {
                    viewModel.updateAiCommands(aiCommands + newAiCommand)
                    newAiCommand = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "추가")
            }
        }

        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(aiCommands.toList()) { command ->
                InputChip(
                    selected = true,
                    onClick = { },
                    label = { Text(command) },
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.updateAiCommands(aiCommands - command) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "삭제",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 녹음 버튼
            Button(
                onClick = {
                    if (isRecording) viewModel.stopCommandRecording()
                    else viewModel.startCommandRecording()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRecording) "녹음 중지" else "음성으로 등록")
            }

            // 다시 듣기 버튼
            if (aiAudioPath.isNotBlank()) {
                OutlinedIconButton(
                    onClick = { viewModel.playCommandAudio() }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "명령어 듣기")
                }
            }
        }
        
        if (isRecording || sttPartialText.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        if (isRecording) "🎙 음성 인식 중..." else "마지막 인식 결과",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (sttPartialText.isBlank() && isRecording) "말씀해 주세요..." else sttPartialText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = briefingTime.first,
            initialMinute = briefingTime.second
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateBriefingTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("취소") }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}
