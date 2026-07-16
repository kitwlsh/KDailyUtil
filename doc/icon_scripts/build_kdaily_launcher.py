# -*- coding: utf-8 -*-
"""
KDailyUtil 런처/스토어 아이콘 생성기 (KLotto645 build_klotto645_icon.py와 동일 레이아웃).

스플래시(풀블리드 투명 100%)와 달리, 런처는 **다크 라디얼 배경 + 엠블럼 0.727 + 부주제 7시 존**으로
만든다(적응형 원형/스퀘어클 마스크가 꼭짓점을 자르지 않도록 여백 확보). 두 K앱 런처가 서로 일관된다.

산출물(app/src/main/res):
  mipmap-*/ic_launcher.webp (48~192)  · ic_launcher_round.webp · ic_launcher_foreground.webp (108~432)
  app/src/main/ic_launcher-playstore.png (512)

입력: 공유 엠블럼(투명) + 부주제(투명 PNG). 새 K앱은 SUBTHEME 경로만 바꿔 재사용.
"""
import os
import numpy as np
from PIL import Image, ImageDraw

ROOT = r"d:/DATA/20_Source/80_Git_HUB/KDailyUtil/KDailyUtil"
RES = os.path.join(ROOT, "app/src/main/res")
PROJ = os.path.join(ROOT, "app/src/main")
FAM = os.path.join(ROOT, "doc/family_icons")
EMB = os.path.join(FAM, "kdaily_emblem_clean_1024.png")
SUB = os.path.join(FAM, "kdaily_subtheme_gears.png")

# 배치 상수(512 기준). K(엠블럼 색상 로고)는 엠블럼 정중앙(≈50%)에 있으므로,
# 엠블럼을 아이콘 정중앙에 두면 K가 딱 중앙에 온다. 부주제는 좌하단 7시 존에 '얹어' 겹친다.
# KLotto645 아이콘 실측(중앙컬럼 gold)에 정확히 맞춤: 육각 top=24 · 다이아끝=487 · 높이 463(90%) · 중심 256.
#   → 엠블럼폭 411(≈0.80), 중심 (256,256). (이전 376은 KLotto보다 작아 다이아 끝·5시 여백이 안 맞았음)
EMBLEM_W_512 = 411
EMBLEM_C_512 = (256, 256)
# 부주제는 '스플래시를 그대로 축소한' 비율/위치(엠블럼 대비 스플래시와 동일).
#   스플래시 실측: 톱니폭/엠블럼폭=0.393, 중심 상대위치 x=0.228·y=0.819.
#   엠블럼 span x[51,461] y[25,487] → 톱니폭 0.393×411≈162, 중심 (145,404).
SUB_C_512 = (145, 404)        # 부주제 클러스터 중심(스플래시 상대위치와 동일)
SUB_W_512 = 162               # 부주제 클러스터 폭 = 스플래시 비율(엠블럼의 0.393)
BG_IN = (44, 44, 50)
BG_OUT = (11, 11, 13)


def content_crop(im):
    a = np.array(im); ys, xs = np.where(a[:, :, 3] > 16)
    return im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def radial_bg(S, inner, outer):
    y, x = np.ogrid[0:S, 0:S]
    d = np.clip(np.sqrt((x - S / 2) ** 2 + (y - S / 2) ** 2) / (S / 2 * 0.95), 0, 1)
    img = np.zeros((S, S, 3))
    for i in range(3):
        img[..., i] = inner[i] * (1 - d) + outer[i] * d
    return Image.fromarray(img.astype(np.uint8), "RGB").convert("RGBA")


def build(S):
    f = S / 512.0
    c = radial_bg(S, BG_IN, BG_OUT)
    emb = content_crop(Image.open(EMB).convert("RGBA"))
    ew = int(EMBLEM_W_512 * f); eh = int(emb.size[1] * ew / emb.size[0])
    emb = emb.resize((ew, eh), Image.LANCZOS)
    c.alpha_composite(emb, (int(EMBLEM_C_512[0] * f - ew / 2), int(EMBLEM_C_512[1] * f - eh / 2)))
    sub = content_crop(Image.open(SUB).convert("RGBA"))
    sw = int(SUB_W_512 * f); sh = int(sub.size[1] * sw / sub.size[0])
    sub = sub.resize((sw, sh), Image.LANCZOS)
    c.alpha_composite(sub, (int(SUB_C_512[0] * f - sw / 2), int(SUB_C_512[1] * f - sh / 2)))
    return c


def circular(im):
    n = im.size[0]; k = 4
    mask = Image.new("L", (n * k, n * k), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, n * k - 1, n * k - 1], fill=255)
    mask = mask.resize((n, n), Image.LANCZOS)
    o = im.copy(); o.putalpha(mask); return o


master = build(1024)
m512 = master.resize((512, 512), Image.LANCZOS)
m512.save(os.path.join(PROJ, "ic_launcher-playstore.png"))
launcher = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
fg = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
for dens, sz in launcher.items():
    d = os.path.join(RES, f"mipmap-{dens}"); os.makedirs(d, exist_ok=True)
    sq = master.resize((sz, sz), Image.LANCZOS)
    sq.save(os.path.join(d, "ic_launcher.webp"), "WEBP", quality=95, method=6)
    circular(sq).save(os.path.join(d, "ic_launcher_round.webp"), "WEBP", quality=95, method=6)
for dens, sz in fg.items():
    d = os.path.join(RES, f"mipmap-{dens}"); os.makedirs(d, exist_ok=True)
    master.resize((sz, sz), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.webp"), "WEBP", quality=95, method=6)
master.save(os.path.join(FAM, "kdailyutil_launcher_1024.png"))
print("KDailyUtil launcher export done (mipmaps + round + foreground + playstore + master)")
