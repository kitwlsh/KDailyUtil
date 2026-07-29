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

## 5. 아이콘 리사이즈 (필요 시)

원본 1024² 아이콘은 `doc/family_icons/` 에 있다. 384² 변환:

```python
from PIL import Image
Image.open("doc/family_icons/kjangbu_icon_1024.png").convert("RGBA") \
     .resize((384, 384), Image.LANCZOS) \
     .save("doc/family_config/icons/kjangbu.png", "PNG", optimize=True)
```
