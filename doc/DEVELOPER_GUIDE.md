# 🛠 KDailyUtil - 개발자 가이드 (Developer Context Guide)

> **신규 세션 또는 AI 어시스턴트가 이 파일을 먼저 읽으면 프로젝트 전체 맥락을 즉시 파악할 수 있습니다.**
> 최종 업데이트: 2026-06-24

---

## 📌 프로젝트 개요

- **앱 이름**: KDailyUtil
- **패키지**: `com.kitwlshcom.kdailyutil`
- **언어**: Kotlin (Jetpack Compose)
- **최소 SDK**: 26 / 타겟 SDK: 36
- **빌드 도구**: AGP 8.13.2, Kotlin 2.0.21
- **GitHub**: `kitwlsh/KDailyUtil`
- **로컬 경로**: `d:\DATA\20_Source\80_Git_HUB\KDailyUtil\KDailyUtil`
- **원격 퀴즈 데이터**: `d:\DATA\20_Source\80_Git_HUB\KDailyUtil\korean_quiz_data\`

---

## 📂 프로젝트 디렉토리 구조

```
KDailyUtil/
├── app/src/main/java/com/kitwlshcom/kdailyutil/
│   ├── MainActivity.kt                     # 앱 진입점(launchMode=singleTask), 알림→복귀 처리
│   ├── audio/
│   │   ├── AudioCaptureService.kt          # 포그라운드 오디오 캡처 서비스(유휴 시 알림 해제)
│   │   └── TtsManager.kt                   # TTS 낭독
│   ├── data/
│   │   ├── model/
│   │   │   ├── QuizQuestion.kt             # 퀴즈 데이터 모델 (imageUrl 필드 포함)
│   │   │   ├── StockModels.kt              # 시세/차트/공시/예정 모델 (EarningsDisclosure 등)
│   │   │   ├── AudioItem.kt
│   │   │   └── NewsItem.kt
│   │   ├── remote/
│   │   │   └── GeminiManager.kt            # Gemini 통합 (퀴즈/요약/공시/OCR/이해도 채점)
│   │   ├── repository/
│   │   │   ├── QuizRepository.kt           # 퀴즈 CRUD, 원격 동기화, 커스텀 저장
│   │   │   ├── StockRepository.kt          # Yahoo 시세/차트 + DART 공시/재무 + 캐시/즐겨찾기/숨김
│   │   │   ├── ReadingTrainingRepository.kt# 빠른 독서 훈련 진척/보관함/WPM이력
│   │   │   ├── AudioRepository.kt          # 오디오 파일 관리
│   │   │   ├── NewsRepository.kt           # 뉴스 RSS 수집 및 크롤링
│   │   │   └── SettingsRepository.kt       # DataStore 기반 설정 저장 (Gemini/DART 키 등)
│   │   ├── QuizFileHandler.kt              # .kquiz 파일 export/import
│   │   └── QuizStatsManager.kt             # 퀴즈 정답률/도전 이력 추적
│   ├── receiver/  BriefingReceiver.kt      # 예약 브리핑 알림 수신
│   ├── scheduler/ BriefingScheduler.kt     # WorkManager/AlarmManager 예약
│   └── ui/
│       ├── MainScreen.kt                   # 하단 네비 + NavHost (VM hoisting: stock/briefing/...)
│       ├── components/  navigation/        # 공통 컴포넌트 / NavGraph
│       ├── screens/
│       │   ├── QuizScreen.kt  QuizCreatorScreen.kt   # 퀴즈 플레이/제작
│       │   ├── StockDashboardScreen.kt     # 증시: 시세·차트(크로스헤어)/AI 실적공시/예정일정
│       │   ├── ReadingTrainingScreen.kt    # ⚡ 빠른 독서 훈련 (배움터 탭1, 드릴5+OCR+이해도)
│       │   ├── LearningHubScreen.kt        # 배움터 허브 (탭0 퀴즈 / 탭1 빠른 독서 훈련)
│       │   ├── AudioCaptureScreen.kt  AudioSubScreens.kt  DrivingShadowingScreen.kt
│       │   ├── MorningBriefingSettingsScreen.kt # 설정 화면 (5탭: 브리핑/증시/AI·키/화면/앱정보)
│       │   ├── NewsBriefingScreen.kt  NewsDetailScreen.kt  SplashScreen.kt  PlaceholderScreen.kt
│       ├── theme/  (Color.kt: Gold24K/DeepCharcoal, Theme.kt, Type.kt)
│       └── viewmodel/
│           ├── QuizViewModel.kt  BriefingViewModel.kt(브리핑 일시정지)  AudioCaptureViewModel.kt
│           ├── StockViewModel.kt            # 증시 상태 + 백그라운드 분석/알림/인앱배너
│           ├── ReadingTrainingViewModel.kt  # 빠른 독서 훈련 상태/AI 호출
│           └── ShadowingViewModel.kt
├── app/src/main/res/xml/
│   ├── file_paths.xml                      # FileProvider (cache+files, path=".")
│   ├── backup_rules.xml / data_extraction_rules.xml  # DataStore(키 저장) 백업 제외
├── app/build.gradle.kts                    # 의존성 + BuildConfig.DART_DEFAULT_KEY(local.properties 주입)
├── local.properties                        # (VCS 제외) dart.default.key=...
├── README.md / DEVELOPER_GUIDE.md / doc/FEATURE_SPEED_READING.md / doc/FEATURE_DART_AI_SUMMARY.md
```

---

## 🏗 아키텍처 개요

```
UI Layer (Compose Screens)
    ↕ StateFlow / collectAsState
ViewModel Layer (AndroidViewModel)
    ↕ suspend functions / coroutines
Repository Layer (Data Access)
    ↕ HTTP / File I/O / DataStore
Data Sources (Remote API / Local Storage)
```

- **상태 관리**: `MutableStateFlow` + `collectAsState()`
- **비동기**: Kotlin Coroutines (`viewModelScope`, `Dispatchers.IO`)
- **설정 저장**: Preferences DataStore (`SettingsRepository`)
- **퀴즈 저장**: Internal File Storage JSON (`filesDir/custom_quizzes.json`)
- **퀴즈 캐시**: `filesDir/quizzes_v2.json` (원격 동기화 캐시)
- **이미지 크롭 저장**: `filesDir/cropped_quizzes/` (PNG 파일)

---

## 🧠 KuizGenius 퀴즈 시스템 상세

### 데이터 모델
```kotlin
// data/model/QuizQuestion.kt
enum class QuizType { MULTIPLE_CHOICE, SUBJECTIVE }

data class QuizQuestion(
    val id: Int,
    val type: QuizType,
    val category: String,
    val subCategory: String = "",
    val question: String,
    val options: List<String>? = null,  // null이면 주관식
    val answer: String,
    val explanation: String,
    val semanticHint: String? = null,
    val imageUrl: String? = null        // 로컬 절대 경로 (크롭 이미지) 또는 null
)
```

### 퀴즈 카테고리 유형 (중요!)
| 유형 | 표시 | 삭제/공유 | 출처 |
|------|------|-----------|------|
| 공식 내장 | ☁️ | 불가 | 하드코딩 (우리말겨루기, 트렌드말하기, 상식백과, 세계여행) |
| 클라우드 동기화 | ☁️ | 불가 | `korean_quiz_data` GitHub 원격 JSON |
| 로컬 커스텀 | ⭐ | 가능 | AI 생성/수동 작성/외부 가져오기 |

> **⚠️ 주의**: `isDefault` 판별 시 공식 내장 + 클라우드 동기화 카테고리 **모두** 읽기 전용으로 처리해야 함.

### 퀴즈 상태 머신 (QuizState)
```
IDLE → CATEGORY_SELECTION → GENERATING → PLAYING ↔ ANSWER_CHECKED → FINISHED
  ↑                                                                        |
  └────────────────────────────────────────────────────────────────────────┘
                         CREATOR (QuizCreatorScreen으로 전환)
```

### QuizRepository 주요 메서드
```kotlin
// 원격 퀴즈 동기화 (앱 시작 시 호출)
suspend fun syncRemoteQuizzes(context: Context)

// 퀴즈 로드 (카테고리 필터 가능, 셔플됨)
suspend fun getQuizzes(context: Context, category: String? = null): List<QuizQuestion>

// 커스텀 퀴즈 저장 (ID 기준 병합)
suspend fun saveCustomQuizzes(context: Context, quizzes: List<QuizQuestion>)

// 커스텀 카테고리 삭제 (크롭 이미지 파일도 함께 삭제)
suspend fun deleteCustomCategory(context: Context, category: String)

// 커스텀/원격 카테고리 목록 조회
suspend fun getCustomCategories(context: Context): List<String>
suspend fun getRemoteCategories(context: Context): List<String>
```

---

## 🖼 이미지(시각) 퀴즈 파이프라인 (2026.06 완성)

```
[사용자] 이미지 선택 (카메라/갤러리)
    ↓
[GeminiManager.generateVisualQuizzesFromImages()]
    - Bounding Box 좌표 [ymin, xmin, ymax, xmax] (0~1000 비율) 반환
    ↓
[QuizCreatorScreen.cropBitmapFromBoundingBox()]
    - 비율 좌표 → 실제 픽셀 변환
    - 5% 여백 패딩 추가
    - Bitmap.createBitmap() 으로 크롭
    ↓
[QuizCreatorScreen.saveBitmapToInternalStorage()]
    - filesDir/cropped_quizzes/crop_{category}_{timestamp}_{index}.png 저장
    - 절대 경로 String 반환
    ↓
[QuizQuestion.imageUrl] = 절대 경로
    ↓
[QuizPlayScreen] - AsyncImage(model = currentQuestion.imageUrl) 로 표시
[저장 다이얼로그] - AsyncImage 로 미리보기 표시
```

### 관련 파일
- `GeminiManager.generateVisualQuizzesFromImages()` - AI API 호출 (라인 245~310)
- `QuizCreatorScreen.cropBitmapFromBoundingBox()` - 크롭 헬퍼 (라인 1896~1930)
- `QuizCreatorScreen.saveBitmapToInternalStorage()` - 저장 헬퍼 (라인 1932~1950)
- `QuizScreen.QuizPlayScreen` - 이미지 표시 (라인 743~763)

---

## 🤖 GeminiManager API 메서드 목록

```kotlin
// 이미지 다중 스캔 → 텍스트 기반 퀴즈 JSON 생성
suspend fun generateQuizzesFromImages(images, previousQuizzesJson, errorStatsJson, count): String

// 이미지 스캔 → Bounding Box 포함 시각 퀴즈 JSON 생성
suspend fun generateVisualQuizzesFromImages(images, previousQuizzesJson, errorStatsJson, count): String

// 웹 크롤링 텍스트 → 퀴즈 JSON 생성
suspend fun generateQuizzesFromText(text, previousQuizzesJson, errorStatsJson, count): String

// 질문+정답 → 오답 보기 3개 + 해설 자동 생성
suspend fun generateOptionsForQuestion(question, answer): String

// 주관식 답안 의미론적 채점 (2단계 채점 시스템)
suspend fun verifySubjectiveAnswer(question, correctAnswer, userAnswer): Boolean
```

---

## 📦 주요 의존성 (libs.versions.toml)

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Jetpack Compose BOM | 2024.09.00 | UI 전체 |
| Material3 | BOM 관리 | UI 컴포넌트 |
| Navigation Compose | 2.8.1 | 화면 전환 |
| Coil Compose | **2.6.0** | 이미지 비동기 로딩 (AsyncImage) |
| Google Generative AI | **0.9.0** | Gemini API |
| Jsoup | 1.18.1 | 웹 크롤링/RSS |
| DataStore Preferences | 1.1.1 | 설정 영속화 |
| WorkManager KTX | 2.10.0 | 백그라운드 작업 |
| Core SplashScreen | 1.0.1 | 스플래시 화면 |
| Material Icons Extended | BOM 관리 | 아이콘 |

---

## 🎨 디자인 시스템

### 핵심 컬러 (Color.kt)
```kotlin
val Gold24K = Color(0xFFFFD700)     // 골드 - 주요 강조색
val DeepCharcoal = Color(0xFF121212) // 다크 배경
```

### 앱 테마
- **고정 다크 테마** (라이트 모드 없음)
- `Scaffold.containerColor = DeepCharcoal`
- 강조색: `Gold24K`
- 보조색: `White.copy(alpha = 0.6~0.8f)`

---

## 📡 원격 퀴즈 데이터 (korean_quiz_data)

- **GitHub 저장소**: `kitwlsh/korean_quiz_data`
- **Base URL**: `https://raw.githubusercontent.com/kitwlsh/korean_quiz_data/refs/heads/main/`
- **퀴즈 파일 목록**:
  - `korean.json` → 우리말 겨루기
  - `trend.json` → 트렌드 말하기
  - `knowledge.json` → 상식 백과
  - `travel.json` → 세계 여행
  - `quiz_updates.json` → 맞춤법 띄어쓰기, 고난이도 고유어, 아름다운 순우리말, 사자성어, 최신 유행어 등

- **Python 업데이트 스크립트**: `d:\DATA\20_Source\80_Git_HUB\KDailyUtil\korean_quiz_data\update_quiz.py`
- **로컬 캐시 파일**: `filesDir/quizzes_v2.json`

---

## 🔐 권한 및 보안

### AndroidManifest.xml 권한
- `INTERNET` - 뉴스 크롤링, Gemini API, 원격 퀴즈 동기화
- `CAMERA` - 퀴즈 이미지 촬영
- `READ_MEDIA_IMAGES` - 갤러리 이미지 선택
- `RECORD_AUDIO` - 오디오 캡처
- `READ_EXTERNAL_STORAGE` (API ≤32) / `WRITE_EXTERNAL_STORAGE` (API ≤29)
- `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS` - 예약 브리핑

### FileProvider
- **Authority**: `com.kitwlshcom.kdailyutil.fileprovider`
- **경로 설정** (`file_paths.xml`):
  - `cache-path` → `.kquiz` 파일 공유 (shared_quizzes)
  - `files-path` → 커스텀 퀴즈 파일 및 크롭 이미지 접근

### 백업 데이터 보호 (2026-06-23)
- API 키 등 민감 설정이 저장되는 DataStore(`files/datastore/`)를 자동 백업에서 제외함.
  - `res/xml/backup_rules.xml` (API ≤30): `<exclude domain="file" path="datastore/" />`
  - `res/xml/data_extraction_rules.xml` (API 31+): `cloud-backup` / `device-transfer` 모두 동일 제외.

### WebView 보안 (2026-06-23)
- 외부 뉴스 페이지를 로드하는 WebView 3곳(`NewsDetailScreen`, `NewsRepository`의 리다이렉트/본문추출 WebView)에서
  단말 내부 파일 접근을 차단: `allowFileAccess = false`, `allowContentAccess = false`,
  `allowFileAccessFromFileURLs = false`, `allowUniversalAccessFromFileURLs = false`.

### API 키
- **저장**: Preferences DataStore (`SettingsRepository.geminiApiKeyFlow`)
- **UI 입력**: `MorningBriefingSettingsScreen` > API 키 설정
- **사용**: `GeminiManager(apiKey)` 생성자 주입

#### ⚠️ DART 기본 제공 키 (빌드 시 필수 확인)
- 사용자가 DART 키를 직접 입력하지 않으면 **기본 제공 키**로 폴백하여 실적 공시 기능이 동작함.
- 이 기본 키는 소스 코드에 하드코딩하지 않고 **`local.properties`(VCS 제외)** 에서 주입함:
  ```properties
  # local.properties
  dart.default.key=발급받은_DART_키
  ```
- `app/build.gradle.kts`가 이 값을 읽어 `BuildConfig.DART_DEFAULT_KEY`로 노출 (`buildConfig = true`).
- 폴백 참조 위치: `SettingsRepository.dartApiKeyFlow`, `StockRepository.fetchRecentDisclosures()` / `fetchCompanyFinancialJson()`, `BriefingViewModel.dartApiKey`.
- **주의**: 다른 PC에서 클론해 빌드할 때 `local.properties`에 `dart.default.key`가 없으면 빈 문자열이 되어, 사용자가 직접 키를 입력해야만 실적 기능이 동작함.

---

## 🔊 사운드 시스템

- **엔진**: Android `SoundPool`
- **리소스** (`app/src/main/res/raw/`):
  - `quiz_correct.mp3` - 정답 효과음
  - `quiz_wrong.mp3` - 오답 효과음
  - `quiz_finish.mp3` - 완료 효과음
- **초기화**: `QuizViewModel.init { }` 블록에서 SoundPool 생성 및 로드
- **백업 파일**: `backup_audio/` 폴더 (git 추적되지 않음)

---

## 📊 퀴즈 통계 시스템 (QuizStatsManager)

- **저장소**: DataStore (사용자별 퀴즈 정답/오답 이력)
- **싱글톤**: `QuizStatsManager.getInstance(context)`
- **주요 메서드**:
  ```kotlin
  // 도전/정답 기록
  fun recordAttempt(category: String, question: String, isCorrect: Boolean)
  // 오답률 높은 상위 N개 질문 조회 (AI 타겟팅용)
  fun getHighErrorQuestions(n: Int): Map<String, Float>
  // 특정 질문 통계
  fun getQuestionStats(category: String, question: String): QuestionStats
  ```

---

## 📤 퀴즈 파일 포맷 (.kquiz)

`.kquiz` 파일은 실제로 JSON 형식입니다:
```json
{
  "category": "카테고리명",
  "creator": "출제자 닉네임",
  "creatorId": "장치해시ID",
  "version": 1,
  "questions": [
    {
      "type": "MULTIPLE_CHOICE",
      "subCategory": "서브카테고리",
      "question": "질문 내용",
      "answer": "정답",
      "explanation": "해설",
      "semanticHint": "힌트",
      "imageUrl": "/data/user/0/.../files/cropped_quizzes/crop_xxx.png",
      "options": ["정답", "오답1", "오답2", "오답3"]
    }
  ]
}
```

---

## ⚠️ 알려진 주의사항 및 제약

1. **이미지 퀴즈 공유 제한**: `.kquiz` 파일에 `imageUrl`(로컬 절대경로)이 포함되지만, 다른 기기에서는 해당 경로의 이미지가 존재하지 않아 이미지가 표시되지 않음. (현재 미해결)

2. **Gemini API 할당량**: `generateVisualQuizzesFromImages()`는 멀티모달 요청으로 토큰 소비가 큼. 무료 플랜에서는 할당량 초과 시 `429` 에러 발생.

3. **Bounding Box 정확도**: AI가 반환하는 좌표가 부정확할 경우 크롭 영역이 빗나갈 수 있음. 특히 그림이 복잡한 이미지에서 발생.

4. **카테고리 유형 구분**: 공식/클라우드 카테고리 이름이 하드코딩되어 있음. 새 카테고리를 원격에 추가할 때 `QuizScreen`의 `isDefault` 목록도 함께 업데이트 필요.

5. **Android 13+ 미디어 권한**: `READ_MEDIA_IMAGES` 권한 필요. 갤러리 접근 시 자동으로 요청됨.

---

## 🚀 다음 구현 예정 과제

> 증시 대시보드·스파크라인 차트는 **완료**. 현재 남은 후보:

- [ ] **AI 스마트 관심종목 포트폴리오 분석**: 보유 종목 실적 트렌드 종합 리포트 (README Phase 3)
- [ ] **빠른 독서**: 보관함 제목 편집, 통계 상세 화면, 난이도 자동 추천
- [ ] **이미지 퀴즈 공유 개선**: 크롭 이미지를 Base64로 인코딩하여 `.kquiz` 파일에 내장
- [ ] **AI 마크다운 렌더링**: 브리핑 결과에 Rich Text 뷰어 적용

---

## 🔄 최근 커밋 이력 (최신순, 2026-06-24 세션)

> 최신 상태는 항상 `git log --oneline -20` 으로 확인. 아래는 이번 세션 주요 작업.

| 커밋 | 내용 |
|------|------|
| `59eb82a` | fix: 증시 차트 크로스헤어 값 드래그 실시간 갱신 (px/dp 단위 버그) |
| `7b42f9a` | feat: 빠른 독서 — 보관함 선택 표시 + WPM 추이 그래프 + 21일 챌린지 |
| `9ea14f4` | feat: 빠른 독서 — 안구 추적 + 워밍업 호흡 신호 개선 + OCR 안정화 |
| `23ec7c2` | feat: 빠른 독서 — 지문 보관함 + 묶어 읽기(청크) |
| `23a141f` | feat: 빠른 독서 — 책 페이지 촬영/업로드 OCR 지문 추출 |
| `a28ddde` | feat: 빠른 독서 — 사용자 텍스트 + AI 이해도 채점 |
| `5fda049` | feat: 배움터 '빠른 독서 훈련' 탭 MVP (워밍업/페이서/RSVP) |
| `1fa2992` | feat: 증시 실적 정상화·AI 백그라운드/캐싱·브리핑 일시정지·설정 5탭 |
| `9e29ca3` | fix: 보안 강화(DART 키 분리/백업·WebView 보호) + 증시 카드 버그 |

---

## 📈 증시 대시보드 기능 메모 (2026-06-24 기준)

증시 탭은 `StockDashboardScreen` + `StockViewModel` + `StockRepository`로 구성. 3개 서브탭: `시세 및 차트(0) / AI 실적 공시(1) / 실적 예정 일정(2)`.

### 데이터 소스
- **시세/차트**: Yahoo Finance 차트 API(`query1.finance.yahoo.com/v8/finance/chart`, 인증 불필요). volume 포함 파싱.
- **실적 공시**: Open DART `list.json` **`pblntf_ty=A`(정기공시)** → 항목 클릭 시 `fnlttSinglAcntAll.json`로 재무 조회. 보고서명 `(YYYY.MM)`으로 reprt_code 판별(`.03→11013 .06→11012 .09→11014 .12→11011`), **CFS→OFS 폴백**.
- **실적 예정 일정**: 무료 컨센서스 소스가 없어, **정기보고서 법정 제출기한 역산**(분기말+45일 등)으로 예상일 표시.

### 로컬 캐시 파일 (`filesDir`)
- `stock_prices_cache.json` — 시세 인메모리+파일 캐시
- `earnings_disclosures_cache.json` — 공시 AI 요약 캐시(rcept_no 기준, 90일 TTL)
- `expected_reports_cache.json` — 사전 전망 리포트 캐시(corp_name 기준)
- `favorite_disclosures.json` / `hidden_disclosures.json` — 즐겨찾기/숨김 공시

### AI 분석 완료 안내 (StockViewModel)
- VM은 **Activity 스코프**(MainScreen에서 hoisting)라 탭 이동 시에도 분석 유지.
- 완료 위치 분기: `!appInForeground`→시스템 알림(채널 `ai_analysis_channel`, id 3001), 앱 내 다른 탭→인앱 스낵바(`analysisCompletedEvent`), 해당 탭→다이얼로그.
- 포그라운드 여부는 MainActivity 생명주기(ON_RESUME/ON_PAUSE)에서 `setAppForeground()`로 전달.
- 알림 탭 시 `MainActivity(launchMode=singleTask)` + `NAVIGATE_TO/STOCK_SUBTAB` extra → `requestStockSubTab()`로 서브탭 이동(새 실행 X).

### 주의
- 실적 공시 매출 계정 동의어는 `parseFinancialAccounts()`의 `setOf(...)`에서 관리(예: `수익(매출액)` 포함). 누락 계정명 발견 시 여기에 추가.
- 공시 캐시 TTL 날짜 포맷은 반드시 `yyyyMMdd`(소문자 dd). `DD`는 연중 일수라 오삭제 발생.

---

## ⚡ 빠른 독서 훈련 (배움터 탭) 메모 (2026-06-24 추가)

- 진입: `LearningHubScreen` 탭1 → `ReadingTrainingScreen` (기존 '지식 플러스' placeholder 교체).
- 파일: `ui/screens/ReadingTrainingScreen.kt`(허브 + 모듈: 워밍업/페이서/RSVP/청크/안구추적/결과/이해도),
  `ui/viewmodel/ReadingTrainingViewModel.kt`, `data/repository/ReadingTrainingRepository.kt`.
- 저장:
  - DataStore `reading_training`: best_wpm / streak_days / last_trained_date / total_sessions / best_comprehension / trained_dates(집합)
  - `filesDir/reading_passages.json`(지문 보관함) + `filesDir/reading_pages/`(썸네일 JPEG)
  - `filesDir/reading_wpm_history.json`(WPM 최근 30회)
- AI: `GeminiManager.extractTextFromImage()`(책 페이지 OCR), `generateComprehensionQuiz()`(이해도 4지선다). 이미지 OCR은 전송 전 최대 2048px 축소.
- ⚠️ 저작권: 일반 속독 기법 + 직접 작성/공개/사용자 텍스트만 사용. '퀀텀독서법'은 브랜드명으로 쓰지 않고 추천 도서로만 언급. 상세 설계: `doc/FEATURE_SPEED_READING.md`.

---

## 💡 새 세션 시작 체크리스트

새 AI 세션에서 개발을 시작할 때 반드시 확인할 사항:

1. **이 파일 읽기** - 전체 맥락 파악
2. **`git log -n 10 --oneline`** - 최신 커밋 확인
3. **`git status`** - 미커밋 변경사항 확인
4. 작업 관련 파일 직접 열기 (위 디렉토리 구조 참조)
5. 영역별 진입 파일:
   - 퀴즈 → `QuizRepository.kt`, `GeminiManager.kt`, `QuizViewModel.kt`, `QuizScreen.kt`, `QuizCreatorScreen.kt`
   - 증시 → `StockRepository.kt`, `StockViewModel.kt`, `StockDashboardScreen.kt` (+ 위 "증시 메모")
   - 빠른 독서 → `ReadingTrainingScreen/ViewModel/Repository.kt` (+ "빠른 독서 메모", `doc/FEATURE_SPEED_READING.md`)
   - 브리핑/설정 → `BriefingViewModel.kt`, `NewsBriefingScreen.kt`, `MorningBriefingSettingsScreen.kt`(5탭)
6. **빌드 시**: `local.properties`에 `dart.default.key` 필요(없으면 DART 기본키 빈값). 빌드 확인은 `./gradlew.bat :app:assembleDebug`.

---

> 이 문서는 개발 진행에 따라 지속적으로 업데이트됩니다. (최신 커밋은 `git log`로 확인)
