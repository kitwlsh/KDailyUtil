# -*- coding: utf-8 -*-
"""
KDailyUtil 앱 아이콘 합성기 (K-시리즈 레시피 §3-1 "7시 사분면 정사각 존" 규칙 구현)

메인 로고(엠블럼)와 부주제(톱니/나침반)를 **따로 준비**해서 합성한다.
- 메인 로고: 캔버스 중앙, 높이 기준으로 축소해 상·하 꼭짓점이 절대 안 잘리게 여백 확보.
- 부주제  : 좌하단(7시) 정사각 존 박스 안에 '완전히' 들어가도록 contain-fit 후 배치
            → 육각 엠블럼 밖으로 이탈하지 않음.

부주제 PNG는 **투명 배경**이 이상적. 흰/단색 배경이 있으면 --strip-bg 로 제거 시도.

사용:
  python build_kdaily_icon.py \
      --emblem  doc/family_icons/kdaily_emblem_clean_1024.png \
      --subtheme doc/family_icons/kdaily_subtheme_gears.png \
      --out     app/src/main/res/drawable-nodpi/ic_k_app_icon.png
"""
import argparse
from collections import deque
import numpy as np
from PIL import Image

CANVAS = 1024
# === K-시리즈 통일 규격 (2026-07-16 확정, KLotto645 레퍼런스에 맞춤) =========
# 메인 엠블럼: 높이 기준 비율. 0.98 = 캔버스를 거의 꽉 채움(여백 ~1%, KLotto와 동일 크기감).
#   ⚠️ 이 엠블럼은 세로가 길어(bbox 약 426:482) '높이 기준'으로 맞춰야 상하 꼭짓점이 안 잘림.
#      1.0을 넘기면 잘림. 스플래시 진입 스케일에 오버슈트가 없어야 함(rest=1.0).
EMBLEM_HEIGHT_FRAC = 1.00  # 캔버스 높이 100% 채움(KLotto645 레퍼런스와 픽셀 동일). 꼭짓점이 상/하 끝에 닿음.
# 부주제 존(7시 사분면 안, 육각 하단좌측). (x1,y1,x2,y2) — 1024 기준. 부주제가 이 박스에 contain-fit.
#   폭 약 35%(=KLotto 645볼 클러스터 크기감)로 좌하단에 배치. 육각 하단좌측 림에 살짝 걸침(볼과 동일).
SUBTHEME_BOX = (85, 665, 470, 1012)
SUBTHEME_ANCHOR = (0.08, 0.85)  # 박스 내 정렬(좌하단 구석 쪽).


def load_rgba(path):
    return Image.open(path).convert("RGBA")


def strip_solid_bg(im):
    """가장자리에서 flood-fill로 근접-단색(흰/밝은) 배경을 투명화."""
    a = np.array(im)
    h, w = a.shape[:2]
    r, g, b, al = (a[:, :, i].astype(int) for i in range(4))
    mx = np.maximum(np.maximum(r, g), b)
    mn = np.minimum(np.minimum(r, g), b)
    bg = (mn > 175) & ((mx - mn) < 28) & (al > 0)   # 밝고 저채도(흰/회백)
    vis = np.zeros((h, w), bool)
    dq = deque()
    for x in range(w):
        for y in (0, h - 1):
            if bg[y, x]:
                vis[y, x] = True; dq.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if bg[y, x] and not vis[y, x]:
                vis[y, x] = True; dq.append((y, x))
    while dq:
        y, x = dq.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and not vis[ny, nx] and bg[ny, nx]:
                vis[ny, nx] = True; dq.append((ny, nx))
    a[:, :, 3] = np.where(vis, 0, al)
    return Image.fromarray(a, "RGBA")


def content_crop(im):
    a = np.array(im)
    ys, xs = np.where(a[:, :, 3] > 16)
    if len(xs) == 0:
        return im
    return im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def fit_contain(im, box_w, box_h):
    w, h = im.size
    s = min(box_w / w, box_h / h)
    return im.resize((max(1, int(w * s)), max(1, int(h * s))), Image.LANCZOS)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--emblem", required=True)
    ap.add_argument("--subtheme", default=None, help="부주제(톱니/나침반) 투명 PNG. 없으면 엠블럼만 출력")
    ap.add_argument("--out", required=True)
    ap.add_argument("--strip-bg", action="store_true", help="부주제의 단색 배경을 제거")
    args = ap.parse_args()

    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))

    # 1) 메인 엠블럼: 높이 기준 축소 → 중앙(상하 여백 확보로 꼭짓점 보존)
    emb = content_crop(strip_solid_bg(load_rgba(args.emblem)))
    eh = int(CANVAS * EMBLEM_HEIGHT_FRAC)
    ew = int(emb.size[0] * eh / emb.size[1])
    emb = emb.resize((ew, eh), Image.LANCZOS)
    canvas.alpha_composite(emb, ((CANVAS - ew) // 2, (CANVAS - eh) // 2))

    # 2) 부주제: 7시 존 박스 안에 contain-fit
    if args.subtheme:
        sub = load_rgba(args.subtheme)
        if args.strip_bg:
            sub = strip_solid_bg(sub)
        sub = content_crop(sub)
        x1, y1, x2, y2 = SUBTHEME_BOX
        bw, bh = x2 - x1, y2 - y1
        sub = fit_contain(sub, bw, bh)
        ax, ay = SUBTHEME_ANCHOR
        px = x1 + int((bw - sub.size[0]) * ax)
        py = y1 + int((bh - sub.size[1]) * ay)
        canvas.alpha_composite(sub, (px, py))

    # 안전 검증: 불투명 영역이 캔버스 안에 있고 여백이 있는지 로그
    arr = np.array(canvas)
    ys, xs = np.where(arr[:, :, 3] > 16)
    print(f"final opaque bbox x[{xs.min()},{xs.max()}] y[{ys.min()},{ys.max()}] (canvas 0~{CANVAS})")
    canvas.save(args.out)
    print("saved:", args.out)


if __name__ == "__main__":
    main()
