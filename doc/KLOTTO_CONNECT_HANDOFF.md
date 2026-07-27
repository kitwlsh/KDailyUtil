# KDailyUtil ↔ KLotto645 상호 연결 작업 핸드오프

> 작성: KLotto645 Claude 세션(2026-07-20). KDailyUtil Claude 세션에 붙여넣어 이어받기 위한 자기완결형 문서.
> KDailyUtil 세션은 KLotto645 코드에 접근할 수 없으므로, 필요한 KLotto 정보(패키지명·브랜드 자산·문구·현황)를 모두 이 문서에 담았다.
>
> 🔁 **양쪽 저장소 동기화 문서**: 이 파일은 **`KDailyUtil/doc/`와 `KLotto645/doc/`에 동일 사본**으로 유지한다.
> 한쪽에서 §5~§7(교환 현황·회신·표준)을 갱신하면 **반대쪽 사본도 같은 내용으로 맞춘다.** (각 앱 세션은 자기 폴더의 사본을 진입점으로 삼음)
> ⚠️ 그러므로 **파일 참조는 상대경로 마크다운 링크가 아니라 저장소 접두 평문 경로**(`` `KLotto645/app/src/...` ``)로 쓴다.
> 두 저장소의 깊이가 달라(`80_Git_HUB/KDailyUtil/KDailyUtil/doc` vs `80_Git_HUB/KLotto645/doc`) 어떤 상대경로도 한쪽 사본에서는 깨진다.

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

> 로컬 저장소 루트: `d:/DATA/20_Source/80_Git_HUB/`(하위에 `KDailyUtil/KDailyUtil`, `KLotto645`).
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
| KLotto645 | `com.kitwlshCom.klotto645` | `ic_k_emblem_balls.png` | 로또 6/45 분석·생성 | `com.kitwlshCom.klotto645` | **라이브 v1.0.0(vc10, 2026-07-23)** · v1.0.1(vc11) 심사 중(2026-07-24 업로드) |
| K장부 | `com.kitwlshcom.kjangbu`(예정) | `ic_kjangbu.png` | AI 생활 기록·관리 장부(가계부·차계부·케어) | `com.kitwlshcom.kjangbu` | **기획·아이콘 준비**(미출시 → 타 앱 카드 '출시예정' 비활성). KDaily에 `ic_kjangbu.png` 스테이징 완료 |
| _(신규앱)_ | _(applicationId)_ | _(아이콘.png)_ | _(소개)_ | _(id 값)_ | _(예정/출시)_ |

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

## 8. 동적 레지스트리 (원격 구성) — 정적→동적 진화 설계 (2026-07-23)

> ⚠️ **설계 문서(미구현).** §7은 **현재의 정적(하드코딩)** 방식이고, §8은 그 한계를 없애는 **진화 방향**이다. 실제 전환은 각 앱의 **일회성 수정+배포**가 필요.

### 8-1. 왜 (현재 §7 정적 방식의 한계)
§7-1 레지스트리(패키지명·아이콘·소개·카드)가 **각 앱에 하드코딩** → 신규 자매앱 하나 추가에 **기존 모든 앱을 수정 + versionCode 상향 + 스토어 재심사**. 출시앱엔 매번 부담·지연.

### 8-2. 해결 — GitHub raw `family.json`을 런타임 로드
`korean_quiz_data`를 raw로 받는 **기존 패턴과 동일**. 자매앱 목록을 원격 JSON에 두고 각 앱이 실행 시 받아 카드를 **동적 렌더** → **새 앱 = JSON 한 줄 편집 → 전 앱 즉시 반영(재빌드 X).**
- 호스팅(권장): `https://raw.githubusercontent.com/kitwlsh/k-series-config/main/family.json` (전용 소형 레포). 무료·서버 0원.
- 아이콘도 URL로 두고 Coil(KDaily 사용 중)/Glide로 로드 → **형제 PNG를 각 앱에 번들할 필요 없음**.
- 샘플: `KDailyUtil/doc/family_config/family.sample.json`

### 8-3. 스키마 (`family.json`)
| 필드 | 뜻 |
|---|---|
| `version` / `updatedAt` | 스키마·갱신 관리 |
| `apps[].id` | applicationId (openAppOrStore 인자·자기제외 판정) |
| `apps[].name` / `tagline` | 카드 제목 / 한줄 소개 |
| `apps[].iconUrl` | 아이콘 이미지 URL (raw) |
| `apps[].storeUrl` | Play 스토어 URL (도메인 화이트리스트만) |
| `apps[].active` | 노출 여부(미출시=false로 숨김/‘출시예정’) |
| `apps[].order` | 정렬 순서 |

### 8-4. 클라이언트 동작
`fetch` → 실패 시 **last-good 캐시** → 그래도 없으면 **번들 기본 family.json** → `active` 필터 → `order` 정렬 → **자기 자신(`id`==자기 applicationId) 제외** → 카드 렌더. 아이콘=`iconUrl` 로드(캐시). 카드 탭 = `openAppOrStore(id)`(§2). (오프라인·첫 실행 대비 캐시/폴백은 KDaily 퀴즈 캐시 패턴과 동일.)

### 8-5. ⚠️ 유일한 컴파일타임 제약 — `<queries>`
Android 11+는 **설치감지(`getLaunchIntentForPackage`)/직접 실행**에 대상 패키지를 `<queries>`에 선언해야 함 → **원격으로 못 바꿈.**

| 항목 | 원격구성 동적? |
|---|:--:|
| 카드 노출/추가/삭제·이름·소개·순서·아이콘 | ✅ |
| 스토어로 이동(설치 유도) | ✅ (URL 열기는 `<queries>` 불필요) |
| **설치됨 배지 / 앱 직접 실행** | ❌ 신규 미등록 패키지는 `<queries>` 필요 |

**완화책**
1. **예약 패키지 사전 등록** ⭐: 각 앱 `<queries>`에 예정 패키지 + **여유분**을 미리 선언 → 그 ID로 출시하는 미래 앱은 **재빌드 없이 설치감지·실행까지** 지원.
   - 권장 초기 예약: `com.kitwlshcom.kjangbu`, `com.kitwlshcom.kunbok`, (여유) `com.kitwlshcom.k…` ×수 개.
2. **우아한 폴백**: 미등록 패키지는 설치감지 없이 **스토어 이동만**(기능적으로 충분).
3. `QUERY_ALL_PACKAGES` **금지**(Google Play 강한 제한·반려 위험).

### 8-6. 마이그레이션 (일회성)
각 앱을 **"정적 카드 → 원격 렌더"로 1회 전환**(이 전환은 재빌드+배포 필요) + **번들 기본 family.json** 포함. 이후 이 목적의 **강제 배포는 없음**(새 앱은 JSON만 수정).

### 8-7. 보안/신뢰
- **자기 소유 레포만** 사용. JSON은 **표시용 데이터(패키지 id·URL·라벨)만** — 임의 인텐트/딥링크 주입 금지.
- `storeUrl`은 `play.google.com`/`market://` **도메인 화이트리스트**만 허용해 파싱.
- `id`는 `openAppOrStore`(런처 인텐트/마켓 URL)에만 사용. 그 외 실행 금지.

### 8-8. 언제 전환?
- 지금(앱 2~3개): §7 정적으로도 감당 가능.
- **앱이 계속 늘 계획이면 §8 전환 이득이 큼.** K장부·K운복 편입을 계기로 전환 검토 권장.

---

*이 문서는 KLotto645 세션이 작성, KDailyUtil 세션(2026-07-20)이 검토·회신(§6) + 신규앱 확장 표준(§7) 추가. 2026-07-23 KDailyUtil 세션이 **동적 레지스트리(원격 구성) 설계 §8** 추가. 이후 신규 자매앱은 §7 절차로 편입(또는 §8 전환 후 JSON 편입)한다. 이 문서는 KDailyUtil/doc 와 KLotto645/doc 에 동일 사본으로 유지한다.*
