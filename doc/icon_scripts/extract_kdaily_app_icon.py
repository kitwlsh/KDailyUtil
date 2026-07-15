# -*- coding: utf-8 -*-
"""KDailyUtil 앱아이콘 투명 추출 v2: 흰/그림자 배경 더 확실히 제거 + 여백(패딩) 추가(육각형 안 잘리게)."""
import numpy as np
from PIL import Image, ImageFilter
import os
SRC=r"D:\DATA\20_Source\80_Git_HUB\KDailyUtil\KDailyUtil\app\src\main\ic_launcher-playstore.png"
DST=r"D:\DATA\20_Source\80_Git_HUB\KDailyUtil\KDailyUtil\app\src\main\res\drawable-nodpi\ic_k_app_icon.png"
SC=r"C:\Users\shlee16\AppData\Local\Temp\claude\d--DATA-20-Source-80-Git-HUB-KDailyUtil-KDailyUtil\c5517410-c134-4714-825a-554377416594\scratchpad"

im=Image.open(SRC).convert("RGB")
W=1024;H=int(im.size[1]*W/im.size[0]);im=im.resize((W,H),Image.LANCZOS)
arr=np.asarray(im).astype(np.int16)
r,g,b=arr[...,0],arr[...,1],arr[...,2]
mx=np.maximum(np.maximum(r,g),b);mn=np.minimum(np.minimum(r,g),b)
# 흰 + 밝은 회색(그림자)까지: 저채도 & 밝음. (골드/유채색은 채도 높아 보존)
lightbg=((mx-mn)<42)&(mn>150)
bg=np.zeros((H,W),bool)
bg[0,:]|=lightbg[0,:];bg[-1,:]|=lightbg[-1,:];bg[:,0]|=lightbg[:,0];bg[:,-1]|=lightbg[:,-1]
while True:
    grown=bg.copy()
    grown[1:,:]|=bg[:-1,:];grown[:-1,:]|=bg[1:,:];grown[:,1:]|=bg[:,:-1];grown[:,:-1]|=bg[:,1:]
    grown&=lightbg
    if np.array_equal(grown,bg):break
    bg=grown
alpha=np.where(bg,0,255).astype(np.uint8)
img=Image.fromarray(np.dstack([arr.astype(np.uint8),alpha]),"RGBA")
# 경계 정리(잔여 밝은 테두리 1px 침식) + 부드럽게
a=img.split()[3].filter(ImageFilter.GaussianBlur(0.8));a=a.point(lambda p:255 if p>140 else 0)
img.putalpha(a)
img=img.crop(img.getbbox())
cw,ch=img.size
# 여백 추가: 내용이 캔버스의 0.84 차지(≈8% 마진) → 육각형 안 잘림
side=int(max(cw,ch)/0.78)
canvas=Image.new("RGBA",(side,side),(0,0,0,0))
canvas.alpha_composite(img,((side-cw)//2,(side-ch)//2))
canvas.save(DST)
print("saved",DST,"content",(cw,ch),"canvas",canvas.size)

# 차콜 미리보기
c=Image.new("RGBA",canvas.size,(26,26,28,255));c.alpha_composite(canvas)
c.convert("RGB").resize((520,520)).save(os.path.join(SC,"kdaily_v2_charcoal.png"))
print("preview saved")
