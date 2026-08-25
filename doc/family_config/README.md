# K-시리즈 자매앱 동적 레지스트리 — 정본 & 업로드 가이드

이 폴더는 **원격 자매앱 레지스트리의 정본(source of truth)** 이다. 표준 정의 = [KLOTTO_CONNECT_HANDOFF.md](../KLOTTO_CONNECT_HANDOFF.md) **§8**.

```
doc/family_config/
├── family.json        # 정본 — 그대로 k-series-config 레포 루트에 올린다
├── icons/             # 아이콘 384×384 PNG — 그대로 레포 icons/ 에 올린다
│   ├── kdailyutil.png
│   ├── klotto645.png
│   └── kjangbu.png
└── README.md          # (이 파일)
```

`family.json` 사본은 **KDailyUtil 앱에도 번들**되어 있다(`app/src/main/res/raw/family.json`) — 오프라인·첫 실행·레포 부재 시의 폴백. 정본을 고치면 **이 사본도 함께 갱신**하는 게 원칙이다(반영은 다음 앱 배포 때).

> 🔴 **정본을 고쳤다고 라이브가 바뀌지 않는다.** 앱이 실제로 읽는 것은 `kitwlsh/k-series-config` 레포다.
> **이 폴더 수정 → 그 레포에 같은 내용 반영**까지 해야 사용자에게 보인다(§3 업로드 절차).
>
> **현재 상태(2026-08-12)**: 3개 앱 전부 라이브(KDailyUtil v1.6 · KLotto645 v1.0.3 · **K장부 v1.0.1**).
> `comingSoon: true→false` 전환은 **정본·번들·라이브 3곳 모두 반영 완료**(08-10). 정본·번들·라이브·K장부 사본 4개가 일치한다(`_note` 설명 문구만 다름 — 앱이 무시하는 키).

---

## 1. 최초 1회 — 호스팅 레포 만들기 (수동)

GitHub 웹에서 **공개(Public)** 레포 `kitwlsh/k-series-config` 를 만들고, 기본 브랜치 **`main`** 에 위 파일들을 그대로 올린다.

```
k-series-config/
├── family.json
└── icons/kdailyutil.png, klotto645.png, kjangbu.png
```

**검증**: 브라우저에서 아래가 JSON 원문으로 열리면 성공.

```
https://raw.githubusercontent.com/kitwlsh/k-series-config/main/family.json
https://raw.githubusercontent.com/kitwlsh/k-series-config/main/icons/kjangbu.png
```

> ⚠️ 이 URL은 **각 앱에 컴파일타임 상수로 박힌다**(`FamilyRepository.REMOTE_URL`). 레포명·브랜치·경로를 바꾸면 전 앱 재배포가 필요하므로 **고정**한다.
> ⚠️ 레포가 아직 없어도 앱은 **번들 기본값으로 정상 동작**한다(카드가 사라지지 않음). 레포가 생기는 순간부터 원격 갱신이 살아난다.

## 2. 새 자매앱 추가 (앱 재배포 없음)

1. `icons/<앱>.png` 업로드 (384×384 PNG 권장).
2. `family.json` 의 `apps[]` 에 항목 추가 — 스키마는 §8-3.
   - `id` 는 **§8-5 예약 패키지 목록에서 선택**(그래야 '설치됨 배지·앱 직접 실행'까지 재빌드 없이 동작).
   - 미출시면 `"active": true, "comingSoon": true` → 출시되면 `"comingSoon": false`.
3. `updatedAt` 갱신 → 커밋·푸시.
4. 이 폴더의 정본 + `app/src/main/res/raw/family.json` 사본 동기화, §7-1 표 갱신.

각 앱은 **최대 6시간 안에**(또는 설정 > 앱정보 > 브랜드 & 자매앱의 🔄 버튼으로 즉시) 새 카드를 표시한다.

## 3. 출시 상태만 바꿀 때

| 상황 | 수정 |
|---|---|
| 기획 단계 (카드 숨김) | `"active": false` |
| 개발·출시 임박 (출시예정 카드) | `"active": true, "comingSoon": true` |
| 출시 완료 (정상 카드) | `"comingSoon": false` |

세 상태 전환 **모두 JSON 한 줄 수정**으로 끝난다.

## 3-1. ⚠️ 최상위 '레버 키' 레지스트리 — **정본을 덮어쓰기 전에 반드시 읽을 것**

`family.json`은 자매앱 목록(`apps[]`)만 담는 파일이 **아니다.** 각 앱이 같은 파일의 **최상위 키**를 비상 레버로 읽는다. 추가 네트워크 요청 없이 이미 받는 파일에 얹는 설계이며, **앱을 재배포하지 않고 원격에서 기능을 끌 수 있는 유일한 수단**이다.

| 키 | 읽는 앱 | 기본값(키 없을 때) | 당기면 | 유효 버전 |
|---|---|---|---|---|
| `aiModel` (문자열) | 🔴 **KDailyUtil + K장부 (둘 다)** | 앱 내장 별칭 사용 | **두 앱 모두** AI 모델이 지정 모델로 교체된다 | KDailyUtil vc7 / v1.6.1+ · K장부 vc2+ |
| `aiTrial` (객체) | K장부 | 체험 활성(한도는 앱 기본) | AI 체험 중단 / 한도 조정 | K장부 vc2+ |
| `fscApi.enabled` (불린) | K장부 | **`true`(켜짐)** | 금융위 배당 API 차단 → Yahoo 폴백(지급일·알림만 사라짐) | **K장부 vc3+** |

> 🔴 **가장 위험한 사고 경로 — "정본을 그대로 올린다"가 레버를 되살린다.**
> 레버는 사고가 났을 때 **라이브(`k-series-config`)에만 직접 넣는 경우가 많다.** 그 상태에서 누군가 §2·§3 절차대로
> *"정본을 그대로 k-series-config 레포에 올린다"* 를 실행하면 **정본에 없는 레버 키가 조용히 사라진다**
> = 껐던 기능이 아무 경고 없이 다시 켜진다. `fscApi`는 기본값이 '켜짐'이라 이 사고에서 **가장 조용하게** 되살아난다.
>
> **그래서 규칙은 하나다: 라이브를 덮어쓰기 전에 라이브의 최상위 키를 먼저 확인한다.**
>
> ```bash
> curl -s https://raw.githubusercontent.com/kitwlsh/k-series-config/main/family.json \
>   | python -c "import json,sys; d=json.load(sys.stdin); print({k:v for k,v in d.items() if k!='apps'})"
> ```
>
> 여기서 `aiModel`·`aiTrial`·`fscApi`가 보이면 **그 값을 정본에도 옮긴 뒤에** 올린다.
> 레버를 당길 때도 **정본·라이브 둘 다** 넣는 것이 원칙이다(라이브만 고치는 건 사고 대응 중의 임시 조치로만).

> 🔴 **`aiModel`은 두 앱이 공유하는 유일한 레버다 — 한쪽만 고치려고 당길 수 없다.**
> K장부도 2026-08-07(`62284d2`)부터 같은 최상위 `aiModel` 키를 읽는다(`FamilyRegistry.loadAiModel`).
> 즉 **KDailyUtil 하나를 구하려고 당긴 레버가 K장부의 모델까지 바꾼다**(그 반대도 같다).
> 값을 넣기 전에 **두 앱 모두에서 그 모델이 되는지** 확인할 것. 한 앱에만 적용하고 싶다면
> 레버가 아니라 **앱별 키**(예: `aiModel.kdailyutil`)를 새로 설계해야 한다 — 지금은 그런 게 없다.
> *(2026-08-12 이 표를 만들 때 「KDailyUtil 전용」으로 잘못 적었다. 2026-08-25 정정.)*

**앱마다 자기 키만 읽고 모르는 키는 무시한다** — `optString`/`optJSONObject` 기반이라 KDailyUtil에 `fscApi`·`aiTrial`이 있어도 아무 일도 일어나지 않는다. **단 `aiModel`은 예외다 — 두 앱이 같은 키를 읽으므로 위 경고를 먼저 볼 것.** 그래서 세 앱이 한 파일을 공유할 수 있다.

**새 레버를 만들 때 지킬 것**(K장부 `fscApi` 설계에서 확립, 2026-08-12):
- **실패 방향은 '켜짐'으로 고정** — 원격이 죽거나 JSON이 깨졌을 때 조용히 꺼지면 사용자는 이유도 모른 채 기능을 잃는다
- **게이트는 단일 진입점에** — 호출부마다 흩으면 한 곳만 빠뜨려도 레버가 새어 나간다(`AiKeyProvider`·`DividendRepository` 패턴)
- **끄면 '없어지는' 게 아니라 '열화되게'** — 폴백으로 내려가되, 모르는 값은 채우지 말고 비운다
- **양방향 회귀 테스트를 붙인다** — 킬스위치는 사고 난 날 처음 당겨보는 물건이라 그날 안 먹히면 최악이다. K장부는 `FamilyRegistryConfigTest` 12건으로 «없으면 켜짐 / false면 실제로 꺼짐 / 한쪽을 당겨도 다른 쪽은 안 꺼짐»을 못 박았다
- **레지스트리(위 표)에 등록한다** — 등록되지 않은 레버는 위 사고 경로에서 사라진다

## 4. 편집 시 지켜야 할 것

- `id` = applicationId **대소문자 그대로** (KLotto = `kitwlshCom` 대문자 C, 나머지 = `kitwlshcom`).
- `storeUrl` 은 `https://play.google.com/…` 또는 `market://…` 만 — 그 외는 앱이 무시하고 `id` 로 URL을 만든다.
- `iconUrl` 은 **https + `githubusercontent.com` / `github.io`** 만 — 그 외는 무시하고 번들 아이콘으로 폴백한다.
- `_` 로 시작하는 키는 주석이며 앱이 무시한다.
- 항목 상한 **20개**. 깨진 항목 1건은 건너뛰고 나머지는 정상 표시된다.
- 푸시 전 JSON 유효성 확인:
  ```bash
  python -c "import json;d=json.load(open('family.json',encoding='utf-8'));print('ok', len(d['apps']))"
  ```

## 5. 아이콘 만들기 — ⚠️ 반드시 배경이 투명한 원본에서

카드 배경이 어두우므로 **알파 채널이 살아 있는 원본**을 써야 한다. 배경이 박힌 마스터로 만들면
다크 카드 위에 **검은 사각형**으로 표시된다(2026-07-29 실제 사고 — 번들 폴백보다 더 나빠졌다).

- ✅ 좋은 원본: 각 앱 리소스의 `drawable-nodpi/ic_<앱>.png` (§5 교환분, 투명)
- ❌ 나쁜 원본: `doc/family_icons/KLotto645_icon_1024.png` 같은 **배경 박힌 마스터**

384² 변환 + 투명도 검증까지 한 번에:

```python
from PIL import Image
src = "app/src/main/res/drawable-nodpi/ic_klotto645.png"   # 투명 원본
im = Image.open(src).convert("RGBA").resize((384, 384), Image.LANCZOS)
im.save("doc/family_config/icons/klotto645.png", "PNG", optimize=True)
# 검증: 최소 알파가 0이어야 투명(255만 나오면 배경이 박힌 것)
print("alpha:", im.split()[3].getextrema(), "corner:", im.getpixel((0, 0)))
```

업로드 후 라이브에서도 확인:

```bash
curl -s -o live.png "https://raw.githubusercontent.com/kitwlsh/k-series-config/main/icons/klotto645.png"
python -c "from PIL import Image; im=Image.open('live.png').convert('RGBA'); print(im.size, im.split()[3].getextrema())"
```

> 아이콘을 교체하면 KDailyUtil·K장부는 약 5분 내(raw `max-age=300`) 새 아이콘을 받는다.
> **KLotto645는 자체 아이콘 캐시 TTL이 길어 반영이 늦다** — 급할 때는 그 앱 캐시를 지워야 한다.
