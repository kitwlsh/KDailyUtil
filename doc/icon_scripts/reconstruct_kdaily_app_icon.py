# -*- coding: utf-8 -*-
"""KDailyUtil 앱아이콘 재구성: 온전한 mainlogo 육각형 + 플레이스토어 톱니바퀴/나침반 합성.
플레이스토어 육각형은 상하가 잘려 있어, mainlogo(온전)를 위에 얹고 톱니바퀴만 뒤에 깔아 노출."""
import numpy as np
from PIL import Image, ImageFilter
import os
ML=r"D:\DATA\20_Source\80_Git_HUB\KDailyUtil\KDailyUtil\app\src\main\res\drawable\ic_k_logo_3d.png"
PS=r"D:\DATA\20_Source\80_Git_HUB\KDailyUtil\KDailyUtil\app\src\main\ic_launcher-playstore.png"
SC=r"C:\Users\shlee16\AppData\Local\Temp\claude\d--DATA-20-Source-80-Git-HUB-KDailyUtil-KDailyUtil\c5517410-c134-4714-825a-554377416594\scratchpad"

def rm_white(img):
    arr=np.array(img.convert("RGBA")).astype(int)
    r,g,b,a=arr[...,0],arr[...,1],arr[...,2],arr[...,3]
    mx=np.maximum(np.maximum(r,g),b);mn=np.minimum(np.minimum(r,g),b)
    light=((mx-mn)<42)&(mn>150)
    H,W=light.shape;bg=np.zeros((H,W),bool)
    bg[0,:]|=light[0,:];bg[-1,:]|=light[-1,:];bg[:,0]|=light[:,0];bg[:,-1]|=light[:,-1]
    while True:
        gr=bg.copy();gr[1:,:]|=bg[:-1,:];gr[:-1,:]|=bg[1:,:];gr[:,1:]|=bg[:,:-1];gr[:,:-1]|=bg[:,1:];gr&=light
        if np.array_equal(gr,bg):break
        bg=gr
    al=np.where(bg,0,arr[...,3]).astype(np.uint8)
    out=Image.fromarray(np.dstack([arr[...,:3].astype(np.uint8),al]),"RGBA")
    aa=out.split()[3].filter(ImageFilter.GaussianBlur(0.6));aa=aa.point(lambda p:255 if p>135 else 0)
    out.putalpha(aa);return out

# 1) mainlogo(온전한 육각형) - 이미 투명. content crop
ml=Image.open(ML).convert("RGBA");ml=ml.crop(ml.getbbox())
mlw,mlh=ml.size
# 2) 플레이스토어에서 톱니바퀴/나침반 영역(좌하단) 크롭 후 흰배경 제거
ps=Image.open(PS).convert("RGB")  # 512
# PS 육각형은 512 꽉 참(중심 256). 좌하단 톱니바퀴 영역
gx0,gy0,gx1,gy1=0,280,220,512
gcrop=rm_white(ps.crop((gx0,gy0,gx1,gy1)))
# 3) 캔버스 구성 (고해상). ML 육각형을 캔버스에 배치, 톱니바퀴는 PS좌표→캔버스 매핑으로 정렬
S=1300
canvas=Image.new("RGBA",(S,S),(0,0,0,0))
# ML 육각형 배치: 폭 = 0.72*S, 중앙(약간 위로 - 톱니바퀴가 아래 공간 차지)
target_hex_w=int(0.72*S)
scale_ml=target_hex_w/mlw
ml2=ml.resize((int(mlw*scale_ml),int(mlh*scale_ml)),Image.LANCZOS)
ml_cx,ml_cy=S//2, int(S*0.46)   # 육각형 중심
ml_x=ml_cx-ml2.size[0]//2; ml_y=ml_cy-ml2.size[1]//2
# PS(512) 육각형 중심(256,256)을 ML 육각형 중심(ml_cx,ml_cy)에, 스케일 = target_hex_w/512
sps=target_hex_w/512.0
g2=gcrop.resize((int(gcrop.size[0]*sps),int(gcrop.size[1]*sps)),Image.LANCZOS)
# PS 크롭 좌상단(gx0,gy0) → 캔버스: ml_c + (p-256)*sps
gpx=int(ml_cx+(gx0-256)*sps); gpy=int(ml_cy+(gy0-256)*sps)
# 톱니바퀴 먼저, ML 육각형 나중(겹치는 부분 덮어 온전한 육각형 노출)
canvas.alpha_composite(g2,(gpx,gpy))
canvas.alpha_composite(ml2,(ml_x,ml_y))
canvas=canvas.crop(canvas.getbbox())
# 여백 패딩(내용 0.82)
cw,ch=canvas.size;side=int(max(cw,ch)/0.82)
final=Image.new("RGBA",(side,side),(0,0,0,0));final.alpha_composite(canvas,((side-cw)//2,(side-ch)//2))
final.save(os.path.join(SC,"kdaily_recon.png"))
c=Image.new("RGBA",final.size,(26,26,28,255));c.alpha_composite(final)
c.convert("RGB").resize((520,520)).save(os.path.join(SC,"kdaily_recon_charcoal.png"))
print("saved kdaily_recon.png",final.size)
