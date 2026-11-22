"""Quest-book planets as 48x48 pixel art (shown at 5x = 240 px in the book). Light from the upper left."""
import json, math, random, sys
from PIL import Image
exec(open('pxicons.py').read().split('\nif __name__')[0])
HEX.update({
    'moon': ["#2c2e36", "#4a4d58", "#6f737f", "#9a9ea8", "#c4c7ce", "#e6e8ec"],
    'merc': ["#2e2620", "#4c3f34", "#6e5d4c", "#94806a", "#b8a48a", "#d8c8ae"],
    'venus': ["#3e2a10", "#6a4a18", "#9a7024", "#c89a3a", "#e6c266", "#f6e2a0"],
    'marsp': ["#3a1208", "#62200e", "#8e3418", "#b84e26", "#d8743e", "#f0a066"],
})
N = 48
R = 21.5
C = (N - 1) / 2

def noise(seed, scale):
    rnd = random.Random(seed)
    pts = [(rnd.uniform(0, N), rnd.uniform(0, N), rnd.uniform(.5, 1)) for _ in range(40)]
    def f(x, y):
        return sum(w * math.exp(-((x - px) ** 2 + (y - py) ** 2) / (2 * scale ** 2)) for px, py, w in pts)
    return f

def planet(ramp, seed, craters=0, bands=False, glow=None, caps=False):
    rnd = random.Random(seed)
    nz = noise(seed, 4.5)
    cr = [(rnd.uniform(10, 38), rnd.uniform(10, 38), rnd.uniform(1.5, 3.8)) for _ in range(craters)]
    g = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            dx, dy = x - C, y - C
            d = math.hypot(dx, dy)
            if d > R + .5:
                if glow and d <= R + 1.6: g[y][x] = glow
                continue
            # sphere normal, light from upper left
            nzv = math.sqrt(max(0, 1 - (d / R) ** 2))
            lit = max(0, (-dx * .55 - dy * .6) / R + nzv * .6)
            v = lit * 3.7 - .2
            if bands: v += .9 * math.sin((y + 2.5 * math.sin(x / 6 + seed)) / 3.1)
            else: v += (nz(x, y) - .9) * .9
            for cx, cy, r in cr:
                e = math.hypot(x - cx, y - cy)
                if e < r - .6: v -= .9          # crater floor
                elif e < r + .4: v += .7 if (x - cx) + (y - cy) > 0 else -.6   # rim: lit lower right edge
            i = int(round(max(0, min(5, v))))
            # ordered dither between steps keeps it pixel-art, not gradient
            if (x + y) % 2 and v - int(v) > .55 and i < 5: i = min(5, int(v) + 1)
            g[y][x] = f'{ramp}.{i}'
            if caps and abs(dy) + nz(x, y) * 1.5 > R * .9: g[y][x] = 'white.2' if lit > .45 else 'white.1' if lit > .15 else 'white.0'
    # outline: the dark step on the shadow side, none on the lit side (reads like the old renders)
    for y in range(N):
        for x in range(N):
            dx, dy = x - C, y - C
            d = math.hypot(dx, dy)
            if R - .5 < d <= R + .5 and dx + dy > -4: g[y][x] = f'{ramp}.0'
    return g

PLANETS = {
    'planet_moon': planet('moon', 3, craters=9),
    'planet_mercury': planet('merc', 7, craters=12),
    'planet_venus': planet('venus', 11, bands=True, glow='venus.3'),
    'planet_mars': planet('marsp', 5, craters=3, glow='marsp.4', caps=True),
}

if __name__ == '__main__':
    out = '/home/claude/installer/assets/ctcquestart/assets/ctc/textures/quests/' if 'save' in sys.argv else 'pl_'
    for n, g in PLANETS.items():
        render(g).resize((240, 240), Image.NEAREST).save(out + n + '.png')
    pv = Image.new('RGBA', (4 * 250, 250), (30, 30, 40, 255))
    for i, g in enumerate(PLANETS.values()): pv.alpha_composite(render(g).resize((240, 240), Image.NEAREST), (i * 250 + 5, 5))
    pv.save('planets_preview.png')
    json.dump(PLANETS, open('planets.json', 'w'))
    print('ok')
