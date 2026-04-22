# 🎙 오디오 시스템 고도화 구현 리포트 (2026-04-22)

## 1. 개요
오디오 캡처 및 재생 시스템의 UI/UX를 전문가 수준으로 고도화하고, 백그라운드 환경에서의 안정성을 극대화함.

## 2. 주요 구현 사항

### 2.1. 3단계 스마트 레코딩 상태 제어
- **Idle (멈춤)**: 서비스 대기 상태. 백그라운드 노출 없음.
- **Ready (준비/녹색)**: `MediaProjection` 권한 획득 후 대기. 다른 앱 실행 시 플로팅 마이크 버튼 노출.
- **Recording (녹음 중/빨간색)**: 내부 소리 캡처 및 AAC 인코딩 진행 중.
- **연속 녹음**: 녹음 중지 시에도 `MediaProjection`을 유지하여 백그라운드에서 즉시 재녹음 가능.

### 2.2. 백그라운드 재생 및 녹음 안정화
- **Dual WakeLock**: `MediaPlayer` 및 `Service` 레벨에서 `PARTIAL_WAKE_LOCK`을 적용하여 화면이 꺼진 상태에서도 작업 유지.
- **Foreground Service**: 재생(MEDIA_PLAYBACK) 및 녹음(MEDIA_PROJECTION) 타입에 맞는 포그라운드 서비스 유지.
- **Audio Attributes**: `USAGE_MEDIA` 및 `CONTENT_TYPE_MUSIC` 설정을 통해 시스템 리소스 우선순위 확보.

### 2.3. UI/UX 정밀 튜닝
- **하단 미니 플레이어**: 시스템 탭바와 유격 없는(Flush) 배치. `Slider`를 이용한 얇은 진행 바 구현.
- **자동 스크롤**: `LazyListState.animateScrollToItem`을 활용하여 재생 중인 곡이 리스트 상에서 항상 보이도록 자동 추적.
- **편집 잠금 (Edit Lock)**: 실수에 의한 삭제/이름 변경 방지 기능을 옵션화하고 `DataStore`에 영구 저장.

## 3. 기술 스택 및 라이브러리
- **언어**: Kotlin (Jetpack Compose)
- **미디어**: `MediaPlayer`, `AudioRecord`, `MediaCodec` (AAC), `MediaMuxer`
- **상태 관리**: `StateFlow`, `SharedFlow`, `DataStore`
- **시스템**: `Foreground Service`, `WakeLock`, `MediaProjection`

## 4. 향후 과제
- **알림창 제어**: 알림창에서 바로 재생/일시정지 및 다음 곡 제어가 가능한 `MediaStyle` 알림 추가 검토.
- **파일 관리**: 녹음 파일의 메타데이터(길이, 비트레이트 등)를 별도 DB로 관리하여 리스트 로딩 속도 최적화.
