# 📱 KDailyUtil 릴리즈 노트 (Release Notes)

> 업로드용 AAB는 같은 폴더의 `kdailyutil-v{버전}.aab` 파일입니다.
> 최신: **`kdailyutil-v1.6.aab`** (versionCode 6 / versionName 1.6) — **⏳ 2026-08-03 업로드 완료·Google Play 검토 중**.
> 스토어 게시 이력: v1.5(vc5) 2026-07-23 · v1.4(vc4) 2026-07-21 · v1.2(vc3) 2026-07-08 · v1.1(vc2).
> 버전 스킴: **versionName 끝자리 = versionCode** 로 맞춤(vc6=1.6, 이후 vc7=1.7…). versionCode는 정수만·증가만, versionName은 표시용 문자열.
> 서명 검증 완료: 업로드 키 SHA-256 `61:12:DE:02:AD:DF:…:A5:12:99` (`keytool -printcert -jarfile`).

---

## 📌 Google Play '출시 노트' 붙여넣기용 문구 — v1.6

> Google Play Console > 프로덕션 > 새 버전 > **출시 노트(ko-KR)** 란에 아래 내용을 붙여넣으세요. (언어별 500자 제한)

### ✅ 권장 (붙여넣기용)

```
같은 제작사의 다른 앱을 더 쉽게 만나보실 수 있게 했습니다.

• 설정 > 앱정보 > '브랜드 & 자매앱'에서 K-시리즈 앱 목록을 확인하고 바로 설치하거나 열 수 있어요.
• 새로운 자매앱이 나오면 앱 업데이트 없이도 목록에 자동으로 나타나요.
• 인터넷이 안 되는 상황에서도 목록이 사라지지 않도록 처리했어요.

이용해 주셔서 감사합니다!
```

### ✂️ 짧은 버전 (한 줄 요약형)

```
'브랜드 & 자매앱'에서 K-시리즈 앱을 바로 설치·실행할 수 있고, 새 자매앱이 나오면 앱 업데이트 없이 목록에 자동으로 반영됩니다.
```

### 🇺🇸 English (en-US, 선택)

```
Discover our other apps more easily.
• Settings > App info > "Brand & Sister apps" now lists the K-series apps — install or open them in one tap.
• New sister apps appear automatically, with no app update required.
• The list stays visible even when you're offline.
Thank you for using KDailyUtil!
```

### 🔧 내부 변경(출시 노트에는 넣지 않음)
- 자매앱 목록을 원격 레지스트리(`family.json`)로 동적 렌더 — 신규 자매앱 추가 시 전 앱 재배포 불필요(표준 = `doc/KLOTTO_CONNECT_HANDOFF.md` §8).
- 폴백 체인: 신선한 캐시(6h) → 원격 → last-good 캐시 → 번들 기본값. 실기기 검증 10/10 통과(§8-12).
- 보안: 스토어/아이콘 URL 도메인 화이트리스트, 패키지명 형식 검증, 항목 상한 20, **응답 본문 상한 256KB**, 항목별 파싱 내구성, 자기 자신 제외.
- `<queries>`에 미래 자매앱 예약 패키지 8개 선언(설치 감지·직접 실행용). `QUERY_ALL_PACKAGES` 미사용.
- 서명 키 관리 정리: 키스토어를 저장소 밖(`_secrets`)으로 이전, git 추적 해제, `.gitignore` 보강.

---

## 📌 Google Play '출시 노트' 붙여넣기용 문구 — v1.5

> Google Play Console > 프로덕션 > 새 버전 > **출시 노트(ko-KR)** 란에 아래 내용을 붙여넣으세요. (언어별 500자 제한)

### ✅ 권장 (붙여넣기용)

```
이번 업데이트로 AI 기능을 크게 확장했습니다.

• 뉴스 'AI' 탭에서 이제 AI와 이어서 대화할 수 있어요(음성으로 묻고 답변을 읽어주는 핸즈프리 모드 포함).
• AI 답변을 굵게·목록 등 보기 좋은 서식으로 표시해요.
• 증시: 관심종목들의 실적을 모아 'AI 포트폴리오 종합 분석'을 받아볼 수 있어요.
• 배움터 빠른 독서: 통계 상세 화면과 실력에 맞춘 난이도(목표 속도) 자동 추천을 추가했어요.
• 그림 퀴즈를 공유할 때 이미지가 함께 담겨 상대방 기기에서도 보여요.

이용해 주셔서 감사합니다!
```

### ✂️ 짧은 버전 (한 줄 요약형)

```
뉴스 AI와 대화(음성·핸즈프리) 및 답변 서식 표시, 증시 'AI 포트폴리오 종합 분석', 빠른 독서 통계·난이도 자동 추천, 그림 퀴즈 이미지 포함 공유 등 AI 기능을 크게 확장했습니다.
```

### 🇺🇸 English (en-US, 선택)

```
This update greatly expands our AI features.
• News "AI" tab: chat back-and-forth with AI, including a hands-free voice mode.
• AI replies now render with rich formatting (bold, lists).
• Stocks: get an "AI portfolio summary" across your watchlist's earnings.
• Speed Reading: added a detailed stats screen and auto difficulty (target speed).
• Sharing image quizzes now embeds the pictures so they show on other devices.
Thank you for using KDailyUtil!
```

---

## 📌 Google Play '출시 노트' 붙여넣기용 문구 — v1.4 (이전, 게시됨 2026-07-21)

> Google Play Console > 프로덕션 > 새 버전 > **출시 노트(ko-KR)** 란에 아래 내용을 붙여넣으세요. (언어별 500자 제한)

### ✅ 권장 (붙여넣기용)

```
이번 업데이트 주요 개선입니다.

• 새 앱 아이콘·스플래시로 브랜드를 리프레시했어요.
• 설정 > 앱 정보에 '브랜드 & 자매앱'을 추가해, 자매앱 KLotto645로 바로 이동/설치할 수 있어요.
• 관심 키워드·카테고리의 순서를 직접 바꿀 수 있어요(앞으로/뒤로 이동).
• 증시 과거 실적에 'AI 추세 종합 코멘트'를 추가했어요.
• 배움터 빠른 독서: 보관함 지문의 제목을 편집할 수 있어요.

이용해 주셔서 감사합니다!
```

### ✂️ 짧은 버전 (한 줄 요약형)

```
새 앱 아이콘·스플래시 적용, '브랜드 & 자매앱'(자매앱 바로가기) 추가, 관심 키워드/카테고리 순서 변경, 증시 AI 추세 종합 코멘트, 빠른 독서 보관함 제목 편집 등 편의 기능을 개선했습니다.
```

### 🇺🇸 English (en-US, 선택)

```
What's new in this update.
• Refreshed the app icon and splash.
• Added "Brand & Sister apps" in Settings > App info — jump to our sister app KLotto645.
• Reorder your keywords/categories (move forward/back).
• Stocks: added an AI trend-summary comment for past earnings.
• Speed Reading: rename saved passages in your library.
Thank you for using KDailyUtil!
```

---

## 📌 Google Play '출시 노트' 붙여넣기용 문구 — v1.2 (이전, 게시됨 2026-07-08)

> Google Play Console > 프로덕션 > 새 버전 > **출시 노트(ko-KR)** 란에 아래 내용을 붙여넣으세요. (언어별 500자 제한)

### ✅ 권장 (붙여넣기용)

```
이번 업데이트로 안정성과 편의성을 크게 개선했습니다.

• 배움터: '상식 백과' 퀴즈가 열리지 않던 문제를 해결하고 퀴즈 데이터 안정성을 강화했어요. 퀴즈·빠른 독서 화면에서 뒤로가기가 자연스러워졌습니다.
• 증시: 과거 실적 조회와 회사명 검색을 추가했어요. AI 실적 공시 목록·스크롤을 개선하고(같은 회사는 최신 1건만) 관심종목 설정을 정리했습니다.
• 오디오: 이전 폴더의 녹음을 다시 가져오는 복구 기능, 블루투스 버튼·통화 후 자동 이어재생을 추가했어요.
• 뉴스: 저작권 보호를 강화했습니다(기사 원문은 언론사 링크로 제공).

이용해 주셔서 감사합니다!
```

### ✂️ 짧은 버전 (한 줄 요약형)

```
'상식 백과' 퀴즈 오류 수정 및 데이터 안정성 강화, 증시 과거 실적 조회·회사 검색 추가, AI 실적 공시 목록/스크롤 개선, 오디오 이전 녹음 복구, 뒤로가기 동작 개선 등 안정성과 편의성을 개선했습니다.
```

### 🇺🇸 English (en-US, 선택)

```
This update improves stability and convenience.
• Learn: Fixed the "General Knowledge" quiz not opening; hardened quiz data; smoother back navigation.
• Stocks: Added past-earnings lookup and company search; improved the AI earnings list & scrolling (one latest report per company); tidied watchlist settings.
• Audio: Restore recordings from previous folders; Bluetooth buttons & auto-resume after calls.
• News: Strengthened copyright handling (articles open via publisher links).
Thank you for using KDailyUtil!
```

---

## 🗂 버전별 수정 내역 (Changelog)

### v1.5 (versionCode 5) — 2026-07-22 빌드 · ✅ 2026-07-23 스토어 게시됨
> 게시본 v1.4(vc4, 07-21) 이후 AI 기능 확장을 모아 배포한 버전. **2026-07-23 검토완료·출시.**

**뉴스 / AI 대화**
- 뉴스 'AI' 탭에 **멀티턴 대화**(맞춤 분석에 이어 묻기) 추가. **음성 입력(STT)·답변 낭독(TTS)**, 답변 자동 낭독 후 다시 듣는 **핸즈프리 모드** 지원.
- 대화는 (명령어+날짜) 세션 단위로 **로컬 30일 보관**(읽기 전용 열람·개별/전체 삭제). 컨텍스트는 제목+RSS 스니펫만 사용(본문 비스크랩·'AI 이용 금지' 매체 제외). 부적절 응답 신고·면책 고지 포함.
- **AI 답변 마크다운 서식 렌더링**(굵게·목록·제목), 낭독 시 기호 제거.
- 브리핑 낭독에서 제목이 두 번 읽히던 문제 수정.

**증시**
- **AI 스마트 관심종목 포트폴리오 종합 분석**: 관심종목(국내 상장사)들의 최근 실적을 모아 전반 흐름·상대 우열·집중 리스크를 AI가 1회 종합(참고용, 파일 캐시·재분석).

**배움터(빠른 독서)**
- **통계 상세 화면**(속도 최고/평균/최근·범위·향상률, WPM 추이, 훈련 기록) + **난이도 자동 추천**(최근 실력 기반 목표 속도 → 드릴 초기값 반영).

**퀴즈**
- **그림 퀴즈 공유 개선**: `.kquiz`에 이미지를 내장(Base64)해 다른 기기에서도 그림이 보이도록 함.

**표시/호환**
- Android 15(SDK 35+) Edge-to-Edge 대응 정리: deprecated 상태바 색상 설정을 제거하고 순수 edge-to-edge 방식으로 통일(다크 배경 유지).

---

### v1.4 (versionCode 4) — 2026-07-20  *(스토어 게시됨 2026-07-21)*
> 스토어 게시본 v1.2(vc3, 07-08) 이후의 변경을 모아 배포하는 버전. (versionName 끝자리를 versionCode(4)와 맞추며 1.3은 건너뜀)

**브랜드/아이콘**
- 앱 아이콘·스플래시·워터마크를 K-시리즈 패밀리(형제 앱 KLotto645 등)와 통일(자체 앱 아이콘 기반 스플래시로 교체).

**자매앱 상호연결**
- 설정 > 앱 정보의 브랜드 갤러리를 **'브랜드 & 자매앱'**으로 개편. KLotto645 카드를 추가해 **설치돼 있으면 바로 실행, 없으면 Play 스토어로 이동**(설치/열기 배지). 자기 로고와 자매앱은 구분선·소제목으로 분리.

**설정/편의**
- 관심 키워드·뉴스 카테고리·증시 키워드·AI 명령의 **순서 변경 지원**(각 칩에서 앞으로/뒤로 이동, 기존 값 자동 유지).

**증시**
- 과거 실적 다이얼로그에 **🤖 추세 종합 AI 코멘트**(다분기 매출·수익성 흐름 1회 요약, 참고용) 추가.

**배움터(빠른 독서)**
- 보관함 지문 **제목 편집**(✏️) 추가.

---

### v1.2 (versionCode 3) — 2026-07-08  *(스토어 게시됨)*
> v1.1 이후 누적된 개선을 모아 배포하는 버전.

**배움터 / 퀴즈**
- '상식 백과' 퀴즈가 열리지 않던 문제 해결(원격 데이터의 잘못된 항목 1건이 카테고리 전체를 못 쓰게 만들던 구조를 개선 — 불량 항목만 건너뛰도록).
- 퀴즈 데이터 파싱 내구성 강화 + 원격 데이터 검증 자동화(CI 게이트).
- 퀴즈 문제 풀이/빠른 독서 훈련 중 시스템 뒤로가기가 뉴스탭으로 튕기지 않고 이전 단계로 이동.
- 같은 정답이 빈 문제가 사라지던 현상 방지(질문 기준 중복 제거).
- (이전) AI로 나만의 퀴즈 만들기 가이드, JSON 붙여넣기 가져오기, 커스텀 문제 직접 편집, 중복 출제 방지 강화.
- (이전) 빠른 독서 훈련 연습 지문 3개 → 19개로 확장(주제 다양화).

**증시**
- 과거 실적 조회(최근 8개 정기보고서) + 회사명 검색 추가.
- AI 실적 공시: 같은 회사는 가장 최근 1건만 표시, 검색·조회기간·숨김보기 헤더가 목록과 함께 스크롤되도록 개선, 숨김 항목 표시/복원 개선.
- 과거 실적 카드에 회계 기준월·연결/개별 재무제표 설명 추가.
- 증시 키워드를 '뉴스탭 증시'와 '증시 대시보드 관심종목'으로 명확히 구분하고 설정에서 각각 관리.
- (이전) '실적 예정 일정' → '실적 뉴스·전망' 개편, 조회기간 변경 시 AI 분석 유지, DART 키 발급 도움말, 탭 '＋ 빠른 추가'.

**오디오**
- (이전) 이전 버전 공용 폴더의 녹음을 다시 가져오는 SAF 복구 기능.
- (이전) 블루투스/이어폰 미디어 버튼 제어, 통화 등 인터럽트 후 자동 이어재생.

**뉴스**
- (이전) 저작권 보호 정책 강화: 기사 본문은 언론사 원문 링크(인앱 브라우저)로 제공, 낭독은 요약 스니펫만, 'AI 이용 금지' 매체는 AI 분석·낭독에서 제외.

---

### v1.1 (versionCode 2) — 2026-06-25
- Google Play '사진·동영상 권한' 정책 대응: `READ_MEDIA_*` 권한 전면 제거.
- 이미지 선택을 Android 사진 선택 도구(Photo Picker)로 전환(권한 불필요).
- 기기 전체 미디어 스캔 제거(앱 전용 폴더만 사용).
- 개인정보처리방침 개정(카메라 권한·Photo Picker·Gemini AI 제3자 처리 명시).

---

### v1.0 (versionCode 1) — 2026-06  *(최초 등록)*
- 최초 버전: AI 뉴스 브리핑, 증시 대시보드, 오디오 캡처, AI 퀴즈(KuizGenius), 배움터 등 통합 유틸리티.
- ⚠️ 사진·동영상 권한 정책 사유로 반려 → v1.1에서 해결.

---

> 상세 개발 맥락은 저장소의 [DEVELOPER_GUIDE.md](../../doc/DEVELOPER_GUIDE.md) 및 커밋 이력 참조.
