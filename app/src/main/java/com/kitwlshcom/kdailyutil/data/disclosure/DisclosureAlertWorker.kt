package com.kitwlshcom.kdailyutil.data.disclosure

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kitwlshcom.kdailyutil.MainActivity
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import com.kitwlshcom.kdailyutil.data.repository.StockRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * 🔔 관심종목 공시 알림 — **앱을 안 켜도** 관심종목의 새 공시를 확인한다(1.7.5 · P1).
 *
 * 🔴 **30분마다 깨지만 실제 조회는 하루 4번뿐이다** — [DisclosureAlertPolicy.isDue]가 «지난 확인 시각 이후로
 * 아직 안 봤나»만 보고, 아니면 네트워크도 안 쓰고 바로 끝난다. 정시 예약(OneTime 체인)을 쓰지 않은 이유:
 * 워커 안에서 자기 자신을 REPLACE로 다시 걸면 **돌고 있는 자신이 취소되는** 함정이 있고, 체인은 한 번
 * 끊기면 아무도 다시 잇지 않는다. 주기 작업은 재부팅에도 WorkManager가 스스로 되살린다.
 *
 * ⚠️ 기존 아침 브리핑(`BriefingScheduler` = AlarmManager · 정시 필요)과는 **섞지 않는다.**
 */
class DisclosureAlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = LOCK.withLock {
        val ctx = applicationContext
        val settings = SettingsRepository(ctx)
        if (!settings.disclosureAlertEnabledFlow.first()) return@withLock Result.success()

        val store = DisclosureAlertStore(ctx)
        val now = LocalDateTime.now()
        val force = inputData.getBoolean(KEY_FORCE, false)
        if (!force && !DisclosureAlertPolicy.isDue(now, store.lastCheck())) return@withLock Result.success()
        if (!force && store.quotaPausedDay() == now.toLocalDate().toEpochDay()) return@withLock Result.success()

        val apiKey = settings.dartApiKeyFlow.first()
        val stocks = StockRepository(ctx)

        // 1. 관심종목 → DART 고유번호 (해외·코인·지수는 건너뛴다 · 상한 10곳)
        val corps = LinkedHashMap<String, String>()
        for (w in settings.watchStockKeywordsFlow.first()) {
            if (corps.size >= DisclosureAlertPolicy.MAX_CORPS_PER_RUN) break
            stocks.resolveCorpCode(w, apiKey)?.let { (code, name) -> corps.putIfAbsent(code, name) }
        }
        if (corps.isEmpty()) {
            store.setChecked(now, AlertCheckResult.NO_CORPS)
            return@withLock Result.success()
        }

        // 2. 회사마다 최근 공시
        val fmt = DateTimeFormatter.BASIC_ISO_DATE
        val bgn = now.toLocalDate().minusDays(DisclosureAlertPolicy.LOOKBACK_DAYS).format(fmt)
        val end = now.toLocalDate().format(fmt)
        val fetched = mutableListOf<DisclosureItem>()
        val okCorps = mutableSetOf<String>()
        var stop: AlertCheckResult? = null
        for (code in corps.keys) {
            when (val r = stocks.fetchCorpDisclosures(code, bgn, end, apiKey)) {
                is StockRepository.DartListResult.Ok -> { fetched += r.items; okCorps += code }
                StockRepository.DartListResult.Quota -> { stop = AlertCheckResult.QUOTA; break }
                StockRepository.DartListResult.BadKey -> { stop = AlertCheckResult.BAD_KEY; break }
                StockRepository.DartListResult.Failed -> Unit // 이 회사만 다음 회차에 다시 본다
            }
        }
        if (stop == AlertCheckResult.QUOTA) store.setQuotaPaused(now.toLocalDate())

        // 3. 처음 보는 회사는 알리지 않고 seen만 채운다(켜자마자 지난 공시가 쏟아지지 않게)
        val primed = store.primedCorps()
        val (newCorpItems, knownCorpItems) = fetched.partition { it.corpCode !in primed }
        val seen = store.seen().keys
        val entries = DisclosureAlertPolicy.select(
            fresh = knownCorpItems,
            seen = seen,
            enabled = settings.disclosureAlertCategoriesFlow.first(),
            includeCorrections = settings.disclosureAlertCorrectionsFlow.first()
        )

        // 4. 알림 + 기록. seen에는 걸러진 것까지 전부 넣는다(설정을 바꿨다고 지난 공시가 뒤늦게 오지 않게)
        store.addSeen((knownCorpItems + newCorpItems).map { it.rceptNo }.filter { it.isNotBlank() }, now.toLocalDate())
        store.setPrimedCorps(primed + okCorps)
        if (entries.isNotEmpty()) {
            store.addHistory(entries, System.currentTimeMillis())
            notify(ctx, DisclosureAlertPolicy.buildNotices(entries))
        }

        val result = stop ?: when {
            okCorps.isEmpty() -> AlertCheckResult.NETWORK
            primed.isEmpty() -> AlertCheckResult.PRIMED
            else -> AlertCheckResult.OK
        }
        // 🔴 네트워크로 전부 실패했으면 lastCheck를 올리지 않는다 — 30분 뒤 깰 때 다시 시도한다.
        if (result == AlertCheckResult.NETWORK) {
            store.setChecked(store.lastCheck() ?: now.minusDays(1), result)
        } else {
            store.setChecked(now, result)
        }
        Log.d(TAG, "공시 확인: 회사 ${corps.size} · 받은 공시 ${fetched.size} · 알림 ${entries.size} · $result")
        Result.success()
    }

    private fun notify(ctx: Context, notices: List<AlertNotice>) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NAVIGATE_TO", "stock")
            putExtra("STOCK_SUBTAB", SUBTAB_ALERT_HISTORY)
        }
        val pi = PendingIntent.getActivity(
            ctx, 4000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notices.forEach { n ->
            val noti = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(n.title)
                .setContentText(n.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(n.text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            // 종목마다 자리가 따로 — 같은 종목의 다음 알림은 앞 것을 갈아 끼운다
            val id = 4100 + ((n.corpCode ?: "group").hashCode() and 0x3FF)
            try {
                nm.notify(id, noti)
            } catch (e: SecurityException) {
                Log.w(TAG, "알림 권한 없음: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "DisclosureAlert"
        const val CHANNEL_ID = "disclosure_alert_channel"
        private const val WORK_PERIODIC = "disclosure_alert_periodic"
        private const val WORK_NOW = "disclosure_alert_now"
        private const val KEY_FORCE = "force"

        /**
         * 알림을 탭하면 증시 탭이 이 값을 받아 «🔔 공시 알림 기록»을 연다.
         * 실제 서브탭 번호(0~2)와 겹치지 않게 둔 신호값이다 → `StockDashboardScreen`.
         */
        const val SUBTAB_ALERT_HISTORY = 90

        /** 워커 두 개(주기 · 즉시)가 겹쳐 돌아도 seen을 동시에 쓰지 않게. */
        private val LOCK = Mutex()

        fun ensureChannel(nm: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "관심종목 공시 알림", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = "관심종목에 새 공시가 올라오면 알려 줍니다(하루 4번 확인)" }
                )
            }
        }

        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /**
         * 켜져 있으면 주기 작업을 **이미 있으면 그대로 두고(KEEP)** 건다 — 앱을 열 때마다 불러도 안전하다.
         * 꺼져 있으면 아무것도 하지 않는다(끄는 쪽은 [disable]).
         */
        suspend fun ensureScheduled(ctx: Context) {
            if (!SettingsRepository(ctx).disclosureAlertEnabledFlow.first()) return
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DisclosureAlertWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(network)
                    .build()
            )
        }

        /** 켜는 순간: 지금 한 번 돌려 seen을 채우고(알림 없음), 주기 작업을 건다. */
        suspend fun enable(ctx: Context) {
            DisclosureAlertStore(ctx).resetForReenable()
            SettingsRepository(ctx).setDisclosureAlertEnabled(true)
            runNow(ctx)
            ensureScheduled(ctx)
        }

        fun disable(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_PERIODIC)
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NOW)
        }

        /** 시각과 무관하게 지금 한 번 확인한다(켤 때 · 설정의 [지금 확인]). */
        fun runNow(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                WORK_NOW,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DisclosureAlertWorker>()
                    .setConstraints(network)
                    .setInputData(androidx.work.workDataOf(KEY_FORCE to true))
                    .build()
            )
        }
    }
}
