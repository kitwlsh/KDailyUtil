# 📱 KDailyUtil 릴리즈 노트 (Release Notes)

> 업로드용 AAB는 같은 폴더의 `kdailyutil-v{버전}.aab` 파일입니다.
> 최신: **`kdailyutil-v1.2.aab`** (versionCode 3 / versionName 1.2)

---

## 📌 Google Play '출시 노트' 붙여넣기용 문구 — v1.2

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

### v1.2 (versionCode 3) — 2026-07-08
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

### v1.1 (versionCode 2) — 2026-06-25  *(현재 스토어 배포본)*
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
