"""Ender Dragon, second half (Void + Lunar + Gravity chips): glowing energy veins over its wings and plates.
Writes textures/entity/elite/dragon_glow.png (drawn full bright on top of the dragon below half health).
The vanilla dragon texture is used only as a mask of where the model's faces are (it is not copied or shipped):
pass its path as the first argument."""
import sys, random, os
from PIL import Image

ref = Image.open(sys.argv[1]).convert('RGBA')
OUT = os.path.join(os.path.dirname(__file__), '../../../src/main/resources/assets/ctccore/textures/entity/elite/dragon_glow.png')
W = Image.new('RGBA', (256, 256), (0, 0, 0, 0)); px = W.load()
mask = lambda x, y: 0 <= x < 256 and 0 <= y < 256 and ref.getpixel((x, y))[3] > 0
VIOLET = [(0x5a, 0x1f, 0x96), (0x84, 0x40, 0xd0), (0xb2, 0x78, 0xf6), (0xdc, 0xbc, 0xff)]
CYAN = [(0x3a, 0x5a, 0x9a), (0x6a, 0x90, 0xd8), (0x9f, 0xc4, 0xff), (0xe0, 0xf0, 0xff)]
rnd = random.Random(7)

def put(x, y, c):
    if mask(x, y): px[x, y] = c + (255,)

def vein(x, y, dx, dy, length, ramp, branch=0.08, depth=0):
    """A crackling line that drifts and splits: bright core, dim edge."""
    fx, fy = float(x), float(y)
    for i in range(length):
        if not mask(int(fx), int(fy)): return
        core = ramp[3] if i < length * 0.35 else ramp[2]
        put(int(fx), int(fy), core)
        put(int(fx) + 1, int(fy), ramp[1]); put(int(fx), int(fy) + 1, ramp[0])
        dx += rnd.uniform(-0.35, 0.35); dy += rnd.uniform(-0.35, 0.35)
        n = max(0.01, (dx * dx + dy * dy) ** 0.5); dx, dy = dx / n, dy / n
        fx += dx; fy += dy
        if depth < 2 and rnd.random() < branch:
            vein(fx, fy, dx + rnd.uniform(-1, 1), dy + rnd.uniform(-1, 1), int(length * 0.5), ramp, branch, depth + 1)

# wing membranes (two triangles at the lower left): veins fan out from the bones
for (sx, sy, dx, dy) in ((4, 100, 1, 0.6), (6, 125, 1, 0.1), (20, 110, 0.7, 0.8), (40, 104, 0.4, 1)):
    vein(sx, sy, dx, dy, 60, VIOLET)
for (sx, sy, dx, dy) in ((16, 158, 1, 0.4), (14, 186, 1, -0.2), (36, 196, 1, -0.9)):
    vein(sx, sy, dx, dy, 42, VIOLET, branch=0.05)
for (sx, sy, dx, dy) in ((8, 120, 1, -0.4), (44, 170, 0.6, 1)):
    vein(sx, sy, dx, dy, 34, CYAN, branch=0.04)
# body box: Gravity rings (cyan) around the body and Void lines along it
for y in (22, 46, 70):
    for x in range(0, 112):
        if x % 7 != 3: put(x, y, CYAN[2] if x % 2 else CYAN[1])
for (sx, sy) in ((10, 30), (40, 10), (70, 60), (90, 34), (24, 76)):
    vein(sx, sy, rnd.choice((1, -1)), rnd.uniform(-0.3, 0.3), 30, VIOLET)
# neck and tail plates (grey strips), head: a few glowing seams
for (sx, sy) in ((122, 92), (122, 100), (120, 140), (190, 92), (190, 100)):
    vein(sx, sy, 1, 0, 70, VIOLET, branch=0.02)
for (sx, sy) in ((116, 30), (150, 70), (200, 30), (196, 74)):
    vein(sx, sy, 1, 0.2, 24, CYAN, branch=0.0)
W.save(OUT)
print('ok', OUT)
