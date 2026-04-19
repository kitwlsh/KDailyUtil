# 📱 KDailyUtil

데일리 유틸리티 안드로이드 애플리케이션입니다. 키워드 뉴스 브리핑, 운전 중 말하기 연습, 오디오 캡처 등 다양한 일상 편의 기능을 제공합니다.

> [!IMPORTANT]
> **본 프로젝트와 관련된 모든 AI와의 대화는 반드시 한국어로 진행합니다.** 이는 프로젝트의 일관성을 유지하고 차후 분석을 용이하게 하기 위함입니다.

---

## ✨ 주요 기능

1.  **📰 뉴스 키워드 브리핑**: 사용자 지정 키워드 기반 실시간 뉴스 수집 및 AI 요약.
2.  **🚗 운전 중 말하기 연습**: TTS 문장 읽기 및 사용자 음성 녹음/재생을 통한 쉐도잉 연습.
3.  **🎙️ 오디오 캡처 및 재생**: 내부/외부 소리 캡처 및 백그라운드 재생 지원.
4.  **⏰ 오전 자동 브리핑**: 출근 시간 등 설정된 시간에 주요 뉴스 및 이슈 자동 브리핑.
5.  **🧩 확장성**: 추가 기능을 위한 플레이스홀더 설계.

## 🛠 기술 스택

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Concurrency**: Coroutines
- **Navigation**: Compose Navigation
- **Audio/Voice**: Foreground Service, MediaSession, Google TTS/STT
- **Data Capture**: Jsoup (Scraping) / Open API

## 📂 프로젝트 구조

- `app/src/main/java/.../ui`: UI 컴포넌트 및 Compose 화면
- [📋 구현 계획서](doc/implementation_plan.md): 기능 구현 설계 및 로드맵
- [✅ 작업 현황](doc/task.md): 현재 진행 상황 및 할 일 목록

## 🚀 시작하기

1. 이 저장소를 클론합니다.
2. Android Studio (Ladybug 이상 권장)에서 프로젝트를 엽니다.
3. `./gradlew assembleDebug` 명령어로 빌드합니다.
