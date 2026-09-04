# -*- coding: utf-8 -*-
"""ic_app_logo_full.png(완전한 로고, 흰배경)에서 흰배경 제거 → 스플래시용 투명 아이콘.
완전한 육각형 + 톱니바퀴/나침반이 원본 통합돼 있어 잘림/크롭 문제 없음."""
import numpy as np
from PIL import Image, ImageFilter
import os
# 경로는 «이 스크립트 위치»에서 계산한다 — 폴더를 옮겨도 그대로 돈다(절대경로 금지).
#   이 파일 = <저장소 루트>/doc/icon_scripts/ → 두 단 위가 저장소 루트다.
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
# 중간 산출물 폴더(저장소에 안 남긴다). ICON_WORK 환경변수로 바꿀 수 있다.
WORK = os.environ.get("ICON_WORK", os.path.join(ROOT, "build", "icon_work"))
os.makedirs(WORK, exist_ok=True)
SRC = os.path.join(ROOT, "app/src/main/res/drawable/ic_app_logo_full.png")
DST = os.path.join(ROOT, "app/src/main/res/drawable-nodpi/ic_k_app_icon.png")
SC = WORK

im=Image.open(SRC).convert("RGB")
print("source size",im.size)
arr=np.array(im).astype(int)
r,g,b=arr[...,0],arr[...,1],arr[...,2]
mx=np.maximum(np.maximum(r,g),b);mn=np.minimum(np.minimum(r,g),b)
white=((mx-mn)<38)&(mn>92)      # 흰 + 회색 그림자(드롭섀도)까지 배경으로 (중간톤 회색까지)
H,W=white.shape;bg=np.zeros((H,W),bool)
bg[0,:]|=white[0,:];bg[-1,:]|=white[-1,:];bg[:,0]|=white[:,0];bg[:,-1]|=white[:,-1]
while True:
    gr=bg.copy();gr[1:,:]|=bg[:-1,:];gr[:-1,:]|=bg[1:,:];gr[:,1:]|=bg[:,:-1];gr[:,:-1]|=bg[:,1:];gr&=white
    if np.array_equal(gr,bg):break
    bg=gr
alpha=np.where(bg,0,255).astype(np.uint8)
img=Image.fromarray(np.dstack([arr.astype(np.uint8),alpha]),"RGBA")
# 흰 프린지 제거: 알파를 살짝 침식(임계 상향)해 가장자리 1~2px 안으로 당김
a=img.split()[3].filter(ImageFilter.GaussianBlur(1.3));a=a.point(lambda p:255 if p>175 else 0)
img.putalpha(a)
img=img.crop(img.getbbox())
cw,ch=img.size
print("content",(cw,ch),"ratio",round(ch/cw,3))
# 여백 패딩(내용 0.86 → 스플래시 확대에도 안 잘림)
side=int(max(cw,ch)/0.86)
canvas=Image.new("RGBA",(side,side),(0,0,0,0))
canvas.alpha_composite(img,((side-cw)//2,(side-ch)//2))
canvas.save(DST)
print("saved",DST,canvas.size)
# 흰끼 잔여 체크
a2=np.array(canvas);R,G,B,A=[a2[...,i].astype(int) for i in range(4)]
mx2=np.maximum(np.maximum(R,G),B);mn2=np.minimum(np.minimum(R,G),B)
w2=(A>150)&(mn2>212)&((mx2-mn2)<22)
print("흰끼 잔여 픽셀",int(w2.sum()))
c=Image.new("RGBA",canvas.size,(26,26,28,255));c.alpha_composite(canvas)
c.convert("RGB").resize((520,520)).save(os.path.join(SC,"logo_full_charcoal.png"))
