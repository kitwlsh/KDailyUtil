# KDailyUtil ↔ KLotto645 상호 연결 작업 핸드오프

> 작성: KLotto645 Claude 세션(2026-07-20). KDailyUtil Claude 세션에 붙여넣어 이어받기 위한 자기완결형 문서.
> KDailyUtil 세션은 KLotto645 코드에 접근할 수 없으므로, 필요한 KLotto 정보(패키지명·브랜드 자산·문구·현황)를 모두 이 문서에 담았다.
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

## 0. 목표

두 자매앱(**KLotto645** ↔ **KDailyUtil**)의 "앱 정보 > 브랜드 아이콘 갤러리"에서
**상대 앱 아이콘을 누르면 Play 스토어(또는 실행)로 이동**하는 상호 유도 기능을 넣는다.

- KDailyUtil 갤러리에 **KLotto645 카드** 추가 → 누르면 KLotto645 설치/실행
- KLotto645 갤러리에 **KDailyUtil 카드** 추가 → 누르면 KDailyUtil 설치/실행
- 겸사겸사 KLotto645에는 아직 없는 "앱 정보"(소개/버전/법적 고지) 화면 자체도 신설(KLotto 세션 담당).

기술 스택이 서로 달라(**KDailyUtil = Jetpack Compose**, **KLotto645 = XML/View**) 코드 공유는 불가.
각 앱이 자기 스택으로 각자 구현하고, **교환할 것은 (a) 패키지명 (b) 브랜드 아이콘 이미지 파일** 두 가지뿐이다.

> 📌 **이 기능은 2개 앱만의 일회성이 아니라 K-시리즈 자매앱 전체에 적용되는 표준이다.**
> 앱이 3개·4개로 늘어도 동일 규칙으로 확장한다 → 신규 앱 추가 절차는 아래 **§7** 참조.
> (브랜드 아이콘 자체의 규격/통일은 [K_SERIES_ICON_RECIPE.md](K_SERIES_ICON_RECIPE.md)가 담당. 이 문서는 '앱 간 연결'을 담당.)

---

## 1. 두 앱 현황 (조사 완료)

### KDailyUtil (이 프로젝트 — Compose)
- "앱정보"는 설정 화면 서브탭(`ScrollableTabRow` index 4)에 이미 존재:
  `app/src/main/java/com/kitwlshcom/kdailyutil/ui/screens/MorningBriefingSettingsScreen.kt`
  - 앱정보 탭 본문: `MorningBriefingSettingsScreen.kt:715-809`
  - 앱 소개/버전 다이얼로그(`showAppInfoDialog`): `:911-1029`
  - **브랜드 아이콘 갤러리 다이얼로그(`showIconGalleryDialog`): `:1031-1135`** ← 여기에 KLotto 카드 추가
  - 법적 고지 다이얼로그(`showLegalNoticeDialog`): `:1137-1210`
  - 전체화면 아이콘 미리보기(`showFullScreenIcon`): `:1212-1255`
- 현재 갤러리는 **자기 앱 아이콘 2종만 전시**(클릭 시 전체화면 미리보기), 스토어/설치 링크 **없음**:
  - `R.drawable.ic_k_logo_3d` — "K-Brand 3D Hexagon Emblem" (`:1064-1091`)
  - `R.drawable.ic_k_app_icon` — "KDailyUtil 앱 아이콘" (`:1093-1120`)
- 버전 표기: `PackageManager.getPackageInfo(...).versionName` 방식 (`:35-42`)
- 외부 링크는 `uriHandler.openUri(...)`/`Intent.ACTION_VIEW`로 URL만 열고, **패키지 기반 스토어 이동/미설치 폴백 로직은 없음** → 신규 구현 필요.
- 문자열 전부 하드코딩(strings.xml엔 `app_name`만).

### KLotto645 (상대 앱 — XML/View, 이미 Play 스토어 출시됨)
- **applicationId(패키지명): `com.kitwlshCom.klotto645`** ← ⚠️ 중간 `C` 대문자 주의
- versionName `0.0.8`, versionCode `8`, minSdk 26 / target 35
- 브랜드 엠블럼: `ic_k_emblem_balls.png`(골드 육각 K + 645 볼). 스플래시/워터마크에 사용.
- "앱 정보" 화면은 **아직 없음** → KLotto 세션에서 신설 예정(AboutActivity 등).
- 법적 고지 문구는 이미 보유(다이얼로그로 앱 시작 시 표시).

---

## 2. 상호 설치 링크 사양 (양쪽 공통)

### 패키지명 (링크에 박을 값)
| 앱 | applicationId | Play 스토어 URL |
|---|---|---|
| KLotto645 | `com.kitwlshCom.klotto645` | `https://play.google.com/store/apps/details?id=com.kitwlshCom.klotto645` |
| KDailyUtil | `com.kitwlshcom.kdailyutil` (소문자 c로 확인됨) | 출시 여부 확인 필요 |

> ⚠️ **표기 불일치 주의**: KLotto=`kitwlshCom`(대문자 C), KDailyUtil=`kitwlshcom`(소문자 c).
> applicationId는 대소문자를 구분하므로 **반드시 실제 Play 콘솔/스토어 등록값을 그대로** 사용할 것.
> KLotto645는 이미 출시됨(위 URL 유효 예상, 실제 리스팅으로 최종 확인 권장).
> KDailyUtil이 아직 미출시면 KLotto 쪽 카드는 "출시 예정"으로 비활성 처리하거나, 출시 후 활성화.

### 스토어 이동 (Compose — KDailyUtil용)
```kotlin
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri

/** 설치돼 있으면 앱 실행, 없으면 마켓(→브라우저 폴백)으로 유도 */
fun openAppOrStore(context: android.content.Context, pkg: String) {
    val launch = context.packageManager.getLaunchIntentForPackage(pkg)
    if (launch != null) { context.startActivity(launch); return }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri())
        )
    }
}
```

---

## 3. KDailyUtil 측 작업 항목 — ✅ 구현 완료(2026-07-20, 커밋 `9fc3c37`)

> ⚠️ **줄번호는 참고용(수시로 밀림)**: 심볼명(`showIconGalleryDialog` 다이얼로그 블록, `openAppOrStore()`, `R.drawable.ic_klotto645`)으로 위치를 찾을 것.

1. ✅ **KLotto645 아이콘 리소스 추가** — KLotto 원본 `ic_k_emblem_balls.png` → KDailyUtil `app/src/main/res/drawable-nodpi/ic_klotto645.png` 복사(코드에서 `R.drawable.ic_klotto645`).
2. ✅ **갤러리에 KLotto645 카드 추가** — `showIconGalleryDialog { ... }` 블록에 3번째 `Card`(이미지 `ic_klotto645`, 라벨 "KLotto645 — 로또 6/45 분석·생성"). 탭 시 **`openAppOrStore(context, "com.kitwlshCom.klotto645")`**(파일 하단 top-level 헬퍼 신설). **설치 상태 배지**: 설치됨=▶ 열기 / 미설치=⬇ 설치하기(`getLaunchIntentForPackage != null` 판정).
3. ✅ **자기 아이콘 카드는 현행 유지**(미리보기). 자매 카드만 스토어/실행으로 분기. 갤러리 안내 문구도 이에 맞게 수정.
   - ✅ **화면 개칭·구획 분리(커밋 `d595c9f`)**: '브랜드 아이콘 갤러리' → **'브랜드 & 자매앱'**, 자기 로고와 자매앱 사이에 구분선 + 소제목 '🧩 K-시리즈 자매앱' 추가(§7-3 명명 규칙). KLotto/신규앱도 동일 적용 권장.
4. (미적용) 문자열 하드코딩 → `strings.xml` 추출: 원 프로젝트 관례상 현재 하드코딩이라 보류(추후 여력 시).

## 4. KLotto645 측 작업 항목 — ✅ 구현 완료(2026-07-20)

- ✅ 신규 `AboutActivity`(HelpActivity 동형: 헤더+NestedScroll+FAB) 신설. 메뉴 버튼행에 '앱 정보' 버튼 추가해 진입.
  앱 소개 + 버전(`BuildConfig.VERSION_NAME` 단일 소스) + 법적 고지(기존 `legal_notice_*` 다이얼로그 재사용) 구성.
- ✅ **'브랜드 & 자매앱' 갤러리(KDailyUtil과 동일 구성, §7-3 명명 규칙 적용)**:
  - 자체 로고 영역 = **패밀리 메인로고 카드(`ic_k_logo_3d`, KDailyUtil에서 수신)** + **앱 아이콘 카드(`ic_k_emblem_balls`)**. 둘 다 탭 시 미리보기.
  - 구분선 + 소제목 '🧩 K-시리즈 자매앱' 으로 구획 분리.
  - 자매앱 영역 = **세로 1칸 1개 카드**를 `AboutActivity`의 `SIBLINGS` 레지스트리를 돌며 `item_sibling_app.xml`로 반복 인플레이트 → **신규앱은 레지스트리 한 줄만 추가**(유동 확장). 카드 탭 = `AppLinkUtil.openAppOrStore(ctx, pkg)`, 배지 = 설치됨(▶ 열기)/미설치(⬇ 설치하기), `onResume` 재판정.
- ✅ `util/AppLinkUtil`(openAppOrStore/isInstalled) 신설 + `AndroidManifest`에 `<queries>`로 `com.kitwlshcom.kdailyutil` 선언(Android 11+ 가시성). JDK21 빌드 통과.
- ✅ **아이콘 수신**: `ic_kdailyutil.png`(원본 KDailyUtil `ic_k_app_icon.png`) + **패밀리 메인로고 `ic_k_logo_3d.png`**(원본 KDailyUtil `drawable/ic_k_logo_3d.png`). 둘 다 `drawable-nodpi/`.
- 채택한 UX(§6 권장안 그대로): 자매앱 카드=즉시 스토어/실행, 설치 배지 표시.
- ⏳ 남은 것: **실기기에서 시각 확인 + 실제 스토어 이동/설치 배지 동작 확인**.

## 5. 서로 주고받을 리소스 (교환 목록) — ✅ 양방향 전달 완료(2026-07-20)

**교환 규칙**: 각 앱은 상대 앱 아이콘을 `ic_<상대앱>.png` 이름으로 자기 `drawable-nodpi/`에 보관한다.

| 방향 | 원본 파일 · 경로 | 수신 측 저장명 · 경로 | 상태 |
|---|---|---|---|
| KLotto → KDailyUtil | `ic_k_emblem_balls.png` · `KLotto645/app/src/main/res/drawable-nodpi/` | `ic_klotto645.png` · `KDailyUtil/app/src/main/res/drawable-nodpi/` | ✅ 완료 |
| KDailyUtil → KLotto | `ic_k_app_icon.png` · `KDailyUtil/app/src/main/res/drawable-nodpi/` | `ic_kdailyutil.png` · `KLotto645/app/src/main/res/drawable-nodpi/` | ✅ 완료 |
| KDailyUtil → KLotto (패밀리 메인로고) | `ic_k_logo_3d.png` · `KDailyUtil/app/src/main/res/drawable/` | `ic_k_logo_3d.png` · `KLotto645/app/src/main/res/drawable-nodpi/` | ✅ 완료 |

> 로컬 배치: 형제 저장소는 **같은 상위 폴더에 나란히** 둔다 — `<루트>/<프로젝트>/main`(`KLotto645/main` · `KDailyUtil/main` · `KJangbu/main` · 정본 `k-series-config/main`), 키·비밀은 `<루트>/_secrets/<프로젝트>/`. **절대경로는 적지 않는다**(또 옮기면 이 문서부터 거짓이 된다).
> **패밀리 메인로고 `ic_k_logo_3d.png`**(K-Brand 3D Hexagon Emblem)는 앱별 아이콘과 별개로 **모든 자매앱이 공유하는 공통 브랜드 자산**이다. 각 앱의 '브랜드 & 자매앱' 최상단 카드로 동일하게 노출한다(파일명도 동일 유지).

## 6. KDailyUtil 세션이 확정할 결정사항

1. KDailyUtil의 **Play 스토어 출시 여부/정확한 applicationId** — 링크 활성화 가능 시점 결정.
2. 상대 카드 UX — 즉시 스토어 이동 vs "미리보기 + 설치 버튼" 중 무엇으로 할지.
3. 설치됨 상태 배지 표시 여부.
4. KLotto에 넘겨줄 아이콘 파일 확정(위 표).

### KDailyUtil 세션 회신 (2026-07-20)
1. **applicationId = `com.kitwlshcom.kdailyutil`(소문자 c) 확정**(build.gradle.kts에서 확인). **KDailyUtil은 v1.1(versionCode 2)로 스토어 출시됨** → 다른 앱의 KDailyUtil 카드 **링크 활성화 가능**(실제 리스팅 최종 확인은 권장). Play URL: `https://play.google.com/store/apps/details?id=com.kitwlshcom.kdailyutil`.
2. ✅ **확정·적용**: 자매앱 카드는 **즉시 `openAppOrStore`**(미리보기 없음), 자기 앱 카드는 현행 미리보기 유지.
3. ✅ **확정·적용**: **설치됨(▶ 열기)/미설치(⬇ 설치하기) 배지 표시**(`getLaunchIntentForPackage != null`).
4. ✅ **확정**: KLotto에 넘긴 아이콘 = `ic_k_app_icon.png` → KLotto엔 `ic_kdailyutil.png`로 저장(§5).
> 갤러리 다이얼로그 실제 위치(2026-07-20): `showIconGalleryDialog { ... }` 블록(자기 아이콘 2종: `ic_k_logo_3d`·`ic_k_app_icon`). 여기 3번째 카드로 자매앱을 추가.

---

## 7. 신규 자매앱(K-시리즈) 추가 시 표준 (확장 규칙)

이 상호연결은 **K-시리즈 자매앱 전체에 적용되는 표준**이다. 앱이 3개·4개로 늘어도 아래 규칙으로 확장한다.

### 7-1. 자매앱 레지스트리 (단일 관리 목록)
새 앱을 추가하거나 정보가 바뀌면 **이 표를 먼저 갱신**하고, 각 앱은 이 표를 기준으로 자기 갤러리 카드를 만든다.

| 앱 | applicationId | 브랜드 아이콘 파일 | 한줄 소개 | Play 스토어(id=) | 출시상태 |
|---|---|---|---|---|---|
| KDailyUtil | `com.kitwlshcom.kdailyutil` | `ic_k_app_icon.png` | 데일리 라이프 유틸(뉴스/증시/오디오/퀴즈/배움터) | `com.kitwlshcom.kdailyutil` | **출시 v1.5(vc5, 2026-07-23)** (이전 v1.4/vc4 07-21) |
| KLotto645 | `com.kitwlshCom.klotto645` | `ic_k_emblem_balls.png` | 로또 6/45 분석·생성 | `com.kitwlshCom.klotto645` | **라이브 v1.0.3(vc13, 2026-08-04 출시)** — §8 동적 레지스트리 적용본은 v1.0.2부터 |
| K장부 | `com.kitwlshcom.kjangbu` | `ic_kjangbu.png` | AI 생활 기록·관리 장부(가계부·차계부·케어) | `com.kitwlshcom.kjangbu` | **라이브 v1.0.0(vc1, 2026-08-10 출시)** — 레지스트리 `comingSoon:false` 전환 완료 |
| _(신규앱)_ | _(applicationId)_ | _(아이콘.png)_ | _(소개)_ | _(id 값)_ | _(예정/출시)_ |

> 🎯 **2026-07-29부터 이 표는 "사람이 읽는 단일 관리 목록"이고, 앱이 실제로 읽는 것은 원격 `family.json`(§8)이다.** KDailyUtil은 이미 동적 렌더로 전환됐으므로, **표 갱신 → `family.json` 갱신** 두 곳만 맞추면 앱 수정·재배포는 필요 없다(§8-9). 아직 전환하지 않은 앱(KLotto645·K장부)은 §8-10 체크리스트로 이식할 때까지 §7-2 정적 절차를 따른다.

> ⚠️ applicationId는 **대소문자 그대로** 사용(KLotto=`kitwlshCom` 대문자 C, KDailyUtil=`kitwlshcom` 소문자 c).
> '브랜드 아이콘 파일'은 각 앱이 **내보내는(export) 자기 원본** 파일명이다. **받는 앱은 `ic_<상대앱>.png`로 저장**한다(예: KDailyUtil엔 `ic_klotto645.png`, KLotto엔 `ic_kdailyutil.png`). 교환 현황은 §5.

### 7-2. 신규 앱 N을 추가할 때 — 양방향 체크리스트
1. **레지스트리 등록**: 7-1 표에 N 행 추가(패키지명·아이콘·소개·출시상태).
2. **기존 모든 앱 → N 카드 추가**: 기존 각 앱의 브랜드 아이콘 갤러리에 N 카드를 넣고 `openAppOrStore(ctx, "N.applicationId")` 연결. (N의 아이콘 파일을 각 앱 리소스에 추가)
3. **N → 기존 모든 앱 카드 구현**: N 앱 자체에도 "앱 정보 > 브랜드 아이콘 갤러리"를 두고, 레지스트리의 **다른 모든 자매앱 카드**를 넣어 `openAppOrStore`로 연결(N은 자기 스택으로 §2 로직 등가 구현).
4. **아이콘 교환(§5)**: N은 자기 브랜드 아이콘을 모든 자매앱에 제공하고, 모든 자매앱의 아이콘을 받아 N 리소스에 추가.
5. **미출시 앱 처리**: N이 아직 스토어 미출시면 다른 앱의 N 카드는 **"출시 예정"으로 비활성/숨김** 후, 출시되면 활성화.

### 7-3. 공통 규칙
- **자기 앱 카드 = 미리보기(현행)**, **자매앱 카드 = `openAppOrStore`(스토어/실행)**. 설치 여부 배지(설치됨/설치하기)는 `getLaunchIntentForPackage != null`로 판정(선택).
- **화면/섹션 명명 규칙(2026-07-20 확정)**: 자매앱을 '브랜드 아이콘 갤러리' 같은 로고 감상용 화면에 그냥 섞지 말 것. 화면 이름은 **'브랜드 & 자매앱'** 처럼 자매앱을 포함함을 드러내고, **자기 로고 영역과 자매앱 영역을 구분선 + 소제목('🧩 K-시리즈 자매앱')으로 분리**해 동작(미리보기 vs 스토어 이동) 차이를 시각적으로 구분한다. (KDailyUtil 적용 완료, KLotto/신규앱 동일 적용)
- 아이콘 규격/패밀리 통일은 [K_SERIES_ICON_RECIPE.md](K_SERIES_ICON_RECIPE.md)를 따른다(그 문서=아이콘 규격, 이 문서=앱 간 연결).
- 코드 공유 불가(스택 상이): 교환 대상은 **(a) applicationId (b) 브랜드 아이콘 파일** 두 가지뿐(§2·§5).
- 새 앱이 늘어도 `openAppOrStore(context, pkg)`(§2) **하나의 헬퍼로 N개 카드 모두 처리** — 카드마다 패키지명만 바꿔 재사용.

---

## 8. 동적 레지스트리 (원격 구성) — ✅ 전 앱 구현 완료 (설계 2026-07-23 / 구현 2026-07-29)

> ✅ **3개 앱 전부 구현 완료(2026-07-29)** — KDailyUtil·KLotto645·K장부의 자매앱 카드가 모두 **하드코딩이 아니라 원격 `family.json`으로 동적 렌더**된다. 호스팅 레포(`kitwlsh/k-series-config`)도 생성·업로드 완료(§8-11).
> ✅ **2026-08-10 — 이 표준의 효과가 실증됐다.** K장부 출시에 맞춰 `family.json`의 `comingSoon`을
> **한 줄 바꾼 것만으로** KLotto645·KDailyUtil에서 K장부가 '출시 예정'에서 활성 카드로 바뀌었다.
> **어느 앱도 재배포하지 않았다** — §8을 만든 이유가 그대로 확인된 첫 사례다.
>
> ⚠️ 단, 이 전환은 **각 앱이 한 번 배포돼야 효력이 생긴다**(KDailyUtil vc6/v1.6, **KLotto645 vc12/v1.0.2 — ✅ 2026-08-03 배포 완료**, K장부는 첫 출시본부터). 그 이후로는 **신규 자매앱 추가에 어떤 앱도 재배포하지 않는다**(§8-9).

### 8-1. 왜 (현재 §7 정적 방식의 한계)
§7-1 레지스트리(패키지명·아이콘·소개·카드)가 **각 앱에 하드코딩** → 신규 자매앱 하나 추가에 **기존 모든 앱을 수정 + versionCode 상향 + 스토어 재심사**. 출시앱엔 매번 부담·지연.

### 8-2. 해결 — GitHub raw `family.json`을 런타임 로드
`korean_quiz_data`를 raw로 받는 **기존 패턴과 동일**. 자매앱 목록을 원격 JSON에 두고 각 앱이 실행 시 받아 카드를 **동적 렌더** → **새 앱 = JSON 한 줄 편집 → 전 앱 즉시 반영(재빌드 X).**
- **호스팅(확정, 2026-07-29)**: `https://raw.githubusercontent.com/kitwlsh/k-series-config/main/family.json` (전용 소형 레포). 무료·서버 0원. **이 URL이 모든 앱에 컴파일타임 상수로 박히므로 변경 금지**(변경 시 전 앱 재배포).
- 아이콘도 URL로 두고 Coil(KDaily 사용 중)/Glide로 로드 → **형제 PNG를 각 앱에 번들할 필요 없음**(단, 오프라인 폴백용으로 아는 앱 아이콘은 번들 유지).
- **정본 파일**: `KDailyUtil/doc/family_config/family.json` + `icons/*.png`(384², 업로드용). 레포 준비 절차 = §8-11.

### 8-3. 스키마 (`family.json`)
| 필드 | 뜻 |
|---|---|
| `version` / `updatedAt` | 스키마·갱신 관리 (모르는 상위 버전이 와도 아는 필드만 읽고 계속 동작) |
| `apps[].id` | applicationId (openAppOrStore 인자·자기제외 판정). **대소문자 그대로** |
| `apps[].name` / `tagline` | 카드 제목 / 한줄 소개 |
| `apps[].iconUrl` | 아이콘 이미지 URL — **https + `githubusercontent.com`/`github.io`만 허용** |
| `apps[].storeUrl` | Play 스토어 URL — **`https://play.google.com/`·`market://`만 허용** |
| `apps[].active` | 노출 여부. **false = 카드 완전 숨김**(기획 단계 앱) |
| `apps[].comingSoon` | **true = 카드는 보이되 '🔜 출시 예정' 비활성**(미출시 앱). 구현 시 추가된 필드(2026-07-29) |
| `apps[].order` | 정렬 순서(작은 값 먼저) |
| `_`로 시작하는 키 | 주석용. 클라이언트는 무시 |

> 💡 **미출시 앱 2단계 표현**: 기획 단계 = `active:false`(숨김) → 개발·출시 임박 = `active:true, comingSoon:true`(출시예정 카드) → 출시 = `comingSoon:false`. **세 상태 전환 모두 JSON만 고치면 되고 앱 재배포는 없다.**

### 8-4. 클라이언트 동작
**신선한 캐시(6h 이내)** → `fetch` → 실패 시 **last-good 캐시(오래돼도)** → 그래도 없으면 **번들 기본 `family.json`** → `active` 필터 → **자기 자신(`id`==자기 applicationId) 제외** → `order` 정렬 → 상한(20개) → 카드 렌더. 아이콘=`iconUrl` 로드(Coil 캐시), 실패 시 번들 폴백. 카드 탭 = `openAppOrStore(id, storeUrl)`(§2). (오프라인·첫 실행 대비 캐시/폴백은 KDaily 퀴즈 캐시 패턴과 동일.)
- **내구성**: 항목 1건이 깨져도 나머지는 살린다(항목별 try/catch). 부적격 `id`(패키지명 형식 아님)·화이트리스트 밖 URL은 조용히 버린다.
- **강제 새로고침**: '브랜드 & 자매앱' 자매앱 섹션의 🔄 버튼 = 캐시 무시 재조회(JSON 수정이 반영됐는지 즉시 확인용).

### 8-5. ⚠️ 유일한 컴파일타임 제약 — `<queries>`
Android 11+는 **설치감지(`getLaunchIntentForPackage`)/직접 실행**에 대상 패키지를 `<queries>`에 선언해야 함 → **원격으로 못 바꿈.**

| 항목 | 원격구성 동적? |
|---|:--:|
| 카드 노출/추가/삭제·이름·소개·순서·아이콘 | ✅ |
| 스토어로 이동(설치 유도) | ✅ (URL 열기는 `<queries>` 불필요) |
| **설치됨 배지 / 앱 직접 실행** | ❌ 신규 미등록 패키지는 `<queries>` 필요 |

**완화책**
1. **예약 패키지 사전 등록** ⭐: 각 앱 `<queries>`에 예정 패키지 + **여유분**을 미리 선언 → 그 ID로 출시하는 미래 앱은 **재빌드 없이 설치감지·실행까지** 지원.
   - **확정 예약 목록(2026-07-29, 전 앱 공통)** — 신규 앱은 **이 id를 우선 사용**할 것:
     ```
     com.kitwlshCom.klotto645   com.kitwlshcom.kjangbu   com.kitwlshcom.kunbok
     com.kitwlshcom.kfamily1 ~ com.kitwlshcom.kfamily5   (여유 5개)
     ```
2. **우아한 폴백**: 미등록 패키지는 설치감지 없이 **스토어 이동만**(기능적으로 충분). 카드 노출·아이콘·소개는 정상.
3. `QUERY_ALL_PACKAGES` **금지**(Google Play 강한 제한·반려 위험).

### 8-6. 마이그레이션 (일회성) — 진행 현황
각 앱을 **"정적 카드 → 원격 렌더"로 1회 전환**(이 전환은 재빌드+배포 필요) + **번들 기본 family.json** 포함. 이후 이 목적의 **강제 배포는 없음**(새 앱은 JSON만 수정).

| 앱 | 전환 상태 | 다음 배포 |
|---|---|---|
| **KDailyUtil** | ✅ **구현 완료(2026-07-29)** — Compose + Coil. **실기기 검증 10/10 통과(§8-12)** | vc6 / v1.6 |
| **KLotto645** | ✅ **구현 + 🚀 배포 완료(2026-08-03)** — XML/View. 이미지 라이브러리가 없어 `RemoteIconCache`(디스크 캐시 + `BitmapFactory`)를 직접 구현, **의존성 추가 없음**. 실기기/에뮬레이터 **T1~T10 전 항목 검증 통과** | **vc12 / v1.0.2 (라이브)** |
| **K장부** | ✅ **구현 + 🚀 출시 완료(2026-08-10)** — Compose + Coil. **첫 출시본(vc1)부터 동적**이라 전환용 추가 배포가 애초에 없었다 | **vc1 / v1.0.0 (라이브)** |

> ⚠️ **전환은 배포돼야 효력이 생긴다.** **KLotto645는 v1.0.2(2026-08-03)로 배포 완료** — 업데이트를 받은 사용자부터 동적 카드가 보인다. KDailyUtil v1.5 사용자에게는 다음 업데이트가 깔릴 때까지 예전 하드코딩 카드가 보인다(K장부 카드 없음). 그 이후로는 영구히 JSON만으로 관리된다.
> ✅ **호스팅 레포는 생성·업로드 완료(2026-07-29)** — `kitwlsh/k-series-config` (§8-11).

### 8-7. 보안/신뢰
- **자기 소유 레포만** 사용. JSON은 **표시용 데이터(패키지 id·URL·라벨)만** — 임의 인텐트/딥링크 주입 금지.
- `storeUrl`은 `play.google.com`/`market://` **도메인 화이트리스트**만 허용해 파싱.
- `iconUrl`은 **https + `githubusercontent.com`/`github.io`** 화이트리스트만 허용(임의 호스트로 요청 나가지 않게).
- `id`는 **패키지명 형식 검증** 후 `openAppOrStore`(런처 인텐트/마켓 URL)에만 사용. 그 외 실행 금지.
- 항목 수 **상한 20개**(레지스트리 오염 대비).

### 8-8. 언제 전환?
- 지금(앱 2~3개): §7 정적으로도 감당 가능.
- **앱이 계속 늘 계획이면 §8 전환 이득이 큼.** → **2026-07-29 K장부 출시 준비를 계기로 전환 결정·KDailyUtil 구현 완료.**

### 8-9. 🎯 신규 자매앱 추가 절차 (전환 후 — **앱 재배포 없음**)
전환이 끝난 앱들에 대해서는, 새 자매앱 N이 생겼을 때 **`family.json` 편집 + 아이콘 업로드**만 하면 된다.
1. `k-series-config` 레포에 **아이콘 업로드**: `icons/<앱>.png` (384×384 권장, PNG).
2. **`family.json`에 항목 추가**(§8-3 스키마) — `id`는 §8-5 **예약 목록에서 고르는 것을 강력 권장**(설치감지·직접실행까지 지원됨).
   - 미출시면 `active:true, comingSoon:true` → 출시되면 `comingSoon:false`로 한 글자 수정.
3. `updatedAt` 갱신 후 커밋·푸시. **끝.** 각 앱은 최대 6시간 내(또는 설정 화면 🔄 즉시) 새 카드를 표시한다.
4. **정본 동기화**: 같은 내용을 `KDailyUtil/doc/family_config/family.json`에도 반영(번들 기본값 갱신은 다음 배포 때 따라감 — 급하지 않음).
5. §7-1 레지스트리 표도 함께 갱신(문서 상 단일 관리 목록 유지).

> ⚠️ **재배포가 필요한 예외는 딱 두 가지**: ① 예약에 없는 새 패키지명의 **설치감지/직접실행**을 원할 때(`<queries>` 추가) ② 호스팅 URL 자체를 바꿀 때. 그 외(카드 추가·삭제·이름·소개·순서·아이콘·출시상태)는 전부 JSON만으로 끝난다.

### 8-10. 자매앱 이식 체크리스트 — ✅ 3개 앱 모두 완료(2026-07-29)
아래 8개 항목이 이식 규격이다. **동작 규격(§8-4)과 보안 규칙(§8-7)만 같으면 UI 구현 방식은 스택에 맞춰 자유**.

| # | 할 일 | KDailyUtil (Compose) | K장부 (Compose) | KLotto645 (XML/View) |
|---|---|---|---|---|
| 1 | 모델: id·name·tagline·iconUrl·storeUrl·comingSoon·order | `data/model/FamilyApp.kt` | `data/remote/FamilyRegistry.kt` | `util/FamilyRegistry.kt` |
| 2 | 로더: 6h 캐시 → 원격 → last-good → 번들, 검증·화이트리스트·자기제외·order정렬 | `data/repository/FamilyRepository.kt` | 〃 (`FamilyRegistry`) | 〃 (`FamilyRegistry`) |
| 3 | 번들 기본값: 정본 `family.json` 사본을 앱 리소스에 포함 | `res/raw/family.json` | `res/raw/family.json` | `res/raw/family.json` |
| 4 | `<queries>`에 **§8-5 예약 목록 전체** 선언 | ✅ | ✅ | ✅ |
| 5 | UI: 자매앱 구획을 **목록 동적 렌더**로(로딩/빈목록/출시예정 + 🔄 새로고침) | `SisterAppCard` | `SiblingCard` | `AboutActivity.loadSiblingCards` + `item_sibling_app.xml` |
| 6 | 아이콘: 원격 URL 로드 + **번들 폴백**(아는 앱은 로컬 이미지, 모르는 앱은 공통 엠블럼) | Coil `AsyncImage` | Coil `AsyncImage` | **`util/RemoteIconCache`**(자체 디스크 캐시) |
| 7 | 스토어 이동: `storeUrl`(화이트리스트) 우선 → `market://` → `https://` 폴백 | `openAppOrStore(ctx,pkg,storeUrl)` | 〃 | `AppLinkUtil.openAppOrStore(ctx,pkg,storeUrl)` |
| 8 | 자기 자신 제외는 **`id` == 자기 applicationId** 비교로(하드코딩 금지) | `BuildConfig.APPLICATION_ID` | 〃 | 〃 |

- **KLotto645는 이미지 라이브러리(Coil/Glide)가 없다** → 새 의존성을 넣지 않고 `RemoteIconCache`(cacheDir 저장 + `BitmapFactory` 디코드, **1일 TTL**, 3MB 상한, `inSampleSize`로 256px 축소 디코드)로 처리했다. 실패 시 번들 아이콘을 그대로 둔다.
- **K장부는 첫 출시본(vc1)부터 동적** — 전환용 추가 배포가 애초에 발생하지 않는다.
- 자매앱 이름·소개 문구는 이제 레지스트리에서 오므로 **각 앱의 문자열 리소스에서 제거**했다(KLotto `about_sibling_*_name/desc`).

### 8-11. 호스팅 레포 — ✅ 생성·업로드 완료 (2026-07-29)
`kitwlsh/k-series-config` (**Public**, 브랜치 **`main`**) — 라이브 확인 완료(family.json + 아이콘 3종 모두 HTTP 200).
```
k-series-config/
├── README.md            # 레포 자체 편집 규칙(고정 URL 경고·상태전환 3단계)
├── family.json          ← 정본 = KDailyUtil/doc/family_config/family.json
└── icons/
    ├── kdailyutil.png   ← 정본 = KDailyUtil/doc/family_config/icons/ (384², PNG)
    ├── klotto645.png
    └── kjangbu.png
```
- 레포명·브랜치·경로는 **각 앱에 컴파일타임 상수로 박혀 있어 변경 금지**(변경 시 전 앱 재배포).
- 검증 URL: `https://raw.githubusercontent.com/kitwlsh/k-series-config/main/family.json`
- 이후 이 레포를 고치는 것만으로 전 앱의 자매앱 카드가 바뀐다. 편집 규칙 = `doc/family_config/README.md`.
- 참고: raw 응답 헤더는 `Cache-Control: max-age=300` + `ETag`(내용 해시)다. CDN 캐시가 최대 5분이라 수정 직후 반영이 조금 늦을 수 있다(앱 캐시 6h는 🔄로 즉시 무시).
- ⚠️ **아이콘은 반드시 배경이 투명한 원본에서 만들 것.** 실제로 배경 박힌 마스터로 만들어 다크 카드에 검은 사각형이 뜬 사고가 있었다(2026-07-29, §8-12 참조).

### 8-12. 실기기 검증 체크리스트 & 진단 방법 (2026-07-29 KDailyUtil 10/10 통과)

신규 앱을 이식했거나 로더를 손댔으면 **아래 10항목**을 실기기(권장: `emulator -wipe-data` 클린 부팅)로 확인한다.
"캐시가 한 번도 없던 첫 실행"을 만들려면 **설치 직후 실행 전에** 네트워크를 끊어야 한다.

| # | 조건 | 기대 결과 |
|---|---|---|
| T1 | **캐시 없음 + 네트워크 없음**(최악) | `⚠️ 원격·캐시 모두 불가 — 번들 기본값: N개` · **카드 유지** |
| T2 | 온라인 첫 조회 | `✅ 원격 레지스트리 로드: N개` · 캐시 파일 생성(현재 3,509 bytes) |
| T3 | 6h 이내 재진입 | `🗄 신선한 캐시 사용(재조회 안 함): N개` · **캐시 mtime 불변**(네트워크 미사용) |
| T4 | 캐시를 6h 이전으로 되돌림(`touch -t`) | 원격 재조회 + 캐시 mtime 갱신 |
| T5 | 오프라인 + 정상 캐시 | `⚠️ 원격 실패 — last-good 캐시 사용: N개` · 카드 유지 |
| T6 | 오프라인 + **캐시 손상**(쓰레기 주입) | `레지스트리 파싱 실패` → `번들 기본값` · 카드 유지 |
| T7 | **악성·불량 레지스트리 주입** | 아래 필터링 로그 전부 + 최종 카드 수 = 상한 20 |
| T8 | 미설치 자매앱 카드 탭 | `com.android.vending`(Play) 실행 · 크래시 없음 |
| T9 | 원격 아이콘 수신 | 아이콘 캐시 파일 크기가 라이브 PNG와 **바이트 일치** · 배경 투명 |
| T10 | 자매앱 0개인 레지스트리 | 빈 화면이 아니라 **'표시할 자매앱이 없습니다'** 안내 |

**T7 주입 fixture에 넣을 것** — 각각 대응 로그가 떠야 한다:
`자기 자신 id`(조용히 제외) · `"not a package!!"`→`부적격 id skip` · `id 없는 항목`→`부적격 id skip: ''` ·
`active:false`(조용히 제외) · `storeUrl: https://evil.example.com/...`→`허용되지 않은 storeUrl 무시` ·
`iconUrl: http://...`→`허용되지 않은 iconUrl 무시` · `version:99`→`스키마 v99 > 지원 v1 — 아는 필드만 읽음` ·
유효 항목 21개 이상 → **20개로 잘림**.

**앱별 진단 정보** — 로그 태그와 문구는 **3개 앱 동일하게 유지**한다(한 번에 훑기 위함):

| 앱 | 로그 태그 | 레지스트리 캐시 | 아이콘 캐시 |
|---|---|---|---|
| KDailyUtil | `FamilyRepository` | `files/family_config.json` | `cache/image_cache/`(Coil) |
| K장부 | `FamilyRegistry` | `files/family_config.json` | `cache/image_cache/`(Coil) |
| KLotto645 | `FamilyRegistry` | `files/family_config.json` | `cache/family_icons/`(자체) |

**검증 시 안전 규칙**
- ⛔ `pm clear` 금지(사용자 데이터 전멸). 캐시만 지울 것: `adb shell run-as <pkg> rm files/family_config.json`
- ⛔ 스토어(Play) 설치본 위에 디버그 APK를 강제 설치하지 말 것(서명 불일치 → 삭제 필요 → 데이터 손실). `dumpsys package <pkg> | grep installerPackageName`이 `null`이면 사이드로드본이라 안전.
- fixture 주입: `adb push x //data/local/tmp/inj.json` → `chmod 644` → `run-as <pkg> cp //data/local/tmp/inj.json files/family_config.json` (주입 직후 mtime이 새것이라 원격을 타지 않고 캐시를 읽는다)
- 오프라인: `svc wifi disable` + `svc data disable` (**끝나고 반드시 원복**)
- Git Bash에서는 기기 경로를 `//data/local/tmp/...`(슬래시 2개)로 써야 MSYS 경로 변환을 피한다. 스크린샷은 `adb exec-out screencap -p > s.png`.
- Compose 다이얼로그 내부는 `uiautomator dump`에 안 잡힌다 → 스크린샷 좌표로 탭.

**검증에서 실제로 잡힌 결함(참고 사례)**: 원격 `icons/klotto645.png`를 **배경 박힌 마스터**에서 만들어, 번들 폴백(투명)보다 오히려 나쁜 **검은 사각형**이 표시됐다. 투명 원본에서 재생성해 레포에 푸시하는 것만으로 **앱 재배포 없이** 해결 — §8 설계의 첫 실전 증명이 됐다.

---

## 9. 개인정보 처리방침 — 앱별 작성 + 공용 호스팅 (2026-08-03 정리)

Play Console은 앱마다 **접근 가능한 방침 URL**을 요구한다. 자매앱이 늘어날수록 흩어지기 쉬워 여기에 표준을 고정한다.

### 9-1. 원칙
- **방침은 앱마다 따로 쓴다.** 앱이 요구하는 권한과 외부 전송 대상이 서로 달라, 한 문서를 돌려쓰면 사실과 어긋난다(= 정책 위반 소지).
- **문구는 매니페스트의 실제 권한과 일치해야 한다.** 권한을 추가·제거하면 방침도 같은 커밋에서 고친다.
- **원본은 각 앱 저장소 `doc/privacy-<앱>.html`**, 배포 사본은 `k-series-config` 레포. 둘을 함께 갱신한다.

### 9-2. 호스팅 (GitHub Pages)
`k-series-config` 레포 → Settings → Pages → Deploy from a branch → **`main` / root**.

| 앱 | Play Console 등록 URL |
|---|---|
| KDailyUtil | `https://kitwlsh.github.io/k-series-config/privacy-kdailyutil.html` |
| KLotto645 | `https://kitwlsh.github.io/k-series-config/privacy-klotto645.html` |
| K장부 | `https://kitwlsh.github.io/k-series-config/privacy-kjangbu.html` |
| 모음 페이지 | `https://kitwlsh.github.io/k-series-config/` |

> **왜 Netlify에서 옮겼나**: 기존에는 앱마다 다른 Netlify 사이트에 손으로 올려 두어(`kdailyutil-privacy.netlify.app`, `kitwlsh.netlify.app/privacy`) **저장소 원본과 배포본이 어긋났고**, 문서에 적힌 URL(`kitwlsh.netlify.app/privacy-kdailyutil`)은 404였다. git을 단일 원본으로 두면 이 어긋남이 구조적으로 사라지고, `family.json`과 동일한 흐름(push = 반영)으로 관리된다.

### 9-3. 각 앱 현황 (2026-08-03 전수 점검)
| 앱 | 실제 권한 | 방침 상태 |
|---|---|---|
| KDailyUtil | INTERNET · CAMERA · RECORD_AUDIO · 알림 등 | ✅ **음성 입력(STT)·음성 출력(TTS) 조항 추가** — 마이크는 있었으나 v1.4~v1.5에 들어간 음성 질문 처리 설명이 빠져 있었다 |
| KLotto645 | INTERNET · ACCESS_NETWORK_STATE · **CAMERA** · 알림 | ✅ **전면 재작성** — 기존 방침은 앱이 아니라 웹사이트 대상 자동생성 템플릿이라 앱 이름도 없고 카메라(QR 스캔) 설명이 없었으며 "계좌정보·게임제공" 같은 무관한 잔재가 남아 있었다 |
| K장부 | INTERNET · 알림 **뿐** | ✅ **카메라 권한 문구 수정** — 권한을 요구한다고 적혀 있었으나 실제로는 시스템 카메라 앱 호출(`TakePicture`) 방식이라 권한이 없다 |

> **Console 등록 현황(2026-08-03)**: 3개 앱 모두 위 GitHub Pages 주소로 교체 완료. 단 **공개 스토어 페이지의 방침 링크는 새 버전이 게시될 때 함께 반영**된다 — 교체 직후 스토어를 보면 옛 Netlify 주소가 남아 있는데(심사 중이라 이전 게시본의 등록정보가 노출됨) 이상이 아니다. 게시 완료 후에도 옛 주소면 그때 Console 값을 재확인할 것.

### 9-4. 신규 자매앱 추가 시
1. 해당 앱 저장소에 `doc/privacy-<앱>.html` 작성(다른 앱 문서를 복사하되 **권한·전송 대상을 실제에 맞게 전부 다시 확인**).
2. `k-series-config`에 같은 파일을 올리고 `index.html` 목록에 한 줄 추가.
3. Play Console > 앱 콘텐츠 > 개인정보처리방침에 위 URL 등록.
4. **데이터 보안(Data safety) 양식은 방침과 내용이 일치**해야 한다(전송 대상·목적·삭제 가능 여부).

---

*이 문서는 KLotto645 세션이 작성, KDailyUtil 세션(2026-07-20)이 검토·회신(§6) + 신규앱 확장 표준(§7) 추가. 2026-07-23 KDailyUtil 세션이 **동적 레지스트리(원격 구성) 설계 §8** 추가. **2026-07-29 KDailyUtil 세션이 §8을 실제 구현**하고 이식 체크리스트(§8-10)·호스팅 절차(§8-11)를 확정했다. **2026-08-03 개인정보 처리방침 표준 §9 추가**(3개 앱 방침 정비 + 공용 호스팅 이전). 이후 신규 자매앱은 §8-9(JSON 편집)로 편입하고, 아직 전환 안 한 앱은 §8-10으로 이식한다. 이 문서는 KDailyUtil/doc · KLotto645/doc · KJangbu/doc 에 동일 사본으로 유지한다.*
