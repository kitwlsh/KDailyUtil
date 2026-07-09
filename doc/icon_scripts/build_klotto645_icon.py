# -*- coding: utf-8 -*-
"""디자인 A 최종본 마스터(1024/512) 생성 + KLotto645 안드로이드 리소스 & 스토어 아이콘 export."""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import numpy as np
import os

SCRATCH = r"C:\Users\shlee16\AppData\Local\Temp\claude\d--DATA-20-Source-80-Git-HUB-KDailyUtil-KDailyUtil\c5517410-c134-4714-825a-554377416594\scratchpad"
EMB = os.path.join(SCRATCH, "emblem_clean.png")
ARIBLK = r"C:\Windows\Fonts\ariblk.ttf"
PROJ = r"D:\DATA\20_Source\80_Git_HUB\KLotto645\app\src\main"
RES = os.path.join(PROJ, "res")
ROOT = r"D:\DATA\20_Source\80_Git_HUB\KLotto645"
emblem = Image.open(EMB).convert("RGBA")

def radial_bg(S, inner, outer):
    n = S*2
    y, x = np.ogrid[0:n, 0:n]
    d = np.clip(np.sqrt((x-n/2)**2+(y-n/2)**2)/(n/2*0.95), 0, 1)
    img = np.zeros((n, n, 3))
    for i in range(3):
        img[..., i] = inner[i]+(outer[i]-inner[i])*d
    return Image.fromarray(img.astype(np.uint8), "RGB").resize((S, S), Image.LANCZOS).convert("RGBA")

def ball3d(r, body, text, text_rgb=(255,255,255), rim=(214,178,58)):
    k = 4
    pad = int(r*0.6); n = int((r+pad)*2)*k; R = r*k; cx = cy = n/2
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    dx = xx-cx; dy = yy-cy; dist = np.sqrt(dx*dx+dy*dy)
    out = np.zeros((n, n, 4), np.float32)
    sdist = np.sqrt(dx*dx+((dy-0.18*R))**2)
    sh = np.clip(1-(sdist/(R*1.02)), 0, 1)**1.5
    ring = dist <= R
    rt = np.clip((R-dist)/(R*0.14), 0, 1)
    rimcol = np.zeros((n, n, 3), np.float32)
    dark = np.array([150,110,30], np.float32); light = np.array(rim, np.float32)
    for i in range(3): rimcol[..., i] = dark[i]+(light[i]-dark[i])*rt
    Rin = R*0.84; core = dist <= Rin
    nz = np.sqrt(np.clip(1-(dist/Rin)**2, 0, 1))
    lx, ly, lz = -0.45, -0.55, 0.70
    lam = np.clip((dx/Rin)*lx+(dy/Rin)*ly+nz*lz, 0, 1)
    shade = 0.45+0.85*lam
    bodycol = np.zeros((n, n, 3), np.float32); bodyarr = np.array(body, np.float32)
    for i in range(3): bodycol[..., i] = np.clip(bodyarr[i]*shade, 0, 255)
    edged = np.clip((Rin-dist)/(Rin*0.5), 0, 1)
    for i in range(3): bodycol[..., i] *= (0.62+0.38*edged)
    rl = np.clip((dy/Rin)*0.9+(-nz)*0.2, 0, 1)*np.clip(1-(dist/Rin), 0, 1)
    for i in range(3): bodycol[..., i] = np.clip(bodycol[..., i]+rl*60, 0, 255)
    hx, hy = cx-0.34*Rin, cy-0.40*Rin
    hd = np.sqrt((xx-hx)**2+(yy-hy)**2)
    spec = np.clip(1-hd/(Rin*0.5), 0, 1)**2.2*255*0.9
    for i in range(3): bodycol[..., i] = np.clip(bodycol[..., i]+spec, 0, 255)
    out[..., 3] = sh*140
    a_rim = ring.astype(np.float32)
    for i in range(3): out[..., i] = out[..., i]*(1-a_rim)+rimcol[..., i]*a_rim
    out[..., 3] = np.maximum(out[..., 3], a_rim*255)
    a_core = core.astype(np.float32)
    for i in range(3): out[..., i] = out[..., i]*(1-a_core)+bodycol[..., i]*a_core
    out[..., 3] = np.maximum(out[..., 3], a_core*255)
    im = Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(im)
    fnt = ImageFont.truetype(ARIBLK, int(R*1.15))
    bb = d.textbbox((0,0), text, font=fnt); tw, th = bb[2]-bb[0], bb[3]-bb[1]
    tx = cx-tw/2-bb[0]; ty = cy-th/2-bb[1]-R*0.02
    d.text((tx+3, ty+3), text, font=fnt, fill=(0,0,0,110))
    d.text((tx, ty), text, font=fnt, fill=text_rgb+(255,))
    return im.resize((n//k, n//k), Image.LANCZOS)

RED=(206,44,50); GOLD=(240,182,28); BLUE=(28,104,200)

def build(S):
    f = S/512.0
    c = radial_bg(S, (44,44,50), (11,11,13))
    ew = int(372*f)
    e = emblem.resize((ew, int(emblem.size[1]*ew/emblem.size[0])), Image.LANCZOS)
    c.alpha_composite(e, (int(262*f-e.size[0]/2), int(222*f-e.size[1]/2)))
    for r, body, txt, trgb, pos in [
        (54, RED,  "6", (255,255,255), (138,352)),
        (54, GOLD, "4", (70,45,0),     (100,452)),
        (54, BLUE, "5", (255,255,255), (196,460)),
    ]:
        b = ball3d(int(r*f), body, txt, trgb)
        c.alpha_composite(b, (int(pos[0]*f-b.size[0]/2), int(pos[1]*f-b.size[1]/2)))
    return c

master = build(1024)
master.save(os.path.join(SCRATCH, "master_final_1024.png"))
m512 = master.resize((512, 512), Image.LANCZOS)
m512.save(os.path.join(SCRATCH, "master_final_512.png"))

def circular(im):
    n = im.size[0]; k = 4
    mask = Image.new("L", (n*k, n*k), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, n*k-1, n*k-1], fill=255)
    mask = mask.resize((n, n), Image.LANCZOS)
    o = im.copy(); o.putalpha(mask); return o

# --- KLotto645 인앱 리소스 ---
m512.save(os.path.join(PROJ, "ic_launcher-playstore.png"))
launcher = {"mdpi":48,"hdpi":72,"xhdpi":96,"xxhdpi":144,"xxxhdpi":192}
fg = {"mdpi":108,"hdpi":162,"xhdpi":216,"xxhdpi":324,"xxxhdpi":432}
for dens, sz in launcher.items():
    d = os.path.join(RES, f"mipmap-{dens}"); os.makedirs(d, exist_ok=True)
    sq = master.resize((sz, sz), Image.LANCZOS)
    sq.save(os.path.join(d, "ic_launcher.webp"), "WEBP", quality=95, method=6)
    circular(sq).save(os.path.join(d, "ic_launcher_round.webp"), "WEBP", quality=95, method=6)
for dens, sz in fg.items():
    d = os.path.join(RES, f"mipmap-{dens}"); os.makedirs(d, exist_ok=True)
    master.resize((sz, sz), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.webp"), "WEBP", quality=95, method=6)

# --- 스토어 업로드용 루트 아이콘 갱신 ---
m512.convert("RGB").save(os.path.join(ROOT, "KLotto645_512.png"))
master.convert("RGB").save(os.path.join(ROOT, "KLotto645icon.png"))  # 1024

print("FINAL export done")
