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

✅ **자매앱(K장부·KLotto645) 관련 대기 항목은 0건이다**(2026-09-07 완결).
다른 저장소를 열어 볼 일도, 회신을 기다리는 것도 없다 — **이제 이 앱 안의 일만 하면 된다.**

**코드 작업 대기 항목은 없다.** 남은 것은 **vc8 출하**이고, 그 앞에 **실기기 확인**이 걸려 있다.

0. ✅ **완료(2026-09-07) — vc8 범위가 두 번 늘었고 둘 다 코드가 끝났다**
   - **퀴즈 카운터 상한 + 복귀 사면**: `QUIZ_NEW_CAP = 20` · `RETURN_AMNESTY_DAYS = 7`을
     [`DailyRecord`](app/src/main/java/com/kitwlshcom/kdailyutil/data/DailyRecord.kt)에 두고 알림·허브 카드에 적용
   - 📖 **매일 새 지문 공급**: 로봇이 하루 1편을 만들고([`korean_quiz_data/update_passages.py`](../../korean_quiz_data/main/update_passages.py)),
     앱이 그것을 받아 **「오늘의 지문」**(날짜로 정해지는 1편)으로 준다. 내장 19편은 **오프라인 폴백으로 남겼다**
   - 🔴 **지문 로봇이 아직 원격에 없으면** 앱은 404를 받고 조용히 내장 19편으로 떨어진다(설계된 폴백).
     지문이 실제로 오려면 `korean_quiz_data`를 **푸시**해야 한다
1. 🔴 **실기기·브라우저 확인** — 절차는 [doc/NEXT_SESSION.md 부록 A](doc/NEXT_SESSION.md)에 화면·버튼 단위로 있다
   (퀴즈 로봇 수동 1회 실행 · `aiModel` 비상 레버 첫 검증 · 알람이 **이틀 연속** 오는지 · **알림 문구 = 상한·복귀 사면**)
2. `versionCode` **7 → 8** · `versionName` `1.6.1` → `1.6.2` 제안
3. `./gradlew :app:bundleRelease` → **`keytool -printcert -jarfile <aab>`로 서명 확인** → 업로드

> 🔴 **`versionCode`가 7 그대로인 것은 실수가 아니라 사용자 지시다**(2026-09-04: 「버전코드는 아직 올리지 말고, release는 다음에」).
> 올리기 전에 사용자에게 확인할 것.

---

## 📌 이 프로젝트의 사실 (2026-09-07 재검증)

| 항목 | 값 |
|---|---|
| 스토어 게시본 | **v1.6.1 (vc7)** · 2026-08-12 라이브 |
| 저장소 | 스토어보다 앞섬 — **vc8 분량이 코드에 있고 아직 안 올라갔다**(503 대응 + 리텐션 한 판) |
| 소스 버전 | `versionCode = 7` / `versionName = "1.6.1"` — 마지막 앱 코드 변경 = **09-07 카운터 상한·복귀 사면 + 지문 공급** |
| 단위 테스트 | **73건 통과** — AiErrorMessage 10 · BriefingScheduler 7 · DailyRecord 46 · GeminiFallback 9 · Example 1 |
| 서명 | `local.properties` `release.*` 4개 + 키스토어 실물 확인. 업로드 키 SHA-256 `61:12:DE:…:A5:12:99` |
| `family.json` 최상위 | 비상 레버 키(`aiModel`·`aiTrial`·`fscApi`) **0개 = 전부 기본값** · 앱 목록은 4곳 동일 |
| 자매앱 동기화 | ✅ 3벌 문서(핸드오프·아이콘 레시피)가 세 저장소에서 **0줄 차이** · 세 저장소 모두 원격과 동기 |
| 퀴즈 로봇 | ✅ 정상 — 09-05·09-06 연속 성공 · `last_run.json` = `ok` · 누적 **515문항**(하루 5문항) |

---

## 🔧 자주 쓰는 명령

```bash
./gradlew :app:testDebugUnitTest      # 단위 테스트 73건 (기기 불필요)
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
- 📖 **지문·퀴즈는 같은 저장소(`korean_quiz_data`)가 공급하고, 스크립트·생존 신호 블록은 따로다.**
  퀴즈가 실패해도 지문은 저장되고 그 반대도 된다 — 한 스크립트로 합치지 말 것
- **모델 이름을 버전으로 박지 말 것** + **폴백 후보에 실재하지 않는 모델을 두지 말 것.** 후보 추가는 **실제 호출로 확인**한다
- **새 문서를 만들면 [README.md](README.md) '문서 인덱스' 표에 등록**한다(K-시리즈 규칙)
- **날짜를 추측하지 말 것** — `git log --date=…`로 확인한다
- 🔴 **세션을 끝낼 때 `git status -sb`로 `ahead N`을 확인한다.** 2026-08-25 세션이 커밋만 하고 푸시를 잊어
  **503 수정이 열흘간 이 기기 안에만 있었다**(09-04 발견). **커밋은 저장이 아니다**
- **KJangbu·KLotto645 저장소는 다른 세션이 작업 중이다.** 그 앱 관련 결정은 그 세션이 단일 기준 —
  건드리기 전에 `git status`·`git log`부터 본다
  - ⚠️ [doc/KLOTTO_CONNECT_HANDOFF.md](doc/KLOTTO_CONNECT_HANDOFF.md)·[doc/K_SERIES_ICON_RECIPE.md](doc/K_SERIES_ICON_RECIPE.md)는
    **KLotto645·K장부와 동일 사본**으로 유지하는 문서다(2026-09-07부터 **사본이 3벌**). 한쪽을 고치면 다른 세션에 알린다
    - 확인법: `diff --strip-trailing-cr doc/<파일> ../../KLotto645/main/doc/<파일>` → **0줄이어야 한다**
    - 🔴 **md5·파일 크기로 대조하지 말 것** — 세 저장소 모두 `core.autocrlf=true`라 작업 트리 줄바꿈이
      기기·세션마다 CRLF/LF로 갈린다. **내용이 같아도 해시가 다르다**(09-07에 실제로 셋 다 달랐다).
      커밋본끼리는 `git show HEAD:<경로>` 비교가 정확하다
    - 🔴 **우리 저장소에만 있는 «예외 한 줄»을 남기지 말 것**(3벌 규칙). 우리에게만 의미 있는 메모는 우리 문서에 둔다
    - 🔴 이 문서들의 **본문은 우리가 정본이지만, 우리가 항상 최신인 것은 아니다** — 09-07에 폴더 이전 문구가
      KLotto645·K장부 사본에서 먼저 갱신돼 **정본이 뒤처져 있었다**(K장부 세션이 발견해 알려 왔고, 받아서 맞췄다)

---

## 📚 읽을 순서

1. **이 파일** — 프로젝트 진입점
2. [doc/NEXT_SESSION.md](doc/NEXT_SESSION.md) — 지금 상황·다음 할 일·부록 A(확인 절차). **작업 시작점은 항상 여기**
3. [README.md](README.md) — 클론 직후 세팅 · 저장소 밖 의존물 · 문서 인덱스
4. [doc/DEVELOPER_GUIDE.md](doc/DEVELOPER_GUIDE.md) — 아키텍처·파일 구조
5. [doc/AI_KEY_NOTES.md](doc/AI_KEY_NOTES.md) — 모델 실측표·AI 장애의 기술 배경
6. `../../README.md` — 폴더·서명키 공용 규칙(저장소 밖 정본)
