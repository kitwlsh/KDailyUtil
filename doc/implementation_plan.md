# KDailyUtil 초기 기능 구현 계획

`KDailyUtil` 어플리케이션의 핵심 기능들을 단계별로 구현하여 실사구시형 데일리 유틸리티 앱을 완성하는 것을 목표로 합니다.

## User Review Required

> [!IMPORTANT]
> **AI 요약 연동 방식**: 현재 README에는 Jsoup을 이용한 스크래핑이 언급되어 있으나, AI 요약을 위한 외부 API(OpenAI, Gemini 등) 연동 여부나 방식에 대한 결정이 필요합니다. 초기에는 간단한 규칙 기반 요약 또는 Mock 데이터를 사용할 예정입니다.

> [!WARNING]
> **오디오 권한**: 오디오 캡처 및 녹음 기능을 위해 `RECORD_AUDIO` 및 `FOREGROUND_SERVICE` 권한 설정이 필수적입니다.

## Proposed Changes

### 1. 뉴스 키워드 브리핑 (News Briefing) 구현
사용자가 설정한 키워드에 맞는 최신 뉴스를 가져와 요약하고 음성으로 읽어주는 기능을 구현합니다.

#### [MODIFY] [NewsBriefingScreen.kt](file:///d:/10.Develop/55.Antigravity/Android/KDailUtil/KDailyUtil/app/src/main/java/com/kitwlshcom/kdailyutil/ui/screens/NewsBriefingScreen.kt)
- 키워드 입력 필드 추가
- 뉴스 리스트 표시 (LazyColumn)
- 브리핑 시작 버튼 및 재생 UI

#### [NEW] [NewsRepository.kt](file:///d:/10.Develop/55.Antigravity/Android/KDailUtil/KDailyUtil/app/src/main/java/com/kitwlshcom/kdailyutil/data/repository/NewsRepository.kt)
- Jsoup을 활용한 뉴스 데이터 크롤링 logic

#### [NEW] [TtsManager.kt](file:///d:/10.Develop/55.Antigravity/Android/KDailUtil/KDailyUtil/app/src/main/java/com/kitwlshcom/kdailyutil/audio/TtsManager.kt)
- Google TTS 엔진 초기화 및 텍스트 읽기 기능

---

### 2. 운전 중 말하기 연습 (Driving Shadowing) 구현
운전 중 안전하게 쉐도잉 연습을 할 수 있도록 큰 UI와 음성 피드백을 제공합니다.

#### [MODIFY] [DrivingShadowingScreen.kt](file:///d:/10.Develop/55.Antigravity/Android/KDailUtil/KDailyUtil/app/src/main/java/com/kitwlshcom/kdailyutil/ui/screens/DrivingShadowingScreen.kt)
- 큰 글씨의 문장 표시 영역
- 원터치 녹음/재생 제어 버튼

---

### 3. 공통 인프라 설정
#### [MODIFY] [AndroidManifest.xml](file:///d:/10.Develop/55.Antigravity/Android/KDailUtil/KDailyUtil/app/src/main/AndroidManifest.xml)
- 필요한 권한(Internet, Audio, Foreground Service) 추가

## Open Questions
- **뉴스 소스**: 특정 뉴스 사이트(예: 네이버 뉴스, 구글 뉴스)를 타겟으로 할지, 아니면 일반적인 검색 결과를 사용할지 결정이 필요합니다.
- **데이터 저장**: 키워드 및 설정 값을 `DataStore` 또는 `Room` 중 어디에 저장할지 선호하시는 방식이 있나요?

## Verification Plan

### Automated Tests
- `NewsRepository` 단위 테스트: Jsoup 크롤링 결과 확인.
- `TtsManager` 동작 테스트: 특정 문구 음성 출력 확인.

### Manual Verification
- 에뮬레이터 또는 실기기에서 뉴스 리스트 로딩 및 음성 출력 확인.
- 권한 부여 프로세스 정상 동작 확인.
