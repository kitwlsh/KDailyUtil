# 🎨 K-시리즈 앱 아이콘 제작 레시피 (K-Series Icon Recipe)

K로 시작하는 형제 앱(KDailyUtil · KLotto645 · 향후 K-DiviTrack, K-Pay 등)의
아이콘을 **일관된 패밀리 룩**으로 만들기 위한 규칙과 실제 제작 절차를 정리한 문서입니다.
새 앱 아이콘을 만들 때 이 문서를 그대로 따라 하면 됩니다.

> 참고: 브랜드 철학은 [`BRANDING_GUIDE.md`](BRANDING_GUIDE.md) 참조.
> 이 문서는 그 철학을 **실제 아이콘 파일로 구현하는 실무 레시피**입니다.

---

## 1. 핵심 원칙 — "Brand Emblem + Feature Sub-theme"

| 요소 | 규칙 |
| :--- | :--- |
| **메인 (Brand Emblem)** | KDailyUtil **골드 육각 K 엠블럼(중앙 태극)** 을 **그대로** 사용. 절대 변형·재색칠 금지. 이것이 패밀리 정체성. |
| **부주제 (Feature)** | 각 앱의 기능을 상징하는 요소(로또볼·트로피·마이크 등)를 엠블럼 **주위에 액센트로** 배치. 엠블럼이 항상 주인공. |
| **톤** | 프리미엄 다크(차콜) 배경 기본. |

---

## 2. 마스터 소스 & 엠블럼 추출

- **원본 엠블럼**: [`KLogo.png`](KLogo.png) (2074×2048, 투명처럼 보이지만 실제로는 **체커보드가 픽셀로 구워져 있음** — 알파 전체 255).
- **다른 K앱 아이콘 참고 시트**: [`KLogo_design.png`](KLogo_design.png) — Klotto/K-Pay/K-suite 등 부주제 아이디어의 원천.
- **체커보드 제거법**: 무채색(|max-min|<20) & 고휘도(min>175) 픽셀을 **테두리에서 flood-fill(반복 팽창)** 로 투명화.
  엠블럼 내부의 흰 하이라이트는 테두리와 연결되지 않아 보존됨.
  → 스크립트: [`icon_scripts/extract_emblem.py`](icon_scripts/extract_emblem.py) → `emblem_clean.png` 생성.

---

## 3. 배치 규칙 ⚠️ (가장 중요)

1. **"by KitwLSH" 각인 보존**: 엠블럼 **5시 방향(우하단)** 경사면에 각인이 있음.
   → 부주제 요소로 **절대 가리지 말 것.**
2. **부주제 위치 = 7시(좌하단)**: 원래 톱니바퀴가 있던 자리. 여기에 배치하면 5시 각인이 살아있음.
3. **엠블럼은 중앙**, 부주제는 하단 액센트. 엠블럼 중앙의 K 글자는 침범 금지.
4. **안전지대 주의**: 원형/적응형(Adaptive) 마스크는 모서리를 자름.
   풀블리드로 만들면 좌하단 요소의 바깥 림이 살짝 깎임 → **숫자/핵심은 볼 중심 쪽에** 두어 판독성 유지.
   (KDailyUtil도 풀블리드로 모서리 일부 잘림을 감수하는 관례.)

---

## 4. 부주제 요소 스타일 — 3D 글로시 볼 (KLotto645 기준)

- **입체감**: 골드 **이중 림(bevel)** + 상단-좌측 **스페큘러 하이라이트** + **가장자리 음영** + **접지 그림자**.
  (numpy 구면 셰이딩: 법선 근사 z + 램버트 확산광, `ball3d()` 함수 참조)
- **숫자/문자**: `Arial Black`(ariblk.ttf), 검은 그림자 동반.
- **색**: 매력적 조합(예: 빨강·금·파랑) + **모든 볼 골드 림으로 통일**해 배경과 조화.
- **의미 전달**: 로또는 문자 K보다 **숫자(6·4·5)** 가 "6/45"를 직관적으로 전달 → 넘버볼 권장.

### KLotto645 확정 스펙 (디자인 A)
- 배경: 라디얼 그라데이션 `#2c2c32`(중앙) → `#0b0b0d`(가장자리)
- 엠블럼: **폭 = 캔버스의 0.80**, **bbox 중앙 정렬**(상하 여백 균형 → 상단 안 비고, KDailyUtil 런처 폭 0.80과 동일)
  - ⚠️ 육각형중심(hex-center)이 아니라 **bbox 중심**으로 정렬해야 상단이 안 빔. 폭 0.88↑는 마스크가 5시 슬랜트를 자름
- 볼: **6**(빨강)/**4**(금)/**5**(파랑), 육각형 중심 기준 **상대 배치**(엠블럼 폭 대비 비율) — 좌하단 삼각 클러스터

---

## 5. 산출물 목록 (Android 인앱 + 스토어)

### 인앱 리소스 (`app/src/main/res/`)
| 파일 | 밀도별 크기(px) |
| :--- | :--- |
| `mipmap-*/ic_launcher.webp` (정사각) | 48 / 72 / 96 / 144 / 192 |
| `mipmap-*/ic_launcher_round.webp` (원형) | 48 / 72 / 96 / 144 / 192 |
| `mipmap-*/ic_launcher_foreground.webp` (적응형 전경) | 108 / 162 / 216 / 324 / 432 |
| `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` | adaptive-icon (background + foreground 참조) |
| `app/src/main/ic_launcher-playstore.png` | 512×512 (스토어 하이레스) |

> 생성 스크립트: [`icon_scripts/build_klotto645_icon.py`](icon_scripts/build_klotto645_icon.py)
> (`emblem_clean.png`을 읽어 마스터 1024→512 합성 후 전 밀도 webp/png export. 새 앱은 볼 색·숫자·경로만 수정해 재사용.)

### 구글 플레이 스토어 업로드 필수 에셋
| 에셋 | 규격 | 비고 |
| :--- | :--- | :--- |
| 앱 아이콘(하이레스) | **512×512** 32bit PNG, ≤1MB | 위 playstore.png |
| Feature Graphic | **1024×500** PNG/JPG | 상세페이지 상단 커버. **정확히 이 크기여야 함** |
| 스크린샷(폰) | 최소 2장, 9:16~16:9, 320~3840px | |
| 태블릿 스크린샷 | (선택) 7"·10" | |
| 개발자 아이콘 | 512×512 PNG | 개발자 페이지(앱 공통) — 오리지널 엠블럼 사용 |
| 개발자 헤더 | 4096×2304 (16:9) | 개발자 페이지(앱 공통) |

---

## 6. 새 K앱 아이콘 만들 때 체크리스트

- [ ] `extract_emblem.py` 로 `emblem_clean.png` 확보 (한 번 만들어두면 재사용 가능)
- [ ] 부주제 요소 디자인 (해당 앱 기능 상징, 3D 스타일 통일)
- [ ] 7시 배치 / "by KitwLSH" 5시 각인 노출 확인
- [ ] `build_*_icon.py` 로 전 밀도 + playstore.png export
- [ ] 원형/적응형 크롭에서 핵심요소 판독되는지 확인
- [ ] 스토어: 512 아이콘 + 1024×500 Feature Graphic + 스크린샷 2장↑ 준비
- [ ] 패밀리 아카이브(`KDailyUtil/doc/family_icons/`)에 마스터 PNG 보관

---

## 7. 스플래시(Splash) 패밀리 룩

모든 K앱은 **동일한 프리미엄 다크 스플래시**를 공유한다: 차콜 라디얼 배경 + 골드 육각 K 엠블럼(페이드/확대/펄스 + shimmer 사선 반사광) + 골드 타이틀 + "by KitwLSH".

| 항목 | 값 |
| :--- | :--- |
| 배경 | 라디얼 `#2C2C2E`(중앙, centerY≈0.42) → `#1A1A1B`(가장자리) |
| 엠블럼 | 투명 육각 엠블럼(`emblem_clean` 계열), 화면 폭의 약 46%, 중앙 상단 |
| 타이틀 | 골드(`#D4AF37`) Bold, 자간 넓게. 표기는 **"K-<앱명>"** 통일 (예: `K-DailyUtil`, `K-Lotto645`) |
| 서브타이틀 | 골드 60% 알파, 앱 설명 (예: `Premium Utility Series`, `Smart Lotto 6/45`) |
| 하단 | "by KitwLSH" 골드 35% 알파 |
| 표시 시간 | 약 2.6~3.5초 후 메인 화면 전환 |

### 구현 분기
- **Compose 앱(KDailyUtil)**: `ui/screens/SplashScreen.kt` — shimmer/meteor Canvas 애니메이션 + `installSplashScreen()`.
- **View/XML 앱(KLotto645)**: `SplashActivity` + `res/layout/activity_splash.xml` + `Theme.App.Starting`(system splash) + `Theme.<App>.Splash`. 애니메이션은 `ViewPropertyAnimator`/`ObjectAnimator`(페이드·확대·펄스)와 shimmer로 재현. 의존성: `androidx.core:core-splashscreen`.
  - **로고 크기**: 코드에서 `화면폭 × 0.80` 정사각으로 지정(KDailyUtil `fillMaxWidth(0.8)`와 동일). 에셋은 tight(여백X) 투명 PNG → fitCenter 시 높이 기준으로 맞아 KDailyUtil과 크기 일치.
  - **확대 시 잘림 방지**: 로고 컨테이너들에 `android:clipChildren="false"` / `clipToPadding="false"` 필수(펄스/확대가 뷰 밖으로 나가도 안 잘림).
  - **shimmer**: 커스텀 `ShimmerLogoView`가 `PorterDuff.SRC_ATOP`로 이미지 불투명 영역(엠블럼)에만 반사광을 얹음 → 아이콘 안에서만 빛이 흐름.
- 공통: 런처 인텐트를 SplashActivity(또는 스플래시 표시 액티비티)에 두고, 실제 메인은 내부에서 실행.
