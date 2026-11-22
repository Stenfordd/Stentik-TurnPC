import math, colorsys
from PIL import Image
CHIPS = [('lunar',0x9FC4FF,0),('martian',0xE8683C,0),('mercury',0x8FA3C0,0),('void',0x9A5CFF,0),('harbinger',0xC0303A,1),('ignis',0xFF9A2E,1),
 ('leviathan',0x3CC8D8,1),('remnant',0xD8C27A,1),('maledictus',0x9EE6FF,1),('guardian',0xC77DFF,1),('ember',0xFF6A2B,0),('terra',0x7FD36B,0),
 ('copper',0xE07B4A,0),('iron',0xD8DCE4,0),('kinetic',0x6FE3C9,0),('lumber',0xB88A4E,0),('vein',0x9AA7B8,0),('harvest',0xE8C84A,0),
 ('quarry',0xC9A35A,0),('gravity',0xB8C8F0,0),('dust',0xD2693C,0),('acid',0x9BE04A,0),('flare',0xFFB23A,0)]
def ramp(c):
    r,g,b=[(c>>s&255)/255 for s in (16,8,0)]; h,l,s=colorsys.rgb_to_hls(r,g,b)
    out=[]
    for L,S in ((0.18,0.55),(0.32,0.6),(0.5,0.7),(0.68,0.8),(0.86,0.9)):
        rr,gg,bb=colorsys.hls_to_rgb(h,L,min(1,max(0.15,s*S+0.1)))
        out.append('#%02x%02x%02x'%(int(rr*255),int(gg*255),int(bb*255)))
    return out
# symbols: 14x14 masks; 'X' body, '+' highlight, '-' shade
S = {}
def blank(): return [['.']*14 for _ in range(14)]
def circ(g, cx, cy, r, ch='X', fill=True):
    for y in range(14):
        for x in range(14):
            d=math.hypot(x-cx,y-cy)
            if (d<=r if fill else abs(d-r)<0.6): g[y][x]=ch
def put(g, rows, ox=0, oy=0):
    for j,r in enumerate(rows):
        for i,c in enumerate(r):
            if c!=' ' and c!='.': g[oy+j][ox+i]=c
def sym(name):
    g=blank()
    if name=='lunar':
        circ(g,6.5,6.5,5.6); 
        for y in range(14):
            for x in range(14):
                if math.hypot(x-9.2,y-4.6)<=4.8: g[y][x]='.'
    elif name=='martian':
        circ(g,6.5,6.5,5.2); put(g,["          ","  --      ","        - ","    --    "],2,3)
        for x in range(14): 
            y=int(6.5-(x-6.5)*0.35)
            if g[y][x]=='.': g[y][x]='-'
    elif name=='mercury':
        for y in range(14):
            for x in range(14):
                if y<6 and abs(x-6.5)<=(y+1)*0.7 and y>0: g[y][x]='X'
        circ(g,6.5,8,4.6)
    elif name=='void':
        circ(g,6.5,6.5,5.6); circ(g,6.5,6.5,3.6,'.'); circ(g,6.5,6.5,1.6,'+')
        for y in range(14):
            for x in range(14):
                if g[y][x]=='X' and (math.atan2(y-6.5,x-6.5)*3)%(2*math.pi)<1.2: g[y][x]='-'
    elif name=='harbinger':
        put(g,["   XXXXXX   ","  XXXXXXXX  "," XXXXXXXXXX "," XX--XX--XX "," XX--XX--XX "," XXXXXXXXXX ","  XXXX-XXX  ","   XXXXXX   ","   X-X-X-   ","   XXXXXX   "],1,2)
    elif name in ('ignis','ember'):
        put(g,["     X      ","     XX     ","    XXX     ","    XXXX  X ","   XXXXX XX ","  XXX+XXXXX ","  XX++XXXXX "," XXX++XXXXX "," XX+++XXXXX "," XX++++XXX  ","  XX++XXXX  ","   XXXXXX   "],1,1)
        if name=='ember':
            g=blank(); put(g,["    X     ","   XX  X  ","  XXX XX  ","  X+XXXX  "," XX+XXXX  "," X++XXX   "," XX+XX    ","  XXX     "],2,3)
            for p in ((11,2),(12,5),(2,1)): g[p[1]][p[0]]='+'
    elif name=='leviathan':
        for x in range(14):
            for k,base in ((0,4),(1,8)):
                y=int(base+1.8*math.sin(x/2.2+k))
                for t in range(2): g[min(13,y+t)][x]='X' if t==0 else '-'
    elif name=='remnant':
        put(g,[" XX         ","XXXX        ","XXXXX       "," XXXXX      ","  XXXXX     ","   XXXXX    ","    XXXXX   ","     XXXXX  ","      XXXXX ","       XXXXX","        XXXX","         XX "],1,1)
        for (x,y) in ((1,4),(4,1),(9,12),(12,9)): g[y][x]='X'
    elif name=='maledictus':
        for a in range(3):
            ang=a*math.pi/3
            for t in range(-6,7):
                x=round(6.5+t*math.cos(ang)); y=round(6.5+t*math.sin(ang))
                if 0<=x<14 and 0<=y<14: g[y][x]='X'
        circ(g,6.5,6.5,1.2,'+')
    elif name=='guardian':
        put(g,[" XXXXXXXXXX ","XX++XXXXXXXX","X++XXXXXXX-X","X+XXX++XXX-X","X+XX++++XX-X","XXXX++++XX-X","XXXXX++XX--X"," XXXXXXXX-X "," XXXXXXX--X ","  XXXXX--X  ","   XXXX-X   ","    XXXX    "],1,1)
    elif name=='terra':
        put(g,["        XXX ","      XXXXX ","    XXX+XXX ","   XX+XXXXX ","  XX+XXXXX  ","  X+XXXXX   "," XX+XXXX    "," X+XXX      "," X+X        ","X-          ","-           "],1,1)
    elif name=='copper':
        put(g,["      XXX  ","     XXX   ","    XXX    ","   XXXXXX  ","  XXXXXX   ","     XXX   ","    XXX    ","   XXX     ","  XX       "," X         "],2,2)
    elif name=='iron':
        put(g,["XXXXXXXXXXX ","X++++++++XX ","XXXXXXXXXXXX"," XXXXXXXXX  ","   XXXXX    ","   XXXXX    ","  XXXXXXX   "," XXXXXXXXX  "," X-------X  "],1,3)
    elif name=='kinetic':
        circ(g,6.5,6.5,4.2); circ(g,6.5,6.5,1.7,'.')
        for a in range(8):
            ang=a*math.pi/4
            for t in (5,6):
                x=round(6.5+t*math.cos(ang)); y=round(6.5+t*math.sin(ang))
                g[y][x]='X'
    elif name=='lumber':
        put(g,["   XXXX     ","  XX++XX    "," XX+XXXXX   "," XXXXXX-X   ","  XXXXXX    ","   XX-WW    ","      WW    ","       WW   ","        WW  ","         WW "],1,2)
    elif name=='vein':
        put(g,["X        X  ","XX      XX  "," XX    X+X  ","  XX  XX    ","   XXXX     ","   X++X     ","  XXXX      "," XX  XX     ","XX    XX    ","X      X+   "],1,2)
    elif name=='harvest':
        put(g,["     X      ","    XXX     ","   X+X+X    ","    XXX     ","   X+X+X    ","    XXX     ","   X+X+X    ","     W      ","     W      ","     W      ","     W      "],1,2)
    elif name=='quarry':
        put(g,["  XXXXXXX   "," XX+++++XX  ","XX       XX ","X    W    X ","     W      ","     W      ","     W      ","     W      ","     W      "],1,2)
    elif name=='gravity':
        circ(g,6.5,6.5,5.6,'X',fill=False); circ(g,6.5,6.5,2.2,'+')
        for p in ((2,2),(11,11)): g[p[1]][p[0]]='-'
    elif name=='dust':
        put(g,["XXXXXXXXXX "," X++++++X  ","  X++++X   ","   X++X    ","    XX     ","   X--X    ","  X----X   "," X------X  ","XXXXXXXXXX "],2,2)
    elif name=='acid':
        for y in range(14):
            for x in range(14):
                if 1<y<8 and abs(x-5.5)<=(y-1)*0.6: g[y][x]='X'
        circ(g,5.5,8.5,3.8); g[7][4]='+'; g[8][4]='+'
        for p in ((11,3),(10,6),(12,9)): g[p[1]][p[0]]='+'
    elif name=='flare':
        circ(g,6.5,6.5,3.6)
        for a in range(8):
            ang=a*math.pi/4+math.pi/8
            for t in (5,6):
                x=round(6.5+t*math.cos(ang)); y=round(6.5+t*math.sin(ang))
                if 0<=x<14 and 0<=y<14: g[y][x]='X'
        g[5][5]='+'; g[5][6]='+'; g[6][5]='+'
    return g
def shade(g):
    """top-left light on the symbol: X next to empty above/left -> +, below/right -> -"""
    out=[r[:] for r in g]
    for y in range(14):
        for x in range(14):
            if g[y][x]!='X': continue
            up = y==0 or g[y-1][x]=='.'; lf = x==0 or g[y][x-1]=='.'
            dn = y==13 or g[y+1][x]=='.'; rt = x==13 or g[y][x+1]=='.'
            if up or lf: out[y][x]='+'
            elif dn or rt: out[y][x]='-'
    return out
# frame: 32x32. keys: o outline, a..e graphite body, p q pins (gold), r1..r3 ring (chip ramp), 'B' boss corner gold
FRAME = []
def frame(boss=False, socket=False, scrap=False):
    g=[['.']*32 for _ in range(32)]
    def P(x,y,c):
        if 0<=x<32 and 0<=y<32: g[y][x]=c
    # pins
    for i in (9,15,21):
        for t in range(3):
            for w in range(2):
                P(i+w,2+t,'p' if w==0 else 'q'); P(i+w,27+t,'p' if w==0 else 'q'); P(2+t,i+w,'p' if w==0 else 'q'); P(27+t,i+w,'p' if w==0 else 'q')
    # body 5..26
    for y in range(5,27):
        for x in range(5,27):
            if (x in (5,26)) and (y in (5,26)): continue
            edge = x in (5,26) or y in (5,26)
            if edge: c='o'
            elif x==6 or y==6: c='d'
            elif x==25 or y==25: c='a'
            else: c='b' if (x+y)%7 else 'c'
            P(x,y,c)
    # ring 8..23 in chip colour
    for y in range(8,24):
        for x in range(8,24):
            if x in (8,23) or y in (8,23):
                if (x in (8,23)) and (y in (8,23)): continue
                P(x,y,'2' if (x==8 or y==8) else '1')
            elif x in (9,22) or y in (9,22):
                P(x,y,'0')
            else: P(x,y,'a')
    if boss:
        for (x,y) in ((6,6),(7,6),(6,7),(25,6),(24,6),(25,7),(6,25),(7,25),(6,24),(25,25),(24,25),(25,24)): P(x,y,'q')
    return g
def chip(name, boss):
    g=frame(boss); s=shade(sym(name))
    for y in range(14):
        for x in range(14):
            c=s[y][x]
            if c=='.': continue
            g[9+y][9+x]={'X':'3','+':'4','-':'2','W':'w'}[c]
    return g

def special(kind):
    g=frame(False)
    if kind=='blank':
        for y in range(10,22):
            for x in range(10,22): g[y][x]='a'
    if kind=='socket':
        for y in range(10,22):
            for x in range(10,22): g[y][x]='.'
        for y in range(5,27):
            for x in range(5,27):
                if g[y][x] in '012': g[y][x]={'0':'b','1':'d','2':'d'}[g[y][x]]
    if kind=='scrap':
        for y in range(32):
            for x in range(32):
                if x + y * 0.8 > 30 + (3 if (y // 3) % 2 else 0): g[y][x]='.'
        for (x,y) in ((12,12),(13,14),(16,11),(10,17)): g[y][x]='2'
    return g
