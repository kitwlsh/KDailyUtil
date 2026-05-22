package com.kitwlshcom.kdailyutil.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.QuizViewModel
import com.kitwlshcom.kdailyutil.ui.viewmodel.QuizState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningHubScreen(
    quizViewModel: QuizViewModel = viewModel()
)
{
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("우리말 퀴즈", "지식 플러스 (준비 중)")

    val quizState by quizViewModel.quizState.collectAsState()
    val isMainHubActive = quizState == QuizState.IDLE || quizState == QuizState.CATEGORY_SELECTION

    LaunchedEffect(Unit)
    {
        quizViewModel.syncRemoteData()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (isMainHubActive)
            {
                TopAppBar(
                    title = { Text("배움터", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                    )
                )
            }
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
        )
        {
            if (isMainHubActive)
            {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Black.copy(alpha = 0.2f),
                    contentColor = com.kitwlshcom.kdailyutil.ui.theme.Gold24K,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = com.kitwlshcom.kdailyutil.ui.theme.Gold24K
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTabIndex == index) com.kitwlshcom.kdailyutil.ui.theme.Gold24K else Color.White.copy(alpha = 0.6f)
                                ) 
                            }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize())
                {
                    when (selectedTabIndex)
                    {
                        0 -> QuizScreen(viewModel = quizViewModel)
                        1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
                        {
                            Text("새로운 학습 기능이 추가될 예정입니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            else
            {
                // Active quiz session - hide topbar and tabrow entirely for full-height space
                Box(modifier = Modifier.fillMaxSize())
                {
                    QuizScreen(viewModel = quizViewModel)
                }
            }
        }
    }
}
