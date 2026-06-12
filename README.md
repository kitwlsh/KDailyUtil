# 📱 KDailyUtil

데일리 라이프스타일을 더 스마트하고 고급스럽게 만들어주는 안드로이드 통합 유틸리티 애플리케이션입니다.  
일상의 편의를 위해 AI 뉴스 브리핑, 운전 중 말하기 연습, 오디오 캡처, AI 퀴즈 크리에이터 등 다양한 스마트 도구들을 제공합니다.

---

## 🗂️ 프로젝트 문서 인덱스 (Documentation Index)
프로젝트의 아키텍처, 브랜딩, 배포 및 이슈 리포트 문서는 모두 [doc/](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/) 폴더 내에 체계적으로 격리 정리되어 있습니다. 아래의 링크를 통해 필요한 상세 문서를 즉시 열람하실 수 있습니다.

| 문서 구분 | 상세 문서 링크 | 설명 |
| :--- | :--- | :--- |
| **개발자 가이드** | [DEVELOPER_GUIDE.md](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/DEVELOPER_GUIDE.md) | 프로젝트 아키텍처, 파일 구조, 개발 시 주의사항 총정리 |
| **구글 플레이 가이드** | [GOOGLE_PLAY_RELEASE_GUIDE.md](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/GOOGLE_PLAY_RELEASE_GUIDE.md) | 구글 플레이 콘솔 업로드 및 릴리즈 빌드 전체 단계 안내 |
| **콘솔 기입 정보 엑셀** | [google_play_release_info.xlsx](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/google_play_release_info.xlsx) | 구글 플레이에 제출한 27개 질문과 상세 텍스트 기록 보관함 |
| **디자인 & 브랜딩** | [BRANDING_GUIDE.md](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/BRANDING_GUIDE.md) | 로고 아이콘 규격, 색상 파레트 및 다크 테마 가이드라인 |
| **개인정보처리방침** | [privacy-kdailyutil.html](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/privacy-kdailyutil.html) | 구글 스토어 마이크 및 오디오 권한 대응 개인정보방침 HTML |
| **뉴스 우회 보고서** | [Google_News_Redirect_Issue_Report.md](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/Google_News_Redirect_Issue_Report.md) | 구글 뉴스 수집 시 발생하는 리디렉션 이슈 분석 및 해결책 |
| **오디오 개선 보고서** | [20260422_Audio_System_Enhancement_Report.md](file:///d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil/doc/20260422_Audio_System_Enhancement_Report.md) | 미니 플레이어 및 오디오 수집 성능 최적화 개선 보고서 |

---

## ✨ 핵심 기능 요약

### 1. 📰 뉴스 키워드 브리핑 (Morning Briefing)
- 관심 종목/키워드 최신 뉴스 수집 및 최상단 자동 스크롤 리셋.
- **AI 요약**: Gemini 1.5 Flash 엔진을 통해 자연스러운 대화체 요약 제공.
- **TTS 낭독**: 수집된 뉴스 요약을 음성으로 들려주어 출근 시간 시각적 자유 제공.

### 2. 📈 관심 증시 퀵 브라우징 (Morning Stocks)
- 코스피, 나스닥, 가상화폐 등 관심 키워드를 입력 칩으로 간편하게 관리.
- 증시 탭 선택 시 관심 종목 최신 뉴스 퀵 서칭 지원.

### 3. 🔊 오디오 캡처 및 관리 (Audio Capture)
- **통합형 플레이어**: 백그라운드 구동이 가능한 지속형 미니 플레이어 및 바텀 시트 플레이어 제공.
- **안전한 저장소**: Scoped Storage 완벽 대응 및 이전 데이터 자동 마이그레이션 적용.
- **블루투스 제어**: 블루투스 이어폰 연결 해제 시 자동 재생 일시정지 (`NoisyAudioReceiver`).

### 4. 🚗 뉴스 쉐도잉 (News Shadowing)
- 칼럼/오피니언 본문을 쉐도잉할 수 있는 반복 학습 루프 및 운전 안전 전용 UI 제공.

### 5. 🧠 AI 퀴즈 창작소 (KuizGenius)
- **시각 매칭형 퀴즈**: AI Bounding Box 디텍션을 통해 이미지 영역을 자동 크롭하여 문제 출제.
- **크리에이터 스위트**: 사진 촬영 스캔 및 웹 크롤러 스캔을 통해 저작권 우회형 독창적 퀴즈 창작.
- **누적 성취도**: 도전 통계 및 정답률에 연동된 프리미엄 골드 배지 엔진 탑재.

---

## 🛠 기술 스택
- **Image Loading**: Coil 2.6.0 (`coil-compose`) — 로컬 파일 경로 및 URL 기반 비동기 이미지 렌더링
- **Networking**: Jsoup (RSS Scraping & Web Crawling)
- **AI**: Google Generative AI SDK (Gemini 2.5 Flash) — 텍스트/이미지 분석, Bounding Box 감지, 의미론적 채점
- **Concurrency**: Kotlin Coroutines & Flow
- **Data Persistence**: Preferences DataStore, Internal File Storage (JSON)
- **Audio**: TextToSpeech (TTS), MediaRecorder, MediaPlayer

---

## 📂 프로젝트 문서 (Documentation)

- [🛠 개발자 컨텍스트 가이드 (신규 세션 필독)](DEVELOPER_GUIDE.md) — 아키텍처, 파일 구조, 퀴즈 파이프라인, 주의사항 총정리
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

## 🛠 최근 업데이트 및 작업 현황 (2026-06-10)

### 🖼️ AI 이미지 크롭 기반 시각(그림 매칭) 퀴즈 완성 (2026.06)
- **Bounding Box 기반 자동 크롭 파이프라인**:
  - Gemini AI가 이미지에서 개별 그림 요소의 2D 좌표(`[ymin, xmin, ymax, xmax]`, 0~1000 비율)를 정밀 감지.
  - `cropBitmapFromBoundingBox()`: 비율 좌표 → 픽셀 변환 + 5% 여백 패딩 적용 후 Bitmap 자동 크롭.
  - `saveBitmapToInternalStorage()`: 크롭 이미지를 `filesDir/cropped_quizzes/` 에 PNG로 영속 저장.
  - 카테고리 삭제 시 연결된 크롭 이미지 파일도 함께 자동 삭제 (저장소 누수 방지).
- **Coil 라이브러리 통합**:
  - `coil-compose 2.6.0` 추가 및 `AsyncImage`로 로컬 절대 경로 이미지 비동기 렌더링.
  - 퀴즈 풀기(`QuizPlayScreen`) 문제 카드와 퀴즈 생성 미리보기 다이얼로그 양쪽 모두에서 이미지 표시.
- **퀴즈 카테고리 권한 분리 UI 버그 수정**:
  - 원격 서버에서 동기화된 퀴즈(`맞춤법 및 띄어쓰기`, `고난이도 고유어` 등)가 커스텀 퀴즈로 잘못 분류되어 별표(`⭐`)와 삭제 버튼이 노출되던 크리티컬 버그 수정.
  - `isDefault` 판별 로직을 공식 내장/원격 동기화 카테고리를 모두 포함하도록 확장.
- **AI 주관식 의미론적 채점 시스템 (2단계 채점)**:
  - 1단계: 로컬 휴리스틱(공백/특수문자 무시) 정확 매칭.
  - 2단계: Gemini AI `verifySubjectiveAnswer()` 메서드로 문맥적 동의어 판정 및 부분 정답 처리.
  - 채점 중 로딩 상태 시각화 및 중복 클릭 방지 처리.
- **효과음 리소스 시스템 고도화**:
  - 기존 레트로 풍 `.wav` 파일 → 고품질 `.mp3` 효과음(`quiz_correct`, `quiz_wrong`, `quiz_finish`)으로 교체.
  - 리소스 컴파일러 충돌 방지를 위한 백업 파일 폴더 분리 구조 적용.

### 🔊 오디오 저작권 경고 및 설정란 브랜드 아이덴티티/법적고지 고도화 (2026.06)
- **오디오 저작권 팝업 고지 연동**:
  - 사용자가 마이크 또는 시스템 오디오 녹음 시작 시 저작권법 제30조(사적이용을 위한 복제) 및 통신비밀보호법에 의거한 경고 다이얼로그 출력.
  - "다시 표시하지 않기" 선택 시 Preferences DataStore에 영속 저장하여 다회 캡처 시의 UX를 저해하지 않도록 보완.
- **설정란 앱 정보 및 개발자 문의 추가**:
  - 설정 화면 하단에 **앱 정보 및 라이선스** 그룹 추가 및 프리미엄 골드 아웃라인 카드 레이아웃 적용.
  - 앱 버전명 동적 렌더링, 제작자 및 이메일 문의(`kitwlsh@gmail.com`) 연동.
  - **Gemini AI 기술 활용 고지** 명시로 구글 플레이 스토어 AIGC 심사 기준 충족.
- **브랜드 로고 갤러리 및 전체 화면 뷰어**:
  - 3D 육각 엠블럼(`ic_k_logo_3d`) 및 대표 통합 로고(`ic_app_logo_full`) 리소스를 시각적 카드 리스트로 제공.
  - 각 카드 터치 시 투명 다크 배경 및 전체 가득 찬 형태의 이미지 대형 뷰어 다이얼로그 연동.
- **기능별 4대 법적 고지 및 면책조항 탑재**:
  - 뉴스 브리핑(AI 요약 한계), 증시(시세 지연), 오디오 캡처(사적복제 제한/도청 처벌), 퀴즈 창작(패키지 공유 사용자 책임 귀속) 등 4대 영역 법적 면책 고지 다이얼로그 추가.

---

## 📌 현재 상태 및 다음 과제

- **현재 상태**: 
    - [x] 사이드 탭 기반 통합 오디오 UI 및 전역 플레이어 상태 관리 구현 완료.
    - [x] 안드로이드 최신 보안 정책 대응 파일 시스템 마이그레이션 완료.
    - [x] AI 다중 명령어 등록 및 탭 연동 완료.
    - [x] AI 분석 스마트 캐싱 및 생성 시간 표시 완료.
    - [x] 증시 고정 탭 추가, 관심 종목 설정 및 상단 서브 탭 연동 완료.
    - [x] 고정 탭 [전체, 증시, AI] 전용 프리미엄 골드 테마 적용 완료.
    - [x] 예약 브리핑 알림 및 경과 시간 캐시 지능적 갱신 완료.
    - [x] 자동 새로고침 주기 및 노출 기사 개수 설정 영속 연동 완료.
    - [x] 본문 및 드라이빙 연습창 워터마크 투과 디자인 통합 완료.
    - [x] 스플래시 화면 텍스트 잘림 해결 및 2단계 반사광 백업 완료.
    - [x] 스플래시 화면 이중 테마 시스템 및 신규 유성 스파이럴 궤도 개발 완료.
    - [x] **KuizGenius AI 퀴즈 크리에이터 스위트** (스캔/크롤링/자동완성) 구축 완료.
    - [x] **퀴즈 도전 이력(정답률) 및 누적 성취도 골드 배지 엔진** 장착 완료.
    - [x] **출제 프로필 공유, 퀴즈 패키지 내보내기/내려받기 파일 파이프라인** 통합 완료.
    - [x] **크리에이터 화면 높이 최적화 및 탭 내부 스크롤 아웃 UX Refactoring** 완료.
    - [x] **🖼️ AI Bounding Box 이미지 크롭 기반 그림/시각 매칭형 퀴즈 완성** (Coil 통합 포함).
    - [x] **퀴즈 카테고리 유형(공식/클라우드/커스텀) 권한 분리 UI 버그 수정** 완료.
    - [x] **AI 주관식 2단계 채점 시스템** (로컬 휴리스틱 + Gemini 의미론적 채점) 완료.
    - [x] **고품질 MP3 효과음 시스템** 및 리소스 컴파일 구조 개선 완료.
    - [x] **오디오 저작권 경고 다이얼로그 및 다시 표시 안함 영속 기능** 완료.
    - [x] **설정란 앱 정보, AI 기술 고지, 브랜드 갤러리(전체 화면 뷰어), 법적고지 연동** 완료.
- **다음 과제 (Phase 2)**: 
    - [ ] **독립된 증시 대시보드 구축**: 야후 파이낸스 API 연동을 통한 실시간 주가(Price) & 환율 수집.
    - [ ] **프리미엄 미니 차트**: Jetpack Compose `Canvas`를 이용해 상승/하락 네온 컬러의 스파크라인 차트 렌더링.
    - [ ] **AI 마크다운 렌더링**: AI 분석 브리핑의 가시성을 위해 Rich text 마크다운 뷰어 탑재.

---

> [!NOTE]
> **커뮤니티 및 기여**: 모든 개발 대화와 문서화는 한국어로 진행됨을 원칙으로 합니다.

