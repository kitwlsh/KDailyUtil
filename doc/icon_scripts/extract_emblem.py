# -*- coding: utf-8 -*-
"""KLogo.png 의 체커보드(가짜 투명) 배경을 제거해 투명 엠블럼 PNG 추출."""
import numpy as np
from PIL import Image
import os

SRC = r"D:\DATA\20_Source\80_Git_HUB\KDailyUtil\KDailyUtil\doc\KLogo.png"
OUT = r"C:\Users\shlee16\AppData\Local\Temp\claude\d--DATA-20-Source-80-Git-HUB-KDailyUtil-KDailyUtil\c5517410-c134-4714-825a-554377416594\scratchpad\emblem_clean.png"

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
