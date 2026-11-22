import sys, numpy as np, json
from PIL import Image
sys.path.insert(0, '/home/claude/stentik-turnpc/ctc-core/tools')
from hires_sprites import blobs
HEX = {'ti': ["#525e6a","#76838f","#9aa7b2","#bcc7cf","#d8e0e6","#eef3f6"], 'cu': ["#5a2e18","#844626","#ac6436","#cf8a52","#e8b07a"],
 'glow': ["#1c5866","#2a8ea2","#46c4d8","#8ae6f0","#d2fafe"], 'st': ["#2e3238","#474d56","#646b76","#848c97","#a7aeb8","#c6ccd4"],
 'leather': ["#3a2414","#5c3a22","#7e5232","#a06c44"], 'tu': ["#1f2126","#2f3238","#43474f","#5a5f69","#737985","#8e95a1"],
 'hot': ["#7a3108","#b45414","#e07a22","#ffa840","#ffd88a"], 'vo': ["#140d1c","#221632","#33224a","#47335f","#5e4a7c","#77649a"],
 'bone': ["#6e6452","#9a8e74","#c4b896","#e2d8bc"], 'violet': ["#5a1f96","#8440d0","#b278f6","#dcbcff"],
 'wood': ["#3b2614","#5a3b20","#7a5230","#9a6c40","#b98a55"], 'white': ["#8f8f99","#c4c4cc","#ececf0"], 'mars': ["#5a1a0e","#8e2a14","#c4441e","#e8703a","#f6a070"], 'blood': ["#2a0a0e","#5a1218","#8e1e24","#c43038"], 'gold': ["#5c4012","#8e6a1e","#c49a32","#e8c45a"], 'slit': ["#121418","#1e2126","#2c3036"], 'dia': ["#0f5a5a","#1a9a92","#3fd9c8","#a6f5ec"]}
def rgb(h): return np.array([int(h[i:i+2], 16) for i in (1, 3, 5)], float)
def lab(c):
    c = np.asarray(c, float) / 255; c = np.where(c > 0.04045, ((c + 0.055) / 1.055) ** 2.4, c / 12.92)
    M = np.array([[0.4124, 0.3576, 0.1805], [0.2126, 0.7152, 0.0722], [0.0193, 0.1192, 0.9505]])
    xyz = c @ M.T / np.array([0.9505, 1, 1.089]); f = np.where(xyz > 0.008856, np.cbrt(xyz), 7.787 * xyz + 16 / 116)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]), 200 * (f[..., 1] - f[..., 2])], -1)
def toks(ramps):
    out = []
    for r in ramps:
        for i, h in enumerate(HEX[r]): out.append((f'{r}.{i}', rgb(h)))
    return out
def pixelize(sprite, size, ramps, pad=1, flip=False, outline=True):
    """sprite -> size x size token grid: palette-snap the drawing, then one colour per cell (majority), dark outline."""
    a = np.array(sprite.convert('RGBA'))
    if flip: a = a[:, ::-1]
    T = toks(ramps); P = lab(np.array([c for _, c in T]))
    H, W = a.shape[:2]; inner = size - 2 * pad
    k = max(H, W) / inner
    gw, gh = max(1, round(W / k)), max(1, round(H / k))
    ox, oy = (size - gw) // 2, (size - gh) // 2
    grid = [[None] * size for _ in range(size)]
    L = lab(a[..., :3])
    for j in range(gh):
        for i in range(gw):
            y0, y1 = int(j * H / gh), int((j + 1) * H / gh); x0, x1 = int(i * W / gw), int((i + 1) * W / gw)
            cell = a[y0:y1, x0:x1]; al = cell[..., 3] > 128
            if al.mean() < 0.45: continue
            l = L[y0:y1, x0:x1][al]
            d = ((l[:, None, :] - P[None]) ** 2).sum(-1).argmin(1)
            grid[oy + j][ox + i] = T[np.bincount(d).argmax()][0]
    if outline:   # edge pixels -> darkest step of their own ramp (vanilla style, no black)
        g2 = [row[:] for row in grid]
        for y in range(size):
            for x in range(size):
                if grid[y][x] is None: continue
                edge = any(not (0 <= y + dy < size and 0 <= x + dx < size) or grid[y + dy][x + dx] is None for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if edge: g2[y][x] = grid[y][x].split('.')[0] + '.0'
        grid = g2
    return grid
def render(grid):
    s = len(grid); im = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    for y in range(s):
        for x in range(s):
            t = grid[y][x]
            if t: n, i = t.split('.'); im.putpixel((x, y), tuple(int(v) for v in rgb(HEX[n][int(i)])) + (255,))
    return im

def lighten(sprite, f, add=0):
    a = np.array(sprite.convert('RGBA')).astype(float); a[..., :3] = np.clip(a[..., :3] * f + add, 0, 255)
    return Image.fromarray(a.astype(np.uint8))
def light(grid):
    """top-left light: inner pixels touching the outline on their upper/left side go one step lighter,
    on their lower/right side one step darker"""
    s = len(grid); g2 = [r[:] for r in grid]
    def step(t, d):
        n, i = t.split('.'); m = len(HEX[n]) - 1
        return f'{n}.{max(1, min(m, int(i) + d))}'
    for y in range(s):
        for x in range(s):
            t = grid[y][x]
            if not t or t.endswith('.0'): continue
            up = y == 0 or grid[y - 1][x] is None or grid[y - 1][x].endswith('.0')
            lf = x == 0 or grid[y][x - 1] is None or grid[y][x - 1].endswith('.0')
            dn = y == s - 1 or grid[y + 1][x] is None or grid[y + 1][x].endswith('.0')
            rt = x == s - 1 or grid[y][x + 1] is None or grid[y][x + 1].endswith('.0')
            if (up or lf) and not (dn and rt): g2[y][x] = step(t, +1)
            elif (dn or rt) and not (up or lf): g2[y][x] = step(t, -1)
    return g2
