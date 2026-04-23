# 📱 KDailyUtil

**데일리 유틸리티 안드로이드 애플리케이션**  
일상의 편의를 위해 뉴스 브리핑, 운전 중 말하기 연습, 오디오 캡처 등 실용적인 기능을 제공합니다.

---

## ✨ 주요 기능

### 📰 뉴스 키워드 브리핑 (Morning Briefing)
- 사용자가 설정한 관심사/키워드별 국내 최신 뉴스 수집.
- **AI 요약**: Gemini 1.5 Flash 엔진을 통해 기사를 자연스러운 대화체로 요약.
- **TTS 낭독**: 수집된 뉴스 요약을 음성으로 들려주어 바쁜 아침 시간에 시각적 자유 제공.

### 🎙 오디오 시스템 기능 및 최적화 (Updated)
*   **하이브리드 녹음 모드**: 
    *   **내부 소리**: 시스템 미디어 소리를 고음질로 캡처 (MediaProjection).
    *   **외부 마이크**: 사용자 목소리를 직접 녹음 (AudioSource.MIC).
*   **파일 접근성 및 관리**:
    *   **공용 폴더 저장**: `Download/KDailyUtil` 폴더에 저장되어 폰의 '내 파일' 앱에서 직접 복사/공유/삭제 가능.
    *   **휴지통(Trash) 시스템**: 실수로 삭제한 파일을 복구할 수 있는 휴지통 기능 제공.
    *   **숨김 파일 관리**: 리스트에서 보이고 싶지 않은 파일을 별도로 숨기고 관리.
*   **사용자 중심 UI/UX**:
    *   **리스트 순서 변경**: 드래그 앤 드롭 방식의 위/아래 이동 및 순서 영구 저장.
    *   **화면 꺼짐 및 백그라운드 재생 안정화**: `MediaSession` 및 `WakeLock` 적용으로 어떠한 환경에서도 끊김 없는 재생 보장.
    *   **다중 파일 가져오기**: 폰에 있는 여러 오디오 파일을 한 번에 리스트로 추가 가능.
    *   **편집 잠금(Edit Lock)**: 데이터 유실 방지를 위한 보안 장치.

### 🚗 운전 중 말하기 연습 (Driving Shadowing)
- **사설/칼럼 중심**: 학습 가치가 높은 오피니언 리더들의 글을 기반으로 학습.
- **스마트 분할 및 대기**: 문장을 자동으로 분리하고, 따라 읽을 수 있도록 최적의 대기 시간을 산출.
- **자동 녹음 및 모니터링**: 연습 음성을 자동 녹음하고 즉시 다시 들으며 발음 교정.
- **운전 안전 UI**: 고대비 블랙 테마와 대형 버튼으로 조작 편리성 극대화.

### 🎙️ 오디오 캡처 및 세팅
- 미디어 음성 캡처 및 백그라운드 재생 지원.
- 브리핑 시간 및 RSS 키워드 개인화 설정.

---

## 🛠 기술 스택

- **UI**: Jetpack Compose (Modern & Reactive UI)
- **Networking**: Jsoup (RSS Scraping)
- **AI**: Google Generative AI SDK (Gemini 1.5 Flash)
- **Concurrency**: Kotlin Coroutines & Flow
- **Data Persistence**: Preferences DataStore
- **Audio**: TextToSpeech (TTS), MediaRecorder, MediaPlayer

---

## 📂 프로젝트 문서 (Documentation)

- [📄 전체 구현 계획 및 설계](doc/implementation_plan.md)
- [✅ 단계별 작업 현황](doc/task.md)

---

## 🚀 시작하기

1. **저장소 클론**
   ```bash
   git clone https://github.com/your-repo/KDailyUtil.git
   ```

2. **환경 설정 (Gemini API)**
   - [Google AI Studio](https://aistudio.google.com/app/apikey)에서 API 키를 발급받습니다.
   - 앱 내 **설정(Morning Settings)** 화면에서 API 키를 입력하면 AI 요약 기능이 활성화됩니다.
   - API 키가 없는 경우 제목만 나열하는 **데모 모드**가 작동합니다.

3. **빌드 및 실행**
   - Android Studio (Ladybug 이상)에서 프로젝트 오픈.
   - `./gradlew assembleDebug` 코드로 빌드 후 실행.

---

> [!NOTE]
> **커뮤니티 및 기여**: 모든 개발 대화와 문서화는 한국어로 진행됨을 원칙으로 합니다.
