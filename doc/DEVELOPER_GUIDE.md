# 🛠 KDailyUtil - 개발자 가이드 (Developer Context Guide)

> **신규 세션 또는 AI 어시스턴트가 이 파일을 먼저 읽으면 프로젝트 전체 맥락을 즉시 파악할 수 있습니다.**
> 최종 업데이트: 2026-07-29

> ### 🚩 지금 상태 (신규 세션 필독)
> - **v1.6 (versionCode 6) — ⏳ 2026-08-03 업로드 완료·Google Play 검토 중.** 산출물 = [`app/release/kdailyutil-v1.6.aab`](../app/release/kdailyutil-v1.6.aab) (11,211,476 bytes, 업로드 키 서명 검증 완료). 소스 = v1.6([app/build.gradle.kts](../app/build.gradle.kts)). **검토 통과 전까지 게시본 = v1.5(vc5, 07-23).** (이전 v1.4=vc4 07-21 / v1.2=vc3 07-08 / v1.1=vc2.) **다음 업로드는 vc7=1.7.**
> - **v1.6 내용**: 자매앱 동적 레지스트리(§8) — 이 배포부터 실사용 시작. 출시 노트 문구 = [`app/release/RELEASE_NOTES.md`](../app/release/RELEASE_NOTES.md).
> - **개인정보처리방침 URL 변경(2026-08-03)**: Netlify → **`https://kitwlsh.github.io/k-series-config/privacy-kdailyutil.html`** (Play Console 등록 완료). 원본 = [doc/privacy-kdailyutil.html](privacy-kdailyutil.html), 표준 = [KLOTTO_CONNECT_HANDOFF.md §9](KLOTTO_CONNECT_HANDOFF.md). ⚠️ **권한을 추가·제거하면 방침도 같은 커밋에서 고칠 것.**
> - **버전 스킴**: versionName **끝자리 = versionCode**(vc5=1.5, 이후 vc6=1.6…). versionCode는 정수만·매 업로드 증가, versionName은 표시용 문자열(점 자유).
> - **v1.5 AAB 빌드·업로드·출시 완료(2026-07-23 게시)**: [`app/release/kdailyutil-v1.5.aab`](../app/release/kdailyutil-v1.5.aab)(게시본 v1.4 이후 변경: 뉴스 AI 대화창+음성·마크다운·핸즈프리 / 증시 AI 포트폴리오 종합 분석 / 빠른독서 통계·난이도 / 이미지 퀴즈 Base64). 스토어 자산 그대로 사용 가능 — 앱 아이콘 512=`app/src/main/ic_launcher-playstore.png`, 그래픽 1024×500=[`doc/screenshot/feature_graphic_1024x500.png`](screenshot/feature_graphic_1024x500.png), 설명=[`doc/STORE_LISTING.md`](STORE_LISTING.md). **업로드 절차**: Play Console > 프로덕션 > 새 버전 > 이 AAB 업로드 + 출시노트(아래) 붙여넣기. ※ 새 기능 대비 STORE_LISTING 설명 갱신은 선택.
>   - 출시노트/버전별 내역: [`app/release/RELEASE_NOTES.md`](../app/release/RELEASE_NOTES.md) (콘솔 '출시 노트'란 붙여넣기용 문구 포함).
> - 2026-07-20 세션분은 `origin/main` **푸시 완료**. 최신 상태는 `git status`로 확인.
> - **v1.1 이후 주요 변경(= v1.2에 담길 분)**: 오디오 SAF 복구·미디어버튼·인터럽트 재개 / 뉴스 저작권 보수화 / 퀴즈 중복방지 3중화·AI 퀴즈 가이드·커스텀 편집·오류신고 게이팅 / 증시 '실적 뉴스·전망' 개편·과거 실적 조회+회사 검색·조회기간 AI캐시 복원·DART 키 도움말 / 탭 ＋빠른추가 / (2026-07-07) 퀴즈 파싱 내구성·상식백과 복구·데이터 검증 CI·퀴즈/독서 뒤로가기·AI 실적공시 헤더통합 스크롤·동일회사 1건 축약·숨김 객체영속·과거실적 회계기준월·증시 키워드 2종 구분 / **(2026-07-15~16) K-시리즈 아이콘·스플래시·워터마크 패밀리 통일** / **(2026-07-20) 키워드 순서변경(Set→List)·과거실적 추세 AI 코멘트·빠른독서 보관함 제목편집·자매앱 상호연결(브랜드&자매앱 갤러리)**.
> - **아이콘/스플래시 패밀리 통일 (2026-07-15~16)**: 런처 아이콘(mipmap 전 밀도)·플레이스토어 아이콘·스플래시(`ic_app_logo_full` 자체 앱 아이콘으로 교체)·워터마크([BrandComponents.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/ui/components/BrandComponents.kt))·설정 브랜드 갤러리를 형제 앱(KLotto645 등)과 통일. 단일 기준 문서 = [doc/K_SERIES_ICON_RECIPE.md](K_SERIES_ICON_RECIPE.md), 원본/스크립트 = `doc/family_icons/`, `doc/icon_scripts/`. **미사용 `HexagonShape` 제거됨.**
> - **자매앱 상호연결 (2026-07-20 정적 → 2026-07-29 동적)**: 설정 > 앱정보 > **'브랜드 & 자매앱'** 갤러리의 자매앱 카드. 탭 시 설치/실행(`openAppOrStore(pkg, storeUrl)`). 2026-07-29부터 **카드 목록이 원격 레지스트리에서 오므로 앱 수정 없이 자매앱을 추가**한다(위 '자매앱 동적 레지스트리' 항목). 표준 = [doc/KLOTTO_CONNECT_HANDOFF.md](KLOTTO_CONNECT_HANDOFF.md)(**KDailyUtil·KLotto645·KJangbu 세 저장소 doc/ 동일 사본 유지**). **KLotto645·K장부 저장소 쪽 이식/커밋은 각 세션 담당**(§8-10 체크리스트).
> - **🆕 뉴스 AI 대화창 (2026-07-21, 구현 완료·미배포)**: 뉴스탭 'AI' 탭에서 맞춤 분석을 첫 답으로 **멀티턴 대화**(이어 묻기) + **음성 입력(STT)·답변 낭독(TTS)**. 컨텍스트=제목+RSS 스니펫(제한매체 필터 승계, 본문 비스크랩). **대화 수명=세션(명령어+날짜)**, 로컬 보관 **30일 자동정리 + 사용자 수동 삭제(개별·전체)**, 지난 대화 읽기 전용 열람. 설계·구현서 = [doc/FEATURE_AI_NEWS_CHAT.md](FEATURE_AI_NEWS_CHAT.md). 구현: `GeminiManager.startNewsChat/sendChatMessage`, `AiChatSession`/`AiChatRepository`, `BriefingViewModel`(대화 상태·`sendChat`·`startChatVoiceInput`·세션관리), `NewsBriefingScreen.AiChatSection`. **v1.4 게시 후 vc5/v1.5로 배포**.
> - **🆕 자매앱 동적 레지스트리 (2026-07-29, 구현 완료·미배포)**: 자매앱 카드를 하드코딩에서 **원격 `family.json` 동적 렌더**로 전환 → **신규 자매앱 추가에 앱 재배포 불필요**. 구현: [`FamilyRepository`](../app/src/main/java/com/kitwlshcom/kdailyutil/data/repository/FamilyRepository.kt)(6h캐시→원격→last-good→번들 폴백, 화이트리스트 검증) · [`FamilyApp`](../app/src/main/java/com/kitwlshcom/kdailyutil/data/model/FamilyApp.kt) · `res/raw/family.json`(번들 기본값) · `AndroidManifest <queries>` 예약 패키지 8개 · `MorningBriefingSettingsScreen.SisterAppCard`(로딩/출시예정/설치배지 + 🔄 새로고침). 표준 = [doc/KLOTTO_CONNECT_HANDOFF.md](KLOTTO_CONNECT_HANDOFF.md) **§8**, 정본·업로드 = [doc/family_config/README.md](family_config/README.md). **진행**: ✅ 호스팅 레포 `kitwlsh/k-series-config` 생성·업로드 완료 ✅ **KLotto645·K장부 이식 완료(3개 앱 전부 동적)** ✅ **실기기 검증 10/10 통과(§8-12)** ✅ **v1.6 AAB 빌드 완료(2026-08-03)** → **남은 일 = Play Console 업로드·출시**(게시돼야 효력 발생).
> - **다음 후보**: (① AI 포트폴리오 분석 = ✅ 2026-07-22 완료) 미배포분 전부 v1.5로 출시 완료(2026-07-23 게시). 남은 후보: (선택) '시세 및 차트' 관심종목 편집 UI 두-목록 안내. **✅ 2026-07-24 완료: AI 추세 코멘트 마크다운 렌더링 + AI 대화 히스토리 토큰 상한(§8-5).** **✅ 2026-07-29 완료: 자매앱 동적 레지스트리(위 항목).** 완료분(2026-07-21~22): 뉴스 AI 대화창·마크다운·핸즈프리·이미지 Base64·빠른독서 통계/난이도·**포트폴리오 종합 분석**.
> - **AAB 재빌드 방법**: `./gradlew.bat :app:bundleRelease` (서명은 `local.properties`의 `release.*` 키로 자동 — VCS 제외). 산출물: `app/build/outputs/bundle/release/app-release.aab`.
> - **🔐 서명 키 위치·정책 (2026-07-29 정리)**: 키스토어는 **저장소 밖** `d:/DATA/20_Source/_secrets/kitwlsh-upload.jks`. 이 앱은 **Play 앱 서명 등록됨** → 로컬 키는 **업로드 키**이고 기기 설치 서명은 Google 보관 앱 서명 키다(유출돼도 사칭 설치 불가, Console에서 재설정 가능). 과거 `user.keystore`가 이 공개 저장소에 커밋돼 있던 것을 추적 해제하고 `.gitignore`(`*.jks`·`*.keystore`·`keystore.properties`)를 보강했다. **업로드 전 `keytool -printcert -jarfile <aab>`로 서명 확인**(업로드 키 SHA-256 = `61:12:DE:02:AD:DF:…:A5:12:99`). 지문·재설정 절차 = [doc/GOOGLE_PLAY_RELEASE_GUIDE.md](GOOGLE_PLAY_RELEASE_GUIDE.md) 1단계 ①. **📌 남은 조치 = 업로드 키 재설정(권장, 사용자 영향 0).**

---

## 📌 프로젝트 개요

- **앱 이름**: KDailyUtil
- **패키지**: `com.kitwlshcom.kdailyutil`
- **언어**: Kotlin (Jetpack Compose)
- **최소 SDK**: 26 / 타겟 SDK: 36
- **버전**: 소스 = versionCode 5 / versionName 1.5 · **현재 게시본 = vc5 / v1.5 (2026-07-23 출시)**. 스킴: versionName 끝자리 = versionCode(다음은 vc6=1.6).
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
│   │   ├── AudioCaptureService.kt          # 포그라운드 오디오 캡처/재생(MediaSession 미디어버튼·오디오포커스, 유휴 시 알림 해제)
│   │   └── TtsManager.kt                   # TTS 낭독
│   ├── data/
│   │   ├── model/
│   │   │   ├── QuizQuestion.kt             # 퀴즈 데이터 모델 (imageUrl 필드 포함)
│   │   │   ├── StockModels.kt              # 시세/차트/공시/예정 모델 (EarningsDisclosure 등)
│   │   │   ├── AudioItem.kt
│   │   │   ├── FamilyApp.kt                # K-시리즈 자매앱 1건(원격 레지스트리 family.json 항목)
│   │   │   └── NewsItem.kt
│   │   ├── remote/
│   │   │   └── GeminiManager.kt            # Gemini 통합 (퀴즈/요약/공시/OCR/이해도 채점)
│   │   ├── repository/
│   │   │   ├── QuizRepository.kt           # 퀴즈 CRUD, 원격 동기화, 커스텀 저장
│   │   │   ├── StockRepository.kt          # Yahoo 시세/차트 + DART 공시/재무 + 캐시/즐겨찾기/숨김
│   │   │   ├── ReadingTrainingRepository.kt# 빠른 독서 훈련 진척/보관함/WPM이력
│   │   │   ├── AudioRepository.kt          # 오디오 파일 관리
│   │   │   ├── NewsRepository.kt           # 뉴스 RSS 수집 및 크롤링
│   │   │   ├── FamilyRepository.kt         # 자매앱 동적 레지스트리(원격 family.json + 캐시/번들 폴백)
│   │   │   └── SettingsRepository.kt       # DataStore 기반 설정 저장 (Gemini/DART 키 등)
│   │   ├── QuizFileHandler.kt              # .kquiz 파일 export/import(+텍스트 파싱)
│   │   ├── QuizAiGuide.kt                  # AI로 개인 퀴즈 만들기 가이드/프롬프트/저장·공유
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
├── app/src/main/res/raw/
│   ├── family.json                         # 자매앱 레지스트리 번들 기본값(정본=doc/family_config/family.json)
│   ├── quiz_correct.mp3 / quiz_wrong.mp3 / quiz_finish.mp3
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

// 퀴즈 로드 (카테고리 필터 가능, 셔플됨). 표시 단계 중복 제거 적용(dedupeQuizzes)
suspend fun getQuizzes(context: Context, category: String? = null): List<QuizQuestion>

// 커스텀 퀴즈 저장 (ID 기준 병합 + 제출 단계 정답/질문 중복 방지)
suspend fun saveCustomQuizzes(context: Context, quizzes: List<QuizQuestion>)

// 커스텀 카테고리 삭제 (크롭 이미지 파일도 함께 삭제)
suspend fun deleteCustomCategory(context: Context, category: String)

// 커스텀/원격 카테고리 목록 조회
suspend fun getCustomCategories(context: Context): List<String>
suspend fun getRemoteCategories(context: Context): List<String>
```

### 퀴즈 중복 방지 3중 구조 (2026-06-26)
같은 정답·질문이 여러 번 나오지 않도록 `QuizRepository`에서 3단계로 막는다. 정규화는 `norm()`(공백 제거+소문자), 키는 `(category, norm(answer))`·`(category, norm(question))`.
1. **생성**: `QuizCreatorScreen`이 `previousQuizzes`에 **정답까지** 담아 AI에 전달 + `GeminiManager` 프롬프트가 '같은 정답이면 출제 금지'를 지시(낭비 생성 감소).
2. **제출(`saveCustomQuizzes`)**: 같은 카테고리에 이미 있는 **다른 ID** 퀴즈와 정답/질문이 겹치면 저장 skip(같은 ID는 갱신 허용) — 영속 단계의 확실한 보장.
3. **표시(`getQuizzes`)**: static+remote+custom 합친 최종 목록을 `dedupeQuizzes()`로 한 번 더 정리(첫 등장만) — 소스 무관 안전망.
> ⚠️ 시각(이미지) 퀴즈(`imageUrl` 존재)는 정답이 같아도 그림이 다를 수 있어 **질문 기준만** 중복 판정한다.
> 원격 데이터 자체의 중복은 `korean_quiz_data`의 `update_quiz.py`(정답·신조어 개념 가드)에서 별도 관리.

### 퀴즈 데이터 파싱 내구성 (2026-07-07)
불량 데이터 1건이 카테고리 전체를 못 쓰게 만들던 문제(상식백과 빈 화면 사고)를 3중으로 방어한다.
1. **`QuizType.fromRaw(raw, hasOptions)`** ([QuizQuestion.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/data/model/QuizQuestion.kt)): 알 수 없거나 오타 난 type(예: `"MULTIPLE_CHOENCE"`)에도 예외를 안 던지고 보기 유무로 폴백. **원격/가져오기/AI생성/크리에이터 6개 파싱 지점 전부** `QuizType.valueOf` → `fromRaw`로 교체(예전엔 valueOf 예외로 배치/파일 전체가 통째로 버려짐).
2. **`parseQuizzes` 문항별 try/catch**: 손상 문항만 스킵하고 나머지는 살림.
3. **`syncRemoteQuizzes` 카테고리 단위 병합**: 이번에 성공적으로 받은 파일의 카테고리만 신선 데이터로 교체하고, **통신 일시 실패한 파일의 카테고리는 기존 캐시(last-good)를 유지**. 완전 오프라인(성공 파일 0)이면 캐시를 건드리지 않음. (예전 '전체 덮어쓰기'는 파일 1개 실패 시 해당 카테고리가 캐시에서 증발했음)
4. **`startQuiz` 빈 목록 가드**: 문항 0개면 PLAYING(빈 화면) 대신 안내 토스트 + 분야 선택 화면 유지.
> 원격 데이터 자체는 `korean_quiz_data/validate.py`(CI 게이트)가 불량이면 push를 차단한다(아래 "원격 퀴즈 데이터" 참조).

### 배움터 시스템 뒤로가기 (2026-07-07)
퀴즈/독서의 내부 단계는 `NavController`가 아니라 화면 상태(`QuizState` / `ReadingModule`)로 관리되므로, 시스템 뒤로가기를 그대로 두면 배움터 목적지가 통째로 pop돼 **시작탭(뉴스)로 튕겼다**. `BackHandler`로 내부 단계를 먼저 되짚게 함.
- `QuizScreen`: 풀이/생성/결과/크리에이터 → 분야 선택(`selectCategory(null)`), 분야 선택 → 초기(`backToIdle()`), 초기(IDLE)만 기본 동작(배움터 이탈) 허용.
- `ReadingTrainingScreen`: 훈련 모듈 진행 중 → 독서 허브(`ReadingModule.HUB`).
- ⚠️ 하단 탭에서 뒤로가기 시 시작탭(뉴스)로 가는 것 자체는 Material 하단내비 표준 패턴이라 **의도적으로 유지**([MainScreen.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/ui/MainScreen.kt) `popUpTo(startDestination)`).

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
  - `quiz_updates.json` → **빈 배열(미러 폐지)**. 과거 이중기록이 ID 충돌·중복 출제를 유발해 폐지. 각 문제는 카테고리 파일 1곳에만 존재.

- **Python 업데이트 스크립트**: `d:\DATA\20_Source\80_Git_HUB\KDailyUtil\korean_quiz_data\update_quiz.py`
  - 매일 워크플로(`daily_update_.yml`, cron KST 9시)로 5문항 추가. **전역 유니크 ID** 부여 + **중복 가드**(① 질문 ② 정답 ③ 트렌드 신조어 개념). 기존 정답·신조어 목록을 프롬프트에 주입해 같은 답 재생성 차단.
  - **유효성 가드(2026-07-07)**: 저장 전 `type`이 규격(MULTIPLE_CHOICE/SUBJECTIVE)이고 객관식은 보기 4개일 때만 저장(AI 오타 문항 skip).
  - 데이터는 (카테고리,정답)/(카테고리,질문)/신조어 개념 기준으로 정리됨(약 216문항).
- **데이터 검증 게이트**: `validate.py`(2026-07-07 신설) — 전체 JSON의 type/필수필드/보기4개/answer∈options/id 전역유니크를 검사. `daily_update_.yml`이 커밋·푸시 **직전에 실행**하여 오류가 하나라도 있으면 exit(1)로 **push를 차단**. ⚠️ 기존 데이터에 오류가 있으면 매일 실행이 계속 실패하니, 검증 실패 시 데이터부터 고쳐야 함.
- **로컬 캐시 파일**: `filesDir/quizzes_v2.json` (동기화 시 카테고리 단위 병합 — 성공 카테고리만 교체, 실패 파일 카테고리는 last-good 유지. 위 "퀴즈 데이터 파싱 내구성" 참조)

---

## 🔐 권한 및 보안

### AndroidManifest.xml 권한
- `INTERNET` - 뉴스 크롤링, Gemini API, 원격 퀴즈 동기화
- `CAMERA` - 퀴즈/책 페이지 이미지 촬영
- `RECORD_AUDIO` - 오디오 캡처
- `READ_EXTERNAL_STORAGE` (API ≤32) / `WRITE_EXTERNAL_STORAGE` (API ≤29) - 공용 Download의 이전 녹음 마이그레이션 전용(버전 상한)
- `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS` - 예약 브리핑

> ⚠️ **READ_MEDIA_IMAGES / READ_MEDIA_VIDEO / READ_MEDIA_AUDIO 제거됨 (2026-06-25)**
> Google Play '사진·동영상 권한 정책' 거부(versionCode 1) 대응. 이미지 선택은 **Android Photo Picker**(`PickVisualMedia`/`PickMultipleVisualMedia`, 권한 불필요)로, 오디오/파일 가져오기는 시스템 문서 피커(`GetContent`)로 처리. 기기 전체 MediaStore 스캔(`AudioRepository`)도 제거 — 녹음/가져온 파일은 앱 전용 폴더에서만 관리.

> ⚠️ **v1.0→v1.1 업그레이드 시 옛 녹음 접근 불가 → SAF 복구 도입 (2026-06-26)**
> `READ_MEDIA_*` 제거 + `READ_EXTERNAL_STORAGE`(maxSdk 32, 런타임 요청은 API≤29만) 때문에 Android 11+에서 공용 `Download/KDailyUtil`의 옛 녹음을 더 이상 읽지 못함(재생 시 `setDataSource` 실패, 마이그레이션 `listFiles()`=null). 권한 복원은 정책 위반이므로 **SAF(Storage Access Framework)** 로 복구: 파일 탭 헤더/빈 화면의 **'기존 폴더에서 복구'**(`OpenDocumentTree`) → `AudioRepository.recoverFromTreeUri()`가 하위 폴더까지 재귀 복사해 앱 전용 폴더로 가져옴(권한 불필요). 누락돼 있던 **'파일 가져오기'**(`GetMultipleContents`) 버튼도 배선. 재생목록(`getRecordedFiles`)은 옛 공용 경로보다 앱 전용 복사본을 우선 해석. 의존성: `androidx.documentfile`.

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

### 📰 뉴스 저작권 정책 (보수적 공개판, 2026-06-26)
- **본문 비스크랩**: 본문 전문 스크랩/복제/AI 전송 금지. `NewsRepository.fetchFullContent`는 `@Deprecated`(호출 금지, 추출 로직 참고용 보존), 대신 `resolveArticleUrl`이 표시용 원본 URL만 해석. 읽기는 언론사 원문 WebView(아웃링크).
- **낭독(TTS)**: 모닝 브리핑/단건 낭독은 RSS 스니펫(`summary`/`description`)만 읽음. 'AI 이용 금지' 매체(`aiRestricted`)는 낭독·AI 분석에서 제외.
- **AI 금지 감지**: ① 도메인 백스톱 `AI_RESTRICTED_DOMAINS`(mbc/imbc/kmib.co.kr/industrynews.co.kr) ② `detectAiRestrictionNotice()`(AI어＋금지어, 일반 '무단전재'는 제외) ③ `NewsDetailScreen` WebView `onPageFinished`의 innerText 점검 → `BriefingViewModel.markSelectedAsRestricted()`. `aiRestricted`는 캐시에 영속.
- **쉐도잉 = 배움터로 이전**: 외부 기사 쉐도잉 진입점(뉴스 리스트 `NewsCard`·상세 FAB) 제거. 쉐도잉은 **배움터(`ReadingTrainingScreen`) "🗣️ 따라 말하기"** 에서 사용자 입력/촬영(OCR) 텍스트로만 제공. `ShadowingViewModel`은 뉴스 의존성 제거 후 `setText(body, title)`로 구동(`MainScreen`이 `LearningHub`↔`DrivingShadowing`에 동일 인스턴스 공유). 사용자 책임 고지 노출.
- **AI 맞춤분석 필터**: `BriefingViewModel.generateAiCustomBriefing`이 `getTopNews(20)`에서 AI금지 도메인/문구 항목을 Gemini 전송 전 `filterNot` 제외.
- 근거 요약: 헤드라인+스니펫+아웃링크는 수집 안전선(전문복제는 위험), 'AI금지' 자연어 문구는 법적 표준 신호 아님(기계판독 robots.txt/TDMRep), OCR/사용자입력은 사적복제(저작권법 제30조). 개인용 외부기사 쉐도잉이 필요하면 공개앱 숨김토글이 아니라 **별도 빌드**로(향후 옵션).

### API 키
- **저장**: Preferences DataStore (`SettingsRepository.geminiApiKeyFlow`)
- **UI 입력**: `MorningBriefingSettingsScreen` > API 키 설정
- **사용**: `GeminiManager(apiKey)` 생성자 주입
- **발급 도움말**: Gemini·**Open DART** 키 모두 입력칸 옆 도움말(?) → 발급 가이드 다이얼로그 + 발급 사이트 이동 버튼 제공(DART = `opendart.fss.or.kr`, 2026-07-06 추가).

### 뉴스/증시/AI 탭 '＋ 빠른 추가' (2026-07-06)
설정 화면에만 있던 카테고리·키워드·명령 추가를, 각 탭에서도 바로 할 수 있게 `NewsBriefingScreen`에 '＋' 진입점 추가(상시 노출은 '＋'만, 삭제/순서변경은 설정 화면 유지).
- 세로 주제(카테고리)탭 끝 '＋' → `updateCategories`, 증시 가로 서브탭 끝 '＋' → `updateStockKeywords`, AI 가로 서브탭 끝 '＋' → `updateAiCommands`. 하나의 추가 다이얼로그(`addTarget`)로 처리하며 설정과 동일 저장 로직 재사용.
- ✅ **키워드 순서 변경 지원(2026-07-20)**: 카테고리·브리핑 키워드·뉴스탭 증시 키워드·증시 대시보드 관심종목·AI 명령을 모두 `Set`→**순서 보존 `List`** 저장으로 전환. 설정 화면의 각 칩을 누르면 **'앞으로/뒤로 이동·삭제'** 메뉴(`ReorderableChipRow`, MorningBriefingSettingsScreen). 자세한 저장 규격은 아래 "키워드 순서 보존 저장" 참조.

#### 키워드 순서 보존 저장 (Set→List, 2026-07-20)
DataStore Preferences에는 List 네이티브 타입이 없어, 순서 있는 목록을 **구분자(`\n`)로 이은 문자열**(`stringPreferencesKey`)로 저장한다.
- 대상 5종 + 저장 키(순서용): `keywords_order` / `news_categories_order` / `ai_briefing_commands_order` / `stock_keywords_order` / `watch_stock_keywords_order`. (기존 `stringSetPreferencesKey`는 마이그레이션 읽기용으로 남겨 둠)
- 읽기(`SettingsRepository.readOrderedList`): 순서 키가 있으면 그대로(빈 문자열 `""`=사용자가 전부 지운 상태 → 빈 목록), 없으면 **레거시 Set 키에서 1회 마이그레이션**(항목 보존, 순서는 임의), 둘 다 없으면 기본값.
- 쓰기(`saveOrderedList`): trim + blank 제거 + `distinct()`(첫 등장만 유지해 Set의 유일성 보존) 후 `joinToString("\n")`.
- 관련 Flow/메서드는 전부 `List<String>`으로 변경(`BriefingViewModel`, `StockViewModel.reorderStockKeywords`). `NewsRepository.getAllNews`는 `Collection<String>` 수신으로 완화.
- ⚠️ 카테고리의 고정 항목("전체"·"증시"·"AI")은 `ReorderableChipRow(fixedItems=...)`로 이동·삭제 불가 처리(고정 항목 앞으로는 이동 못 함).

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

## 🧩 자매앱 동적 레지스트리 (family.json, 2026-07-29)

자매앱 카드를 **앱에 하드코딩하지 않고 원격 JSON에서 받아 렌더**한다. 목적은 단 하나 — **신규 자매앱 추가에 기존 앱을 재배포하지 않는 것**. 표준 정의(스키마·보안·이식 절차) = [doc/KLOTTO_CONNECT_HANDOFF.md §8](KLOTTO_CONNECT_HANDOFF.md).

- **원격 URL(고정)**: `https://raw.githubusercontent.com/kitwlsh/k-series-config/main/family.json`
  `FamilyRepository.REMOTE_URL` 상수. **이 값을 바꾸면 전 앱 재배포가 필요하므로 고정**한다.
- **폴백 체인**: 신선한 캐시(6h, `filesDir/family_config.json`) → 원격 fetch → **last-good 캐시(오래돼도)** → **번들 `res/raw/family.json`**. 어느 단계에서도 카드가 사라지지 않는다(퀴즈 캐시 패턴과 동일).
- **필터·정렬**: `active=false` 제외 → **자기 자신(`id == BuildConfig.APPLICATION_ID`) 제외** → `order` 정렬 → 상한 20개.
- **`comingSoon=true`**: 카드는 보이지만 '🔜 출시 예정' **비활성**(클릭 무반응). 출시 시 JSON에서 `false`로만 바꾸면 활성화 — 앱 수정 없음.
- **아이콘**: `iconUrl`을 Coil `AsyncImage`로 로드, placeholder/error = **번들 폴백**(`bundledSisterIcon()`: 아는 앱은 로컬 PNG, 모르는 앱은 공통 엠블럼 `ic_k_logo_3d`). 그래서 **번들 리소스가 없는 새 앱도 카드가 정상 표시**된다.
- **검증(신뢰 경계)**: `id`는 패키지명 정규식 통과분만, `storeUrl`은 `play.google.com`/`market://`만, `iconUrl`은 https + `githubusercontent.com`/`github.io`만 허용. 항목별 try/catch로 **1건이 깨져도 나머지는 살린다**. JSON은 표시용 데이터만 담고 **임의 인텐트/딥링크는 스키마에 없다**.
- **UI**: 설정 > 앱정보 > **'브랜드 & 자매앱'** 다이얼로그의 자매앱 구획(`SisterAppCard`). 로딩 스피너 / 빈 목록 안내 / **🔄 강제 새로고침**(`forceRefresh=true`, JSON 수정 즉시 확인용).
- **조회 시점**: 앱 시작 시가 아니라 **다이얼로그를 열 때만**(`produceState`). 6h 이내면 네트워크를 아예 타지 않으므로 배터리·트래픽 영향 없음(하루 최대 4회 × 3.5KB ≈ 14KB).
- **조건부 요청(ETag) 미도입**: raw가 `ETag`/`304`를 지원하지만(확인됨) 절약분이 3.5KB뿐이고, TTL을 줄이면 오히려 TLS 핸드셰이크 비용이 커져 **현행 유지**로 결론(검토 기록은 §8 논의).

### 실기기 진단 (문제 생겼을 때 여기부터)
```bash
adb logcat -s FamilyRepository:V          # 어느 폴백 단계를 탔는지 한 줄로 나온다
adb shell run-as com.kitwlshcom.kdailyutil ls -l files/family_config.json   # last-good 캐시(정상 3509B)
adb shell run-as com.kitwlshcom.kdailyutil ls -l cache/image_cache/         # Coil 아이콘 캐시
```
로그 문구는 **K-시리즈 3개 앱 동일**하게 유지한다(§8-12): `🗄 신선한 캐시 사용(재조회 안 함)` / `✅ 원격 레지스트리 로드` / `⚠️ 원격 실패 — last-good 캐시 사용` / `⚠️ 원격·캐시 모두 불가 — 번들 기본값`.
검증 체크리스트(10항목)·주입 테스트 방법·안전 규칙(⛔ `pm clear` 금지 등)은 [doc/KLOTTO_CONNECT_HANDOFF.md §8-12](KLOTTO_CONNECT_HANDOFF.md).

> ✅ **2026-07-29 실기기 검증 10/10 통과**: 캐시 없는 오프라인 첫 실행에도 카드 유지(번들 폴백), 캐시 손상 시 번들 폴백, 악성 레지스트리(부적격 id·외부 URL·상한 초과·상위 스키마) 전부 차단, 6h 이내 네트워크 미사용·6h 후 재조회, 스토어 이동, 원격 아이콘 바이트 일치, 자매앱 0개 시 안내 문구. 이 과정에서 **원격 아이콘 검은 배경 결함 1건을 발견해 앱 재배포 없이 수정**(§8-12 사례).

### ⚠️ `<queries>` — 원격으로 못 바꾸는 유일한 제약
Android 11+는 `getLaunchIntentForPackage`(설치 감지)·직접 실행에 **매니페스트 `<queries>` 선언**이 필요하다. 그래서 미래 자매앱 패키지를 **예약분까지 미리 선언**해 뒀다(`AndroidManifest.xml`):

```
com.kitwlshCom.klotto645  com.kitwlshcom.kjangbu  com.kitwlshcom.kunbok
com.kitwlshcom.kfamily1 ~ kfamily5   (여유 5개)
```

- 신규 앱은 **이 예약 id를 우선 사용**할 것 → 설치배지·직접실행까지 재빌드 없이 동작.
- 예약에 없는 패키지도 **카드 노출·스토어 이동은 정상**(설치 감지만 안 됨) — 우아한 폴백.
- `QUERY_ALL_PACKAGES`는 Google Play 정책 리스크로 **사용 금지**.

### 레지스트리를 고칠 때 (운영)
정본 = [doc/family_config/family.json](family_config/family.json), 업로드·편집 가이드 = [doc/family_config/README.md](family_config/README.md).
1. `k-series-config` 레포의 `family.json`(+ `icons/`) 수정·푸시 → 전 앱 반영(최대 6h, 🔄로 즉시).
2. **정본과 번들 사본(`app/src/main/res/raw/family.json`)도 동기화** — 번들 갱신은 다음 배포에 따라감(급하지 않음).
3. §7-1 레지스트리 표(사람이 읽는 목록)도 함께 갱신.

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

### AI로 개인 퀴즈 만들기 가이드 (2026-06-26)
외부 AI(ChatGPT·Gemini·Claude 등)에게 위 `.kquiz` 형식대로 퀴즈를 만들게 하고, 앱으로 불러오도록 돕는 기능. 진입점은 `QuizScreen`(우리말 퀴즈, 분야 선택 화면).
- **`data/QuizAiGuide.kt`**: `PROMPT_TEMPLATE`(AI에 붙여넣는 프롬프트 — **주제만 말하면** 기본값 10문항·중·객관식 위주로 즉시 완성, .kquiz 다운로드 요청+텍스트 폴백 명시) / `GUIDE_MARKDOWN`(전체 가이드) / `writeGuideTo(uri)`(SAF 저장) / `exportGuide()`·`shareGuide()`(FileProvider 공유, MIME=application/octet-stream이라 카톡에도 파일 첨부됨).
- **AI 가이드 다이얼로그**(`AiQuizGuideDialog`): 프롬프트 복사 / 가이드 전체 복사 / **가이드 파일로 저장(SAF, 위치 선택)** / 파일 공유 + 사용법·이미지 첨부 팁.
- **가져오기 2경로**: ① 파일 → `📥 (.kquiz) 가져오기`(`GetContent`) ② **텍스트(JSON) 붙여넣기**(`📋` 버튼 → `importQuizFromText`). 파일 생성이 안 되는 AI가 텍스트만 줄 때 대응. 파서(`QuizFileHandler.importQuizzesFromText`)는 코드블록/잡텍스트가 섞여도 첫 `{`~마지막 `}`만 추출.

### 커스텀 퀴즈 오류신고·편집 정책 (2026-06-26)
- **오류 신고(개발자 메일) 게이팅**: 커스텀(개인 제작·가져온·AI생성 저장) 문제는 개발자가 못 고치므로 오류 신고 버튼을 숨김. 공식/클라우드(앱 관리) 문제에서만 노출. 판별 = `customCategories.contains(문제.category)`(`isPersonalQuiz`).
- **직접 편집(폼)**: 커스텀 문제는 오류 신고 대신 ✏️ 편집 제공 → `EditQuizDialog`(유형·질문·보기4[정답 라디오]·정답·해설·힌트 칸별 폼). 저장은 `QuizViewModel.updateCustomQuestion()`이 **원 id 유지로 갱신** + 진행 중 목록 즉시 반영.
- **편집 잠금 기준**: 편집 화면엔 정답이 보이므로 **정답 확인 전(`quizState != ANSWER_CHECKED`)엔 🔒 잠금**(안내 토스트), 정답 확인 후에만 ✏️ 활성. (풀이 중 정답 미리보기 방지)

---

## ⚠️ 알려진 주의사항 및 제약

1. ~~**이미지 퀴즈 공유 제한**~~ ✅ **해결(2026-07-21)**: `.kquiz` 내보내기 시 크롭 이미지를 **Base64로 내장**(`imageBase64`/`imageExt`), 가져오기 시 디코딩해 `cropped_quizzes/imported_*.png`로 복원 후 `imageUrl` 세팅([QuizFileHandler.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/data/QuizFileHandler.kt) `decodeAndSaveImage`). 텍스트 붙여넣기 가져오기도 context를 넘겨 동일 동작. 구버전 파일(내장 없음)은 기존처럼 imageUrl 폴백.

2. **Gemini API 할당량**: `generateVisualQuizzesFromImages()`는 멀티모달 요청으로 토큰 소비가 큼. 무료 플랜에서는 할당량 초과 시 `429` 에러 발생.

3. **Bounding Box 정확도**: AI가 반환하는 좌표가 부정확할 경우 크롭 영역이 빗나갈 수 있음. 특히 그림이 복잡한 이미지에서 발생.

4. **카테고리 유형 구분**: 공식/클라우드 카테고리 이름이 하드코딩되어 있음. 새 카테고리를 원격에 추가할 때 `QuizScreen`의 `isDefault` 목록도 함께 업데이트 필요.

5. **이미지 선택은 권한 없이**: `READ_MEDIA_*` 권한을 쓰지 않음. 이미지 가져오기는 **Photo Picker**(`PickVisualMedia`)로만 처리하므로 갤러리 권한 요청 없음. 새 이미지 진입점을 추가할 때도 `GetContent("image/*")`가 아니라 Photo Picker를 사용할 것(정책 준수).

---

## 🚀 다음 구현 예정 과제

> **최우선(배포)**: 다음 스토어 업로드 전 **versionCode 3 / versionName 1.2** 상향 → 릴리즈 AAB 빌드/업로드. 실기기 최종 점검(검색·과거실적·조회기간 유지·오디오 복구·퀴즈 가져오기).

- [ ] **자매앱 동적 레지스트리 후속** (2026-07-29 구현 완료 → 남은 3가지)
  - [x] ~~**`kitwlsh/k-series-config` 공개 레포 생성 + `family.json`·`icons/` 업로드**~~ ✅ 완료(2026-07-29) — 라이브 확인됨(family.json + 아이콘 3종 HTTP 200). 편집 가이드 = [doc/family_config/README.md](family_config/README.md).
  - [x] ~~**KLotto645·K장부 이식**~~ ✅ 완료(2026-07-29) — 3개 앱 전부 동적 렌더. KLotto645는 이미지 라이브러리가 없어 `RemoteIconCache`(자체 디스크 캐시)로 처리, **의존성 추가 없음**. K장부는 **첫 출시본부터 동적**이라 전환용 추가 배포 없음. 앱별 대응표 = §8-10.
  - [ ] **vc6 / v1.6 상향 + AAB 빌드·업로드** — 동적 레지스트리는 이 배포부터 실사용(그 전 게시본 v1.5는 여전히 KLotto 하드코딩 카드·K장부 카드 없음). KLotto645도 다음 업데이트(vc12+) 필요.
- [ ] **버전 상향 + 스토어 업로드** (v1.1 이후 변경분 배포)
- [x] ~~(선택) **키워드 순서 변경**: `Set`→`List` 전환~~ ✅ 완료(2026-07-20, 위 "키워드 순서 보존 저장" 참조)
- [x] ~~(선택) **과거 실적 추세 종합 AI 코멘트** 버튼(다분기 흐름 1회 요약)~~ ✅ 완료(2026-07-20, 위 증시 메모 "추세 종합 AI 코멘트" 참조)
- [x] ~~**AI 스마트 관심종목 포트폴리오 분석** (README Phase 3)~~ ✅ **완료(2026-07-22)**:
  - `GeminiManager.summarizePortfolio(portfolioText)` — 다종목 실적을 1회 종합(전반 흐름·상대 우열·집중 리스크·면책).
  - `StockViewModel.generatePortfolioAnalysis(forceRefresh)` — `watchStockKeywords`(≤10) 각각 `searchCorpByName`→corpCode→`fetchFinancialHistory(maxPeriods=4)` 취합→AI 호출. 지수·해외·가상자산 등 DART 미대상 자동 skip.
  - 캐시: `StockRepository.load/savePortfolioAnalysis`(filesDir/portfolio_analysis.json). UI: `StockDashboardScreen.PortfolioAnalysisCard`(관심 종목 탭 상단, `MarkdownText` 재사용, 재분석 버튼).
- [x] ~~**빠른 독서**: 보관함 제목 편집(✅ 2026-07-20), 통계 상세 화면, 난이도 자동 추천~~ ✅ 완료(2026-07-21: `StatsModule` 통계 상세 + `ReadingTrainingViewModel.recommendedWpm` 난이도 추천→드릴 초기 속도 반영)
- [x] ~~**이미지 퀴즈 공유 개선**: 크롭 이미지를 Base64로 `.kquiz`에 내장~~ ✅ 완료(2026-07-21)
- [x] ~~**AI 마크다운 렌더링**: 브리핑/요약 결과에 Rich Text 뷰어~~ ✅ 완료(2026-07-24): AI 대화·맞춤분석·포트폴리오에 이어 증시 과거실적 **'추세 종합 코멘트'** 도 `MarkdownText` 적용(마지막 raw 텍스트 노출 제거, `StockDashboardScreen`).
- [x] ~~**AI 대화 히스토리 토큰 상한(§8-5)**~~ ✅ 완료(2026-07-24): `GeminiManager.startNewsChat`이 재구성 히스토리를 **최근 16메시지로 제한**(`MAX_CHAT_HISTORY_MESSAGES`)해 장기 대화 토큰 폭증·429 방지.

> ✅ **완료(2026-07-07)**: `QuizViewModel.startQuiz` 중복 제거를 정답 기준 → **질문 기준**으로 변경. 정답이 빈 문항(그림 매칭 퀴즈 등)이 조용히 사라지던 잠재 취약 해소(정답 중복 제거는 `QuizRepository.dedupeQuizzes`가 이미 담당).

> ⚠️ 개인용 외부기사 낭독·쉐도잉이 필요하면 공개앱 숨김토글 금지 → `personalDebug` 등 **별도 빌드**로(Google Play 숨김기능/심사회피 정책 리스크).

---

## 🔄 최근 커밋 이력 (최신순)

> 최신 상태는 항상 `git log --oneline -20` 으로 확인. **v1.5(versionCode 5) 2026-07-23 출시 완료(현재 게시본 = v1.5)**(이전 게시본 v1.4=vc4 07-21, v1.2=vc3 07-08). **다음 배포 = vc6/v1.6** — 자매앱 동적 레지스트리(2026-07-29)가 배포돼야 실사용된다.

**2026-08-03 세션 (v1.6 릴리즈·서명 키 정리·방침 3건 정비 — 전부 푸시됨)**
| 커밋 | 내용 |
|------|------|
| (이번) | docs: v1.6 업로드·검토중 반영 + 낡은 Netlify 방침 URL 폐기 |
| `17b0e6a` | docs(privacy): 음성 입력(STT)·자매앱 조회 조항 추가 + 방침 호스팅 표준(§9) |
| `2ef8303` | release: v1.6(vc6) AAB + 출시 노트 + 원격 응답 본문 상한 256KB 보강 |
| `f679b1e`·`96b8f9c` | chore(security): 키스토어를 저장소 밖(`_secrets`)으로 이전·추적 해제·`.gitignore` 보강 + 키 유지 결정 기록 |
| `KLotto645 a45ae9c` | docs(privacy): 방침 신규 작성(기존은 웹사이트용 템플릿이라 카메라·앱이름 누락) |
| `KJangbu bde79b6` | docs(privacy): 카메라 권한 문구를 실제 구현과 일치시킴 |
| `k-series-config 2591ea5` | feat: 방침 3건 + 모음 index GitHub Pages 호스팅 |
> **오늘 출시**: K장부 v1.0.0(vc1) 첫 출시·검토중 / KDailyUtil v1.6·검토중 / KLotto645 대기.
> **K장부 게시 확인 후** `family.json`의 `comingSoon: true → false` 필요(§8-9).

**2026-07-29 세션 (자매앱 동적 레지스트리 §8 구현·3개 앱 이식·실기기 검증 — 커밋·푸시 완료)**
| 커밋 | 내용 |
|------|------|
| (이번) | docs: 검증 결과·진단 방법(§8-12) 반영 + 로그 문구 3앱 통일 |
| `86329ad` | docs: 자매앱 동적 레지스트리 이식 완료 반영(3개 앱 + 호스팅 레포) |
| `1eb925b` | feat: 자매앱 동적 레지스트리 — 재배포 없이 자매앱 추가(§8 구현) |
| `8598f6c`·`da57a1e` | docs: 핸드오프 §7-1 KLotto645 라이브 v1.0.1 반영 / 양쪽 사본 링크 정합 |
| `KLotto645 de42102` | feat: 동적 레지스트리 이식(XML/View, `RemoteIconCache` 자체 구현 — 의존성 추가 없음) |
| `KJangbu f019c4b` | feat: 동적 레지스트리 이식(첫 출시본부터 동적 → 전환용 추가 배포 없음) |
| `k-series-config 2745669`·`7e7a7fd` | init: 레지스트리 정본 업로드 / fix: KLotto 아이콘 투명 배경 교체 |
> **⚠️ 배포돼야 효력 발생**: 게시본 v1.5 사용자에겐 여전히 KLotto 하드코딩 카드가 보인다(K장부 카드 없음). 다음 업로드 = vc6/v1.6.

**2026-07-22 세션 (AI 포트폴리오 분석 + v1.5 배포 빌드 — v1.5 AAB 빌드 완료·업로드 대기)**
| 커밋 | 내용 |
|------|------|
| (이번) | release: versionCode 5 / versionName 1.5 상향 + `kdailyutil-v1.5.aab` 빌드 + RELEASE_NOTES v1.5 |
| `a0f9318` | feat: 관심종목 포트폴리오 종합 AI 분석 — `GeminiManager.summarizePortfolio` + `StockViewModel.generatePortfolioAnalysis`(watchStockKeywords 취합·캐시) + `StockDashboardScreen.PortfolioAnalysisCard`(MarkdownText 재사용) |
> v1.5 = 게시본 v1.4 이후 누적분(뉴스 AI 대화창·음성·마크다운·핸즈프리 / 포트폴리오 분석 / 빠른독서 통계·난이도 / 이미지 퀴즈 Base64). **✅ 2026-07-23 출시(게시)됨 — 게시본=v1.5.**

**2026-07-21 세션 (뉴스 AI 대화창 + 후속 편의기능 4종 — 커밋됨·미배포, v1.5 대기분)**
| 커밋 | 내용 |
|------|------|
| `ad0e379` | feat: 빠른 독서 통계 상세(`StatsModule`) + 난이도 자동 추천(`recommendedWpm`→드릴 초기 속도) |
| `0a422c4` | feat: 이미지 퀴즈 공유 개선 — 그림을 Base64로 `.kquiz`에 내장/복원(다른 기기 표시) |
| `e9bb0de` | feat: AI 대화 핸즈프리 모드(자동 낭독→다시 듣기) |
| `371cd1e` | feat: AI 답변 마크다운 서식 렌더링(`MarkdownText`, 낭독 시 기호 제거) |
| `ad65fff`·`2666617` | fix: 브리핑 낭독 제목 중복 제거 / AI 탭 브리핑 FAB 숨김(입력바 겹침) |
| `79c5005`·`43888db` | feat/docs: 뉴스 AI 대화창(멀티턴+음성 STT/TTS+30일 보관) + 설계서 |
> ✅ 이 세션 변경분은 **v1.5(vc5)로 2026-07-23 출시됨**. 상세 = [doc/FEATURE_AI_NEWS_CHAT.md](FEATURE_AI_NEWS_CHAT.md).

**2026-07-20 세션 (키워드 순서변경·과거실적 추세 AI·보관함 제목편집·자매앱 상호연결·v1.4 배포준비 — origin/main 푸시 완료)**
| 커밋 | 내용 |
|------|------|
| `ba1adc1` | docs: 상호연결 표준에 화면/섹션 명명 규칙(§7-3) + 개칭 반영(§3) |
| `d595c9f` | refactor: 갤러리 '브랜드 & 자매앱' 개칭 + 자매앱 구획(구분선·소제목) 분리 |
| `28e2650`·`9312078` | docs: 상호연결 표준 §3 완료·§6 확정 + 양쪽 저장소 동기화 규칙 |
| `9fc3c37` | feat: '브랜드 & 자매앱' 갤러리에 KLotto645 카드(openAppOrStore·설치배지) |
| `e8c89ba` | docs: 자매앱 상호연결 표준 정리 — 핸드오프 doc/ 이동·README 인덱싱 + 아이콘 교환(ic_klotto645) |
| `3fdddcc` | feat: 빠른 독서 보관함 지문 제목 편집(renamePassage) |
| `1299ba4` | feat: 과거 실적 '추세 종합 AI 코멘트'(summarizeFinancialTrend) |
| `aafc7fa` | feat: 관심 키워드 순서변경(Set→List·ReorderableChipRow·마이그레이션) |
| `657b577` | docs: 2026-07-20 세션 문서 동기화(아이콘 통일 반영 + 기능 문서화) |
> ⚠️ 이 세션 변경분도 v1.2 AAB에 미포함 → 스토어 업로드 전 AAB 재빌드 필요.

**2026-07-15~16 세션 (K-시리즈 아이콘/스플래시/워터마크 패밀리 통일, 모두 푸시)**
| 커밋 | 내용 |
|------|------|
| `ac6d41b`·`8e4fb3c`·`a1a44bc` | docs: README 문서 인덱스 완성 + '새 문서는 인덱스에 등록' 규칙 명시(K_SERIES_ICON_RECIPE·RELEASE_NOTES 등록) |
| `1744b6d` | feat: K-시리즈 아이콘/스플래시/워터마크 패밀리 통일 + 표준 문서화(런처 전 밀도·플레이스토어·워터마크·설정 갤러리) |
| `a374358` | chore: 미사용 `HexagonShape` 제거 + 아이콘 레시피 §7~8 정리 |
| `2a8ee18`~`d329f0a` | fix: 스플래시를 자체 앱 아이콘(`ic_app_logo_full`)으로 교체 — 육각형 상하 잘림·흰 배경 잔여·하단 드롭섀도 해결 |
| `8667616`~`402c811` | docs: K-시리즈 아이콘/스플래시 제작 레시피 + KLotto645 패밀리 아이콘 아카이브 |
> ⚠️ **이 세션 변경분은 2026-07-08 빌드된 `kdailyutil-v1.2.aab`에 미포함** → 스토어 업로드 전 AAB 재빌드 필요(위 "지금 상태" 참조).

**2026-07-08 세션 (v1.2 AAB·릴리즈 노트)**
| 커밋 | 내용 |
|------|------|
| `c77e709` | release: v1.2 업로드용 AAB(`kdailyutil-v1.2.aab`) + 릴리즈 노트(`RELEASE_NOTES.md`) 추가, 가이드 배포 상태 최신화 |
| `2bd034c` | docs: 2026-07-07 세션 커밋 이력/현재 상태 최신화 + AAB 재빌드 안내 |

**2026-07-07 세션 (배움터 퀴즈 안정화·뒤로가기·증시 개선·v1.2 상향, 모두 푸시)**
| 커밋 | 내용 |
|------|------|
| `7e9e08a` | fix: 숨김 공시를 객체로 저장 — 조회기간 밖 옛 숨김 항목도 '숨김 보기'에 표시(레거시 id 호환·크래시 방어) |
| `a3b0a9b` | feat: 증시 키워드 2종(뉴스탭/대시보드) 구분 + 설정에서 대시보드 관심종목 관리 |
| `9ead9d0` | feat: 증시 AI 실적공시/과거실적/실적뉴스 개선(동일회사 1건·숨김구분·회계기준월·안내) |
| `391f93e` | chore: versionCode 3 / versionName 1.2 상향 + 릴리즈 AAB 빌드 |
| `4a9f58d` | fix: AI 실적 공시 탭 헤더(검색·조회기간·숨김보기)를 목록과 함께 스크롤(하나의 LazyColumn) |
| `a8dce76` | refactor: startQuiz 중복 제거를 정답 기준→질문 기준(정답 빈 문항 사라짐 방지) |
| `7aa9c39` | feat: 퀴즈·빠른독서 내부 화면 시스템 뒤로가기 처리(뉴스탭 튕김 방지) |
| `6849ede` | fix: 퀴즈 데이터 파싱 내구성(불량 1건이 카테고리 전체를 안 깨게)·fromRaw·sync 병합·빈목록 가드 |
| `60c85c5` | docs: 2026-07-07 세션 반영 |
| `korean_quiz_data d325584` | fix: 상식백과 안 열리던 type 오타(30036) 수정 + `validate.py` 검증 CI 게이트 |

> ⚠️ **AAB 재빌드 필요**: `391f93e`에서 AAB를 빌드했지만 이후 증시 개선(`9ead9d0`·`a3b0a9b`·`7e9e08a`)이 추가됨. **스토어 업로드 전 `./gradlew.bat :app:bundleRelease` 재실행**(versionCode는 이미 3이라 그대로).

**2026-07-06 세션 (증시·탭·과거실적, 모두 푸시)**
| 커밋 | 내용 |
|------|------|
| `9c5d71f` | docs: 과거 실적 조회·회사 검색/AI 공시 개선 문서화 |
| `6f4c78c` | fix: 회사 검색 시 앱 다운 — 디바운스+다운로드 단일화(Mutex)+XML 스트리밍 파싱 |
| `71f1daa` | feat: 과거 실적 조회 + 회사 이름 검색 (AI 실적 공시 탭) |
| `04e7aa4` | fix: 조회기간 변경 시 AI 분석 초기화 버그(캐시 복원) + 즐겨찾기 카드 가독성 |
| `10f6492` | feat: 증시 '실적보고 기한' → '실적 뉴스·전망' 개편 |
| `decfccd` | feat: 뉴스/증시/AI 탭 '＋ 빠른 추가' + 설정 DART 키 발급 도움말 |

**2026-07-03 세션 (퀴즈 도구)**
| 커밋 | 내용 |
|------|------|
| `810817e`~`5cc3229` | AI로 개인 퀴즈 만들기 가이드(주제만 입력·저장/공유), JSON 텍스트 붙여넣기 가져오기, 커스텀 편집(정답 확인 후 잠금 해제), 오류신고 게이팅 |

**2026-06-26~29 세션 (오디오·뉴스·퀴즈 데이터)**
| 커밋 | 내용 |
|------|------|
| `74ffffa` | feat: 빠른 독서 랜덤 지문 확장(3→19) |
| `333ce7b`/`60befa6` | feat: 블루투스 미디어버튼 제어 / 인터럽트 후 오디오 자동 재개 |
| `aa624c4` | feat: 앱 퀴즈 중복 방지 3중화(생성/제출/표시) |
| `95b1c12` | fix: 뉴스 저작권 보수적 정책(본문 비스크랩·낭독 스니펫·쉐도잉 배움터 이전) |
| `172dd89` | fix: v1.1 권한 정리 후 옛 공용폴더 녹음 접근 불가 → SAF 폴더 복구 |
| `8f51481` | **fix: v1.1 릴리즈 (스토어 배포본 기준선, versionCode 2)** |

> 별도 저장소 `korean_quiz_data`도 정답·신조어 중복 정리 완료(푸시): `840e065`, `45426e2`.

---

## 📈 증시 대시보드 기능 메모 (2026-06-24 기준)

증시 탭은 `StockDashboardScreen` + `StockViewModel` + `StockRepository`로 구성. 3개 서브탭: `시세 및 차트(0) / AI 실적 공시(1) / 실적 뉴스·전망(2)`.

### 데이터 소스
- **시세/차트**: Yahoo Finance 차트 API(`query1.finance.yahoo.com/v8/finance/chart`, 인증 불필요). volume 포함 파싱.
- **실적 공시**: Open DART `list.json` **`pblntf_ty=A`(정기공시)** → 항목(=특정 보고서 1건) 클릭 시 `fnlttSinglAcntAll.json`로 재무 조회 후 AI 요약. 보고서명 `(YYYY.MM)`으로 reprt_code 판별(`.03→11013 .06→11012 .09→11014 .12→11011`), **CFS→OFS 폴백**. 단건 요약은 재무의 현재값+전년동기(previous)를 함께 써서 흑자전환/서프라이즈/쇼크 판정. 카드=회사가 아니라 '보고서 1건'이며, `(YYYY.MM)`은 제출일이 아니라 회계기간말(예: FY2025 사업보고서=2025.12; 기재정정은 이후에도 제출됨).
  - **조회기간(1/3/7/30일) 변경 시** DART 재조회하되, 캐시된 AI 결과를 rcept_no로 복원해 초기화되지 않음(재분석은 새로고침/재분석만). 즐겨찾기(★) 카드는 불투명 골드 강조.
  - **레이아웃(2026-07-07)**: `DisclosuresTab`은 검색창·조회기간·숨김보기 헤더와 공시 목록을 **하나의 `LazyColumn`으로 통합**(헤더는 `item{}`). 예전엔 헤더가 고정이고 목록만 남은 좁은 영역에서 내부 스크롤돼 2건만 보였음 → 이제 위로 스크롤하면 헤더가 밀려나며 목록이 화면을 꽉 채움. 회사 검색결과 드롭다운은 `Card(heightIn max 220)` 안의 중첩 `LazyColumn`이라 경계가 있어 안전.
  - **같은 회사 1건만(2026-07-07)**: 기재정정 등으로 같은 회사의 과거연도 보고서가 여러 건 잡혀 중복돼 보이던 문제 → `loadDisclosures`에서 `corpKey(corp_code‖corp_name)`로 그룹해 **가장 최근 접수일(rcept_dt) 1건만** 노출. 즐겨찾기(★)는 특정 보고서 고정이라 예외 유지, **숨김 보기 모드에선 축약 미적용**(숨긴 항목 찾아 해제 가능하도록).
  - **숨김 항목 표시(2026-07-07)**: 숨김 보기 모드에서 숨긴 카드는 **흐리게 + "숨김" 뱃지**로 구분(`DisclosureCard(isHidden)`).
  - **숨김을 객체로 영속화(2026-07-07)**: 예전엔 `hidden_disclosures.json`에 `rcept_no`(id)만 저장해, 조회기간 밖의 옛 숨김 항목은 숨김 보기에도 안 나타나고 "(2)" 숫자만 남는 문제가 있었음. → 즐겨찾기처럼 **전체 객체 저장**(`saveHiddenObjects`/`loadHiddenObjects`, 문자열 원소는 레거시 호환 파싱)하고, `loadDisclosures`가 **숨김 보기 시 조회기간 밖 숨김 항목을 강제 포함**(레거시 id-only는 AI캐시 `cachedById`에서 회사명 보강, 없으면 "(이전에 숨긴 공시)" 표기로 해제만 가능). `rcept_dt` 빈 값 대비 `DisclosureCard` 날짜 포맷 방어(`length>=8`).
  - **과거실적 다이얼로그(2026-07-07)**: 카드에 **회계 기준월**(`periodEndLabel`: 사업=YYYY.12/3분기=.09/반기=.06/1분기=.03) 표기, 캡션에 **연결=연결재무제표/개별=별도재무제표** 설명 추가('연결'은 라벨일 뿐 클릭 대상 아님).
- **과거 실적 조회 + 회사 검색 (2026-07-06)**: 공시 카드의 **📊 과거실적** 또는 상단 **회사명 검색**(리스트에 없는 회사도) → `StockRepository.fetchFinancialHistory(corpCode)`가 최근 8개 정기보고서(연도×보고서코드)를 조회해 **매출·영업이익·순이익 + 전년동기%** 목록으로 표시(참고용, 분석 X). 분기·반기는 **누적(YTD)**.
  - **추세 종합 AI 코멘트(2026-07-20)**: 과거 실적 다이얼로그 하단 **🤖 추세 종합 AI 코멘트** 버튼 → `StockViewModel.generateFinancialTrendComment()`가 조회된 보고서들을 텍스트로 이어 `GeminiManager.summarizeFinancialTrend()`에 1회 전달, 매출/수익성 추세를 마크다운 요약(`financialTrendComment` state)으로 표시. Gemini 키 필요, 🔄 다시 분석 지원, 다이얼로그 닫으면(`clearFinancialHistory`) 초기화. (마크다운 렌더러는 아직 없어 원문 텍스트로 노출) 검색은 `ensureCorpCodes()`가 DART `corpCode.xml`(zip)을 1회 다운로드→상장사만 캐시(`corp_codes.json`) 후 이름 검색. ⚠️ 검색은 **디바운스(350ms·2글자)+Mutex 단일화+스트리밍 파싱**(과거 입력마다 대용량 재다운로드→OOM 크래시 있었음).
- **실적 뉴스·전망 (2026-07-06 개편)**: 국내는 정확한 실적 발표 예정일·컨센서스를 무료로 제공하지 않아, '예정일'을 맞추는 대신 **관심 종목의 '실적' 관련 뉴스 + AI 사전 전망**을 제공하도록 개편(이전 '실적 예정 일정' 탭 대체). 종목 카드 탭=AI 사전 전망(`generatePreReport`), **📰 실적 뉴스 보기**=`StockViewModel.loadEarningsNews()`→`NewsRepository.getNewsByKeyword("{종목} 실적")`→다이얼로그, 헤드라인 탭 시 원문을 외부 브라우저로 오픈. 날짜 배지는 `nextStatutoryDeadline()`의 **정기보고서 법정 제출기한(분기말+45일 등)** 을 '기한' 참고로만 표기(실제 발표일 아님).
  - **목록 출처**: `fetchExpectedEarnings(watchNames)`가 `watchStockKeywordsFlow`(증시 대시보드 관심종목)에서 **`CORP_CODE_MAP`에 매핑된 한국 상장사만** 필터. 매핑 결과가 없으면 대표 6종목으로 폴백. 그래서 관심종목에 삼성전자·SK하이닉스만 매핑되면 그 둘만 보임(지수·해외·가상자산은 실적 공시 없어 제외). 탭 상단 안내 문구로 출처·관리 위치 고지(2026-07-07).

### ⚠️ 증시 키워드 = 저장소 2개 (혼동 주의, 2026-07-07 명확화)
DataStore에 **독립된 두 키워드 저장소**가 있고 UI에서 각각 관리한다. 서로 값이 공유되지 않는다.
| 저장소 | 용도 | Flow / 갱신 | 편집 위치 | 기본값 |
|--------|------|-------------|-----------|--------|
| `STOCK_KEYWORDS` | 📰 **뉴스탭 > 증시 서브탭** 뉴스 필터 | `stockKeywordsFlow` / `updateStockKeywords` (BriefingViewModel) | 설정 > 증시 ‘📰 뉴스탭 증시 키워드’ · 뉴스탭 증시 서브탭 ＋ | 나스닥·코스피·테슬라·비트코인 |
| `WATCH_STOCK_KEYWORDS` | 📈 **증시 대시보드**(시세·차트 + 실적 뉴스·전망) 종목 | `watchStockKeywordsFlow` / `updateWatchStockKeywords` (Stock/BriefingViewModel) | 증시탭 > 시세 및 차트 관심종목(＋·편집) · **설정 > 증시 ‘📈 증시 대시보드 관심종목’(2026-07-07 신설)** | 나스닥·코스피·테슬라·비트코인 |
> 이전엔 대시보드 관심종목을 설정에서 못 고쳐 "설정>증시에 넣어도 실적뉴스에 안 나온다"는 혼동이 있었음. 설정 > 증시 탭을 **두 섹션(뉴스/대시보드)** 으로 나누고 각 설명을 달아 해소. `BriefingViewModel`에 `watchStockKeywords`/`updateWatchStockKeywords` 추가(같은 DataStore 키라 증시 대시보드 `StockViewModel` collector가 실시간 반영).

### 로컬 캐시 파일 (`filesDir`)
- `stock_prices_cache.json` — 시세 인메모리+파일 캐시
- `earnings_disclosures_cache.json` — 공시 AI 요약 캐시(rcept_no 기준, 90일 TTL)
- `expected_reports_cache.json` — 사전 전망 리포트 캐시(corp_name 기준)
- `financial_history_cache.json` — 과거 실적 조회 캐시(corp_code 기준, 3일 TTL)
- `corp_codes.json` — DART 전체 상장사 고유번호 캐시(회사 이름 검색용, corpCode.xml에서 1회 생성)
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
    - **보관함 제목 편집(2026-07-20)**: 보관함 카드의 ✏️ → 제목 편집 다이얼로그 → `ReadingTrainingViewModel.renamePassage(id, title)` → `ReadingTrainingRepository.renamePassage()`(본문·이미지·생성시각 유지, 최대 40자). 카드의 ✕는 삭제.
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
