exec(open('pxicons.py').read())
import random
from PIL import ImageDraw
def shapegrid(polys):
    """polys: [(points, level)] painted in order on a 16x16 index map; returns grid of levels"""
    im = Image.new('L', (16, 16), 255); d = ImageDraw.Draw(im)
    for pts, lv in polys: d.polygon(pts, fill=lv)
    return [[None if im.getpixel((x, y)) == 255 else im.getpixel((x, y)) for x in range(16)] for y in range(16)]
def colorize(g, ramp, top=None):
    n = len(HEX[ramp]) - 1
    out = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if g[y][x] is None: continue
            edge = any(not (0 <= y+dy < 16 and 0 <= x+dx < 16) or g[y+dy][x+dx] is None for dy, dx in ((1,0),(-1,0),(0,1),(0,-1)))
            out[y][x] = f'{ramp}.0' if edge else f'{ramp}.{max(1, min(n, g[y][x]))}'
    return out
def ingot(ramp):
    n = len(HEX[ramp]) - 1
    g = shapegrid([([(2,8),(9,4),(14,7),(7,11)], n), ([(2,8),(7,11),(7,13),(2,10)], max(1,n-2)), ([(7,11),(14,7),(14,9),(7,13)], max(1,n-3))])
    out = colorize(g, ramp)
    for (x, y) in ((6,6),(7,6),(8,5)): out[y][x] = f'{ramp}.{n}'
    return out
def gem(ramp):
    n = len(HEX[ramp]) - 1
    g = shapegrid([([(5,3),(10,3),(13,6),(8,13),(2,6)], 2), ([(5,3),(8,3),(8,6),(2,6)], n), ([(8,3),(10,3),(13,6),(8,6)], max(1,n-1)),
                   ([(2,6),(8,6),(8,13)], max(1,n-1)), ([(8,6),(13,6),(8,13)], max(1,n-3))])
    out = colorize(g, ramp)
    for (x, y) in ((5,4),(6,4),(4,5)): out[y][x] = f'{ramp}.{n}'
    return out
def chunk(ramp, seed=3):
    random.seed(seed); n = len(HEX[ramp]) - 1
    g = shapegrid([([(3,6),(6,3),(10,3),(13,6),(13,10),(10,13),(5,13),(2,10)], 2)])
    for y in range(16):
        for x in range(16):
            if g[y][x] is None: continue
            lv = 2 + (1 if x + y < 14 else 0) + (1 if random.random() < 0.25 else 0) - (1 if x + y > 19 else 0)
            g[y][x] = max(1, min(n, lv))
    out = colorize(g, ramp)
    for (x, y) in ((5,5),(6,4),(9,7)): out[y][x] = f'{ramp}.{n}'
    return out
