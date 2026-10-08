package com.kitwlshcom.kdailyutil.update

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 「새 버전이 있어요」 — **유연(FLEXIBLE) 인앱 업데이트** (2026-10-08).
 *
 * ## 🔴 이게 없으면 어떻게 되나 (2026-10-08에 코드로 확인한 사실)
 *
 * **앱은 자기가 업데이트됐다는 것도, 새 버전이 나왔다는 것도 전혀 모른다.** 사용자가 아는 길은
 * Play 스토어의 자동 업데이트(조용히 깔린다)와 스토어 앱의 «업데이트 사용 가능» 목록뿐이고,
 * **자동 업데이트를 꺼 둔 사람은 우리 앱을 열어도 아무 말도 듣지 못한다.**
 *
 * ## 왜 FLEXIBLE인가 (IMMEDIATE가 아니라)
 *
 * - **FLEXIBLE** = 쓰던 것을 막지 않는다. 뒤에서 받아 두고, 다 받으면 «다시 시작할까요»만 띄운다
 * - **IMMEDIATE** = 전체 화면으로 막고 강제한다. **치명적 버그를 긴급 배포할 때만** 쓸 물건이다
 *
 * 🔴 우리는 그런 긴급 배포를 **한 번도 해 본 적이 없다**(1.7.4도 결국 필요 없었다).
 * 막을 이유가 없는데 막으면 그건 기능이 아니라 방해다.
 *
 * ## ⚠️ 조용히 아무 일도 안 하는 경우가 정상이다
 *
 * 이 API는 **Play 스토어가 설치한 앱**에서만 작동한다. 그래서 다음은 **실패가 아니라 정상**이다:
 * - 개발용 빌드(`.debug`) · Android Studio에서 ▶ Run으로 깔린 앱 · APK 직접 설치
 * - Play가 아직 그 버전을 그 기기에 안 내려 줬을 때(단계적 출시 중)
 *
 * 그래서 **실패는 전부 삼키고 로그만 남긴다.** 사용자에게 「업데이트 확인 실패」를 보여 줄 이유가 없다 —
 * 그것은 사용자가 할 수 있는 일이 없는 소식이다.
 *
 * ## 쓰는 법 (자매앱에 옮길 때도 이 네 줄이 전부다)
 *
 * ```kotlin
 * // Activity의 필드
 * private val inAppUpdate by lazy { InAppUpdateController(this) }
 * // onCreate에서 — setContent보다 먼저
 * inAppUpdate.register()
 * // Compose 안에서 — 다 받았을 때 띄울 자리
 * val ready by inAppUpdate.readyToInstall.collectAsState()
 * // 사용자가 «다시 시작»을 눌렀을 때
 * inAppUpdate.completeUpdate()
 * ```
 */
class InAppUpdateController(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "InAppUpdate"
        private const val PREFS = "in_app_update"
        private const val KEY_ASKED_VERSION = "asked_version_code"
        private const val KEY_ASKED_AT = "asked_at_ms"
    }

    private val manager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(activity) }
    private val prefs by lazy { activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /**
     * **다 받아서 다시 시작하면 되는 상태**인가. 화면이 이 값을 보고 띄운다.
     *
     * 🔴 다 받아 놓고 말하지 않으면 사용자는 **영영 새 버전을 쓰지 못한다** — 받은 것이
     * 설치되려면 앱이 한 번 꺼졌다 켜져야 하는데, 그 사실을 아는 사람은 우리뿐이다.
     */
    private val _readyToInstall = MutableStateFlow(false)
    val readyToInstall: StateFlow<Boolean> = _readyToInstall.asStateFlow()

    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "내려받기 끝 — 다시 시작하면 적용된다")
                _readyToInstall.value = true
            }
            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                Log.d(TAG, "내려받기 중단(status=${state.installStatus()}) — 조용히 지나간다")
                _readyToInstall.value = false
            }
            else -> Unit
        }
    }

    /** onCreate에서 **setContent보다 먼저** 부른다(ActivityResult 등록은 STARTED 전에 해야 한다). */
    fun register() {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            // 🔴 사용자가 거절해도 **아무 말도 하지 않는다.** 거절은 선택이지 오류가 아니다.
            //    다만 «물어봤다»는 사실은 이미 checkForUpdate에서 적어 뒀으므로
            //    [UpdatePromptPolicy.QUIET_DAYS] 동안 다시 묻지 않는다.
            if (result.resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "사용자가 업데이트를 받지 않기로 했다(code=${result.resultCode})")
            }
        }
        manager.registerListener(installListener)
    }

    /** onDestroy에서 부른다. 안 부르면 리스너가 Activity를 붙잡고 있는다. */
    fun unregister() {
        runCatching { manager.unregisterListener(installListener) }
    }

    /**
     * 새 버전이 있는지 보고, 있으면 **조건이 맞을 때만** 묻는다.
     * onCreate와 onResume 양쪽에서 불러도 된다 — 조르기는 [UpdatePromptPolicy]가 막는다.
     */
    fun checkForUpdate() {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                // ① 이미 받아 둔 것이 있나 — 앱이 꺼져 있는 동안 끝났을 수 있다.
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _readyToInstall.value = true
                    return@addOnSuccessListener
                }
                // ② 새 버전이 있고, 유연 업데이트가 허용되나.
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                if (!available || !info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return@addOnSuccessListener

                val version = info.availableVersionCode()
                val ask = UpdatePromptPolicy.shouldPrompt(
                    availableVersionCode = version,
                    lastAskedVersionCode = prefs.getInt(KEY_ASKED_VERSION, UpdatePromptPolicy.NEVER_ASKED),
                    lastAskedAtMs = prefs.getLong(KEY_ASKED_AT, 0L),
                    nowMs = System.currentTimeMillis()
                )
                if (!ask) {
                    Log.d(TAG, "새 버전 $version 이 있지만 최근에 물어봤다 — 쉬어 간다")
                    return@addOnSuccessListener
                }

                val target = launcher ?: return@addOnSuccessListener
                // 🔴 **묻기 «전»에 적는다.** 다이얼로그가 뜬 뒤 앱이 죽으면 적을 기회가 없고,
                //    그러면 다음 실행에 또 묻는다.
                prefs.edit()
                    .putInt(KEY_ASKED_VERSION, version)
                    .putLong(KEY_ASKED_AT, System.currentTimeMillis())
                    .apply()
                runCatching {
                    manager.startUpdateFlowForResult(
                        info,
                        target,
                        com.google.android.play.core.appupdate.AppUpdateOptions
                            .newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }.onFailure { Log.w(TAG, "업데이트 창을 열지 못했다: ${it.message}") }
            }
            .addOnFailureListener {
                // 🔴 **정상인 실패다** — 개발용 빌드·직접 설치한 APK에서는 항상 여기로 온다.
                //    사용자가 할 수 있는 일이 없는 소식이라 화면에 올리지 않는다.
                Log.d(TAG, "업데이트 정보를 못 받았다(Play 설치본이 아닐 수 있다): ${it.message}")
            }
    }

    /** 사용자가 「지금 다시 시작」을 눌렀다 → 받아 둔 것을 설치하고 앱을 재시작한다. */
    fun completeUpdate() {
        runCatching { manager.completeUpdate() }
            .onFailure { Log.w(TAG, "설치를 시작하지 못했다: ${it.message}") }
    }

    /** 사용자가 「나중에」를 눌렀다 → 배너만 내린다. 받아 둔 것은 그대로 있다가 다음에 깔린다. */
    fun dismissReady() {
        _readyToInstall.value = false
    }
}
