package com.kitwlshcom.kdailyutil.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kitwlshcom.kdailyutil.data.ai.errorMessage
import com.kitwlshcom.kdailyutil.data.ai.unavailableMessage
import com.kitwlshcom.kdailyutil.data.disclosure.AlertHistoryEntry
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureAlertPolicy
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureAlertStore
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureAlertWorker
import com.kitwlshcom.kdailyutil.data.disclosure.DisclosureCategory
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import com.kitwlshcom.kdailyutil.ui.theme.Gold24K
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 면책 한 줄 — STORE_LISTING.md의 문구와 같은 뜻. 🔴 «매수·매도·신호» 같은 말은 이 기능 어디에도 쓰지 않는다. */
const val DISCLOSURE_DISCLAIMER = "공시 사실만 전합니다. 투자 권유가 아니며, 투자 판단과 책임은 본인에게 있습니다."

private val TIME_FMT = DateTimeFormatter.ofPattern("M/d HH:mm")

/**
 * 설정 › 증시 — «🔔 관심종목 공시 알림» 섹션(1.7.5 · FEATURE_DISCLOSURE_ALERT §6).
 *
 * BriefingViewModel을 더 키우지 않으려고 저장소를 직접 읽는다(이 섹션만의 상태).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisclosureAlertSettingsSection() {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val store = remember { DisclosureAlertStore(context) }
    val scope = rememberCoroutineScope()

    val enabled by settings.disclosureAlertEnabledFlow.collectAsState(initial = false)
    val categories by settings.disclosureAlertCategoriesFlow.collectAsState(initial = DisclosureCategory.defaults)
    val corrections by settings.disclosureAlertCorrectionsFlow.collectAsState(initial = false)

    // 마지막 확인 — 워커가 SharedPreferences에 쓰므로, 켜고 끌 때·[지금 확인] 때 다시 읽는다
    var refreshTick by remember { mutableIntStateOf(0) }
    val lastCheck = remember(refreshTick, enabled) { store.lastCheck() }
    val lastResult = remember(refreshTick, enabled) { store.lastResult() }
    var notifAllowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifAllowed = granted
    }

    Text("🔔 관심종목 공시 알림", style = MaterialTheme.typography.titleMedium, color = Gold24K)
    Text(
        "앱을 켜지 않아도 위 관심종목 중 국내 상장사에 새 공시가 올라오면 알려 드려요.\n" +
            "평일 하루 4번(${DisclosureAlertPolicy.SLOTS.joinToString(" · ")} 무렵) 확인합니다. " +
            "한 번에 최대 ${DisclosureAlertPolicy.MAX_CORPS_PER_RUN}종목 · 해외 종목·코인·지수는 제외돼요.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.6f)
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("공시 알림 받기", modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { on ->
            scope.launch {
                if (on) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    DisclosureAlertWorker.enable(context)
                } else {
                    settings.setDisclosureAlertEnabled(false)
                    DisclosureAlertWorker.disable(context)
                }
                refreshTick++
            }
        })
    }

    if (!enabled) return

    if (!notifAllowed) {
        Text(
            "⚠️ 이 앱의 알림이 꺼져 있어요. 휴대폰 설정 › 앱 › KDailyUtil › 알림에서 켜 주세요. (기록은 증시 탭 🔔에 계속 남아요)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFFB74D)
        )
    }

    Spacer(Modifier.height(6.dp))
    Text("알릴 갈래", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DisclosureCategory.selectable.forEach { c ->
            FilterChip(
                selected = c in categories,
                onClick = {
                    scope.launch {
                        settings.setDisclosureAlertCategories(
                            if (c in categories) categories - c else categories + c
                        )
                    }
                },
                label = { Text("${c.badge} ${c.label}") }
            )
        }
        FilterChip(
            selected = corrections,
            onClick = { scope.launch { settings.setDisclosureAlertCorrections(!corrections) } },
            label = { Text("✏️ 정정공시") }
        )
    }
    Text(
        "희석·위험 = 유상증자·전환사채·감자·관리종목 등 · 구조 변화 = 최대주주 변경·합병·분할·지분 매각 등 · " +
            "주주환원 = 자사주·배당 · 실적 = 잠정실적·분기/사업보고서 · 사업 = 공급계약·시설투자. " +
            "임원 지분 보고서처럼 매일 쏟아지는 공시는 알리지 않아요.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.5f)
    )

    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                "마지막 확인: " + (lastCheck?.format(TIME_FMT) ?: "아직 없음") +
                    (lastResult?.let { " · ${it.label}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                "다음 확인: " + DisclosureAlertPolicy.nextSlot(LocalDateTime.now()).format(TIME_FMT) + " 무렵",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        TextButton(onClick = {
            DisclosureAlertWorker.runNow(context)
            refreshTick++
        }) { Text("지금 확인") }
    }
}

/**
 * 증시 탭 › «🔔 공시 알림 기록» — 최근 30일 받은 알림. 알림을 놓쳐도 여기서 본다(§6).
 * 알림을 탭하면 이 창이 열린다.
 */
@Composable
fun DisclosureAlertHistoryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entries = remember { DisclosureAlertStore(context).history() }
    val enabled by remember { SettingsRepository(context) }.disclosureAlertEnabledFlow.collectAsState(initial = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        title = { Text("🔔 공시 알림 기록", fontWeight = FontWeight.Bold, color = Gold24K) },
        text = {
            Column {
                if (entries.isEmpty()) {
                    Text(
                        if (enabled) "아직 받은 알림이 없어요. 관심종목에 새 공시가 올라오면 여기에 쌓여요."
                        else "공시 알림이 꺼져 있어요. 설정 › 증시 › «🔔 관심종목 공시 알림»에서 켤 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(entries, key = { it.item.rceptNo }) { e -> HistoryRow(e) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(DISCLOSURE_DISCLAIMER, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

@Composable
private fun HistoryRow(e: AlertHistoryEntry) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { DisclosureAlertStore(context) }
    val at = Instant.ofEpochMilli(e.notifiedAtMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    // 🤖 3줄 해설 — 누를 때만 만든다(결정 ③). 한 번 만든 것은 캐시에서 바로 보인다.
    var explanation by remember(e.item.rceptNo) { mutableStateOf(store.explanation(e.item.rceptNo)) }
    var explaining by remember { mutableStateOf(false) }
    var explainError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "${e.category.badge} ${e.category.label} · ${e.item.corpName} · ${at.format(TIME_FMT)}",
            style = MaterialTheme.typography.labelMedium,
            color = Gold24K
        )
        Text(DisclosureCategory.displayTitle(e.item.reportNm), style = MaterialTheme.typography.bodyMedium)

        explanation?.let { text ->
            Text(
                text.lines().joinToString("\n") { "🤖 $it" },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "AI가 공시 제목만 보고 쓴 일반 해설이에요. 숫자·세부는 원문으로 확인하세요.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
        explainError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB74D)) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                contentPadding = PaddingValues(0.dp),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=${e.item.rceptNo}"))
                        )
                    }
                }
            ) { Text("DART 원문", fontSize = 12.sp) }
            Spacer(Modifier.width(12.dp))
            when {
                explaining -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                explanation == null -> TextButton(
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        explaining = true
                        explainError = null
                        scope.launch {
                            val (text, err) = explainDisclosure(context, e)
                            if (text != null) {
                                store.saveExplanation(e.item.rceptNo, text)
                                explanation = text
                            }
                            explainError = err
                            explaining = false
                        }
                    }
                ) { Text("🤖 3줄 해설", fontSize = 12.sp) }
                else -> DisclosureReportLink(e, explanation!!)
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
    }
}

/** 해설 1건 — (해설, 오류 안내) 중 하나만 채워 돌려준다. 체험 횟수는 **성공했을 때만** 1회 쓴다. */
private suspend fun explainDisclosure(context: android.content.Context, e: AlertHistoryEntry): Pair<String?, String?> {
    val keys = com.kitwlshcom.kdailyutil.data.ai.AiKeyProvider(SettingsRepository(context))
    val access = keys.resolve(com.kitwlshcom.kdailyutil.data.ai.AiArea.STOCK)
    val key = access.keyOrNull
        ?: return null to access.unavailableMessage("설정 › AI·키 에서 Gemini API Key를 넣으면 3줄 해설을 볼 수 있어요.")
    return try {
        val raw = com.kitwlshcom.kdailyutil.data.remote.GeminiManager(key).explainDisclosure(
            com.kitwlshcom.kdailyutil.data.disclosure.DisclosureExplain.prompt(
                e.item.corpName, e.category, e.item.reportNm, e.item.rceptDt
            )
        )
        val text = com.kitwlshcom.kdailyutil.data.disclosure.DisclosureExplain.clean(raw)
        if (text == null) {
            null to "해설을 만들지 못했어요(형식이 맞지 않았어요). 잠시 뒤 다시 눌러 주세요."
        } else {
            keys.recordUse(access)
            text to null
        }
    } catch (ex: Exception) {
        null to "해설을 만들지 못했어요.\n" + access.errorMessage(ex)
    }
}

/** 🚩 신고 — 「📊 AI 실적 요약」과 같은 메일 신고(Play «AI 생성 콘텐츠» 정책). 식별 정보는 우리가 채운다. */
@Composable
private fun DisclosureReportLink(e: AlertHistoryEntry, text: String) {
    val context = LocalContext.current
    val what = "공시 해설 · ${e.item.corpName} · ${DisclosureCategory.displayTitle(e.item.reportNm)} (${e.item.rceptNo})"
    TextButton(
        contentPadding = PaddingValues(0.dp),
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(com.kitwlshcom.kdailyutil.data.AiContentReport.EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, com.kitwlshcom.kdailyutil.data.AiContentReport.aiSubject(what))
                putExtra(Intent.EXTRA_TEXT, com.kitwlshcom.kdailyutil.data.AiContentReport.aiBody(what, text))
            }
            // 메일 앱이 없는 기기가 실제로 있다 — 잡지 않으면 신고하려다 앱이 죽는다
            try {
                context.startActivity(intent)
            } catch (ex: Exception) {
                android.widget.Toast.makeText(context, "이메일 앱을 찾을 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    ) { Text("🚩 이 해설 신고", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) }
}
