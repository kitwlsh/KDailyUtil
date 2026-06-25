package com.kitwlshcom.kdailyutil.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kitwlshcom.kdailyutil.data.QuizFileHandler
import com.kitwlshcom.kdailyutil.data.model.QuizQuestion
import com.kitwlshcom.kdailyutil.data.model.QuizType
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import com.kitwlshcom.kdailyutil.ui.theme.DeepCharcoal
import com.kitwlshcom.kdailyutil.ui.viewmodel.QuizViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.flow.first
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreatorScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit
)
{
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("📷 AI 사진 스캔", "🌐 AI 링크 크롤링", "✍️ 수동 작성")

    // Creator Profile Settings
    var creatorNickname by remember { mutableStateOf("익명의 출제자") }
    val deviceHashId = remember { getDeviceHashId(context) }
    var isAnonymousSharing by remember { mutableStateOf(false) }

    // UI state for previewing generated quiz package before saving
    var pendingQuizzes by remember { mutableStateOf<List<QuizQuestion>?>(null) }
    var pendingCategoryName by remember { mutableStateOf("") }
    var isSaveAndShareDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepCharcoal,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "나만의 AI 퀴즈 제작소",
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold24K
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack)
                    {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Gold24K
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    titleContentColor = Gold24K
                )
            )
        }
    )
    { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
                .background(DeepCharcoal)
        )
        {
            // Tab Row
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
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex)
                {
                    0 -> ImageScannerTab(
                        viewModel = viewModel,
                        creatorNickname = creatorNickname,
                        onNicknameChange = { creatorNickname = it },
                        isAnonymous = isAnonymousSharing,
                        onAnonymousChange = { isAnonymousSharing = it },
                        deviceHashId = deviceHashId,
                        onQuizGenerated = { quizzes, category ->
                            pendingQuizzes = quizzes
                            pendingCategoryName = category
                            isSaveAndShareDialogVisible = true
                        }
                    )
                    1 -> WebCrawlingTab(
                        viewModel = viewModel,
                        creatorNickname = creatorNickname,
                        onNicknameChange = { creatorNickname = it },
                        isAnonymous = isAnonymousSharing,
                        onAnonymousChange = { isAnonymousSharing = it },
                        deviceHashId = deviceHashId,
                        onQuizGenerated = { quizzes, category ->
                            pendingQuizzes = quizzes
                            pendingCategoryName = category
                            isSaveAndShareDialogVisible = true
                        }
                    )
                    2 -> ManualCreatorTab(
                        viewModel = viewModel,
                        creatorNickname = creatorNickname,
                        onNicknameChange = { creatorNickname = it },
                        isAnonymous = isAnonymousSharing,
                        onAnonymousChange = { isAnonymousSharing = it },
                        deviceHashId = deviceHashId,
                        onQuizSaved = { quizzes, category ->
                            pendingQuizzes = quizzes
                            pendingCategoryName = category
                            isSaveAndShareDialogVisible = true
                        }
                    )
                }
            }

            // Copyright Disclaimer Banner
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "주의",
                        tint = Gold24K,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "본 퀴즈는 AI를 통해 요약 및 가공하여 생성됩니다. 개인 학습 용도 이외의 무단 배포는 저작권법(제30조 사적 복제 활용 범위 초과 시)에 저촉될 수 있습니다.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Save & Share Dialog
    if (isSaveAndShareDialogVisible && pendingQuizzes != null)
    {
        val quizzes = pendingQuizzes!!
        Dialog(
            onDismissRequest = { isSaveAndShareDialogVisible = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                border = BorderStroke(1.5.dp, Gold24K)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "🎯 생성된 퀴즈 검토 및 저장",
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold24K,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "카테고리명: $pendingCategoryName\n총 ${quizzes.size}개의 문제가 생성되었습니다.\n\n• [로컬 저장]: 앱 내부에 저장되어 '퀴즈 분야 선택' 목록에 즉시 추가됩니다.\n• [공유 아이콘]: 외부 파일(.kquiz)로 카카오톡 전송, 구글 드라이브 업로드 또는 파일 저장이 가능합니다.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quizzes) { q ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "Q. ${q.question}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "A. ${q.answer}",
                                        fontWeight = FontWeight.Medium,
                                        color = Gold24K,
                                        fontSize = 12.sp
                                    )
                                    if (q.options != null)
                                    {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "보기: " + q.options.joinToString(", "),
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (!q.imageUrl.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = q.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, Gold24K.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isSaveAndShareDialogVisible = false
                            },
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("취소")
                        }

                        Button(
                            onClick = {
                                viewModel.saveCustomQuizzes(quizzes)
                                isSaveAndShareDialogVisible = false
                                Toast.makeText(context, "퀴즈가 로컬 보관함에 저장되었습니다!", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold24K,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("로컬 저장", fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = {
                                viewModel.saveCustomQuizzes(quizzes)
                                val finalCreatorName = if (isAnonymousSharing) "익명의 출제자" else creatorNickname
                                val finalCreatorId = if (isAnonymousSharing) "anonymous" else deviceHashId
                                val fileUri = QuizFileHandler.exportQuizzes(
                                    context = context,
                                    categoryName = pendingCategoryName,
                                    creatorName = finalCreatorName,
                                    creatorId = finalCreatorId,
                                    quizzes = quizzes
                                )
                                if (fileUri != null)
                                {
                                    QuizFileHandler.triggerShareSheet(context, fileUri, pendingCategoryName)
                                }
                                else
                                {
                                    Toast.makeText(context, "파일 생성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                }
                                isSaveAndShareDialogVisible = false
                            },
                            modifier = Modifier
                                .background(Gold24K, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "공유",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 1: AI 사진 촬영 스캔
// ----------------------------------------------------
@Composable
fun ImageScannerTab(
    viewModel: QuizViewModel,
    creatorNickname: String,
    onNicknameChange: (String) -> Unit,
    isAnonymous: Boolean,
    onAnonymousChange: (Boolean) -> Unit,
    deviceHashId: String,
    onQuizGenerated: (List<QuizQuestion>, String) -> Unit
)
{
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var categoryName by remember { mutableStateOf("") }
    val capturedImages = remember { mutableStateListOf<Bitmap>() }
    var isScanningInProgress by remember { mutableStateOf(false) }
    var questionCount by remember { mutableIntStateOf(5) }
    var isVisualQuizType by remember { mutableStateOf(false) }

    // Camera State management
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null)
        {
            scope.launch {
                val compressed = loadAndCompressImage(context, tempPhotoUri!!)
                if (compressed != null)
                {
                    if (capturedImages.size < 5)
                    {
                        capturedImages.add(compressed)
                    }
                    else
                    {
                        Toast.makeText(context, "사진은 최대 5장까지만 추가 가능합니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        scope.launch {
            var addedCount = 0
            for (uri in uris)
            {
                if (capturedImages.size >= 5)
                {
                    Toast.makeText(context, "사진은 최대 5장까지만 추가 가능합니다.", Toast.LENGTH_SHORT).show()
                    break
                }
                val compressed = loadAndCompressImage(context, uri)
                if (compressed != null)
                {
                    capturedImages.add(compressed)
                    addedCount++
                }
            }
            if (addedCount > 0)
            {
                Toast.makeText(context, "${addedCount}장의 사진을 추가했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted)
        {
            tempPhotoUri = getTempImageUri(context)
            if (tempPhotoUri != null)
            {
                takePictureLauncher.launch(tempPhotoUri!!)
            }
        }
        else
        {
            Toast.makeText(context, "카메라 권한이 거부되어 갤러리로 대체합니다.", Toast.LENGTH_LONG).show()
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        CreatorSettingsCard(
            creatorNickname = creatorNickname,
            onNicknameChange = onNicknameChange,
            isAnonymous = isAnonymous,
            onAnonymousChange = onAnonymousChange,
            deviceHashId = deviceHashId
        )

        Text(
            text = "📷 교과서 및 도서 다중 사진 스캔",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
        )

        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text("퀴즈 카테고리 이름 입력") },
            placeholder = { Text("예: 고등 국사 2단원 복습") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Quiz Type selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎨 퀴즈 출제 형식 선택",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type 1: Text Quiz
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isVisualQuizType = false },
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isVisualQuizType) Gold24K.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(
                        1.5.dp, 
                        if (!isVisualQuizType) Gold24K else Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (!isVisualQuizType) Gold24K else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "개념/텍스트형",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!isVisualQuizType) Gold24K else Color.White
                        )
                        Text(
                            text = "이미지 내용을 분석해 글로 출제",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Type 2: Visual Quiz (Crop)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isVisualQuizType = true },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVisualQuizType) Gold24K.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(
                        1.5.dp, 
                        if (isVisualQuizType) Gold24K else Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            tint = if (isVisualQuizType) Gold24K else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "그림/시각 매칭형",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isVisualQuizType) Gold24K else Color.White
                        )
                        Text(
                            text = "그림 부분을 자동 크롭하여 출제",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Question count selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎯 생성할 문제 수 선택",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "💡 단어장이나 목록 이미지인 경우, 문제 수를 15~30개로 설정하여 스캔하시면 이미지의 모든 단어를 골고루 문제로 만들 수 있습니다.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = listOf(5, 10, 15, 20, 30)
                counts.forEach { count ->
                    val isSelected = questionCount == count
                    QuestionCountChip(
                        count = count,
                        isSelected = isSelected,
                        onClick = { questionCount = count }
                    )
                }
            }
        }

        // Photo Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val permission = Manifest.permission.CAMERA
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
                    {
                        tempPhotoUri = getTempImageUri(context)
                        if (tempPhotoUri != null)
                        {
                            takePictureLauncher.launch(tempPhotoUri!!)
                        }
                    }
                    else
                    {
                        requestPermissionLauncher.launch(permission)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold24K,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("카메라 촬영", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                border = BorderStroke(1.5.dp, Gold24K),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold24K),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("갤러리 선택", fontWeight = FontWeight.Bold)
            }
        }

        // Captured/Selected Images Horizontal List
        if (capturedImages.isNotEmpty())
        {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "스캔할 페이지 (${capturedImages.size}/5)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold24K
                        )
                        Text(
                            text = "길게 터치 시 사진 삭제",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(capturedImages) { bitmap ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Gold24K.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        capturedImages.remove(bitmap)
                                    }
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "촬영된 페이지",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "삭제",
                                        tint = Color.Red,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        else
        {
            // Empty view placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        RoundedCornerShape(12.dp)
                    )
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "스캔할 책이나 문서 사진을 등록해 주세요 (최대 5장)",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isScanningInProgress)
        {
            Card(
                colors = CardDefaults.cardColors(containerColor = Gold24K.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Gold24K.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Gold24K,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Gemini가 다중 사진을 결합하여 맥락 분석 중...",
                            fontWeight = FontWeight.Bold,
                            color = Gold24K,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "중복 제거 및 취약점 타겟팅을 결합하여 퀴즈 생성 중입니다.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        else
        {
            Button(
                onClick = {
                    if (categoryName.isBlank())
                    {
                        Toast.makeText(context, "카테고리 이름을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (capturedImages.isEmpty())
                    {
                        Toast.makeText(context, "최소 1장 이상의 사진이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isScanningInProgress = true
                    scope.launch {
                        try
                        {
                            val apiKey = viewModel.getApplication<android.app.Application>()
                                .let { app -> com.kitwlshcom.kdailyutil.data.repository.SettingsRepository(app) }
                                .geminiApiKeyFlow.first()
                            
                            if (apiKey.isNullOrBlank())
                            {
                                Toast.makeText(context, "API 키가 설정되지 않았습니다. 설정에서 등록하세요.", Toast.LENGTH_LONG).show()
                                isScanningInProgress = false
                                return@launch
                            }

                            val geminiManager = com.kitwlshcom.kdailyutil.data.remote.GeminiManager(apiKey)
                            
                            // Retrieve error statistics and history for smart prompt blending
                            val statsManager = com.kitwlshcom.kdailyutil.data.QuizStatsManager.getInstance(context)
                            val highErrorStats = statsManager.getHighErrorQuestions(5)
                            val errorStatsArray = org.json.JSONArray().apply {
                                highErrorStats.forEach { (key, rate) ->
                                    put(org.json.JSONObject().apply {
                                        put("questionKey", key)
                                        put("errorRate", rate)
                                    })
                                }
                            }

                            val previousQuizzes = viewModel.questions.value
                            val prevQuizzesArray = org.json.JSONArray().apply {
                                previousQuizzes.forEach { q ->
                                    put(org.json.JSONObject().apply {
                                        put("question", q.question)
                                    })
                                }
                            }

                            val jsonResult = if (isVisualQuizType) {
                                geminiManager.generateVisualQuizzesFromImages(
                                    images = capturedImages.toList(),
                                    previousQuizzesJson = prevQuizzesArray.toString(),
                                    errorStatsJson = errorStatsArray.toString(),
                                    count = questionCount
                                )
                            } else {
                                geminiManager.generateQuizzesFromImages(
                                    images = capturedImages.toList(),
                                    previousQuizzesJson = prevQuizzesArray.toString(),
                                    errorStatsJson = errorStatsArray.toString(),
                                    count = questionCount
                                )
                            }

                            if (jsonResult.isNotBlank())
                            {
                                val jsonArray = org.json.JSONArray(jsonResult)
                                val list = mutableListOf<QuizQuestion>()
                                for (i in 0 until jsonArray.length())
                                {
                                    val obj = jsonArray.getJSONObject(i)
                                    val optionsArray = obj.optJSONArray("options")
                                    val optionsList = if (optionsArray != null)
                                    {
                                        List(optionsArray.length()) { idx -> optionsArray.getString(idx) }
                                    }
                                    else null

                                    val baseQuestion = obj.getString("question")
                                    val uniqueId = Math.abs((categoryName + baseQuestion).hashCode())

                                    var croppedLocalPath: String? = null
                                    if (isVisualQuizType && capturedImages.isNotEmpty())
                                    {
                                        val boxArray = obj.optJSONArray("boundingBox")
                                        if (boxArray != null && boxArray.length() == 4)
                                        {
                                            val sourceBitmap = capturedImages.first()
                                            val croppedBmp = cropBitmapFromBoundingBox(sourceBitmap, boxArray)
                                            if (croppedBmp != null)
                                            {
                                                croppedLocalPath = saveBitmapToInternalStorage(context, croppedBmp, categoryName, i)
                                            }
                                        }
                                    }

                                    list.add(
                                        QuizQuestion(
                                            id = uniqueId,
                                            type = QuizType.valueOf(obj.getString("type")),
                                            category = categoryName,
                                            subCategory = obj.optString("subCategory", if (isVisualQuizType) "그림 매칭" else "AI 이미지 분석"),
                                            question = baseQuestion,
                                            options = optionsList,
                                            answer = obj.getString("answer"),
                                            explanation = obj.getString("explanation"),
                                            semanticHint = obj.optString("semanticHint", null),
                                            imageUrl = croppedLocalPath
                                        )
                                    )
                                }
                                onQuizGenerated(list, categoryName)
                            }
                            else
                            {
                                Toast.makeText(context, "퀴즈 생성 실패: AI가 유효한 퀴즈 데이터를 생성하지 못했습니다. (빈 응답)", Toast.LENGTH_LONG).show()
                            }
                        }
                        catch (e: Exception)
                        {
                            Toast.makeText(context, "에러 발생: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        finally
                        {
                            isScanningInProgress = false
                        }
                    }
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold24K,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✨ 다중 사진 스캔 기반 퀴즈 패키지 생성",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ----------------------------------------------------
// Tab 2: AI 링크 크롤링 및 수집
// ----------------------------------------------------
@Composable
fun WebCrawlingTab(
    viewModel: QuizViewModel,
    creatorNickname: String,
    onNicknameChange: (String) -> Unit,
    isAnonymous: Boolean,
    onAnonymousChange: (Boolean) -> Unit,
    deviceHashId: String,
    onQuizGenerated: (List<QuizQuestion>, String) -> Unit
)
{
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var categoryName by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var fallbackTextContent by remember { mutableStateOf("") }
    var isCrawlingInProgress by remember { mutableStateOf(false) }
    var questionCount by remember { mutableIntStateOf(5) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        CreatorSettingsCard(
            creatorNickname = creatorNickname,
            onNicknameChange = onNicknameChange,
            isAnonymous = isAnonymous,
            onAnonymousChange = onAnonymousChange,
            deviceHashId = deviceHashId
        )

        Text(
            text = "🌐 인터넷 위키백과 및 기사 링크 스캔",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
        )

        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text("퀴즈 카테고리 이름 입력") },
            placeholder = { Text("예: 위키백과: 조선 시대의 전쟁사") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Question count selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎯 생성할 문제 수 선택",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "💡 요약본이나 기사 단어 목록을 다룰 때 15~30개로 넉넉하게 설정하시면 모든 중요 단어를 골고루 문제로 만들 수 있습니다.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = listOf(5, 10, 15, 20, 30)
                counts.forEach { count ->
                    val isSelected = questionCount == count
                    QuestionCountChip(
                        count = count,
                        isSelected = isSelected,
                        onClick = { questionCount = count }
                    )
                }
            }
        }

        OutlinedTextField(
            value = websiteUrl,
            onValueChange = { websiteUrl = it },
            label = { Text("위키백과 / 기사 URL 주소 복사 붙여넣기") },
            placeholder = { Text("https://ko.wikipedia.org/wiki/...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Text Area Fallback in case of blocked URL
        OutlinedTextField(
            value = fallbackTextContent,
            onValueChange = { fallbackTextContent = it },
            label = { Text("본문 직접 붙여넣기 (크롤링 차단 대안 필드)") },
            placeholder = { Text("인터넷 본문 내용을 복사해서 여기에 직접 붙여넣으셔도 퀴즈가 바로 생성됩니다.") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        if (isCrawlingInProgress)
        {
            Card(
                colors = CardDefaults.cardColors(containerColor = Gold24K.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Gold24K.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Gold24K,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "지식 소스 분석 및 AI 퀴즈 제작 중...",
                        fontWeight = FontWeight.Bold,
                        color = Gold24K,
                        fontSize = 13.sp
                    )
                }
            }
        }
        else
        {
            Button(
                onClick = {
                    if (categoryName.isBlank())
                    {
                        Toast.makeText(context, "카테고리 이름을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (websiteUrl.isBlank() && fallbackTextContent.isBlank())
                    {
                        Toast.makeText(context, "URL 주소 또는 직접 입력 본문이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isCrawlingInProgress = true
                    scope.launch {
                        try
                        {
                            var scannedContent = fallbackTextContent.trim()
                            
                            // If URL is supplied, try to fetch with Jsoup on Dispatchers.IO
                            if (websiteUrl.isNotBlank())
                            {
                                try
                                {
                                    val fetched = withContext(Dispatchers.IO) {
                                        val doc = Jsoup.connect(websiteUrl.trim())
                                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                            .timeout(8000)
                                            .get()
                                        
                                        // Main body parsing support
                                        val bodyContent = doc.select("div#mw-content-text").firstOrNull()?.text()
                                            ?: doc.body().text()
                                        bodyContent
                                    }
                                    if (fetched.isNotBlank())
                                    {
                                        scannedContent += "\n\n[크롤링된 링크 정보]\n$fetched"
                                    }
                                }
                                catch (e: Exception)
                                {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "링크 자동 크롤링 실패 (보안 제한). 붙여넣기 텍스트로 대체 시도합니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                            if (scannedContent.isBlank())
                            {
                                Toast.makeText(context, "스캔된 본문 내용이 없습니다. 직접 붙여넣기를 써보세요.", Toast.LENGTH_LONG).show()
                                isCrawlingInProgress = false
                                return@launch
                            }

                            val apiKey = viewModel.getApplication<android.app.Application>()
                                .let { app -> com.kitwlshcom.kdailyutil.data.repository.SettingsRepository(app) }
                                .geminiApiKeyFlow.first()

                            if (apiKey.isNullOrBlank())
                            {
                                Toast.makeText(context, "API 키가 설정되지 않았습니다.", Toast.LENGTH_LONG).show()
                                isCrawlingInProgress = false
                                return@launch
                            }

                            val geminiManager = com.kitwlshcom.kdailyutil.data.remote.GeminiManager(apiKey)
                            val jsonResult = geminiManager.generateQuizFromText(
                                topic = scannedContent.take(4000), // Protect context length limit
                                count = questionCount
                            )

                            if (jsonResult.isNotBlank())
                            {
                                val jsonArray = org.json.JSONArray(jsonResult)
                                val list = mutableListOf<QuizQuestion>()
                                for (i in 0 until jsonArray.length())
                                {
                                    val obj = jsonArray.getJSONObject(i)
                                    val optionsArray = obj.optJSONArray("options")
                                    val optionsList = if (optionsArray != null)
                                    {
                                        List(optionsArray.length()) { idx -> optionsArray.getString(idx) }
                                    }
                                    else null

                                    val baseQuestion = obj.getString("question")
                                    val uniqueId = Math.abs((categoryName + baseQuestion).hashCode())

                                    list.add(
                                        QuizQuestion(
                                            id = uniqueId,
                                            type = QuizType.valueOf(obj.getString("type")),
                                            category = categoryName,
                                            subCategory = "인터넷 크롤링 분석",
                                            question = baseQuestion,
                                            options = optionsList,
                                            answer = obj.getString("answer"),
                                            explanation = obj.getString("explanation"),
                                            semanticHint = obj.optString("semanticHint", null)
                                        )
                                    )
                                }
                                onQuizGenerated(list, categoryName)
                            }
                            else
                            {
                                Toast.makeText(context, "퀴즈 생성 실패: AI가 유효한 퀴즈 데이터를 생성하지 못했습니다. (빈 응답)", Toast.LENGTH_LONG).show()
                            }
                        }
                        catch (e: Exception)
                        {
                            Toast.makeText(context, "에러 발생: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        finally
                        {
                            isCrawlingInProgress = false
                        }
                    }
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold24K,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✨ 링크 및 텍스트 긁어와 AI 퀴즈 만들기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ----------------------------------------------------
// Tab 3: 수동 작성 에디터 & AI 보기 완성
// ----------------------------------------------------
@Composable
fun ManualCreatorTab(
    viewModel: QuizViewModel,
    creatorNickname: String,
    onNicknameChange: (String) -> Unit,
    isAnonymous: Boolean,
    onAnonymousChange: (Boolean) -> Unit,
    deviceHashId: String,
    onQuizSaved: (List<QuizQuestion>, String) -> Unit
)
{
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var categoryName by remember { mutableStateOf("") }
    
    // Question formulation
    var questionInput by remember { mutableStateOf("") }
    var answerInput by remember { mutableStateOf("") }
    var optionType by remember { mutableStateOf(QuizType.MULTIPLE_CHOICE) }

    // Option slots for manual/AI completion
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }
    var option4 by remember { mutableStateOf("") }
    
    var explanationInput by remember { mutableStateOf("") }
    var semanticHintInput by remember { mutableStateOf("") }

    var isAiGeneratingOptions by remember { mutableStateOf(false) }
    val customQuizSet = remember { mutableStateListOf<QuizQuestion>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        CreatorSettingsCard(
            creatorNickname = creatorNickname,
            onNicknameChange = onNicknameChange,
            isAnonymous = isAnonymous,
            onAnonymousChange = onAnonymousChange,
            deviceHashId = deviceHashId
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✍️ 고품격 퀴즈 수동 편집기",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
            if (customQuizSet.isNotEmpty())
            {
                Text(
                    text = "작성 중인 문제: ${customQuizSet.size}개",
                    color = Gold24K,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text("퀴즈 카테고리 이름") },
            placeholder = { Text("예: 나만의 영어 단어 암기장") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Type selection toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "문제 형식: ",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = optionType == QuizType.MULTIPLE_CHOICE,
                onClick = { optionType = QuizType.MULTIPLE_CHOICE },
                colors = RadioButtonDefaults.colors(selectedColor = Gold24K)
            )
            Text(
                text = "객관식(사지선다)",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clickable { optionType = QuizType.MULTIPLE_CHOICE }
            )
            Spacer(modifier = Modifier.width(20.dp))
            RadioButton(
                selected = optionType == QuizType.SUBJECTIVE,
                onClick = { optionType = QuizType.SUBJECTIVE },
                colors = RadioButtonDefaults.colors(selectedColor = Gold24K)
            )
            Text(
                text = "주관식",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clickable { optionType = QuizType.SUBJECTIVE }
            )
        }

        OutlinedTextField(
            value = questionInput,
            onValueChange = { questionInput = it },
            label = { Text("질문 내용 입력") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        OutlinedTextField(
            value = answerInput,
            onValueChange = { answerInput = it },
            label = { Text("정답 입력") },
            placeholder = { Text("객관식인 경우 아래 보기 중 하나와 완벽히 같아야 합니다.") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Multiple choice options section
        if (optionType == QuizType.MULTIPLE_CHOICE)
        {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "사지선다 보기 항목 설정",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold24K
                        )

                        if (isAiGeneratingOptions)
                        {
                            CircularProgressIndicator(
                                color = Gold24K,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        else
                        {
                            Text(
                                text = "🤖 AI 보기 자동 완성",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gold24K,
                                modifier = Modifier.clickable {
                                    if (questionInput.isBlank() || answerInput.isBlank())
                                    {
                                        Toast.makeText(context, "먼저 질문과 정답을 채워야 AI 오답 보기를 만들 수 있습니다.", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    isAiGeneratingOptions = true
                                    viewModel.generateWrongOptions(
                                        question = questionInput,
                                        answer = answerInput
                                    ) { options, explanation ->
                                        isAiGeneratingOptions = false
                                        if (options != null && options.size >= 4)
                                        {
                                            // Shuffling the options or binding directly
                                            val shuffled = options.shuffled()
                                            option1 = shuffled[0]
                                            option2 = shuffled[1]
                                            option3 = shuffled[2]
                                            option4 = shuffled[3]
                                            explanationInput = explanation
                                            Toast.makeText(context, "AI가 오답지와 정교한 해설을 생성했습니다!", Toast.LENGTH_SHORT).show()
                                        }
                                        else
                                        {
                                            Toast.makeText(context, "AI 보기 생성 실패: " + explanation, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = option1,
                        onValueChange = { option1 = it },
                        label = { Text("보기 1") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = option2,
                        onValueChange = { option2 = it },
                        label = { Text("보기 2") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = option3,
                        onValueChange = { option3 = it },
                        label = { Text("보기 3") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = option4,
                        onValueChange = { option4 = it },
                        label = { Text("보기 4") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        OutlinedTextField(
            value = explanationInput,
            onValueChange = { explanationInput = it },
            label = { Text("문제 해설 입력") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        OutlinedTextField(
            value = semanticHintInput,
            onValueChange = { semanticHintInput = it },
            label = { Text("풀이 힌트 입력(선택 사항)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold24K,
                focusedLabelColor = Gold24K,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Command Button: Add to package list
        Button(
            onClick = {
                if (categoryName.isBlank())
                {
                    Toast.makeText(context, "카테고리 이름을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (questionInput.isBlank() || answerInput.isBlank())
                {
                    Toast.makeText(context, "질문과 정답을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val options = if (optionType == QuizType.MULTIPLE_CHOICE)
                {
                    val list = listOf(option1.trim(), option2.trim(), option3.trim(), option4.trim())
                    if (list.any { it.isBlank() })
                    {
                        Toast.makeText(context, "모든 보기 칸을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!list.contains(answerInput.trim()))
                    {
                        Toast.makeText(context, "보기 4개 중에 반드시 '정답' 텍스트와 정확하게 일치하는 항목이 존재해야 합니다.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    list
                }
                else null

                val uniqueId = Math.abs((categoryName + questionInput).hashCode())
                val newQ = QuizQuestion(
                    id = uniqueId,
                    type = optionType,
                    category = categoryName,
                    subCategory = "수동 작성 문제",
                    question = questionInput,
                    options = options,
                    answer = answerInput.trim(),
                    explanation = explanationInput,
                    semanticHint = semanticHintInput.ifBlank { null }
                )

                customQuizSet.add(newQ)
                
                // Clear fields for next question formulation
                questionInput = ""
                answerInput = ""
                option1 = ""
                option2 = ""
                option3 = ""
                option4 = ""
                explanationInput = ""
                semanticHintInput = ""
                Toast.makeText(context, "문제가 리스트에 추가되었습니다!", Toast.LENGTH_SHORT).show()
            },
            enabled = true,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Gold24K
            ),
            border = BorderStroke(1.dp, Gold24K.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("작성 중인 문제 꾸러미에 추가 (+)", fontWeight = FontWeight.Bold)
        }

        // Action Buttons: Complete Package creation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (customQuizSet.isEmpty())
                    {
                        Toast.makeText(context, "먼저 문제를 최소 1개 이상 추가해야 패키지를 저장할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onQuizSaved(customQuizSet.toList(), categoryName)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold24K,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("📦 최종 퀴즈 세트 등록", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------
// Smart compression utilities on Dispatchers.IO
// ----------------------------------------------------
private suspend fun loadAndCompressImage(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO)
{
    var inputStream = context.contentResolver.openInputStream(uri)
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream?.close()

    // Calculate scale sample size
    val maxDim = 1280
    var scale = 1
    if (options.outWidth > maxDim || options.outHeight > maxDim)
    {
        val largerDim = if (options.outWidth > options.outHeight) options.outWidth else options.outHeight
        scale = largerDim / maxDim
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = if (scale < 1) 1 else scale
    }
    
    inputStream = context.contentResolver.openInputStream(uri)
    val loaded = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
    inputStream?.close()
    
    loaded ?: return@withContext null

    val width = loaded.width
    val height = loaded.height
    if (width > maxDim || height > maxDim)
    {
        val ratio = width.toFloat() / height.toFloat()
        val newWidth = if (width > height) maxDim else (maxDim * ratio).toInt()
        val newHeight = if (width > height) (maxDim / ratio).toInt() else maxDim
        val resized = Bitmap.createScaledBitmap(loaded, newWidth, newHeight, true)
        if (resized != loaded)
        {
            loaded.recycle()
        }
        return@withContext resized
    }

    return@withContext loaded
}

private fun getTempImageUri(context: Context): Uri?
{
    return try
    {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir
        val tempFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
    catch (e: Exception)
    {
        null
    }
}

private fun getDeviceHashId(context: Context): String
{
    return try
    {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(androidId.toByteArray(Charsets.UTF_8))
        hash.joinToString("") { "%02x".format(it) }
    }
    catch (e: Exception)
    {
        "usr_" + UUID.randomUUID().toString().take(12)
    }
}

@Composable
fun CreatorSettingsCard(
    creatorNickname: String,
    onNicknameChange: (String) -> Unit,
    isAnonymous: Boolean,
    onAnonymousChange: (Boolean) -> Unit,
    deviceHashId: String
)
{
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, Gold24K.copy(alpha = 0.2f))
    )
    {
        Column(
            modifier = Modifier.padding(16.dp)
        )
        {
            Text(
                text = "👤 출제자 프로필 설정",
                fontWeight = FontWeight.Bold,
                color = Gold24K,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                OutlinedTextField(
                    value = creatorNickname,
                    onValueChange = onNicknameChange,
                    label = { Text("닉네임") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold24K,
                        focusedLabelColor = Gold24K,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.End
                )
                {
                    Text(
                        text = "기기 고유 ID",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = deviceHashId.take(10) + "...",
                        color = Gold24K,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        Text(
                            text = "익명 토글",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Switch(
                            checked = isAnonymous,
                            onCheckedChange = onAnonymousChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Gold24K,
                                checkedTrackColor = Gold24K.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionCountChip(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Gold24K else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (isSelected) Gold24K else Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${count}개",
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

fun cropBitmapFromBoundingBox(
    bitmap: Bitmap,
    boundingBox: org.json.JSONArray?
): Bitmap? {
    if (boundingBox == null || boundingBox.length() != 4) return null
    return try {
        val ymin = boundingBox.getInt(0)
        val xmin = boundingBox.getInt(1)
        val ymax = boundingBox.getInt(2)
        val xmax = boundingBox.getInt(3)

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Add 5% padding to prevent clipping of borders
        val boxWidth = xmax - xmin
        val boxHeight = ymax - ymin
        val paddingX = (boxWidth * 0.05).toInt()
        val paddingY = (boxHeight * 0.05).toInt()

        val left = Math.max(0, ((xmin - paddingX) * originalWidth) / 1000)
        val top = Math.max(0, ((ymin - paddingY) * originalHeight) / 1000)
        val right = Math.min(originalWidth, ((xmax + paddingX) * originalWidth) / 1000)
        val bottom = Math.min(originalHeight, ((ymax + paddingY) * originalHeight) / 1000)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) null
        else Bitmap.createBitmap(bitmap, left, top, width, height)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, categoryName: String, questionIndex: Int): String? {
    return try {
        val directory = File(context.filesDir, "cropped_quizzes")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val safeCategory = categoryName.replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
        val fileName = "crop_${safeCategory}_${System.currentTimeMillis()}_${questionIndex}.png"
        val file = File(directory, fileName)
        
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


