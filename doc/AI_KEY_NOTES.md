# 🔑 AI 키 — KDailyUtil 현황 및 남은 일

> 작성 2026-08-07 · 상태: **모델 폴백·키 안내 완료(미배포) / 체험·킬스위치 미구현**
> 📌 **정책의 단일 기준은 K장부다** — `KJangbu/doc/AI_KEY_POLICY.md`
> (약관 실측·"앱에 넣은 키는 숨길 수 없다" 실증·체험 20회 설계·킬스위치·§9 방침 문안·후원/광고 기각 논리).
> 이 문서는 **KDailyUtil에만 해당하는 사실과 남은 일**만 담는다. 정책을 다시 논의하려면 위 문서를 먼저 읽는다.

---

## 1. 🔴 왜 v1.6.1이 급한가

> 📛 **버전명**: 이 수정판은 08-10에 `1.7`로 빌드했다가, 순수 버그 수정이라 **`1.6.1`이 맞다**는 판단으로 08-11에 **유의적 버전으로 전환하며 재빌드**했다(`versionCode`는 예정대로 **7**). **스토어에 `1.7`이 올라간 적은 없다.** 옛 커밋·문서의 "v1.7"은 모두 이 `1.6.1`을 가리킨다. 스킴 = [DEVELOPER_GUIDE.md § 버전 스킴](DEVELOPER_GUIDE.md).

`GeminiManager`에 `gemini-2.5-flash`가 하드코딩돼 있었고, **이 모델은 신규 사용자에게 닫혔다.**

```
404 NOT_FOUND
"This model models/gemini-2.5-flash is no longer available to new users.
 Please update your code to use a newer model…"
```

**기존 계정 키는 계속 되기 때문에 개발 중엔 보이지 않는다.** 증상은 이렇게 갈린다:

| 사용자 | v1.6에서 |
|---|---|
| 예전부터 쓰던 키 | 정상 (그래서 눈치채기 어렵다) |
| **새로 설치 + 새로 발급한 키** | **AI 13종 전부 실패** |

→ **게시본 v1.6에 지금도 해당된다.** v1.6.1 배포 전까지 신규 설치자는 AI를 쓸 수 없다.

## 2. 적용한 것 (2026-08-07 · 커밋 `9a1a4f7`, `50e7fa8`)

### 2-1. 모델 4중 방어 — [GeminiManager.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/data/remote/GeminiManager.kt)
1. **별칭 기본값** `gemini-flash-latest` (버전을 박지 않는다)
2. **404 전용 폴백** `FALLBACK_MODELS` 순회. ⚠️ 키 오류·한도·네트워크에는 폴백하지 **않는다** — 폴백하면 같은 오류를 후보 수만큼 반복하고 사용자에게 보여줄 사유도 흐려진다
3. **`resolved` 기억** — 매 호출 재탐색 방지. 채팅 세션은 `chatModel()`이 재사용
4. **원격 지정** `family.json`의 `aiModel` → [FamilyRepository.applyAiModel](../app/src/main/java/com/kitwlshcom/kdailyutil/data/repository/FamilyRepository.kt). 형식 화이트리스트(영숫자·하이픈·점, 64자)로 원격이 앱을 망가뜨리지 못하게

**AI 호출 14곳이 `ask(prompt)` 한 곳을 통과한다** → 폴백이 전 기능에 자동 적용.
`MainActivity.primeRemoteConfig()`가 시작 시 레지스트리를 1회 읽는다 — **AI가 다 죽은 상황에 사용자가 '브랜드 & 자매앱' 화면에 들어가 줄 거라고 기대할 수 없어서**다. 6시간 신선도 캐시가 있어 대부분 네트워크를 타지 않는다.

### 2-2. 키 안내
- **형식 단정 제거**: `(AIzaSy...)` 삭제. 새 키는 `AQ.Ab8…`(53자)라 **멀쩡한 키를 틀린 키로 오해**하게 만들었다
- **첫 실행 배너 + 발급 가이드**: [AiNotice.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/ui/components/AiNotice.kt) — `AiKeyOnboardingBanner` / `AiKeyGuideDialog` / `GeminiDataNotice`
- **연결 테스트**가 `testConnection()`에 위임 → **잡힌 모델명 표시** + 실패 사유(키·권한·한도·모델) 구분
- **무료 등급 데이터 취급 고지**: 방침 제8조 + 앱 내. 기존 *"오직 해당 기능의 결과 생성 목적으로만 사용"* 은 약관과 모순이라 정정
- `generateAiQuiz`가 키 없을 때 **안내 없이 일반 퀴즈를 시작**하던 것 수정(`// 임시로` 주석이 남아 있던 미완성 코드)

## 3. 실측 결과 (2026-08-07, 실제 키 · HTTP 직접 호출)

| 모델 | 결과 |
|---|---|
| **`gemini-flash-latest`** | ✅ 200 — 응답 메타데이터상 **실제 `gemini-3.6-flash`** |
| `gemini-3.5-flash` | ✅ 200 |
| `gemini-2.0-flash` | ⚠️ 429 `RESOURCE_EXHAUSTED` — **무료 등급에 쿼터가 없다.** 3번째 후보라 무해하지만, 앞의 둘이 다 막히면 사용자는 모델 오류가 아니라 한도 오류를 본다 |
| `gemini-2.5-flash` | ❌ 404 `NOT_FOUND` |
| `gemini-2.5-flash-lite` | ❌ 404 |

- 404 문구가 `isModelUnavailable()`의 판정어(`NOT_FOUND` / `no longer available`)와 **일치 확인** → 폴백 정상
- ⚠️ **ListModels 목록에 있어도 `generateContent`는 거부될 수 있다.** 목록을 믿지 말고 실제 호출로 확인한다
- ⚠️ **`gemini-flash-latest`는 thinking 모델**이다 — "OK" 한 마디에 총 76토큰(thought 69). 예전 `2.5-flash`보다 토큰 소모가 크다 → **체험 횟수·무료 등급 분당 한도 설계에 영향**
- 실기기 확인: AI 기능 정상 동작 → 레거시 SDK(`com.google.ai.client.generativeai`)가 thinking 응답(`thoughtSignature` 파트)의 `.text`를 정상 처리함이 확인됨

**진단 방법**: `adb logcat -s GeminiModel` → `✅ 모델 결정: <이름> (후보=[…])`

## 4. 남은 일

### 4-1. ⭐ v1.6.1 릴리스 — **빌드 완료(2026-08-11) · 업로드만 남음**
1. ~~versionCode 6→7~~ ✅ / ~~versionName → **"1.6.1"**~~ ✅ (08-10에 `1.7`로 빌드했다가 08-11에 스킴 전환하며 재빌드)
2. ~~`./gradlew.bat :app:bundleRelease`~~ ✅ → ~~서명 확인~~ ✅ SHA-256 `61:12:DE:02:AD:DF:…:A5:12:99`(v1.6과 동일 업로드 키)
3. ~~`app/release/kdailyutil-v1.6.1.aab`로 복사~~ ✅ 11,225,422 bytes · 산출물과 sha256 일치 확인
4. ⏳ **Play Console 업로드** · 출시 노트 = [RELEASE_NOTES.md](../app/release/RELEASE_NOTES.md) 'v1.6.1' 절(249자 / 한도 500)

> **빌드 전에 서명 준비를 먼저 확인하는 것이 핵심이다** — `local.properties`의 `release.store.file` /
> `release.store.password` / `release.key.alias` / `release.key.password` 4개와 키스토어 파일 존재.
> `hasReleaseSigning`이 false면 **경고 없이 서명 없는 AAB가 나온다.**
> ⚠️ `keytool`의 **"SHA1withRSA 보안 위험"** 경고는 인증서 서명 알고리즘 문제이고 v1.0부터 쓴 동일 키다 — 키 교체는 하지 않기로 결정된 사항이다(근거 = [GOOGLE_PLAY_RELEASE_GUIDE.md](GOOGLE_PLAY_RELEASE_GUIDE.md) 1단계 ①).
4. 절차 상세 = [GOOGLE_PLAY_RELEASE_GUIDE.md](GOOGLE_PLAY_RELEASE_GUIDE.md)

### 4-1c. ⚠️ `aiModel` 원격 교체 — **배선만 있고 실전 미검증** (v1.6.1 배포 후 할 것)
`FamilyRepository.applyAiModel`이 `family.json`의 `aiModel`을 읽지만, **정본·번들·호스팅 세 파일 어디에도 `aiModel` 키가 없다.** 없으면 기본값(`gemini-flash-latest`)을 쓰므로 지금 동작은 정상이다.

문제는 **이 비상 레버를 한 번도 당겨본 적이 없다**는 것이다. 다음에 또 모델이 막히면 그때 처음 시험하게 된다 — 급할 때 처음 쓰는 장치는 위험하다.

> **v1.6.1 배포 후** 호스팅 `family.json`에 `"aiModel": "gemini-3.5-flash"` 를 한 줄 넣고
> `adb logcat -s GeminiModel` 에서 `✅ 모델 결정: gemini-3.5-flash` 가 찍히는지 확인 → 확인 후 그 줄을 되돌린다.
> **v1.6에는 이 배선이 없으므로 배포 전에는 검증 불가.** 캐시가 6시간이라 즉시 보려면 설정 > 브랜드 & 자매앱 > 🔄 새로고침.

### 4-1b. ✅ `k-series-config` 반영 — **완료(2026-08-10)**
**K장부가 2026-08-10 출시**되어 `comingSoon: true→false` 전환을 **정본·번들에 반영**했다.
호스팅 레포에 커밋·푸시했고 라이브 URL에서 `comingSoon:false` 응답을 확인했다. 사용자 화면은 앱 6시간 캐시라 즉시 보려면 설정 > 브랜드 & 자매앱 > 🔄 새로고침.

### 4-2. ✅ 방침 배포 사본 교체 — **완료(2026-08-10)**
원본 `doc/privacy-kdailyutil.html`(개정일자 2026-08-07)을 **배포본 `k-series-config`에 반영**했다(커밋 `7c6abd4`, 푸시됨). GitHub Pages라 앱 배포와 무관하게 즉시 라이브. 게시본 v1.6에도 해당되는 내용이라 v1.6.1보다 먼저 처리했다.

> ⚠️ 복사 시 **줄바꿈을 CRLF로 변환**했다 — 호스팅 레포의 방침 3종이 모두 CRLF인데 원본은 LF라, 그냥 복사하면 전체 파일이 diff로 잡혀 실제 변경을 검토할 수 없다. `sed 's/$/\r/'`로 변환하니 diff가 5+/4-로 줄었다.

### 4-2b. ⚠️ Play Console '데이터 안전' 양식 대조 (v1.6.1 업로드 시)
방침 제8조에 **"무료 등급에서는 Google이 제품 개선에 사용할 수 있고 사람이 검토할 수 있다"** 를 새로 명시했다. Play Console의 **앱 콘텐츠 > 데이터 안전** 양식은 방침과 **별개로** 관리되며, **둘이 어긋나면 정책 위반**이다. 텍스트·사진이 제3자(Google AI)로 전송되는 사실과 그 목적이 양식에 반영돼 있는지 확인할 것. (방침만 고치고 양식을 잊는 것이 흔한 함정.)

### 4-3. 체험 + 킬스위치 (선택 · 미구현)
K장부에 **구현·검증 완료**돼 있다(`core/ai/AiKeyProvider.kt` · `PrefsStore.aiTrialUsed` · `FamilyRegistry.loadAiTrialConfig`). KDailyUtil로 이식할 때 그대로 쓸 수 없는 부분:

| 항목 | KDailyUtil 사정 |
|---|---|
| **한도 배분** | K장부는 AI 접점이 사실상 영수증 OCR 하나라 총량 20회가 맞다. KDailyUtil은 **13종**이라 한 번씩 눌러봐도 절반이 사라진다 → **카테고리별 배분**(뉴스/배움터/증시) 검토. 제안: 7/7/6 |
| **카운터 단위** | K장부 원칙 승계 — **호출이 아니라 사용자 작업 단위**. 실패는 차감하지 않는다 |
| **429 구분** | 공용 키 분당 한도는 전체 체험자가 공유 → **`소진`과 `혼잡`을 다르게 안내**해야 한다. 횟수 남은 사용자에게 소진이라 하면 거짓말. thinking 토큰 때문에 한도에 더 빨리 닿는다 |
| **선행 리팩터** | KDailyUtil은 키 조회가 **15곳에 흩어져** 있다(각자 `geminiApiKeyFlow.first()`) → K장부처럼 **단일 진입점으로 먼저 모아야** 한 곳 누락으로 미집계 호출이 생기지 않는다 |

> ⚠️ **순서 고정**: 단일 진입점 → 카운터 → 안내 → 킬스위치 → **키 투입은 반드시 마지막.**
> 먼저 넣으면 카운터 없이 전 기능이 공용 키로 무제한 동작한다(K장부 §8-2).
> 현재 KDailyUtil에는 `gemini.default.key` 배선이 **없다** — 넣을 자리가 없는 게 정상 상태다.

### 4-4. 정리해도 되는 것 (기능 영향 없음)
`GeminiManager.summarizeNews()` · `extractArticleContent()` 는 **호출부가 0인 죽은 코드**다(저작권 보수화로 스니펫 낭독·아웃링크로 바뀔 때 남은 잔재). README가 안내하던 "키 없으면 제목만 나열하는 데모 모드"가 실제로는 동작하지 않던 이유이며, README 서술은 이미 정정했다. **데모 모드를 되살릴 계획이 없으면 삭제해도 된다.**
