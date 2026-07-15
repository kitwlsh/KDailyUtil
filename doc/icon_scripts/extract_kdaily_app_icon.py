# -*- coding: utf-8 -*-
"""KDailyUtil 플레이스토어 아이콘(흰 배경)에서 엠블럼+톱니바퀴/나침반을 투명 추출."""
import numpy as np
from PIL import Image, ImageFilter, ImageDraw
import os
SRC=r"D:\DATA\20_Source\80_Git_HUB\KDailyUtil\KDailyUtil\app\src\main\ic_launcher-playstore.png"
SC=r"C:\Users\shlee16\AppData\Local\Temp\claude\d--DATA-20-Source-80-Git-HUB-KDailyUtil-KDailyUtil\c5517410-c134-4714-825a-554377416594\scratchpad"
OUT=os.path.join(SC,"kdaily_app_icon.png")

im=Image.open(SRC).convert("RGB")
W=1024; H=int(im.size[1]*W/im.size[0]); im=im.resize((W,H),Image.LANCZOS)
arr=np.asarray(im).astype(np.int16)
r,g,b=arr[...,0],arr[...,1],arr[...,2]
mx=np.maximum(np.maximum(r,g),b); mn=np.minimum(np.minimum(r,g),b)
white=((mx-mn)<22)&(mn>205)     # 흰/연회색 배경 후보
bg=np.zeros((H,W),bool)
bg[0,:]|=white[0,:];bg[-1,:]|=white[-1,:];bg[:,0]|=white[:,0];bg[:,-1]|=white[:,-1]
while True:
    grown=bg.copy()
    grown[1:,:]|=bg[:-1,:];grown[:-1,:]|=bg[1:,:];grown[:,1:]|=bg[:,:-1];grown[:,:-1]|=bg[:,1:]
    grown&=white
    if np.array_equal(grown,bg):break
    bg=grown
alpha=np.where(bg,0,255).astype(np.uint8)
img=Image.fromarray(np.dstack([arr.astype(np.uint8),alpha]),"RGBA")
a=img.split()[3].filter(ImageFilter.GaussianBlur(0.6)); a=a.point(lambda p:255 if p>128 else 0)
img.putalpha(a)
img=img.crop(img.getbbox())
img.save(OUT)
print("saved",OUT,img.size,"ratio",round(img.size[1]/img.size[0],3))

# 마젠타 확인 + 차콜 확인
for name,bgc in [("kdaily_check_magenta.png",(255,0,255,255)),("kdaily_check_charcoal.png",(26,26,28,255))]:
    c=Image.new("RGBA",img.size,bgc);c.alpha_composite(img)
    c.convert("RGB").resize((360,int(360*img.size[1]/img.size[0]))).save(os.path.join(SC,name))
print("previews saved")
