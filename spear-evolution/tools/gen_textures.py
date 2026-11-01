from PIL import Image
import colorsys, json, os
OUT="/home/claude/spear-evolution/src/main/resources/assets/spear_evolution/textures/item"
def H(h):
    h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in (0,2,4))+(255,)

def blade_cells(prof):
    cells={}
    for r,(a,b) in enumerate(prof):
        for k in range(-a,b+1):
            c=15-r+k
            if 8<=c<=15: cells[(r,c)]=k
    return cells

def render(spec, img, oy=0, frame=0, god=False):
    px=lambda r,c,col: img.putpixel((c,r+oy),col) if 0<=r<16 and 0<=c<16 else None
    cells=blade_cells(spec['prof'])
    L,M,D,O=[H(x) for x in spec['pal']]
    # outline
    for (r,c) in cells:
        for dr,dc in((1,0),(-1,0),(0,1),(0,-1)):
            n=(r+dr,c+dc)
            if n not in cells and 0<=n[0]<16 and 0<=n[1]<16: px(n[0],n[1],O)
    for (r,c),k in cells.items():
        if god:
            hue=(frame/16.0 + r*0.07 + (k)*0.05)%1.0
            rgb=lambda s,v: tuple(int(x*255) for x in colorsys.hsv_to_rgb(hue,s,v))+(255,)
            col = rgb(0.4,1.0) if k<0 else (rgb(0.9,1.0) if k==0 else rgb(1.0,0.72))
        else:
            col = L if k<0 else (M if k==0 else D)
        px(r,c,col)
    hm,hd,ha=[H(x) for x in spec['handle']]
    wraps=spec.get('wraps',(8,9,12))
    for r in range(7,16):
        px(r,15-r, ha if r in wraps else hm)
        if 16-r<=15: px(r,16-r,hd)
    px(15,0,H(spec['pommel']))
    g=spec.get('guard',0)
    if g:
        gl,gd=[H(x) for x in spec['gcol']]
        px(6,7,gl); px(8,9,gd); px(7,9,gd); px(7,7,gl)
        if g==2:
            px(5,6,gl); px(9,10,gd); px(6,6,gd); px(8,10,gd)
    for (r,c,col) in spec.get('decor',[]): px(r,c,H(col))
    if god:
        # wings + twinkling halo sparkles
        W=H('#ffffff'); G=H('#ffd95a'); G2=H('#c9961e')
        for (r,c,col) in [(4,5,W),(5,6,W),(6,6,G),(6,7,G),(7,7,G),(5,5,G2),(8,9,G),(9,10,G),(10,11,W),(9,11,G2),(7,9,G2),(3,5,G2),(11,12,G2)]: px(r,c,col)
        tw=[(1,12),(3,9),(0,13),(2,15),(5,13)]
        for i,(r,c) in enumerate(tw):
            if (frame+i*3)%8<2 and (r,c) not in cells: px(r,c,H('#ffffff'))

S={}
S['wooden']=dict(pal=('#d9b27a','#a8763f','#7a5128','#3b2713'),prof=[(0,0),(0,0),(0,1),(1,1),(1,1),(1,1),(0,1)],handle=('#8a5a2e','#5e3b1c','#d8cba0'),guard=0,pommel='#5e3b1c',decor=[(4,11,'#7a5128'),(3,13,'#7a5128')],wraps=(7,8,11))
S['copper']=dict(pal=('#f4b183','#d2763f','#9a4a22','#4a2010'),prof=[(0,0),(1,0),(1,1),(2,1),(2,1),(1,1),(1,0)],handle=('#a0612c','#6f3f1a','#e8c070'),guard=1,gcol=('#c8683a','#8f4023'),pommel='#e8c070',decor=[(4,10,'#4fb594'),(5,11,'#4fb594'),(3,12,'#ffd0a8')])
S['iron']=dict(pal=('#f5f7fa','#c3c9d0','#858c96','#30343a'),prof=[(0,0),(1,0),(2,1),(2,2),(2,2),(1,1),(0,1)],handle=('#3a2f33','#221a1d','#6a6f78'),guard=2,gcol=('#9aa0a8','#5f656e'),pommel='#6a6f78',decor=[(3,12,'#ffffff'),(4,11,'#ffffff'),(2,13,'#ffffff')])
S['gold']=dict(pal=('#fff6a8','#f7c928','#c58a14','#6a4508'),prof=[(0,0),(1,0),(1,1),(2,2),(3,2),(2,1),(1,1)],handle=('#7b1d1d','#4a1010','#f7c928'),guard=2,gcol=('#f7c928','#c58a14'),pommel='#fff6a8',decor=[(7,8,'#e83030'),(3,12,'#ffffff'),(5,10,'#fff6a8')])
S['lapis']=dict(pal=('#7fa0ff','#2f54d4','#18278a','#0a0f40'),prof=[(0,0),(1,0),(1,1),(2,1),(3,2),(2,2),(1,1)],handle=('#2b3050','#1a1d33','#4a78ff'),guard=1,gcol=('#2f54d4','#18278a'),pommel='#4a78ff',decor=[(2,13,'#cfe6ff'),(4,10,'#9fd0ff'),(4,12,'#cfe6ff'),(5,11,'#9fd0ff'),(1,14,'#e6f2ff')])
S['redstone']=dict(pal=('#ff6a5a','#c01818','#6e0a0a','#2a0404'),prof=[(0,0),(1,0),(1,2),(2,1),(1,3),(2,1),(1,0)],handle=('#4a2a22','#2e1814','#ff2a2a'),guard=1,gcol=('#8a1010','#4a0808'),pommel='#ff2a2a',decor=[(3,12,'#ff2a2a'),(5,10,'#ff2a2a'),(5,12,'#ff2a2a'),(2,14,'#ff9a8a')])
S['emerald']=dict(pal=('#c5ffd2','#2fd870','#138c42','#05381a'),prof=[(0,0),(1,1),(2,2),(2,2),(2,2),(1,1),(0,0)],handle=('#3a2a1a','#241a10','#2fd870'),guard=1,gcol=('#2fd870','#138c42'),pommel='#7dffa0',decor=[(3,12,'#eafff0'),(2,12,'#7dffa0'),(4,13,'#7dffa0'),(4,10,'#7dffa0')])
S['diamond']=dict(pal=('#e6ffff','#4de6e0','#1d9fb5','#08404f'),prof=[(0,0),(1,0),(2,1),(3,1),(3,2),(2,1),(1,1)],handle=('#5a4030','#3a281c','#4de6e0'),guard=2,gcol=('#4de6e0','#1d9fb5'),pommel='#e6ffff',decor=[(3,11,'#ffffff'),(2,12,'#ffffff'),(4,12,'#1d9fb5'),(5,11,'#ffffff')])
S['netherite']=dict(pal=('#6e6376','#413a48','#27222d','#0c0a0f'),prof=[(0,0),(1,0),(1,1),(2,2),(1,3),(2,2),(1,1)],handle=('#2b2430','#18141c','#8a3cf0'),guard=2,gcol=('#413a48','#8a3cf0'),pommel='#8a3cf0',decor=[(2,13,'#b06bff'),(3,12,'#8a3cf0'),(4,11,'#b06bff'),(5,10,'#8a3cf0'),(1,14,'#c58bff')])
S['god']=dict(pal=('#ffffff','#ffffff','#ffffff','#6b4f10'),prof=[(0,0),(1,1),(2,2),(3,2),(3,3),(2,2),(1,1)],handle=('#ffd95a','#c9961e','#ffffff'),guard=0,pommel='#ffffff',wraps=(8,10,12))
os.makedirs(OUT,exist_ok=True)
for n,s in S.items():
    if n=='god':
        sheet=Image.new('RGBA',(16,16*16),(0,0,0,0))
        for f in range(16): render(s,sheet,oy=f*16,frame=f,god=True)
        sheet.save(f"{OUT}/god_spear.png")
        json.dump({"animation":{"frametime":3,"interpolate":True}},open(f"{OUT}/god_spear.png.mcmeta","w"),indent=2)
    else:
        im=Image.new('RGBA',(16,16),(0,0,0,0)); render(s,im); im.save(f"{OUT}/{n}_spear.png")
# preview montage
names=list(S)
M=Image.new('RGBA',(16*10*8,16*8*3),(60,60,70,255))
for i,n in enumerate(names):
    im=Image.open(f"{OUT}/{n}_spear.png").convert('RGBA')
    if n=='god':
        for j,f in enumerate((0,5,10)):
            M.alpha_composite(im.crop((0,f*16,16,f*16+16)).resize((128,128),Image.NEAREST),(i*128,j*128))
    else: M.alpha_composite(im.resize((128,128),Image.NEAREST),(i*128,0))
M.save('/home/claude/preview.png')
# icon
g=Image.open(f"{OUT}/god_spear.png").crop((0,0,16,16)).resize((128,128),Image.NEAREST)
g.save('/home/claude/spear-evolution/src/main/resources/assets/spear_evolution/icon.png')
print("ok")
