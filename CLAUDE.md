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

## 🎬 지금 할 일 (2026-09-08 갱신)

✅ **자매앱(K장부·KLotto645) 관련 대기 항목은 0건이다**(2026-09-07 완결).
다른 저장소를 열어 볼 일도, 회신을 기다리는 것도 없다 — **이제 이 앱 안의 일만 하면 된다.**

✅ **v1.7.0(vc8)이 2026-09-08에 출시됐다**(업로드 09-07 → 다음날 라이브).
🔴 **`versionCode 9`는 이번 빌드가 가져갔다. 다음 업로드는 vc10이다**(반려되어 다시 올려도 10이다 — vc는 되돌릴 수 없다).

📦 **v1.7.1(vc9)이 빌드도 서명 확인도 끝나 업로드만 남았다**(2026-09-08).
들어간 것 = «독서 훈련 시작까지 가는 길» 단축. 사용자 신고(「지문 고르고 훈련까지 스크롤이
너무 멀다 · 훈련 끝나면 또 그만큼」)에서 출발해 원인 3가지를 코드에서 확인해 고쳤다
→ [doc/NEXT_SESSION.md §할 일 0-B](doc/NEXT_SESSION.md). ✅ **실기기 확인은 사용자가 완료했다.**
⚠️ 스킴 규칙(`기능추가 → MINOR`)대로면 1.8.0인데 **사용자가 1.7.1로 결정했다** — 스킴이 바뀐 것은 아니다.

🔴 **신규 세션은 [doc/NEXT_SESSION.md](doc/NEXT_SESSION.md)부터 본다** — vc8 몫이던 확인이 아직 안 끝났다:
**알람 이틀 연속**(가장 중요 · 안 본 채로 출시됐다) · 지문이 매일 쌓이는지 · edge-to-edge 눈확인 ·
`aiModel` 레버 검증 · 데이터 보안 칸 · 깨끗한 계정 신규 설치. 여기에 vc9 몫(부록 A §E)이 얹혔다.

> **릴리즈 산출물(2026-09-08)**: [`app/release/kdailyutil-v1.7.1.aab`](app/release/kdailyutil-v1.7.1.aab) (11,348,510 bytes)
> · `versionCode 9` / `versionName 1.7.1` — **병합 매니페스트와 AAB 내부 양쪽에서 확인**(`1.7.0` 잔재 0건)
> · 서명 SHA-256 `61:12:DE:…:A5:12:99` 확인 완료(`signingReport` → `bundleRelease` → `keytool`)
> · 붙여넣기용 출시 노트 = [`app/release/RELEASE_NOTES.md`](app/release/RELEASE_NOTES.md) §v1.7.1
> 🔴 **올릴 파일은 `app/release/`의 그것이다** — `app/build/outputs/`는 다음 빌드에 갈아치워진다

0. ✅ **끝난 것(2026-09-07 · vc8으로 출시됨)**
   - **퀴즈 카운터 상한 + 복귀 사면**: `QUIZ_NEW_CAP = 20` · `RETURN_AMNESTY_DAYS = 7`을
     [`DailyRecord`](app/src/main/java/com/kitwlshcom/kdailyutil/data/DailyRecord.kt)에 두고 알림·허브 카드에 적용
   - 📖 **매일 새 지문 공급**: 로봇이 하루 1편을 만들고([`korean_quiz_data/update_passages.py`](../../korean_quiz_data/main/update_passages.py)),
     앱이 그것을 받아 **「오늘의 지문」**(날짜로 정해지는 1편)으로 준다. 내장 19편은 **오프라인 폴백으로 남겨둔다**
   - ✅ **로봇은 살아 있다**(09-08 실측) — 원격 `passages_2026.json`이 **HTTP 200**을 준다.
     예전에 적혀 있던 «푸시해야 지문이 온다» 경고는 **해소됐다**
1. ✅ **끝난 것(2026-09-08 · vc9 내용)** — 독서 훈련 동선
   - 지문을 고른 그 자리에서 시작 · 버튼 이름에 훈련명 · 결과 화면 「다시·다음」 · 스크롤 보존 · 보관함 상한
   - 근거·설계 판단(일부러 안 한 것 4가지 포함) = [doc/NEXT_SESSION.md §할 일 0-B](doc/NEXT_SESSION.md)
   - 검증: 단위 테스트 **82건 통과**(9건 신설) · 🔬 뮤테이션 2건이 정확히 실패 · ✅ 실기기 확인 완료
2. 🔴 **남은 것 = v1.7.1 업로드 하나다**
   - 올릴 파일 = [`app/release/kdailyutil-v1.7.1.aab`](app/release/kdailyutil-v1.7.1.aab) · 출시 노트 = `RELEASE_NOTES.md` §v1.7.1
   - 빌드·서명·버전 대조는 09-08에 끝났다 — **다시 빌드할 필요 없다**(재빌드하면 서명 확인도 다시 해야 한다)
3. 🔴 **업로드 뒤에도 vc8 몫이던 확인이 남는다** — 부록 A
   - 가장 급한 것 = **알람이 이틀 연속 오는지**(C-5). 안 본 채로 출시됐고,
     🔴 **v1.7.1은 그 알람 코드를 손대지 않았다** — 둘째 날 안 오면 **vc10으로** 고쳐 올린다
   - 나머지 = `aiModel` 레버 · 데이터 보안 칸 · 깨끗한 계정 신규 설치 · edge-to-edge 눈확인
4. 🔴 **vc10 후보가 09-08에 생겼다 — AI 「한도 초과(429)」 대응**
   - 로봇 3연속 실패의 원인을 파다가 나왔다: 별칭 `gemini-flash-latest`가 지금 **`gemini-3.8-flash`로
     풀리고 그 모델의 무료 한도가 20**이다. 앱은 **429에 폴백하지 않고**, 안내 문구는 «잠시 뒤 풀린다»고
     말하는데 **일일 한도면 최대 24시간**일 수 있다
   - 🔴 **실측이 먼저다** — 「한도가 모델별인가 키 단위인가」를 확인해야 폴백을 켤지 정할 수 있고,
     그 실측은 **429가 실제로 나는 순간에** 해야 한다(퀴즈 로봇이 지금 그 상태다)
   - 절차·근거 = [doc/AI_KEY_NOTES.md §3-2](doc/AI_KEY_NOTES.md) · 할 일 = [doc/NEXT_SESSION.md §할 일 0-C](doc/NEXT_SESSION.md)
   - 🔴 **오늘 코드를 안 건드린 이유**: 소스가 빌드해 둔 `v1.7.1.aab`와 일치하는 상태여야 한다.
     여기서 고치면 **«업로드한 1.7.1과 다른 1.7.1»**이 생긴다 → **업로드 뒤에 착수**

---

## 📌 이 프로젝트의 사실 (2026-09-08 재검증)

| 항목 | 값 |
|---|---|
| 스토어 게시본 | **v1.7.0 (vc8)** · **2026-09-08 라이브**(09-07 업로드 → 다음날 출시) |
| 저장소 | 스토어보다 앞섬 — **v1.7.1(vc9) AAB까지 만들어 둔 상태**이고 업로드만 안 했다 |
| 소스 버전 | `versionCode = 9` / `versionName = "1.7.1"` — 09-08 상향·빌드·서명 확인 완료. **업로드만 남았다** · 다음은 **vc10** |
| 단위 테스트 | **82건 통과**(09-08 실측) — AiErrorMessage 10 · BriefingScheduler 7 · DailyRecord 46 · GeminiFallback 9 · **ReadingTrainingModule 9** · Example 1 |
| 서명 | `local.properties` `release.*` 4개 + 키스토어 실물 확인. 업로드 키 SHA-256 `61:12:DE:…:A5:12:99` |
| `family.json` 최상위 | 비상 레버 키(`aiModel`·`aiTrial`·`fscApi`) **0개 = 전부 기본값** · 앱 목록은 4곳 동일 |
| Play 대시보드 권장 조치 | vc7 기준 4건 — **조사 완료(09-07)**. 셋은 우리가 고칠 것이 없고(①②③) 하나는 선택(④ AGP 9). 판정·근거 = [doc/NEXT_SESSION.md](doc/NEXT_SESSION.md) §권장 조치 4건 |
| 자매앱 동기화 | ✅ 3벌 문서(핸드오프·아이콘 레시피)가 세 저장소에서 **0줄 차이** · 세 저장소 모두 원격과 동기 |
| 퀴즈·지문 로봇 | 🟡 **반만 정상**(09-08 11:09 실측) — 📖 **지문은 정상**(`passages_2026.json` **2편**) · 🔴 **퀴즈는 429로 계속 실패**(누적 525문항에서 멈춤). 같은 실행에서 1초 차로 퀴즈만 429다 → [doc/AI_KEY_NOTES.md §3-2](doc/AI_KEY_NOTES.md) |

---

## 🔧 자주 쓰는 명령

```bash
./gradlew :app:testDebugUnitTest      # 단위 테스트 82건 (기기 불필요)
./gradlew :app:assembleDebug          # 실기기 확인용 디버그 APK
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
  - 🔴 **다만 별칭(`gemini-flash-latest`)이 «한도가 훨씬 짠 신모델»로 옮겨갈 수 있다**(2026-09-08에 실제로 겪었다 —
    `gemini-3.8-flash` · 무료 한도 20). 별칭을 쓰는 판단은 여전히 맞지만, **AI가 갑자기 안 되면 별칭이
    어디로 풀렸는지부터 본다** → 앱의 [연결 테스트]가 «(모델 ○○○)»로 알려준다. 배경 = [doc/AI_KEY_NOTES.md §3-2](doc/AI_KEY_NOTES.md)
- 🔴 **AAB를 빌드해 두고 아직 안 올렸다면 앱 코드를 건드리지 말 것.** 소스와 산출물이 어긋나
  «같은 versionCode인데 내용이 다른 빌드»가 생긴다. 다음 판 작업은 **업로드한 뒤에** 시작한다
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
