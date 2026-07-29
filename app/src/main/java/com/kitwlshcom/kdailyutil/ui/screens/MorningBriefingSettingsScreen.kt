package com.kitwlshcom.kdailyutil.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kitwlshcom.kdailyutil.R
import com.kitwlshcom.kdailyutil.data.model.FamilyApp
import com.kitwlshcom.kdailyutil.data.repository.FamilyRepository
import com.kitwlshcom.kdailyutil.ui.viewmodel.BriefingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningBriefingSettingsScreen(
    viewModel: BriefingViewModel = viewModel()
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    val keywords by viewModel.keywords.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val briefingTime by viewModel.briefingTime.collectAsState()
    val isEnabled by viewModel.isBriefingEnabled.collectAsState()
    val apiKey by viewModel.geminiApiKey.collectAsState()
    val aiCommands by viewModel.aiBriefingCommands.collectAsState()
    val stockKeywords by viewModel.stockKeywords.collectAsState()
    val watchStockKeywords by viewModel.watchStockKeywords.collectAsState()
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
    val dartApiKey by viewModel.dartApiKey.collectAsState()
    var localDartApiKey by remember { mutableStateOf(dartApiKey) }
    
    // 외부(DataStore)에서 값이 변경되었을 때만 로컬 동기화
    LaunchedEffect(apiKey) {
        if (localApiKey != (apiKey ?: "")) {
            localApiKey = apiKey ?: ""
        }
    }

    LaunchedEffect(dartApiKey) {
        if (localDartApiKey != dartApiKey) {
            localDartApiKey = dartApiKey
        }
    }

    var newKeyword by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newStockKeyword by remember { mutableStateOf("") }
    var newWatchKeyword by remember { mutableStateOf("") }
    var newAiCommand by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDartHelpDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showIconGalleryDialog by remember { mutableStateOf(false) }
    var showLegalNoticeDialog by remember { mutableStateOf(false) }
    var showFullScreenIcon by remember { mutableStateOf<Int?>(null) }
    var selectedSettingsTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp) // 네비게이션 바 고려
    ) {
        Text("오전 브리핑 설정", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        val settingsTabs = listOf("브리핑", "증시", "AI·키", "화면", "앱정보")
        ScrollableTabRow(selectedTabIndex = selectedSettingsTab) {
            settingsTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSettingsTab == index,
                    onClick = { selectedSettingsTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {

        // ===== 브리핑 탭 (0) =====
        if (selectedSettingsTab == 0) {
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

        ReorderableChipRow(
            items = categories,
            onReorder = { viewModel.updateCategories(it) },
            onDelete = { viewModel.updateCategories(categories - it) },
            fixedItems = setOf("전체", "증시", "AI")
        )

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

        ReorderableChipRow(
            items = keywords,
            onReorder = { viewModel.updateKeywords(it) },
            onDelete = { viewModel.updateKeywords(keywords - it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        } // end 브리핑 탭 (0) - 키워드까지

        // ===== 증시 탭 (1) =====
        if (selectedSettingsTab == 1) {
        // ── 섹션 1: 뉴스탭 증시 서브탭 필터 키워드 (STOCK_KEYWORDS) ──
        Text("📰 뉴스탭 · 증시 키워드", style = MaterialTheme.typography.titleMedium, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
        Text(
            "‘뉴스’ 탭 > 증시 서브탭에 표시되는 뉴스 필터 키워드입니다. 지수·해외·가상자산(예: 나스닥·테슬라·비트코인)도 자유롭게 넣을 수 있어요.\n" +
                "※ 아래 ‘증시 대시보드 관심종목’과는 서로 다른 목록입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
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

        ReorderableChipRow(
            items = stockKeywords,
            onReorder = { viewModel.updateStockKeywords(it) },
            onDelete = { viewModel.updateStockKeywords(stockKeywords - it) }
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        // ── 섹션 2: 증시 대시보드 관심종목 (WATCH_STOCK_KEYWORDS) ──
        Text("📈 증시 대시보드 관심종목", style = MaterialTheme.typography.titleMedium, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
        Text(
            "‘증시’ 탭의 시세·차트와 실적 뉴스·전망에 나오는 종목입니다. (증시 탭 > 시세 및 차트에서도 관리할 수 있어요)\n" +
                "※ 시세 조회를 위해 실제 종목명/티커를 권장합니다(예: 삼성전자, AAPL, 비트코인). 실적 뉴스·전망에는 이 중 ‘한국 상장사’만 표시됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newWatchKeyword,
                onValueChange = { newWatchKeyword = it },
                label = { Text("종목 추가 (예: 삼성전자, AAPL)") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (newWatchKeyword.isNotBlank()) {
                    viewModel.updateWatchStockKeywords(watchStockKeywords + newWatchKeyword.trim())
                    newWatchKeyword = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "추가")
            }
        }

        ReorderableChipRow(
            items = watchStockKeywords,
            onReorder = { viewModel.updateWatchStockKeywords(it) },
            onDelete = { viewModel.updateWatchStockKeywords(watchStockKeywords - it) }
        )

        } // end 증시 탭 (1)

        // ===== 브리핑 탭 (0) - 기본 뉴스 서비스 설정 =====
        if (selectedSettingsTab == 0) {
        // 기본 뉴스 서비스 설정
        Text("기본 뉴스 서비스 설정", style = MaterialTheme.typography.titleMedium, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. 자동 새로고침 주기 설정
        val autoRefreshInterval by viewModel.autoRefreshIntervalHours.collectAsState()
        var showIntervalDropdown by remember { mutableStateOf(false) }
        val intervalOptions = listOf(
            Pair(0, "안 함"),
            Pair(1, "1시간"),
            Pair(2, "2시간 (추천)"),
            Pair(3, "3시간"),
            Pair(5, "5시간"),
            Pair(12, "12시간"),
            Pair(24, "24시간")
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("자동 새로고침 주기", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "설정 시간 경과 후 앱 실행 시 뉴스를 자동으로 갱신합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box {
                val currentText = intervalOptions.find { it.first == autoRefreshInterval }?.second ?: "${autoRefreshInterval}시간"
                OutlinedButton(
                    onClick = { showIntervalDropdown = true },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.5f))
                ) {
                    Text(currentText, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                }
                DropdownMenu(
                    expanded = showIntervalDropdown,
                    onDismissRequest = { showIntervalDropdown = false }
                ) {
                    intervalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.second) },
                            onClick = {
                                viewModel.updateAutoRefreshInterval(option.first)
                                showIntervalDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 뉴스 노출 개수 설정
        val limitValue by viewModel.newsLimit.collectAsState()
        var showLimitDropdown by remember { mutableStateOf(false) }
        val limitOptions = listOf(10, 20, 30, 50)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("기사 표시 개수", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "뉴스 탭당 불러올 기사의 개수를 설정합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box {
                OutlinedButton(
                    onClick = { showLimitDropdown = true },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.5f))
                ) {
                    Text("${limitValue}개", color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                }
                DropdownMenu(
                    expanded = showLimitDropdown,
                    onDismissRequest = { showLimitDropdown = false }
                ) {
                    limitOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option}개") },
                            onClick = {
                                viewModel.updateNewsLimit(option)
                                showLimitDropdown = false
                            }
                        )
                    }
                }
            }
        }

        } // end 브리핑 탭 (0) - 기본 뉴스 서비스 설정

        // ===== 화면 탭 (3) =====
        if (selectedSettingsTab == 3) {
        // 3. 스플래시 화면 테마 설정
        val splashTheme by viewModel.splashTheme.collectAsState()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("스플래시 화면 디자인", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                Text(
                    "앱 실행 시 시작 화면의 프리미엄 연출 방식을 선택합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 프리미엄 2주기 반사광 옵션
            val isShimmer = splashTheme == "shimmer"
            OutlinedButton(
                onClick = { viewModel.updateSplashTheme("shimmer") },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isShimmer) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.12f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isShimmer) com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.08f) else Color.Transparent
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isShimmer) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "2주기 사선 반사광",
                        fontWeight = if (isShimmer) FontWeight.Bold else FontWeight.Normal,
                        color = if (isShimmer) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }

            // 유성 스파이럴 궤도 옵션
            val isMeteor = splashTheme == "meteor"
            OutlinedButton(
                onClick = { viewModel.updateSplashTheme("meteor") },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isMeteor) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.12f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isMeteor) com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.08f) else Color.Transparent
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isMeteor) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "유성 스파이럴 궤도",
                        fontWeight = if (isMeteor) FontWeight.Bold else FontWeight.Normal,
                        color = if (isMeteor) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        } // end 화면 탭 (3)

        // ===== AI·키 탭 (2) =====
        if (selectedSettingsTab == 2) {
        // Gemini API Key
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gemini API Key", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "도움말",
                        tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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

        // Open DART API Key
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Open DART API Key (선택)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { showDartHelpDialog = true }, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "DART 키 발급 도움말",
                    tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            "미입력 시 기본 공용 키가 제공되며, 한도 초과 시 개인 키를 발급받아 등록할 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        var isDartKeyVisible by remember { mutableStateOf(false) }
        
        OutlinedTextField(
            value = localDartApiKey,
            onValueChange = { 
                localDartApiKey = it
                viewModel.updateDartApiKey(it) 
            },
            placeholder = { Text("기본 공용 키 사용 중 (비어있음)") },
            visualTransformation = if (isDartKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { isDartKeyVisible = !isDartKeyVisible }) {
                    Icon(
                        imageVector = if (isDartKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isDartKeyVisible) "비밀번호 숨기기" else "비밀번호 보기"
                    )
                }
            }
        )

        if (showDartHelpDialog) {
            val dartUriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            AlertDialog(
                onDismissRequest = { showDartHelpDialog = false },
                title = { Text("🔑 Open DART API Key 발급 안내", fontWeight = FontWeight.Bold, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "증시 탭의 '실적 공시·재무 데이터'는 금융감독원 Open DART를 사용합니다. 기본 공용 키가 내장돼 있어 그냥 쓸 수 있지만, 여러 사용자가 함께 쓰다 보니 한도 초과 시 조회가 막힐 수 있어요. 이때 개인 키(무료)를 발급받아 등록하면 안정적으로 이용할 수 있습니다.",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.sp
                        )
                        Text("💡 발급 방법 (무료)", fontWeight = FontWeight.Bold, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K, fontSize = 13.sp)
                        Text(
                            "1. 아래 [Open DART 발급 사이트 이동] 버튼 클릭\n2. 회원가입 후 '인증키 신청/관리 > 오픈API 인증키 신청'에서 신청\n3. 발급된 인증키(40자리)를 복사해 위 입력칸에 붙여넣기",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f), lineHeight = 20.sp
                        )
                        Text(
                            "⚠️ 발급받은 키는 이 기기에만 안전하게 저장되며 외부로 유출되지 않습니다.",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), lineHeight = 17.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dartUriHandler.openUri("https://opendart.fss.or.kr/")
                            showDartHelpDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                            contentColor = Color.Black
                        )
                    ) { Text("Open DART 발급 사이트 이동", fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { showDartHelpDialog = false }) { Text("닫기") } }
            )
        }

        } // end AI·키 탭 (2)

        // ===== 브리핑 탭 (0) - AI 커스텀 브리핑 명령어 =====
        if (selectedSettingsTab == 0) {
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

        ReorderableChipRow(
            items = aiCommands,
            onReorder = { viewModel.updateAiCommands(it) },
            onDelete = { viewModel.updateAiCommands(aiCommands - it) }
        )

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

        } // end 브리핑 탭 (0) - AI 명령어

        // ===== 앱정보 탭 (4) =====
        if (selectedSettingsTab == 4) {
        Text("앱 정보 및 라이선스", style = MaterialTheme.typography.titleMedium, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedCard(
                onClick = { showAppInfoDialog = true },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.White.copy(alpha = 0.02f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("앱 소개 및 버전", fontWeight = FontWeight.Bold)
                        Text("KDailyUtil 정보 및 개발자 문의", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            OutlinedCard(
                onClick = { showIconGalleryDialog = true },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.White.copy(alpha = 0.02f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("브랜드 & 자매앱", fontWeight = FontWeight.Bold)
                        Text("앱 로고 갤러리 + K-시리즈 자매앱", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            OutlinedCard(
                onClick = { showLegalNoticeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.3f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.White.copy(alpha = 0.02f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("법적 고지 및 면책조항", fontWeight = FontWeight.Bold)
                        Text("뉴스/증시/오디오/퀴즈 관련 법적 주의사항", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "버전 $versionName (KDailyUtil)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
        } // end 앱정보 탭 (4)

        } // end inner scrollable Column
    }

    if (showHelpDialog) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "🔑 Gemini API Key 무료 발급 가이드",
                    fontWeight = FontWeight.Bold,
                    color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "KDailyUtil의 스마트 AI 브리핑은 구글의 공식 Gemini AI를 사용하며, API 키를 등록하면 평생 무료로 분석 요약을 이용하실 수 있습니다.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "💡 3단계 초간단 발급 방법:",
                                fontWeight = FontWeight.Bold,
                                color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("1. 아래 [무료 발급 사이트 이동] 버튼을 클릭합니다.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("2. Google AI Studio에서 [Create API Key] 버튼을 누릅니다.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("3. 생성된 키(AIzaSy...)를 복사한 뒤, 설정창에 붙여넣고 [연결 테스트]를 진행하세요!", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    
                    Text(
                        "⚠️ 주의: 발급받으신 API 키는 개인 기기에만 안전하게 암호화되어 저장되며, 외부로 절대 유출되지 않습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri("https://aistudio.google.com/app/apikey")
                        } catch (e: Exception) {
                            // fallback or error logging
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        contentColor = Color.Black
                    )
                ) {
                    Text("무료 발급 사이트 이동", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("닫기", color = Color.White)
                }
            }
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

    if (showAppInfoDialog) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("앱 소개 및 정보", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.05f), shape = MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            // 앱 소개에는 '해당 앱의 실제 아이콘'(엠블럼+톱니/나침반)을 노출
                            painter = painterResource(id = R.drawable.ic_k_app_icon),
                            contentDescription = "Logo",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    
                    Text(
                        "KDailyUtil",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                    )
                    Text(
                        "Premium Lifestyle Utility Series",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row {
                            Text("현재 버전: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(versionName, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Row {
                            Text("개발자: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("KitwLSH", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Row {
                            Text("개발자 문의: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("kitwlsh@gmail.com", fontSize = 13.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K, modifier = Modifier.clickable {
                                try {
                                    uriHandler.openUri("mailto:kitwlsh@gmail.com")
                                } catch (e: Exception) {}
                            })
                        }
                    }
                    
                    Text(
                        "KDailyUtil은 데일리 라이프스타일을 더욱 스마트하고 편리하며 고급스럽게 만들어주는 통합 유틸리티 앱입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            com.kitwlshcom.kdailyutil.ui.theme.Gold24K.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "💡 AI 기술 활용 안내",
                                fontWeight = FontWeight.Bold,
                                color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "본 앱은 뉴스 기사의 대화체 요약 및 퀴즈 생성 품질 향상을 위해 Google Gemini AI 모델을 활용합니다.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        contentColor = Color.Black
                    )
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showIconGalleryDialog) {
        AlertDialog(
            onDismissRequest = { showIconGalleryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("브랜드 & 자매앱", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "K-시리즈의 통일성 및 프리미엄 이미지를 위해 설계된 Brand Emblem + Feature Hero 전략 기반의 공식 리소스 리스트입니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            "🔍 자체 로고 카드는 전체 화면 보기, 자매앱 카드는 스토어로 이동합니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                        )
                    }
                    
                    // 1. 3D 엠블럼
                    Card(
                        onClick = { showFullScreenIcon = R.drawable.ic_k_logo_3d },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_k_logo_3d),
                                contentDescription = "3D Emblem",
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("K-Brand 3D Hexagon Emblem", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "패밀리 로고이자 정품 인증 마크처럼 기능하는 3D 입체 육각형 골드 엠블럼입니다.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // 2. 앱 아이콘 (엠블럼 + 톱니/나침반) — 런처·스플래시·워터마크 공통
                    Card(
                        onClick = { showFullScreenIcon = R.drawable.ic_k_app_icon },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_k_app_icon),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("KDailyUtil 앱 아이콘", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "3D 골드 육각 엠블럼과 앱 정체성을 보여주는 히어로 심볼(톱니바퀴 및 나침반)이 결합된 공식 앱 아이콘입니다. 런처·스플래시·배경 워터마크에 동일하게 사용됩니다.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // ── 구획: 자기 로고(위) / 자매앱(아래) 시각 분리 ──
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // 3. 자매앱 — 원격 레지스트리(family.json)로 동적 렌더 (2026-07-29, §8)
                    //    목록이 앱에 하드코딩돼 있지 않으므로 새 자매앱이 생겨도 이 앱을 재배포할 필요가 없다.
                    var familyReloadKey by remember { mutableStateOf(0) }
                    val sisterApps by produceState<List<FamilyApp>?>(null, familyReloadKey) {
                        value = FamilyRepository.loadSisterApps(context, forceRefresh = familyReloadKey > 0)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("🧩 K-시리즈 자매앱", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                            Text(
                                "같은 제작사(KITWLSH)의 다른 앱입니다. 카드를 누르면 스토어로 이동하거나 설치된 앱을 실행합니다. 목록은 온라인에서 자동 갱신됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { familyReloadKey++ }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "자매앱 목록 새로고침",
                                tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                            )
                        }
                    }

                    val apps = sisterApps
                    when {
                        apps == null -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                            )
                            Text(
                                "자매앱 목록을 불러오는 중…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        apps.isEmpty() -> Text(
                            "표시할 자매앱이 없습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        else -> apps.forEach { app -> SisterAppCard(app) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showIconGalleryDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        contentColor = Color.Black
                    )
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showLegalNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showLegalNoticeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("법적 고지 및 면책조항", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // 1. 뉴스 요약
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📰 뉴스 키워드 브리핑 (Morning Briefing)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                        Text(
                            "본 서비스의 뉴스 요약은 구글 Gemini AI 모델을 통한 기계적 결과물입니다. 요약 과정에서 일부 오류가 있을 수 있으며 사실 관계를 완벽히 보증하지 않으므로 참고용으로만 활용해 주시기 바랍니다.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // 2. 증시 정보
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📈 증시 카테고리 (Morning Stocks)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                        Text(
                            "관련 증시 정보 및 뉴스는 실시간 시세와 수 분 내지 수 시간의 지연이 있을 수 있으며, 어떠한 투자 결과에 대해서도 책임을 지지 않습니다.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // 3. 오디오 캡처
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔊 오디오 캡처 및 관리 (Audio Capture)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                        Text(
                            "시스템 오디오 캡처를 포함한 녹음 파일은 저작권법 제30조(사적이용을 위한 복제)의 범위 내에서 본인만 사용해야 하며, 타인에게 공유/전송 시 처벌받을 수 있습니다. 또한, 통신비밀보호법에 의거 타인 간의 대화를 무단 청취/녹음 시 형사 처벌 대상이 됩니다.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // 4. AI 퀴즈
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🧠 AI 퀴즈 및 창작 플랫폼 (KuizGenius)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K)
                        Text(
                            "사용자가 이미지를 스캔하거나 크롤링하여 개별적으로 작성한 커스텀 퀴즈 패키지 및 크롭된 사진 자산은 로컬 기기에만 저장되며, 이를 공유 파일(.kquiz) 형태로 외부 유포하여 발생하는 모든 저작권 책임은 사용자 본인에게 있습니다.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLegalNoticeDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                        contentColor = Color.Black
                    )
                ) {
                    Text("동의 및 확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showFullScreenIcon != null) {
        AlertDialog(
            onDismissRequest = { showFullScreenIcon = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            ),
            title = null,
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { showFullScreenIcon = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = showFullScreenIcon!!),
                            contentDescription = "Full Screen Icon",
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "화면 아무 곳이나 누르면 닫힙니다.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = null,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        )
    }
}

/**
 * 순서 변경이 가능한 키워드/카테고리 칩 목록. (2026-07-20)
 * 칩을 누르면 '◀ 앞으로 / ▶ 뒤로 / 삭제' 메뉴가 열린다.
 * fixedItems 는 이동·삭제 불가(예: 뉴스 카테고리의 "전체"·"증시"·"AI").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderableChipRow(
    items: List<String>,
    onReorder: (List<String>) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    fixedItems: Set<String> = emptySet()
) {
    LazyRow(modifier = modifier.padding(vertical = 8.dp)) {
        items(items, key = { it }) { item ->
            val index = items.indexOf(item)
            val isFixed = item in fixedItems
            var expanded by remember(item) { mutableStateOf(false) }
            Box(modifier = Modifier.padding(end = 4.dp)) {
                InputChip(
                    selected = true,
                    onClick = { if (!isFixed) expanded = true },
                    label = { Text(item) },
                    trailingIcon = {
                        if (!isFixed) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "순서 변경 / 삭제",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
                if (!isFixed) {
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        val canLeft = index > 0 && items[index - 1] !in fixedItems
                        val canRight = index in 0 until items.lastIndex
                        DropdownMenuItem(
                            text = { Text("앞으로 이동") },
                            enabled = canLeft,
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) },
                            onClick = { onReorder(items.movedItem(index, index - 1)); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("뒤로 이동") },
                            enabled = canRight,
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                            onClick = { onReorder(items.movedItem(index, index + 1)); expanded = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { onDelete(item); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

/** 리스트에서 from 위치 원소를 to 위치로 옮긴 새 리스트 반환(범위 밖이면 원본 유지). */
private fun <T> List<T>.movedItem(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    val m = toMutableList()
    val e = m.removeAt(from)
    m.add(to, e)
    return m
}

/**
 * 자매앱 카드 1건 — 원격 레지스트리(`family.json`) 항목을 그대로 렌더한다. (2026-07-29, §8-4)
 *
 * - 아이콘: `iconUrl`(Coil) → 실패/부재 시 번들 폴백([bundledSisterIcon]).
 * - `comingSoon`: 미출시 앱은 '출시 예정' 비활성 카드(누르면 아무 일도 안 함).
 * - 설치 배지: `<queries>` 미등록 패키지는 항상 미설치로 보이며 스토어 이동만 된다(§8-5 폴백).
 */
@Composable
private fun SisterAppCard(app: FamilyApp) {
    val context = LocalContext.current
    val installed = remember(app.id) {
        context.packageManager.getLaunchIntentForPackage(app.id) != null
    }
    val fallbackIcon = remember(app.id) { bundledSisterIcon(app.id) }
    val gold = com.kitwlshcom.kdailyutil.ui.theme.Gold24K

    Card(
        onClick = { openAppOrStore(context, app.id, app.storeUrl) },
        enabled = !app.comingSoon,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.03f),
            // 출시예정(enabled=false)에도 다크 테마 톤을 유지 — 기본 disabled 색은 밝게 튄다
            disabledContainerColor = Color.White.copy(alpha = 0.02f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (app.comingSoon) Color.White.copy(alpha = 0.1f) else gold.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconAlpha = if (app.comingSoon) 0.45f else 1f
            if (app.iconUrl != null) {
                AsyncImage(
                    model = app.iconUrl,
                    contentDescription = app.name,
                    placeholder = painterResource(id = fallbackIcon),
                    error = painterResource(id = fallbackIcon),
                    modifier = Modifier.size(80.dp).alpha(iconAlpha),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = fallbackIcon),
                    contentDescription = app.name,
                    modifier = Modifier.size(80.dp).alpha(iconAlpha),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                app.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (app.comingSoon) Color.White.copy(alpha = 0.6f) else gold
            )
            if (app.tagline.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    app.tagline,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = if (app.comingSoon) 0.45f else 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 출시예정 / 설치됨=열기 / 미설치=설치하기 배지
            Surface(
                color = when {
                    app.comingSoon -> Color.White.copy(alpha = 0.06f)
                    installed -> Color.White.copy(alpha = 0.10f)
                    else -> gold
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    when {
                        app.comingSoon -> "🔜 출시 예정"
                        installed -> "▶ 열기"
                        else -> "⬇ 설치하기"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        app.comingSoon -> Color.White.copy(alpha = 0.55f)
                        installed -> Color.White
                        else -> Color.Black
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * 오프라인·첫 실행·아이콘 로드 실패 대비 **번들 아이콘 폴백**.
 * 레지스트리에만 있고 번들 리소스가 없는 신규 앱은 패밀리 공통 엠블럼으로 표시한다(재배포 불필요).
 */
private fun bundledSisterIcon(pkg: String): Int = when (pkg) {
    "com.kitwlshCom.klotto645" -> R.drawable.ic_klotto645
    "com.kitwlshcom.kjangbu" -> R.drawable.ic_kjangbu
    else -> R.drawable.ic_k_logo_3d
}

/**
 * 자매앱 유도: 설치돼 있으면 앱 실행, 없으면 Play 스토어(마켓→브라우저 폴백)로 이동. (2026-07-20)
 * K-시리즈 자매앱 상호연결 표준(doc/KLOTTO_CONNECT_HANDOFF.md §2)의 Compose 구현.
 *
 * @param storeUrl 레지스트리가 준 스토어 URL(화이트리스트 통과분). 있으면 먼저 시도하고,
 *                 없거나 열리지 않으면 패키지명으로 만든 `market://` → `https://` 순으로 폴백한다.
 */
private fun openAppOrStore(
    context: android.content.Context,
    pkg: String,
    storeUrl: String? = null
) {
    val launch = context.packageManager.getLaunchIntentForPackage(pkg)
    if (launch != null) {
        context.startActivity(launch)
        return
    }
    val candidates = listOfNotNull(
        storeUrl,
        "market://details?id=$pkg",
        "https://play.google.com/store/apps/details?id=$pkg"
    )
    for (url in candidates) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return
        } catch (e: ActivityNotFoundException) {
            // 다음 후보로 폴백
        }
    }
}
