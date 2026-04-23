# [Issue Report] Google News RSS Redirect Resolution in Android (Kotlin)

## 1. 개요 및 목표
- **환경**: Android (Kotlin), Jsoup 라이브러리 사용
- **목표**: 구글 뉴스 RSS 피드(`https://news.google.com/rss/articles/...`)의 암호화된 링크를 해독하여 실제 언론사의 기사 원문 URL(예: naver.com, bloomberg.com 등)을 알아내고 본문을 추출하는 것.

## 2. 지금까지 시도한 방법 (Attempts)

### ① Jsoup 기본 리다이렉트 추적
- `Jsoup.connect(url).followRedirects(true).execute()`를 사용해 HTTP 수준의 리다이렉트를 추적함.
- **결과**: 구글이 클라이언트 사이드(JS) 리다이렉트를 사용하기 때문에 여전히 `google.com` 도메인에 머무름.

### ② HTML 파싱 및 셀렉터 추출
- 구글 리다이렉트 페이지의 HTML을 파싱하여 `c-wiz`, `<a>` 태그 내의 `href`를 탐색함.
- **결과**: 구글이 봇 탐지를 강화하여 유효한 링크를 노출하지 않거나, 이미지 서버(`googleusercontent.com`) 주소만 반환함.

### ③ Base64 URL 디코딩 시도
- 링크 중간의 암호화된 ID(`CBMi...`)를 Base64로 디코딩하여 문자열 내에서 `http` 패턴을 정규식으로 추출 시도.
- **결과**: 구글의 최신 암호화 방식은 단순 문자열이 아닌 Protobuf(Protocol Buffers) 형태의 바이너리로 추정되며, 단순 디코딩으로는 실제 URL 추출이 불완전함.

### ④ Meta Refresh 및 Script 분석
- `<meta http-equiv="refresh">` 태그나 `window.location.replace` 스크립트 내의 URL을 추출하려 함.
- **결과**: 해당 태그가 존재하지 않거나 자바스크립트에 의해 동적으로 생성되어 Jsoup으로는 접근 불가.

### ⑤ 도메인 필터링 및 우선순위 매칭
- 추출된 모든 링크 중 구글 관련 도메인(gstatic, googleusercontent 등)을 제외하고 외부 도메인을 찾는 로직 적용.
- **결과**: 실제 기사 링크는 여전히 숨겨져 있어 찾지 못함.

## 3. 핵심 장애 요인 (Key Blockers)
1. **Jsoup의 한계**: Jsoup은 HTML 파서일 뿐, 자바스크립트 엔진이 없어 구글의 JS 기반 리다이렉트 로직을 실행하지 못함.
2. **구글의 암호화**: `/articles/` 뒤의 문자열이 단순 Base64가 아닌 복합적인 구조로 변경됨.
3. **Headless Browser 부재**: 안드로이드 환경에서 가벼운 Jsoup만으로는 한계가 있음.

## 5. 해결 방안 (Solution Implemented - 2026-04-23)

### ① 3단계 하이브리드 추출 파이프라인 구축
- **Tier 1 (Protobuf Decoder)**: `/articles/` 뒤의 암호화된 문자열을 `URL_SAFE` 및 `NO_PADDING` Base64로 디코딩한 후, 바이너리 데이터 내에서 유효한 URL 패턴을 직접 추출하는 전용 디코더 제작.
- **Tier 2 (Jsoup + Meta Refresh 추적)**: 표준 리다이렉트 외에도 HTML 내의 `<meta http-equiv="refresh">` 태그를 분석하여 2차 리다이렉트 수행.
- **Tier 3 (백그라운드 WebView Fallback)**: 자바스크립트 실행이 필수적인 고강도 보안 사이트를 위해 `WebView`를 메모리에서 로딩하여 최종 URL을 가로채는 방식 적용.

### ② 본문 정밀 정화 시스템
- **SELECTOR 우선순위**: `.text` 및 `.article-text` 기반의 다중 블록 결합 로직 구현.
- **NOISE 필터**: `figcaption`, `figure`, `.subtitle`, `.author`, `.audio-player` 등 기사 외 요소를 사전에 제거.
- **CLEANING**: 정규식을 통해 바이라인(지역/기자 정보, 복합 이메일 주소) 및 잔여 특수기호 청소.

## 6. 결론 및 향후 과제
- **성과**: 한겨레를 포함한 다수의 국내 주요 언론사 기사 본문 추출 및 정제 성공.
- **남은 과제 (조선일보 튜닝)**: 조선일보의 경우 더욱 강력한 안티 봇 대응 및 전용 셀렉터 튜닝이 필요하며, 이는 다음 개발 세션에서 집중적으로 다룰 예정임.
