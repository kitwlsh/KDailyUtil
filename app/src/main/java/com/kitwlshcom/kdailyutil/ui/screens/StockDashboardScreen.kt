package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kitwlshcom.kdailyutil.data.model.ChartRange
import com.kitwlshcom.kdailyutil.data.model.CurrencyType
import com.kitwlshcom.kdailyutil.data.model.EarningsDisclosure
import com.kitwlshcom.kdailyutil.data.model.ExpectedEarnings
import com.kitwlshcom.kdailyutil.data.model.StockChartData
import com.kitwlshcom.kdailyutil.data.model.StockPriceItem
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import com.kitwlshcom.kdailyutil.ui.viewmodel.StockViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// ─────────────────────────────────────────────────────────────
// 📐 통화 포맷터: 심볼 타입에 따라 올바른 화폐 단위를 붙임
//  - CurrencyType.KRW   → ₩80,000 (한국 주식)
//  - CurrencyType.USD   → $234.56 (US 주식 / 비트코인 등)
//  - CurrencyType.INDEX → 18,234.12 pt (나스닥, 코스피 등 지수)
// ─────────────────────────────────────────────────────────────
fun formatStockPrice(price: Double, currencyType: CurrencyType): String = when (currencyType) {
    CurrencyType.KRW   -> "₩${String.format("%,.0f", price)}"
    CurrencyType.USD   -> "\$${String.format("%,.2f", price)}"
    CurrencyType.INDEX -> String.format("%,.2f", price)  // 단위 없이 숫자만 (소수 2자리)
}

fun formatChartPrice(price: Float, currencyType: CurrencyType): String = when (currencyType) {
    CurrencyType.KRW   -> "₩${String.format("%,.0f", price)}"
    CurrencyType.USD   -> "\$${String.format("%,.2f", price)}"
    CurrencyType.INDEX -> String.format("%,.2f", price)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDashboardScreen(
    navController: NavController,
    viewModel: StockViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("시세 및 차트", "AI 실적 공시", "실적 예정 일정")

    val isPricesLoading by viewModel.isPricesLoading.collectAsState()
    val isDisclosuresLoading by viewModel.isDisclosuresLoading.collectAsState()
    val isExpectedLoading by viewModel.isExpectedLoading.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startPricePolling()
        onDispose {
            viewModel.stopPricePolling()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("📈 증시 대시보드 (DART AI)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Gold24K
                ),
                actions = {
                    IconButton(onClick = {
                        when (selectedTabIndex) {
                            0 -> viewModel.loadStockPrices(showLoading = true)
                            1 -> viewModel.loadDisclosures(forceRefresh = true)
                            2 -> viewModel.loadExpectedEarnings()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = Gold24K)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 상단 서브 탭바 구성
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Black.copy(alpha = 0.2f),
                contentColor = Gold24K,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Gold24K
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTabIndex == index) Gold24K else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> PricesTab(viewModel, isPricesLoading)
                    1 -> DisclosuresTab(viewModel, isDisclosuresLoading)
                    2 -> ExpectedCalendarTab(viewModel, isExpectedLoading)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 빠른 종목 추가 칩 목록 (사전 정의)
// ─────────────────────────────────────────────────────────────
private val QUICK_ADD_PRESETS = listOf(
    "삼성전자", "SK하이닉스", "현대차", "카카오", "NAVER",
    "애플", "엔비디아", "구글", "마이크로소프트", "아마존",
    "테슬라", "코스피", "나스닥", "비트코인", "이더리움"
)

@Composable
fun PricesTab(viewModel: StockViewModel, isLoading: Boolean) {
    val stockPrices by viewModel.stockPrices.collectAsState()
    val selectedStock by viewModel.selectedStock.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val chartRange by viewModel.chartRange.collectAsState()
    val isChartLoading by viewModel.isChartLoading.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 차트 바텀시트
    if (selectedStock != null) {
        StockChartBottomSheet(
            item = selectedStock!!,
            chartData = chartData,
            chartRange = chartRange,
            isLoading = isChartLoading,
            onRangeSelected = { viewModel.setChartRange(it) },
            onDismiss = { viewModel.closeStockDetail() }
        )
    }

    // 종목 추가 다이얼로그
    if (showAddDialog) {
        StockAddDialog(
            onAdd = { name ->
                viewModel.addStockKeyword(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 종목 관리 툴바 ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "관심 종목  ${stockPrices.size}개",
                fontSize = 12.sp,
                color = Color.Gray
            )
            TextButton(
                onClick = { isEditMode = !isEditMode },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (isEditMode) Color(0xFF2ECC71) else Gold24K,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEditMode) "완료" else "편집",
                    color = if (isEditMode) Color(0xFF2ECC71) else Gold24K,
                    fontSize = 12.sp
                )
            }
        }

        // ── 종목 목록 ─────────────────────────────────────────
        if (isLoading && stockPrices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold24K)
            }
        } else if (stockPrices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("등록된 관심 종목이 없습니다.", textAlign = TextAlign.Center, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold24K.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Add, null, tint = Gold24K)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("종목 추가", color = Gold24K)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(stockPrices) { item ->
                    StockPriceCard(
                        item = item,
                        isEditMode = isEditMode,
                        onChartTap = { viewModel.openStockDetail(item) },
                        onDelete = { viewModel.removeStockKeyword(item.name) }
                    )
                }
                // 편집 모드 하단 추가 버튼
                if (isEditMode) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Gold24K.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Gold24K)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("종목 추가", color = Gold24K)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 종목 추가 다이얼로그
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun StockAddDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Gold24K.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "📌 종목 추가",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Gold24K
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 검색 입력창
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("종목명 또는 심볼", color = Color.Gray, fontSize = 13.sp) },
                    placeholder = { Text("예: 삼성전자, AAPL, 비트코인", color = Color.DarkGray, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold24K,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Gold24K
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        if (inputText.isNotBlank()) onAdd(inputText.trim())
                    })
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("빠른 선택", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                // 빠른 선택 칩 그리드 (LazyRow × 2줄)
                val half = QUICK_ADD_PRESETS.size / 2
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        QUICK_ADD_PRESETS.take(half),
                        QUICK_ADD_PRESETS.drop(half)
                    ).forEach { row ->
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(row) { preset ->
                                FilterChip(
                                    selected = inputText == preset,
                                    onClick = { inputText = preset },
                                    label = { Text(preset, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold24K.copy(alpha = 0.25f),
                                        selectedLabelColor = Gold24K,
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        labelColor = Color.LightGray
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) onAdd(inputText.trim())
                        },
                        enabled = inputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold24K.copy(alpha = 0.15f),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (inputText.isNotBlank()) Gold24K else Color.Gray.copy(alpha = 0.3f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("추가", color = if (inputText.isNotBlank()) Gold24K else Color.Gray)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 종목 카드 (편집 모드 지원 + 차트 탭)
// ─────────────────────────────────────────────────────────────
@Composable
fun StockPriceCard(
    item: StockPriceItem,
    isEditMode: Boolean = false,
    onChartTap: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isPositive = item.change >= 0.0
    val trendColor = if (isPositive) Color(0xFFFF4D4D) else Color(0xFF4D94FF)

    // 카드 기간 선택 상태 (카드 자체 내 기간 버튼)
    var cardRange by remember { mutableStateOf(ChartRange.TODAY) }
    // 기간 버튼이 눌리면 onChartTap 으로 바텀시트를 열어 해당 기간으로 시작
    // (카드 미니 차트는 항상 당일 스파크라인 유지 — 기간 버튼 탭 시 바텀시트 전환)

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.85f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(alpha = 0.2f))
        ) {
            Column {
                // ── 상단 행: 이름, 가격, 차트, 등락률 ────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 왼쪽: 이름, 심볼, 가격
                    Column(modifier = Modifier.weight(1.2f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val isRealtime = item.delayInfo == "실시간급"
                            val bgColor = if (isRealtime) Color(0xFF2ECC71).copy(0.15f) else Color.White.copy(0.05f)
                            val fgColor = if (isRealtime) Color(0xFF2ECC71) else Color.LightGray.copy(0.6f)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bgColor)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(item.delayInfo, fontSize = 8.sp, color = fgColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                        val timeSuffix = if (item.updateTime.isNotBlank()) " • ${item.updateTime}" else ""
                        Text("${item.symbol}$timeSuffix", fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedPrice = formatStockPrice(item.price, item.currencyType)
                        Text(formattedPrice, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color.White)
                    }

                    // 가운데: 스파크라인 (탭하면 바텀시트)
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                            .padding(horizontal = 6.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onChartTap() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.sparkline.size > 1) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width; val h = size.height
                                val minVal = item.sparkline.minOrNull() ?: 0f
                                val maxVal = item.sparkline.maxOrNull() ?: 100f
                                val valRange = (maxVal - minVal).coerceAtLeast(0.1f)
                                val count = item.sparkline.size
                                val path = Path(); val fillPath = Path()
                                item.sparkline.forEachIndexed { i, v ->
                                    val x = (i.toFloat() / (count - 1)) * w
                                    val y = h - ((v - minVal) / valRange) * (h - 10f) - 5f
                                    if (i == 0) { path.moveTo(x, y); fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
                                    else { path.lineTo(x, y); fillPath.lineTo(x, y) }
                                    if (i == count - 1) { fillPath.lineTo(x, h); fillPath.close() }
                                }
                                drawPath(fillPath, Brush.verticalGradient(listOf(trendColor.copy(0.25f), Color.Transparent), 0f, h))
                                drawPath(path, trendColor, style = Stroke(2.dp.toPx()))
                            }
                            // "탭하면 확대" 힌트
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp)) {
                                Text("⤢", fontSize = 9.sp, color = Color.White.copy(0.3f))
                            }
                        } else {
                            Text("데이터 없음", fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }

                    // 오른쪽: 등락률 (증감금액 + 증감율 세로 표시)
                    Box(
                        modifier = Modifier
                            .weight(0.8f) // 크기를 0.8f로 약간 넓혀 가독성 향상
                            .clip(RoundedCornerShape(8.dp))
                            .background(trendColor.copy(alpha = 0.15f))
                            .border(1.dp, trendColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val prefix = if (isPositive) "+" else ""
                        val formattedAmount = formatStockPrice(item.changeAmount, item.currencyType)
                        val finalAmountText = if (item.changeAmount > 0.0) "+$formattedAmount" else formattedAmount
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = finalAmountText,
                                color = trendColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${prefix}${String.format("%.2f", item.change)}%",
                                color = trendColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ── 하단 기간 버튼 행 ─────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChartRange.values().forEach { range ->
                        val isSelected = cardRange == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Gold24K.copy(0.2f) else Color.Transparent)
                                .border(0.5.dp, if (isSelected) Gold24K.copy(0.6f) else Color.White.copy(0.1f), RoundedCornerShape(4.dp))
                                .clickable {
                                    cardRange = range
                                    onChartTap()   // 바텀시트 열기 (range는 ViewModel에서 설정됨)
                                }
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(range.label, fontSize = 10.sp, color = if (isSelected) Gold24K else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("차트 상세 ▸", fontSize = 9.sp, color = Color.Gray.copy(0.5f), modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }

        // 편집 모드: 삭제 ❌ 버튼 (카드 우상단)
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4D4D))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 차트 상세 바텀시트 (팬 + 핀치줌)
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockChartBottomSheet(
    item: StockPriceItem,
    chartData: StockChartData?,
    chartRange: ChartRange,
    isLoading: Boolean,
    onRangeSelected: (ChartRange) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12151F),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // ── 헤더 ─────────────────────────────────────────
            val isPositive = item.change >= 0.0
            val trendColor = if (isPositive) Color(0xFFFF4D4D) else Color(0xFF4D94FF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text(item.symbol, fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val formattedPrice = formatStockPrice(item.price, item.currencyType)
                    Text(formattedPrice, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
                    val prefix = if (isPositive) "+" else ""
                    val formattedAmount = formatStockPrice(item.changeAmount, item.currencyType)
                    val finalAmountText = if (item.changeAmount > 0.0) "+$formattedAmount" else formattedAmount
                    Text(
                        text = "$finalAmountText (${prefix}${String.format("%.2f", item.change)}%)",
                        color = trendColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val isRealtime = item.delayInfo == "실시간급"
                val fgColor = if (isRealtime) Color(0xFF2ECC71) else Color.LightGray.copy(0.6f)
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(fgColor.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(item.delayInfo, fontSize = 10.sp, color = fgColor, fontWeight = FontWeight.Bold)
                }
                if (item.updateTime.isNotBlank()) {
                    Text("기준: ${item.updateTime}", fontSize = 10.sp, color = Color.Gray)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(0.08f))

            // ── 기간 탭 ──────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartRange.values().forEach { range ->
                    val selected = chartRange == range
                    FilterChip(
                        selected = selected,
                        onClick = { onRangeSelected(range) },
                        label = { Text(range.label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold24K.copy(0.25f),
                            selectedLabelColor = Gold24K,
                            containerColor = Color.White.copy(0.05f),
                            labelColor = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 인터랙티브 캔버스 차트 ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading || chartData == null) {
                    CircularProgressIndicator(color = Gold24K, modifier = Modifier.size(36.dp))
                } else if (chartData.prices.isEmpty()) {
                    Text("차트 데이터 없음", color = Color.Gray)
                } else {
                    val isPos = item.change >= 0.0
                    val lineColor = if (isPos) Color(0xFFFF4D4D) else Color(0xFF4D94FF)
                    val prices = chartData.prices
                    val timestamps = chartData.timestamps

                    // 제스처 상태: offsetX (팬), zoomX (줌)
                    var offsetX by remember { mutableStateOf(0f) }
                    var zoomX by remember { mutableStateOf(1f) }
                    // 크로스헤어 상태
                    var touchX by remember { mutableStateOf<Float?>(null) }

                    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                        zoomX = (zoomX * zoomChange).coerceIn(0.5f, 5f)
                        offsetX = (offsetX + panChange.x).coerceIn(
                            -(prices.size * 4f * zoomX),
                            prices.size * 4f * zoomX
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformableState)
                            .pointerInput(prices) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    touchX = change.position.x
                                    offsetX = (offsetX + dragAmount).coerceIn(
                                        -(prices.size * 8f),
                                        prices.size * 8f
                                    )
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val padding = 16f
                            val chartH = h - padding * 2

                            val count = prices.size
                            if (count < 2) return@Canvas

                            // 뷰포트 범위: zoomX에 따라 보이는 데이터 범위
                            val visibleCount = (count / zoomX).toInt().coerceIn(5, count)
                            val maxStartIdx = (count - visibleCount).coerceAtLeast(0)
                            val startIdxFloat = ((-offsetX / w) * count).coerceIn(0f, maxStartIdx.toFloat())
                            val startIdx = startIdxFloat.toInt()
                            val endIdx = (startIdx + visibleCount).coerceAtMost(count)

                            val visiblePrices = prices.subList(startIdx, endIdx)
                            val minVal = visiblePrices.minOrNull() ?: 0f
                            val maxVal = visiblePrices.maxOrNull() ?: 1f
                            val valRange = (maxVal - minVal).coerceAtLeast(0.1f)

                            val path = Path()
                            val fillPath = Path()

                            visiblePrices.forEachIndexed { i, v ->
                                val x = (i.toFloat() / (visiblePrices.size - 1)) * w
                                val y = padding + chartH - ((v - minVal) / valRange) * chartH
                                if (i == 0) { path.moveTo(x, y); fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
                                else { path.lineTo(x, y); fillPath.lineTo(x, y) }
                                if (i == visiblePrices.size - 1) { fillPath.lineTo(x, h); fillPath.close() }
                            }

                            // 그라디언트 배경
                            drawPath(fillPath, Brush.verticalGradient(listOf(lineColor.copy(0.25f), Color.Transparent), padding, h))
                            // 가격 라인 (둥근 끝 처리)
                            drawPath(path, lineColor, style = Stroke(2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

                            // ── Y축 수평 그리드 (4단계) ──────────────────
                            val gridSteps = 4
                            for (i in 0..gridSteps) {
                                val yPos = padding + (chartH / gridSteps) * i
                                drawLine(
                                    color = Color.White.copy(alpha = 0.07f),
                                    start = Offset(0f, yPos),
                                    end = Offset(w, yPos),
                                    strokeWidth = 0.5.dp.toPx()
                                )
                            }

                            // ── Y축 수평 그리드 (4단계) 레이블은 Canvas 외부 Compose에서 처리

                            // 크로스헤어
                            touchX?.let { tx ->
                                val clampedTx = tx.coerceIn(0f, w)
                                // 수직선
                                drawLine(Color.White.copy(0.35f), Offset(clampedTx, padding), Offset(clampedTx, h - padding), strokeWidth = 1.dp.toPx())
                                // 가격 계산
                                val idx = ((clampedTx / w) * (visiblePrices.size - 1)).toInt().coerceIn(0, visiblePrices.lastIndex)
                                val priceAtTouch = visiblePrices.getOrNull(idx)
                                val cy = if (priceAtTouch != null) padding + chartH - ((priceAtTouch - minVal) / valRange) * chartH else h / 2
                                // 가격 도트 (흰 테두리 + 라인 색 내부)
                                drawCircle(Color.White, radius = 5.5.dp.toPx(), center = Offset(clampedTx, cy))
                                drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(clampedTx, cy))
                                // 수평선 (점선 효과)
                                drawLine(lineColor.copy(0.5f), Offset(0f, cy), Offset(w, cy), strokeWidth = 0.5.dp.toPx())
                            }
                        }  // end Canvas

                        // ── Y축 가격 레이블 (Compose Text 오버레이) ──────────
                        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(end = 2.dp)) {
                            val chartH2 = maxHeight
                            val padding2 = 4.dp
                            val visiblePricesSnap = run {
                                val count = prices.size
                                val vc = (count / zoomX).toInt().coerceIn(5, count)
                                val maxS = (count - vc).coerceAtLeast(0)
                                val si = ((-offsetX / 1f) * count / 1f).toFloat().coerceIn(0f, maxS.toFloat()).toInt()
                                val ei = (si + vc).coerceAtMost(count)
                                prices.subList(si, ei)
                            }
                            val minP = visiblePricesSnap.minOrNull() ?: 0f
                            val maxP = visiblePricesSnap.maxOrNull() ?: 1f
                            listOf(0, 1, 2).forEach { i ->
                                val fraction = i.toFloat() / 2f
                                val priceLabel = minP + (maxP - minP) * (1f - fraction)
                                val yOffset = (chartH2 * fraction)
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .align(Alignment.TopEnd)
                                        .offset(y = yOffset - 8.dp)
                                ) {
                                    Text(
                                        text = formatChartPrice(priceLabel, item.currencyType),
                                        fontSize = 8.sp,
                                        color = Color.White.copy(0.45f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        // 크로스헤어 가격 말풍선

                        touchX?.let { tx ->
                            val count2 = prices.size
                            val visibleCount2 = (count2 / zoomX).toInt().coerceIn(5, count2)
                            val maxStart2 = (count2 - visibleCount2).coerceAtLeast(0)
                            val startIdx2 = ((-offsetX / 1f) * count2 / 1f).toFloat().coerceIn(0f, maxStart2.toFloat()).toInt()
                            val endIdx2 = (startIdx2 + visibleCount2).coerceAtMost(count2)
                            val visiblePrices2 = prices.subList(startIdx2, endIdx2)

                            // 타임스탬프 계산
                            val visibleTs = if (timestamps.size >= endIdx2) timestamps.subList(startIdx2, endIdx2) else emptyList()

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val canvasW = maxWidth
                                val idxFloat = (tx / canvasW.value.coerceAtLeast(1f)) * (visiblePrices2.size - 1)
                                val idx = idxFloat.toInt().coerceIn(0, visiblePrices2.lastIndex)
                                val priceVal = visiblePrices2.getOrNull(idx)
                                val tsVal = visibleTs.getOrNull(idx)

                                if (priceVal != null) {
                                    val formP = formatChartPrice(priceVal, item.currencyType)
                                    val timeStr = if (tsVal != null && tsVal > 0L) {
                                        val pattern = when (chartRange) {
                                            ChartRange.TODAY -> "HH:mm"
                                            ChartRange.WEEK -> "MM/dd HH:mm"
                                            else -> "MM/dd"
                                        }
                                        SimpleDateFormat(pattern, Locale.KOREA).format(Date(tsVal * 1000L))
                                    } else ""

                                    val xDp = (tx / maxWidth.value.coerceAtLeast(1f) * maxWidth.value).dp
                                    val bubbleAlignEnd = tx > maxWidth.value * 0.6f

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = if (bubbleAlignEnd) 0.dp else xDp.coerceIn(0.dp, maxWidth - 90.dp))
                                            .padding(end = if (bubbleAlignEnd) (maxWidth - xDp).coerceIn(0.dp, maxWidth - 90.dp) else 0.dp)
                                            .padding(top = 6.dp)
                                            .let { if (bubbleAlignEnd) it.wrapContentWidth(Alignment.End) else it }
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(0.2f)),
                                            shape = RoundedCornerShape(6.dp),
                                            elevation = CardDefaults.cardElevation(4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                Text(formP, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                if (timeStr.isNotEmpty()) Text(timeStr, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 하단 힌트
                    Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                        Text("← 드래그로 이동  |  핀치로 확대/축소", fontSize = 9.sp, color = Color.Gray.copy(0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}



@Composable
fun DisclosuresTab(viewModel: StockViewModel, isLoading: Boolean) {
    val disclosures by viewModel.disclosures.collectAsState()
    val isAiSummarizing by viewModel.isAiSummarizing.collectAsState()
    val activeDisclosure by viewModel.activeAiSummaryDisclosure.collectAsState()
    val selectedFilterDays by viewModel.selectedDateFilterDays.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    var reportTitle by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showReportDialog && activeDisclosure != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = "📊 AI 실적 요약 - ${activeDisclosure?.corp_name}",
                    fontWeight = FontWeight.Bold,
                    color = Gold24K
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = activeDisclosure?.report_nm ?: "",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    
                    // 간단한 커스텀 마크다운 렌더러 처리
                    val lines = reportContent.split("\n")
                    lines.forEach { line ->
                        when {
                            line.startsWith("###") -> {
                                Text(
                                    text = line.replace("###", "").trim(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Gold24K,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            line.startsWith("*") || line.startsWith("-") -> {
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("• ", color = Gold24K, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = line.substring(1).trim(),
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            line.matches(Regex("^[0-9]+\\..*")) -> {
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    val numPart = line.takeWhile { it != '.' } + "."
                                    val rest = line.dropWhile { it != '.' }.drop(1).trim()
                                    Text("$numPart ", color = Gold24K, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = rest,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            line.isNotBlank() -> {
                                Text(
                                    text = line.trim(),
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showReportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (isAiSummarizing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold24K.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Gold24K)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gemini AI가 실적 재무 수치를 분석하는 중입니다...", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 기간 필터 로우
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("조회 기간:", fontSize = 12.sp, color = Color.Gray)
            
            val filterOptions = listOf(
                Pair(1, "1일"),
                Pair(3, "3일"),
                Pair(7, "1주일"),
                Pair(30, "1개월")
            )

            filterOptions.forEach { opt ->
                val selected = selectedFilterDays == opt.first
                InputChip(
                    selected = selected,
                    onClick = { viewModel.setDateFilter(opt.first) },
                    label = { Text(opt.second, fontSize = 11.sp) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = Gold24K,
                        selectedLabelColor = Color.Black,
                        containerColor = DeepCharcoal,
                        labelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    border = InputChipDefaults.inputChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) Gold24K else Color.White.copy(alpha = 0.1f)
                    )
                )
            }
        }

        if (isLoading && disclosures.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold24K)
            }
        } else if (disclosures.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("선택한 기간 동안 등록된 실적 공시가 없습니다.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(disclosures) { item ->
                    DisclosureCard(item) {
                        viewModel.summarizeDisclosure(item) { success, result ->
                            if (success) {
                                reportContent = result
                                reportTitle = item.corp_name
                                showReportDialog = true
                            } else {
                                android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisclosureCard(item: EarningsDisclosure, onClick: () -> Unit) {
    val dateText = "${item.rcept_dt.take(4)}.${item.rcept_dt.substring(4, 6)}.${item.rcept_dt.substring(6)}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.corp_name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(dateText, fontSize = 11.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                item.report_nm,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 뱃지 판별
                when {
                    item.isTurnaround -> {
                        BadgeView("흑자전환 🟢", Color(0xFF2ECC71))
                    }
                    item.isSurprise == true -> {
                        BadgeView("어닝 서프라이즈 🚀", Color(0xFFFF4D4D))
                    }
                    item.isSurprise == false -> {
                        BadgeView("어닝 쇼크 📉", Color(0xFF4D94FF))
                    }
                }

                if (item.aiSummary != null) {
                    BadgeView("AI 분석완료 📄", Gold24K)
                } else {
                    BadgeView("AI 미분석 🔍", Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun BadgeView(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(vertical = 2.dp, horizontal = 6.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExpectedCalendarTab(viewModel: StockViewModel, isLoading: Boolean) {
    val expectedEarnings by viewModel.expectedEarnings.collectAsState()
    
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    var reportTitle by remember { mutableStateOf("") }
    var isGeneratingReport by remember { mutableStateOf(false) }

    if (isGeneratingReport) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold24K.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Gold24K)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI가 실적 사전 전망 리포트를 작성하는 중입니다...", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = "🗓️ AI 사전 전망 - $reportTitle",
                    fontWeight = FontWeight.Bold,
                    color = Gold24K
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val lines = reportContent.split("\n")
                    lines.forEach { line ->
                        when {
                            line.startsWith("###") -> {
                                Text(
                                    text = line.replace("###", "").trim(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Gold24K,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            line.startsWith("*") || line.startsWith("-") -> {
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("• ", color = Gold24K, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = line.substring(1).trim(),
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            line.matches(Regex("^[0-9]+\\..*")) -> {
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    val numPart = line.takeWhile { it != '.' } + "."
                                    val rest = line.dropWhile { it != '.' }.drop(1).trim()
                                    Text("$numPart ", color = Gold24K, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = rest,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            line.isNotBlank() -> {
                                Text(
                                    text = line.trim(),
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showReportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (isLoading && expectedEarnings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold24K)
        }
    } else if (expectedEarnings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("발표 예정인 공시 일정이 없습니다.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(expectedEarnings) { item ->
                ExpectedEarningsCard(item) {
                    isGeneratingReport = true
                    viewModel.generatePreReport(item) { report ->
                        isGeneratingReport = false
                        reportContent = report
                        reportTitle = item.corp_name
                        showReportDialog = true
                    }
                }
            }
        }
    }
}

@Composable
fun ExpectedEarningsCard(item: ExpectedEarnings, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 날짜 박스
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gold24K.copy(alpha = 0.15f))
                    .border(1.dp, Gold24K.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("발표", fontSize = 9.sp, color = Gold24K, fontWeight = FontWeight.Bold)
                    Text(item.release_date, fontSize = 14.sp, color = Gold24K, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 내용
            Column(modifier = Modifier.weight(1f)) {
                Text(item.corp_name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("예상 매출: ${item.consensus_revenue}", fontSize = 12.sp, color = Color.Gray)
                    Text("예상 이익: ${item.consensus_profit}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "사전 리포트 보기",
                tint = Gold24K.copy(alpha = 0.7f)
            )
        }
    }
}
