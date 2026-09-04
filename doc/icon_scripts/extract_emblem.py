# -*- coding: utf-8 -*-
"""KLogo.png 의 체커보드(가짜 투명) 배경을 제거해 투명 엠블럼 PNG 추출."""
import numpy as np
from PIL import Image
import os

# 경로는 «이 스크립트 위치»에서 계산한다 — 폴더를 옮겨도 그대로 돈다(절대경로 금지).
#   이 파일 = <저장소 루트>/doc/icon_scripts/ → 두 단 위가 저장소 루트다.
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
# 중간 산출물 폴더(저장소에 안 남긴다). ICON_WORK 환경변수로 바꿀 수 있다.
WORK = os.environ.get("ICON_WORK", os.path.join(ROOT, "build", "icon_work"))
os.makedirs(WORK, exist_ok=True)
SRC = os.path.join(ROOT, "doc", "KLogo.png")
OUT = os.path.join(WORK, "emblem_clean.png")

im = Image.open(SRC).convert("RGB")
# 작업 해상도 1024 로 축소
W = 1024
H = int(im.size[1] * W / im.size[0])
im = im.resize((W, H), Image.LANCZOS)
arr = np.asarray(im).astype(np.int16)
r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
mx = np.maximum(np.maximum(r, g), b)
mn = np.minimum(np.minimum(r, g), b)
neutral_bright = ((mx - mn) < 20) & (mn > 175)  # 체커보드 후보(흰/연회색)

# 테두리에서 시작해 neutral_bright 영역을 flood (반복 팽창)
bg = np.zeros((H, W), dtype=bool)
bg[0, :] |= neutral_bright[0, :]
bg[-1, :] |= neutral_bright[-1, :]
bg[:, 0] |= neutral_bright[:, 0]
bg[:, -1] |= neutral_bright[:, -1]
while True:
    grown = bg.copy()
    grown[1:, :]  |= bg[:-1, :]
    grown[:-1, :] |= bg[1:, :]
    grown[:, 1:]  |= bg[:, :-1]
    grown[:, :-1] |= bg[:, 1:]
    grown &= neutral_bright
    if np.array_equal(grown, bg):
        break
    bg = grown

alpha = np.where(bg, 0, 255).astype(np.uint8)
# 가장자리 살짝 침식(체커 경계 잔여 방지) + 부드럽게
out = np.dstack([arr.astype(np.uint8), alpha])
img = Image.fromarray(out, "RGBA")

# 알파 경계 1px 매트 제거: 알파 블러 후 임계
from PIL import ImageFilter
a = img.split()[3].filter(ImageFilter.GaussianBlur(0.6))
a = a.point(lambda p: 255 if p > 128 else 0)
img.putalpha(a)

# 컨텐츠 bbox 로 트림
bbox = img.split()[3].getbbox()
img = img.crop(bbox)
img.save(OUT)
print("saved emblem_clean.png", img.size)
