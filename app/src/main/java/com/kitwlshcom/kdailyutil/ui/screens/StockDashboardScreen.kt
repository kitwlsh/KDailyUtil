package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kitwlshcom.kdailyutil.data.model.EarningsDisclosure
import com.kitwlshcom.kdailyutil.data.model.ExpectedEarnings
import com.kitwlshcom.kdailyutil.data.model.StockPriceItem
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import com.kitwlshcom.kdailyutil.ui.viewmodel.StockViewModel

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
                            0 -> viewModel.loadStockPrices()
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

@Composable
fun PricesTab(viewModel: StockViewModel, isLoading: Boolean) {
    val stockPrices by viewModel.stockPrices.collectAsState()

    if (isLoading && stockPrices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold24K)
        }
    } else if (stockPrices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("등록된 관심 종목이 없습니다.\n설정 탭에서 종목 키워드를 추가해 주세요.", textAlign = TextAlign.Center, color = Color.Gray)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(stockPrices) { item ->
                StockPriceCard(item)
            }
        }
    }
}

@Composable
fun StockPriceCard(item: StockPriceItem) {
    val isPositive = item.change >= 0.0
    val trendColor = if (isPositive) Color(0xFFFF4D4D) else Color(0xFF4D94FF) // Red vs Blue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold24K.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    
                    val isRealtime = item.delayInfo == "실시간급"
                    val badgeBgColor = if (isRealtime) Color(0xFF2ECC71).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                    val badgeContentColor = if (isRealtime) Color(0xFF2ECC71) else Color.LightGray.copy(alpha = 0.6f)
                    val badgeBorderColor = if (isRealtime) Color(0xFF2ECC71).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBgColor)
                            .border(0.5.dp, badgeBorderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(item.delayInfo, fontSize = 9.sp, color = badgeContentColor, fontWeight = FontWeight.Bold)
                    }
                }
                
                val timeSuffix = if (item.updateTime.isNotBlank()) " • ${item.updateTime}" else ""
                Text(
                    text = "${item.symbol}$timeSuffix", 
                    fontSize = 10.sp, 
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                val formattedPrice = if (item.price > 1000.0) {
                    String.format("₩%,.0f", item.price)
                } else {
                    String.format("$%,.2f", item.price)
                }
                Text(formattedPrice, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
            }

            // 네온 스파크라인 미니 차트 (Canvas 그림)
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.sparkline.size > 1) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val minVal = item.sparkline.minOrNull() ?: 0f
                        val maxVal = item.sparkline.maxOrNull() ?: 100f
                        val range = (maxVal - minVal).coerceAtLeast(0.1f)

                        val path = Path()
                        val fillPath = Path()
                        val pointsCount = item.sparkline.size
                        
                        item.sparkline.forEachIndexed { index, valPoint ->
                            val x = (index.toFloat() / (pointsCount - 1)) * width
                            val y = height - ((valPoint - minVal) / range) * (height - 10f) - 5f // padding 5f
                            
                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (index == pointsCount - 1) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }
                        }

                        // 그라디언트 배경 채우기
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(trendColor.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // 라인 그리기
                        drawPath(
                            path = path,
                            color = trendColor,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                } else {
                    Text("차트 데이터 없음", fontSize = 10.sp, color = Color.DarkGray)
                }
            }

            // 등락률 박스
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(trendColor.copy(alpha = 0.15f))
                    .border(1.dp, trendColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val prefix = if (isPositive) "+" else ""
                Text(
                    text = String.format("%s%.2f%%", prefix, item.change),
                    color = trendColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
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
