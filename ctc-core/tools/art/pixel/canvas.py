import json,sys
class C:
    def __init__(s,w,h): s.g=[['.']*w for _ in range(h)]
    def put(s,x,y,rows):
        for j,r in enumerate(rows):
            for i,ch in enumerate(r):
                if ch!=' ': s.g[y+j][x+i]=ch
    def rows(s,x0,y0,w,h):
        out=[]
        for y in range(y0,y0+h):
            r=''.join(s.g[y][x0:x0+w]); out.append('|'.join(r[i:i+8] for i in range(0,len(r),8)))
        return out
