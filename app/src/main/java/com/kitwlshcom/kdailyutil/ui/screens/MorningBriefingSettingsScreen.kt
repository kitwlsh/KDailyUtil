package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val briefingTime by viewModel.briefingTime.collectAsState()
    val isEnabled by viewModel.isBriefingEnabled.collectAsState()
    val apiKey by viewModel.geminiApiKey.collectAsState()

    var newKeyword by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp) // 네비게이션 바 고려
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

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // 시간 설정
        Text("브리핑 시간", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { showTimePicker = true }) {
            Text(
                text = String.format("%02d:%02d", briefingTime.first, briefingTime.second),
                style = MaterialTheme.typography.displaySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 키워드 관리
        Text("관심 키워드", style = MaterialTheme.typography.titleMedium)
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

        // Gemini API Key
        Text("Gemini API Key", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = apiKey ?: "",
            onValueChange = { viewModel.updateApiKey(it) },
            label = { Text("API Key를 입력하세요") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
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
