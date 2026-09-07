# 📱 KDailyUtil — 프로젝트 컨텍스트 (신규 세션 필독)

> 이 폴더에서 Claude 신규 세션을 시작하면 **이 파일 → [doc/NEXT_SESSION.md](doc/NEXT_SESSION.md) 순으로 읽고** 이어서 진행한다.
>
> 📁 **폴더 배치·서명키·이전 절차는 저장소 밖 공용 문서가 정본이다** — `../../README.md`
> (= `<작업 폴더>/README.md`, 이 기기에서는 `D:/PERSONAL/20_GitHub/README.md`). 그 상위는 `D:/00_폴더구조_가이드.md`.
> **여기(CLAUDE.md)는 KDailyUtil 내부만** 다룬다 — 같은 내용을 두 곳에 복붙하지 않는다.
>
> 🔴 **방금 `git clone`한 새 폴더라면 [README §클론 직후 세팅](README.md)을 먼저 본다** —
> `local.properties`·키스토어는 **커밋되지 않아** clone만으로는 빌드가 반쪽이다.

---

## 🎬 지금 할 일 (2026-09-07 갱신)

남은 것은 **vc8 출하**이고, 그 앞에 **실기기 확인**이 걸려 있다.
🔴 **09-07에 코드 작업이 하나 붙었다** — 아래 0번(사용자 결정).

0. 🔴 **퀴즈 「새 문제 N개」 카운터 상한 + 복귀 사면** — **vc8에 포함하기로 결정**(2026-09-07).
   지금은 상한이 없어 **두 달 비운 사용자에게 「새 문제 300개」**가 나간다([`BriefingReceiver.kt:127`](app/src/main/java/com/kitwlshcom/kdailyutil/receiver/BriefingReceiver.kt#L127)).
   설계·수치 = [doc/FEATURE_DAILY_PASSAGES.md §6](doc/FEATURE_DAILY_PASSAGES.md)
1. 🔴 **실기기·브라우저 확인** — 절차는 [doc/NEXT_SESSION.md 부록 A](doc/NEXT_SESSION.md)에 화면·버튼 단위로 있다
   (퀴즈 로봇 수동 1회 실행 · `aiModel` 비상 레버 첫 검증 · 알람이 **이틀 연속** 오는지 · **0번을 넣었다면 알림 문구도**)
2. `versionCode` **7 → 8** · `versionName` `1.6.1` → `1.6.2` 제안
3. `./gradlew :app:bundleRelease` → **`keytool -printcert -jarfile <aab>`로 서명 확인** → 업로드

> 🔴 **`versionCode`가 7 그대로인 것은 실수가 아니라 사용자 지시다**(2026-09-04: 「버전코드는 아직 올리지 말고, release는 다음에」).
> 올리기 전에 사용자에게 확인할 것.

---

## 📌 이 프로젝트의 사실 (2026-09-04 재검증)

| 항목 | 값 |
|---|---|
| 스토어 게시본 | **v1.6.1 (vc7)** · 2026-08-12 라이브 |
| 저장소 | 스토어보다 앞섬 — **vc8 분량이 코드에 있고 아직 안 올라갔다**(503 대응 + 리텐션 한 판) |
| 단위 테스트 | **52건 통과** — AiErrorMessage 10 · BriefingScheduler 7 · DailyRecord 25 · GeminiFallback 9 · Example 1 |
| 서명 | `local.properties` `release.*` 4개 + 키스토어 실물 확인. 업로드 키 SHA-256 `61:12:DE:…:A5:12:99` |
| `family.json` 최상위 | 비상 레버 키(`aiModel`·`aiTrial`·`fscApi`) **0개 = 전부 기본값** |

---

## 🔧 자주 쓰는 명령

```bash
./gradlew :app:testDebugUnitTest      # 단위 테스트 52건 (기기 불필요)
./gradlew :app:assembleDebug          # 디버그 APK
./gradlew :app:signingReport          # 🔑 서명 설정이 실제로 어느 키스토어를 잡는지 확인
./gradlew :app:bundleRelease          # 업로드용 AAB
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab   # 업로드 전 필수
```

🔴 **`hasReleaseSigning`이 false면 «경고 한 줄 없이 서명 없는 AAB»가 나온다.** 빌드 전에 `signingReport`로 한 번 본다.

---

## 🧭 프로젝트 내부 규칙 (지키지 않으면 사고가 난다)

- **경로는 전부 상대경로로 쓴다.** 문서·스크립트 모두. 절대경로는 폴더를 옮기는 순간 죽는다
  (2026-09-04에 `d:/DATA/20_Source/80_Git_HUB/…` 잔재를 전부 걷어냈다).
  - `release.store.file`은 `rootProject.file()`로 풀린다 → **저장소 루트 기준 상대경로**(`../../_secrets/KDailyUtil/…`)로 적는다
  - `doc/icon_scripts/*.py`는 `__file__` 기준으로 저장소 루트를 계산한다. 새 스크립트도 그 형태를 따른다
  - ⚠️ 예외 = `gradle.properties`의 `org.gradle.java.home`(JDK 실경로). **이 한 줄만 기기마다 다르다** — JAVA_HOME이 21이 아니면 빌드가 설정단계에서 깨진다
- **`family.json`은 네 곳을 맞춘다** — 정본 `doc/family_config/family.json` + 번들 `app/src/main/res/raw/family.json`
  + 라이브 `../../k-series-config/main` + K장부 사본(`../../KJangbu/main`).
  🔴 라이브를 덮어쓰기 전에 **최상위 비상 레버 키부터 확인**한다 → [doc/family_config/README.md](doc/family_config/README.md) §3-1
- 🔴 **`aiModel` 레버는 KDailyUtil과 K장부에 동시에 적용된다.** 한 앱만 구할 수 없다
- **모델 이름을 버전으로 박지 말 것** + **폴백 후보에 실재하지 않는 모델을 두지 말 것.** 후보 추가는 **실제 호출로 확인**한다
- **새 문서를 만들면 [README.md](README.md) '문서 인덱스' 표에 등록**한다(K-시리즈 규칙)
- **날짜를 추측하지 말 것** — `git log --date=…`로 확인한다
- 🔴 **세션을 끝낼 때 `git status -sb`로 `ahead N`을 확인한다.** 2026-08-25 세션이 커밋만 하고 푸시를 잊어
  **503 수정이 열흘간 이 기기 안에만 있었다**(09-04 발견). **커밋은 저장이 아니다**
- **KJangbu·KLotto645 저장소는 다른 세션이 작업 중이다.** 그 앱 관련 결정은 그 세션이 단일 기준 —
  건드리기 전에 `git status`·`git log`부터 본다
  - ⚠️ [doc/KLOTTO_CONNECT_HANDOFF.md](doc/KLOTTO_CONNECT_HANDOFF.md)·[doc/K_SERIES_ICON_RECIPE.md](doc/K_SERIES_ICON_RECIPE.md)는
    **KLotto645 저장소와 동일 사본**으로 유지하는 문서다. 한쪽을 고치면 반대쪽 세션에 알린다

---

## 📚 읽을 순서

1. **이 파일** — 프로젝트 진입점
2. [doc/NEXT_SESSION.md](doc/NEXT_SESSION.md) — 지금 상황·다음 할 일·부록 A(확인 절차). **작업 시작점은 항상 여기**
3. [README.md](README.md) — 클론 직후 세팅 · 저장소 밖 의존물 · 문서 인덱스
4. [doc/DEVELOPER_GUIDE.md](doc/DEVELOPER_GUIDE.md) — 아키텍처·파일 구조
5. [doc/AI_KEY_NOTES.md](doc/AI_KEY_NOTES.md) — 모델 실측표·AI 장애의 기술 배경
6. `../../README.md` — 폴더·서명키 공용 규칙(저장소 밖 정본)
