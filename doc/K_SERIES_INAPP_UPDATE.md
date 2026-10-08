# 🔄 K-시리즈 「새 버전이 있어요」 인앱 업데이트 표준 (K-Series In-App Update)

K로 시작하는 형제 앱(**KDailyUtil · KLotto645 · K장부 · K달인**)이 **같은 방식으로**
업데이트를 안내하기 위한 **단일 기준 문서**입니다. 2026-10-08에 **KDailyUtil에서 먼저 구현**했고,
이 문서는 그것을 **그대로 옮기는 절차**입니다.

> 🔁 **4벌 동기화 문서**: 이 파일은 **`KDailyUtil/doc/`·`KLotto645/doc/`·`KJangbu/doc/`·`KDalin/doc/`**
> 네 곳에 **동일 사본**으로 유지한다. 어느 한 곳에서 고치면 나머지도 같은 내용으로 맞춘다.
> 🔴 **저장소별 예외 줄을 두지 않는다** — 자기 저장소에서만 의미 있는 메모는 그 저장소의 자기 문서에 둔다.
> ✅ 동일성 확인·복사는 **`K's works/tools/sync_docs.py`** 로 한다(작업 트리 md5 대조는 CRLF 때문에 실패한다).
> ⚠️ 파일 참조는 **저장소 접두 평문 경로**(`` `KDailyUtil/app/src/...` ``)로 쓴다.

---

## 0. 🔴 이게 없으면 어떻게 되나 (2026-10-08 코드로 확인한 사실)

**앱은 자기가 업데이트됐다는 것도, 새 버전이 나왔다는 것도 전혀 모른다.**
사용자가 아는 길은 둘뿐이다:

| 경로 | 사용자가 겪는 것 |
|---|---|
| Play **자동 업데이트가 켜져 있다**(기본값: Wi-Fi에서만) | 백그라운드에서 **조용히** 깔린다. Play의 «업데이트 알림» 토글이 켜져 있어야 알림이라도 온다 |
| Play **자동 업데이트가 꺼져 있다** | 🔴 **스토어 앱의 «업데이트 사용 가능» 목록에만 뜬다. 우리 앱을 열어도 아무 말도 안 한다** |

즉 **«실행할 때 업데이트 받으라고 나오는 것»은 앱이 그 기능을 넣어야 나온다.** 안 넣으면 안 나오는 것이 정상이다.

## 1. 왜 FLEXIBLE인가 (IMMEDIATE가 아니라)

| 방식 | 동작 | 언제 쓰나 |
|---|---|---|
| **FLEXIBLE** ✅ | 쓰던 것을 막지 않는다. 뒤에서 받아 두고, 다 받으면 «다시 시작할까요»만 띄운다 | **기본값** |
| IMMEDIATE | 전체 화면으로 막고 강제한다 | **치명적 버그를 긴급 배포할 때만** |

🔴 **K-시리즈는 그런 긴급 배포를 한 번도 해 본 적이 없다.** 막을 이유가 없는데 막으면 기능이 아니라 방해다.

## 2. ⚠️ 조용히 아무 일도 안 하는 경우가 «정상»이다

이 API는 **Play 스토어가 설치한 앱**에서만 작동한다. 다음은 **실패가 아니라 정상**이다:

- 개발용 빌드(`.debug`) · Android Studio ▶ Run으로 깔린 앱 · APK 직접 설치
- Play가 아직 그 버전을 그 기기에 안 내려 줬을 때(단계적 출시 중)

🔴 **그래서 실패는 전부 삼키고 로그만 남긴다.** 「업데이트 확인 실패」는 **사용자가 할 수 있는 일이 없는 소식**이다.
⚠️ **이 때문에 폰에서 «되는지» 확인하기가 어렵다** → §6 참고.

---

## 3. 넣는 절차 — 다섯 단계

### ① 버전 카탈로그 (`gradle/libs.versions.toml`)

```toml
[versions]
appUpdate = "2.1.0"

[libraries]
play-app-update-ktx = { group = "com.google.android.play", name = "app-update-ktx", version.ref = "appUpdate" }
```

### ② 의존성 (`app/build.gradle.kts`)

```kotlin
implementation(libs.play.app.update.ktx)
```

⚠️ `settings.gradle.kts`의 `dependencyResolutionManagement`에 **`google()`이 있어야 한다**(K-시리즈는 전부 있다).

### ③ 조르기 정책 — `app/src/main/java/com/kitwlshcom/<앱>/update/UpdatePromptPolicy.kt`

🔴 **이 파일이 이 기능의 핵심이다.** 인앱 업데이트에서 제일 쉽게 망치는 것이 **조르기**다.
순수 계산이라 **단위 테스트 대상**이고, 앱마다 그대로 복사하면 된다.

```kotlin
package com.kitwlshcom.<앱>.update

/**
 * 「새 버전이 있어요」를 **언제 물어도 되는가** (2026-10-08).
 *
 * 🔴 **왜 정책이 따로 필요한가** — 인앱 업데이트에서 제일 쉽게 망치는 것이 **조르기**다.
 * 앱을 열 때마다 「업데이트하세요」가 뜨면 사용자는 그 창을 **읽지 않고 닫는 버릇**이 들고,
 * 그러면 정작 중요한 판을 올렸을 때도 똑같이 닫는다. **안내가 소음이 되는 순간 안내가 아니다.**
 *
 * 그래서 규칙은 둘뿐이다:
 * 1. **같은 버전은 [QUIET_DAYS]일에 한 번만 묻는다** — 거절한 사람에게 내일 또 묻지 않는다.
 * 2. **새 버전이 나오면 그 날짜를 잊는다** — 사용자가 1.7.3을 거절한 것이지 1.8.0을 거절한 것이 아니다.
 *
 * ⚠️ **«업데이트를 받았으니 다시 시작하자»는 이 정책을 타지 않는다.** 그것은 사용자가 이미
 * «받겠다»고 누른 뒤의 일이라 조르기가 아니다 — 받아 놓고 안 알리면 그게 더 이상하다.
 *
 * 안드로이드 API를 쓰지 않는 순수 계산이다(단위 테스트 대상). 자매앱에 그대로 옮길 수 있다.
 */
object UpdatePromptPolicy {

    /** 같은 버전을 다시 묻기까지 쉬는 기간. */
    const val QUIET_DAYS = 3L

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** 아직 아무것도 물어본 적이 없음을 뜻하는 값. */
    const val NEVER_ASKED = -1

    /**
     * 지금 물어도 되는가.
     *
     * @param availableVersionCode 스토어에 올라와 있는 새 버전
     * @param lastAskedVersionCode 마지막으로 물어본 버전([NEVER_ASKED]이면 처음)
     * @param lastAskedAtMs 그때의 시각
     * @param nowMs 지금
     */
    fun shouldPrompt(
        availableVersionCode: Int,
        lastAskedVersionCode: Int,
        lastAskedAtMs: Long,
        nowMs: Long
    ): Boolean {
        // 처음이거나, **다른(더 새로운) 버전**이 올라왔다 → 묻는다.
        if (lastAskedVersionCode != availableVersionCode) return true
        // 같은 버전이다 → 쉬는 기간이 지났을 때만.
        val elapsed = nowMs - lastAskedAtMs
        // 🔴 시각이 거꾸로 간 경우(사용자가 폰 날짜를 바꿨다)도 «지났다»로 본다.
        //    음수를 그냥 두면 «영원히 안 물어보는» 상태에 갇힌다.
        if (elapsed < 0) return true
        return elapsed >= QUIET_DAYS * DAY_MS
    }
}

```

### ④ 본체 — `app/src/main/java/com/kitwlshcom/<앱>/update/InAppUpdateController.kt`

```kotlin
package com.kitwlshcom.<앱>.update

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

```

### ⑤ 다 받았을 때 띄울 카드 — `app/src/main/java/com/kitwlshcom/<앱>/ui/components/UpdateReadyBanner.kt`

⚠️ 색·테마 이름은 **그 앱의 것으로 바꾼다**(아래는 KDailyUtil 기준).

```kotlin
package com.kitwlshcom.<앱>.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitwlshcom.<앱>.ui.theme.DeepCharcoal
import com.kitwlshcom.<앱>.ui.theme.Gold24K

/**
 * 「새 버전을 다 받았어요 — 다시 시작할까요?」 (2026-10-08).
 *
 * 🔴 **유연 업데이트의 마지막 한 칸이다.** 내려받기는 뒤에서 끝났는데 앱이 한 번 꺼졌다 켜지지
 * 않으면 **설치가 되지 않는다.** 그 사실을 아는 것은 앱뿐이라, 말해 주지 않으면 사용자는
 * 받아 놓고도 영영 옛 버전을 쓴다.
 *
 * ⚠️ **막지 않는다** — 위에 얹는 카드일 뿐이고 「나중에」로 내릴 수 있다. 내려도 받아 둔 것은
 * 그대로 남아 다음에 앱을 껐다 켤 때 저절로 깔린다. **지금 당장 해야 하는 일이 아니다.**
 */
@Composable
fun UpdateReadyBanner(
    onRestart: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.97f)),
        border = BorderStroke(1.dp, Gold24K.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                "🎉 새 버전을 다 받았어요",
                color = Gold24K, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "앱을 한 번 다시 시작하면 적용돼요. 지금 안 하셔도 괜찮아요 — 다음에 앱을 껐다 켜면 저절로 적용됩니다.",
                color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onLater) {
                    Text("나중에", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                }
                Spacer(Modifier.height(0.dp))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold24K, contentColor = Color.Black)
                ) { Text("지금 다시 시작", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }
    }
}

```

---

## 4. Activity 배선 — 네 자리

```kotlin
class MainActivity : ComponentActivity() {
    private val inAppUpdate by lazy { InAppUpdateController(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ① 🔴 setContent보다 «먼저» — ActivityResult 등록은 STARTED 전에만 된다
        inAppUpdate.register()
        inAppUpdate.checkForUpdate()

        setContent {
            Box(Modifier.fillMaxSize()) {
                MainScreen(...)
                // ② 다 받았을 때만 뜬다. 화면을 막지 않고 «위에 얹는다»
                val updateReady by inAppUpdate.readyToInstall.collectAsState()
                if (updateReady) {
                    UpdateReadyBanner(
                        onRestart = { inAppUpdate.completeUpdate() },
                        onLater = { inAppUpdate.dismissReady() },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)
                    )
                }
            }
        }

        // ③ 앱이 꺼져 있는 동안 내려받기가 끝났을 수 있다 — 돌아올 때 한 번 더 본다
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) inAppUpdate.checkForUpdate()
        })
    }

    // ④ 안 부르면 리스너가 Activity를 붙잡고 있는다
    override fun onDestroy() { inAppUpdate.unregister(); super.onDestroy() }
}
```

🔴 **배너를 띄울 자리의 `padding(bottom = ...)`은 앱마다 다르다** — 하단 탭 바 높이만큼 띄워야
버튼이 탭에 가리지 않는다. KDailyUtil은 96dp.

---

## 5. 🔴 밟기 쉬운 함정 넷

| 함정 | 무슨 일이 생기나 |
|---|---|
| `register()`를 `setContent` **뒤에** 부른다 | `registerForActivityResult`가 **예외를 던진다**(LifecycleOwner가 이미 STARTED) |
| `unregister()`를 빠뜨린다 | 리스너가 Activity를 붙잡아 **메모리 누수** |
| 다 받은 뒤 **아무 말도 안 한다** | 내려받기는 끝났는데 **설치가 안 된다**. 앱이 한 번 꺼졌다 켜져야 깔리는데 그걸 아는 건 앱뿐이다 — 사용자는 받아 놓고 영영 옛 버전을 쓴다 |
| 조르기 정책 없이 **매번 묻는다** | 사용자가 그 창을 **읽지 않고 닫는 버릇**이 든다. 그러면 정작 중요한 판을 올렸을 때도 똑같이 닫는다 |

---

## 6. 확인 방법 — 🔴 **개발용 빌드로는 확인할 수 없다**

이 기능은 **Play가 설치한 앱**에서만 돈다. 그래서 확인 경로는 셋뿐이다:

1. 🥇 **내부 테스트 트랙**(권장) — Play Console의 내부 테스트에 **버전 둘**을 올리고,
   낮은 쪽을 깐 상태에서 높은 쪽을 게시한다. 실제 동작과 가장 가깝다
2. 🥈 **단위 테스트** — 조르기 정책(§3-③)은 기기 없이 전부 검증된다. **로직의 버그는 여기서 다 잡힌다**
3. 🥉 **로그 확인** — 개발용 빌드로 앱을 켜고 `adb logcat -s InAppUpdate`
   → `업데이트 정보를 못 받았다(Play 설치본이 아닐 수 있다)`가 보이면 **배선은 맞은 것이다**

⚠️ **«폰에서 눌러 봤는데 아무것도 안 뜬다»는 실패가 아니다.** 위 2·3으로 배선을 확인하고,
진짜 동작은 **다음 출시 때 자연스럽게** 드러난다.

---

## 7. 진행 현황

| 앱 | 상태 | 비고 |
|---|---|---|
| **KDailyUtil** | ✅ **2026-10-08 구현** | vc12에 실림(출시는 기능이 쌓인 뒤) · 단위 테스트 7건 |
| **KLotto645** | ⬜ 미착수 | 그 세션이 이 문서대로 넣는다 |
| **K장부(KJangbu)** | ⬜ 미착수 | 그 세션이 이 문서대로 넣는다 |
| **K달인(KDalin)** | ⬜ 미착수 | 그 세션이 이 문서대로 넣는다 |

🔴 **각 앱 세션이 자기 저장소에 직접 넣는다.** KDailyUtil 세션은 **다른 저장소에 커밋하지 않는다**
(다른 세션이 작업 중일 수 있다 — 2026-10-08 실제로 K장부에 미푸시 커밋 5건이 있었다).
넣은 뒤에는 **이 표의 자기 줄을 ✅로 바꾸고 네 사본에 전부 반영한다**(`sync_docs.py --fix`).
