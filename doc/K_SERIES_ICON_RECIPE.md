# 🎨 K-시리즈 앱 아이콘·스플래시 표준 (K-Series Icon & Splash Standard)

K로 시작하는 형제 앱(**KDailyUtil · KLotto645** · 향후 K-DiviTrack, K-Pay 등)의
아이콘·스플래시·워터마크를 **하나의 패밀리 룩**으로 만들기 위한 **단일 기준 문서(Single Source of Truth)** 입니다.

> **새 K앱을 만들 때는 이 문서 §6 절차만 그대로 따라 하면** 기존 앱과 동일한 품질·크기·정렬이 재현됩니다.
> 브랜드 철학은 `KDailyUtil/doc/BRANDING_GUIDE.md`, 이 문서는 그 철학의 **실무 구현 규격**입니다.
> (2026-07-16 재정리 — 그 이전 판의 산발적 수치는 아래 §2 표준 규격으로 통합됨)
>
> 🔁 **3벌 동기화 문서**: 이 파일은 **`KLotto645/doc/`·`KDailyUtil/doc/`·`KJangbu/doc/` 세 곳에 동일 사본**으로 유지한다.
> 어느 한 곳에서 고치면 **나머지 두 곳도 같은 내용으로 맞춘다**(각 앱 세션은 자기 저장소의 사본을 진입점으로 삼는다).
> 🔴 **저장소별 예외 줄을 두지 않는다** — 한 곳에만 있는 안내 한 줄이라도 남기면 다음 세션이 그 예외를 모르고 덮어쓴다.
>   자기 저장소에서만 의미 있는 메모는 **그 저장소의 자기 문서**에 둔다.
> ✅ 동일성 확인은 **`diff --strip-trailing-cr`** 로 한다 — 세 저장소 모두 `core.autocrlf=true`라 작업 트리 줄바꿈이
>   PC·세션마다 CRLF/LF로 갈리고, 그래서 **작업 트리 md5 대조는 내용이 같아도 실패한다**(2026-09-07 실제로 셋 다 달랐다).
>   커밋본은 git이 LF로 정규화하므로 `git show HEAD:<경로>` 비교도 정확하다.
> ⚠️ 그러므로 **파일 참조는 상대경로 마크다운 링크가 아니라 저장소 접두 평문 경로**(`` `KLotto645/app/src/...` ``)로 쓴다.
> 사본들의 깊이는 이제 같지만(`KLotto645/main/doc` · `KDailyUtil/main/doc` · `KJangbu/main/doc`) **접두 평문 경로 규칙은 유지한다** — 배치가 또 바뀌어도 안 깨진다. 폴더 규칙 정본 = 저장소들의 **상위 폴더에 있는 `README.md`**(각 저장소 기준 `../../../README.md`).

---

## 1. 핵심 원칙 — "Brand Emblem + Feature Sub-theme"

| 요소 | 규칙 |
| :--- | :--- |
| **메인 (Brand Emblem)** | **골드 육각 K 엠블럼(중앙 태극)** 을 **모든 앱이 그대로 공유**. 변형·재색칠 금지. 이것이 패밀리 정체성. |
| **부주제 (Feature)** | 각 앱 기능을 상징하는 3D 글로시 오브젝트(로또볼·톱니/나침반·트로피 등)를 **좌하단(7시)에 액센트로**. 엠블럼이 항상 주인공. |
| **톤** | 프리미엄 다크(차콜). 런처는 다크 배경, 스플래시/워터마크는 투명 위에 다크 화면. |
| **원칙** | **메인 로고와 부주제를 각각 '투명 PNG'로 따로 준비 → 스크립트로 합성.** (통짜 이미지 금지 — 크기·위치 통일이 불가능해짐) |

---

## 2. 📐 표준 규격 (Single Source of Truth)

모든 수치는 **1024×1024 캔버스 기준**. 공유 엠블럼 bbox 비율 ≈ **폭:높이 = 0.88 : 1** (세로가 김 — 반드시 '높이/폭 기준'을 지킬 것).

### 2-1. 공통
| 항목 | 값 |
| :--- | :--- |
| 캔버스 | **1024×1024** |
| 다크 배경(런처) | 라디얼 `#2C2C32`(중앙) → `#0B0B0D`(가장자리) *(런처용; 스플래시는 §7 화면 배경)* |
| 부주제 위치 | **7시(좌하단)**, "by KitwLSH" 5시 각인 절대 침범 금지, 중앙 K 침범 금지 |
| **부주제 크기/위치(엠블럼 대비 고정 비율)** | **폭 = 엠블럼폭 × 0.393**, 중심 = 엠블럼 bbox의 **(x 0.228, y 0.819)** 지점 |

### 2-2. 두 가지 산출 모드
아이콘은 용도에 따라 **엠블럼 크기·배경이 다르다**(부주제 비율은 §2-1로 동일).

| 모드 | 용도 | 엠블럼 | 배경 | 생성 스크립트 |
| :--- | :--- | :--- | :--- | :--- |
| **A. 풀블리드(투명)** | **스플래시 · 배경 워터마크 · 설정 갤러리** | **높이 100%** 채움, 중앙(폭 ~0.88, 상/하 꼭짓점이 끝에 닿음) | 투명(RGBA) | `KDailyUtil/doc/icon_scripts/build_kdaily_icon.py` |
| **B. 런처/스토어** | **mipmap · adaptive · playstore** | **폭 0.80**(=411/512), 중앙 `(256,256)` | 다크 라디얼 | `KDailyUtil/doc/icon_scripts/build_kdaily_launcher.py` |

> 모드 B(런처)는 **모드 A(스플래시)를 그대로 축소한 비율**이 되도록 부주제를 §2-1 비율로 배치한다 → "아이콘 = 스플래시 축소판".
> 런처가 엠블럼 0.80(여백)인 이유: 원형/적응형(Adaptive) 마스크가 모서리를 깎으므로 여백이 필요. 스플래시는 화면 위에 얹혀 마스크가 없어 100% 가능.

### 2-3. 레퍼런스 실측값 (KLotto645 아이콘 = 기준)
새 앱 런처가 이 값과 **픽셀 단위로 일치**해야 패밀리 통일 (512² playstore 기준, 중앙 컬럼):

| 지표 | 값 |
| :--- | :--- |
| 육각 상단 꼭짓점 | y = **24** (5%) |
| 다이아몬드(하단 화살표) 끝 | y = **487** (95%) |
| 엠블럼 높이 | **463** (90%) |
| 엠블럼 세로 중심 | **255** (≈ 정중앙) |
| K(색상 로고) 상단 여백 | **≈6%** |

### 2-4. 스플래시 표시 & 워터마크
| 항목 | 값 |
| :--- | :--- |
| 스플래시 로고 표시 | 정사각 박스 **화면폭 × 0.80** + `Fit`. ⚠️ 진입 스케일 **오버슈트 없어야**(rest=1.0) 100%채움 꼭짓점 안 잘림 |
| 배경 워터마크 | **화면폭 × 0.80**(스플래시와 동일 크기, 상대비율), alpha **0.28**, 중앙, Fit |
| 스플래시 타이틀 | 골드 `#D4AF37` Bold, 자간 넓게, **"K-<앱명>"**(예: `K-DailyUtil`, `K-Lotto645`) |
| 서브타이틀 / 하단 | 서브=골드 60%α 앱설명 / 하단="by KitwLSH" 골드 35%α |

---

## 3. 배치 규칙 — "7시 사분면 정사각 존" ⚠️(가장 중요)

> 부주제(로또볼·톱니 등)는 **육각 엠블럼 밖으로 나가지 않는다.** 캔버스를 중심점 기준 4등분한 **좌하단(7시) 정사각 칸** 안에 부주제 bbox가 **완전히 수납**되도록 배치·크기 조정.

```
 ┌───────────┬───────────┐   • 부주제 존 = 좌하단(7시) 사분면(가로 0~50%, 세로 50~100%)
 │  (10시)   │   (2시)   │   • 부주제 bbox 전체가 이 칸 안에 → 육각 테두리 안 넘음
 ├───────────┼───────────┤   • "by KitwLSH"(5시=우하단) 각인 절대 안 가림
 │▓▓ 부주제 ▓▓│   (4시)   │   • 중앙 K 글자 침범 금지
 │▓▓ (7시)  ▓▓│           │   • 핵심(숫자·나침반면)은 칸 안쪽(중심점 쪽) → 마스크 잘려도 판독 유지
 └───────────┴───────────┘
```

- **판정**: 부주제 클러스터 bbox가 좌하단 사분면을 벗어나면 위반.
- **왜 이 규칙?** 육각 내부는 이미 복잡(양각 세계지도+텍스처+K)해 "완전 안쪽"은 부주제가 뭉개지고, "크게 바깥"은 마스크 잘림+분리감. **7시 존 수납**이 프리미엄 배지룩과 이탈 방지를 동시에 만족.
- 실무 구현은 §2-1의 정량 비율(0.393 / (0.228,0.819))로 고정돼 있음.

---

## 4. 부주제 스타일 가이드 (3D 글로시)

- **입체감**: 골드 **이중 림(bevel)** + 상단-좌측 **스페큘러 하이라이트** + 가장자리 음영 + 접지 그림자.
- **색**: 매력적 조합(예: 빨강·금·파랑). 금속류는 **골드/황동 톤**으로 엠블럼과 조화.
- **문자/숫자**: `Arial Black`(ariblk.ttf), 검은 그림자 동반.
- **제작 방식 2가지**:
  1. **numpy 코드 렌더** — 예: KLotto645 645볼(`build_klotto645_icon.py`의 `ball3d()`: 구면 셰이딩+램버트광). 색·숫자·위치를 코드로 자유 조정. 재현성 최고.
  2. **AI 이미지 생성** — 투명 배경 오브젝트 PNG. 프롬프트 예시는 §6-A. (KDailyUtil 톱니/나침반이 이 방식)
- ⚠️ 어느 방식이든 **투명 배경 단독 오브젝트**로 뽑아 §2 비율로 합성한다. 엠블럼과 한 이미지로 굽지 말 것.

---

## 5. 자산 & 스크립트 (파이프라인)

```
[공유 엠블럼]  KLogo.png ──extract_emblem.py──▶ emblem_clean(투명)
                                                    │
[부주제]  numpy 렌더 또는 AI PNG ──(배경제거)──▶ subtheme(투명)
                                                    │
              ┌─────────────────────────────────────┴───────────────────┐
              ▼                                                           ▼
   build_kdaily_icon.py  (모드 A)                       build_kdaily_launcher.py (모드 B)
   → ic_<app>_icon.png (1024² 투명)                     → mipmap-*/ic_launcher(.round/_foreground).webp
     = 스플래시·워터마크·갤러리                             + ic_launcher-playstore.png (512²)
```

### 패밀리 아카이브 (`doc/family_icons/`)
| 파일 | 내용 |
| :--- | :--- |
| `kdaily_emblem_clean_1024.png` | **공유 엠블럼**(투명) — 모든 앱 공용 메인 로고 |
| `kdaily_subtheme_gears.png` | KDailyUtil 부주제(톱니/나침반, 투명, 배경제거 완료) |
| `kdaily_subtheme_gears_source.png` | 위의 원본(AI 생성, 체커배경) — 아카이브 |
| `kdailyutil_icon_1024.png` | KDailyUtil 스플래시 마스터(모드 A 결과) |
| `kdailyutil_launcher_1024.png` | KDailyUtil 런처 마스터(모드 B 결과) |
| `KLotto645_icon_512/1024.png` | KLotto645 아이콘 마스터 |

### 스크립트 (`doc/icon_scripts/`)
| 스크립트 | 역할 |
| :--- | :--- |
| `extract_emblem.py` | KLogo.png 체커보드 제거 → 투명 엠블럼 |
| `build_kdaily_icon.py` | **모드 A**: 엠블럼(100%) + 부주제(7시 존) → 스플래시/워터마크 마스터. `--strip-bg`로 부주제 단색배경 제거 |
| `build_kdaily_launcher.py` | **모드 B**: 다크배경 + 엠블럼(0.80) + 부주제 → 런처 전 밀도 + playstore |
| `build_klotto645_icon.py` | KLotto645 런처 생성(numpy 볼 렌더 포함) — 모드 B의 KLotto판 |
| `extract_kdaily_app_icon.py` | (legacy) 통짜 로고에서 앱아이콘 추출 — 현재는 build_kdaily_icon.py로 대체 |

---

## 6. ✅ 새 K앱 만들기 — 단계별 절차

> 예: 새 앱 `K-Foo`. 부주제 = 앱 기능 상징 오브젝트.

**A. 부주제 투명 PNG 준비** (둘 중 하나)
- **numpy 렌더**: `build_klotto645_icon.py`의 `ball3d()`류를 참고해 오브젝트를 그림 → 투명 PNG.
- **AI 생성** (GPT-4o 이미지 / Gemini Nano Banana 권장): 아래 프롬프트로 뽑고 배경이 있으면 `--strip-bg`로 제거.
  ```
  A tight cluster of <앱 상징 오브젝트>, isolated on a fully transparent background (PNG with alpha).
  Premium 3D glossy render, rich 24K gold / antique brass tones, beveled metallic edges,
  warm specular highlights (top-left light), subtle reflections. Square 1:1, 2048px, centered ~12% padding.
  IMPORTANT: transparent background only — no scene, no floor, no cast shadow, no text, no hexagon, nothing else.
  ```

**B. 스플래시/워터마크 마스터 생성 (모드 A)**
```bash
python doc/icon_scripts/build_kdaily_icon.py \
  --emblem  doc/family_icons/kdaily_emblem_clean_1024.png \
  --subtheme doc/family_icons/<kfoo_subtheme>.png \
  --out     app/src/main/res/drawable-nodpi/ic_k_foo_icon.png [--strip-bg]
```
→ 이 PNG를 **스플래시·워터마크·설정 갤러리**에 사용.

**C. 런처/스토어 아이콘 생성 (모드 B)**
`build_kdaily_launcher.py`의 `SUB`(부주제 경로)·`ROOT`/`RES`/`PROJ`(앱 경로)만 새 앱에 맞게 바꿔 실행 → mipmap 전 밀도 + round + foreground + playstore 512 export.
→ 산출물 §2-3 레퍼런스 실측값과 일치하는지 확인.

**D. 배선**
- 스플래시: §7 (Compose 또는 View/XML) — 로고 = ic_k_foo_icon, 화면폭×0.80 Fit.
- 워터마크: §8-1 — 같은 아이콘, 화면폭×0.80, alpha 0.28.
- 앱정보/브랜드 갤러리: §8-2.

**E. 체크리스트**
- [ ] 부주제가 7시 존 안 (육각 밖 이탈 X), 5시 "by KitwLSH" 안 가림, 중앙 K 안 가림
- [ ] 런처 실측 = §2-3(육각 top 24·다이아끝 487·중심 255)
- [ ] 원형/적응형 마스크에서 핵심 판독됨
- [ ] 스플래시/워터마크/런처가 서로 "축소판" 관계(비율 일치)
- [ ] 마스터 PNG를 `doc/family_icons/`에 보관
- [ ] 스토어: 512 아이콘 + 1024×500 Feature Graphic + 스크린샷 2장↑
- [ ] **출시앱이면 배포 시 versionCode 상향**

---

## 7. 스플래시(Splash) 구현

모든 K앱은 프리미엄 다크 스플래시를 공유, **각 앱은 자기 아이콘**을 표시. 규격은 §2-4.

- **"덮어쓰기 = 즉시 적용"**: 스플래시 로고는 **1024² 투명 정사각** 한 파일. 이 파일만 같은 규격으로 교체하면 코드 수정 없이 반영.
- **Compose(KDailyUtil)**: `KDailyUtil/app/src/main/java/com/kitwlshcom/kdailyutil/ui/screens/SplashScreen.kt` — `Image(ic_k_app_icon)` on `fillMaxWidth(0.8f).aspectRatio(1f)`, shimmer를 `BlendMode.SrcAtop`(offscreen)로 아이콘 형태에만 + meteor Canvas. 진입 `scaleAnim` rest=1.0. `installSplashScreen()`.
- **View/XML(KLotto645)**: `SplashActivity` + `activity_splash.xml` + 커스텀 `ShimmerLogoView`(`PorterDuff.SRC_ATOP`). 로고 크기 코드에서 `화면폭×0.80`, 컨테이너 `clipChildren=false`.
- 공통: shimmer는 **아이콘 형태 안에서만**(SrcAtop 마스킹).

| 앱 | 스플래시 로고 파일 |
| :--- | :--- |
| KDailyUtil | `app/src/main/res/drawable-nodpi/ic_k_app_icon.png` |
| KLotto645 | `app/src/main/res/drawable-nodpi/ic_k_emblem_balls.png` (워터마크도 재사용) |

---

## 8. 인앱 브랜딩 (워터마크 · 앱 정보 · 갤러리)

### 8-1. 배경 워터마크
주요 화면 중앙에 **자기 앱 아이콘**을 은은히(§2-4: 화면폭×0.80, alpha 0.28, Fit).
- KDailyUtil: `KDailyUtil/app/src/main/java/com/kitwlshcom/kdailyutil/ui/components/BrandComponents.kt` `BrandWatermark` — `Modifier.fillMaxWidth(0.8f).aspectRatio(1f)`.
- KLotto645: `KLotto645/app/src/main/res/layout/bg_watermark.xml`(id `iv_watermark`) + `Activity.sizeBrandWatermark()`(`KLotto645/app/src/main/java/com/kitwlshCom/klotto645/util/InsetsUtil.kt`)를 각 액티비티 `onCreate`에서 호출(폭×0.80 코드 사이징).

### 8-2. 앱 정보 + 브랜드 아이콘 갤러리 (설정/메뉴)
1. **앱 정보**: 앱 아이콘 + 앱명 + 시리즈 서브타이틀 + `BuildConfig.VERSION_NAME` + 개발자(KitwLSH) + 이메일(mailto) + (AI 앱은) AI 활용 고지.
2. **브랜드 갤러리**: 카드 ① 공유 엠블럼(`ic_k_logo_3d`) ② 해당 앱 아이콘 → 탭 시 투명 다크 전체화면 뷰어.
- **KDailyUtil 레퍼런스(Compose)**: `KDailyUtil/app/src/main/java/com/kitwlshcom/kdailyutil/ui/screens/MorningBriefingSettingsScreen.kt` — `showAppInfoDialog`/`showIconGalleryDialog`/`showFullScreenIcon`.
- **KLotto645(View/XML, ✅ 구현 완료)**: 메뉴 '앱 정보' → 전용 `AboutActivity`(`KLotto645/app/src/main/java/com/kitwlshCom/klotto645/AboutActivity.kt`, AlertDialog가 아닌 별도 액티비티). 앱 소개/버전/법적 고지 + '브랜드 & 자매앱' 갤러리(패밀리 메인로고 `ic_k_logo_3d` 카드 + 앱 아이콘 카드 → 구분선 → 세로 자매앱 카드, 레지스트리 기반 유동 확장). 버전은 이미 `BuildConfig.VERSION_NAME` 사용(`AboutActivity.kt:64`, `strings.xml version_label` 하드코딩 미의존). `ic_k_logo_3d.webp`·`ic_k_emblem_balls.webp` 자산 배선 완료.

---

## 9. Android 산출물 목록 & 스토어 에셋

### 인앱 리소스 (`app/src/main/res/`)
| 파일 | 밀도별 px |
| :--- | :--- |
| `mipmap-*/ic_launcher.webp` (정사각) | 48 / 72 / 96 / 144 / 192 |
| `mipmap-*/ic_launcher_round.webp` (원형) | 48 / 72 / 96 / 144 / 192 |
| `mipmap-*/ic_launcher_foreground.webp` (적응형 전경) | 108 / 162 / 216 / 324 / 432 |
| `mipmap-anydpi-v26/ic_launcher.xml` + `_round.xml` | adaptive-icon 참조 |
| `app/src/main/ic_launcher-playstore.png` | 512×512 |

> ⚠️ adaptive `ic_launcher_background.xml`은 안드로이드 기본 초록(`#3DDC84`) 템플릿이 남아있으나 **전경이 불투명**이라 안 보임(정리는 선택).

### 구글 플레이 스토어 필수 에셋
| 에셋 | 규격 |
| :--- | :--- |
| 앱 아이콘(하이레스) | **512×512** 32bit PNG, ≤1MB |
| Feature Graphic | **1024×500** PNG/JPG (정확히) |
| 스크린샷(폰) | 최소 2장, 9:16~16:9 |
| 개발자 아이콘 / 헤더 | 512×512 / 4096×2304 (앱 공통, 오리지널 엠블럼) |

---

## 10. ⚙️ 빌드/환경 주의사항

- 🔧 **JDK 버전**: 이 PC `JAVA_HOME=Java 25`. Gradle/Kotlin DSL이 "25"를 파싱 못 해 **설정단계에서 빌드 실패**(`IllegalArgumentException: 25`). → 각 앱 `gradle.properties`에 **`org.gradle.java.home=C:\Program Files\Microsoft\jdk-21.0.4.7-hotspot`** 필수(두 앱 모두 설정됨).
- ⚠️ **빌드 exit code 확인**: `./gradlew.bat ... | tail` 처럼 **파이프하면 gradle 실패가 tail의 성공(0)으로 마스킹**된다. `> log 2>&1; echo $?` 또는 redirect로 실제 결과 확인.
- ⚠️ **KLotto APK 경로**: 신선 빌드는 `app/build/outputs/apk/debug/app-debug.apk`. `intermediates/apk/debug/`의 것은 오래되거나 test-only(설치 시 `-t` 필요)일 수 있음.
- ⚠️ **출시앱 반영**: KDailyUtil·KLotto645 모두 스토어 출시본이라, 아이콘/스플래시 변경을 배포하려면 **versionCode 상향** 필요.

---

## 11. 변경 이력 (요약)

| 날짜 | 내용 |
| :--- | :--- |
| 2026-07-09 | KLotto645 스플래시를 자체 아이콘(엠블럼+645볼)으로 통일 |
| 2026-07-15 | KDailyUtil 스플래시를 자체 앱아이콘으로 교체 |
| **2026-07-16** | **패밀리 전면 통일**: ①"7시 존" 배치규칙 확정(§3) ②스플래시/런처 크기공식 확정(§2, 엠블럼 스플래시100%·런처0.80, 부주제 0.393) ③KDailyUtil 아이콘을 [엠블럼+톱니 분리합성] 파이프라인으로 재구성(스플래시·런처·워터마크·갤러리 전부) ④두 앱 런처를 KLotto 레퍼런스(§2-3)에 픽셀 일치 ⑤워터마크를 스플래시 크기(폭0.80 상대비율)로 통일 ⑥KLotto JDK21 빌드설정·워터마크 코드사이징 추가 ⑦본 문서 단일기준으로 재정리 |
| **2026-07-24** | §8-2 KLotto645 항목 사실 정정: '미구현'→**구현 완료**(전용 `AboutActivity`+브랜드/자매앱 갤러리, `BuildConfig.VERSION_NAME` 사용, `ic_k_logo_3d`·`ic_k_emblem_balls` 배선). 정본을 실제 코드와 일치시킴. (KDailyUtil/doc 사본 동기화 완료 — KDaily `2dddd23`) |
| **2026-07-27** | **양쪽 사본 링크 정합**: 상대경로 마크다운 링크 5개가 저장소 깊이 차이로 반대편 사본에서 깨지던 문제 해소 → 모든 파일 참조를 **저장소 접두 평문 경로**로 통일(§7·§8-1·§8-2 + 헤더 BRANDING_GUIDE·§2-2 스크립트 2건). 헤더에 동기화·경로 표기 규약 명시. |

> 실기기(R3CX307AQVK)에서 두 앱 스플래시·런처·워터마크 확인 완료. (배포는 versionCode 상향 후)
