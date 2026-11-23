"""Elite mob models: real Minecraft-layout skins (our own pixel art, 64x64 / 64x32) + a 3D preview of the vanilla
box models. One skin per chip, so 'Ember Zombie', 'Kinetic Skeleton'... Also the Wither in phase 2."""
import math, json, random, sys
import numpy as np
from PIL import Image

def hx(h): return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))
R = {
    'zskin': ["#1f3a1c", "#2f5a2a", "#457f3a", "#5f9c4c", "#7cb866"],
    'shirt': ["#16314f", "#22507a", "#2f6fa2", "#4a90c2"],
    'pants': ["#262a52", "#363b78", "#4a50a0"],
    'bone': ["#6e6452", "#9a8e74", "#c4b896", "#e2d8bc"],
    'creep': ["#1d3a17", "#2f6624", "#45902f", "#6cbf4a", "#a3e07a"],
    'ender': ["#0b0a10", "#16141e", "#221f2e", "#2e2a3d"],
    'tu': ["#1f2126", "#2f3238", "#43474f", "#5a5f69", "#737985"],
    'st': ["#2e3238", "#474d56", "#646b76", "#848c97", "#a7aeb8"],
    'gold': ["#5c4012", "#8e6a1e", "#c49a32", "#e8c45a"],
    'pcb': ["#0e0d12", "#1a1920", "#24232c"],
    'out': ["#1d1922"],
    'ember': ["#4b2110", "#8a3a18", "#e55619", "#f68f63", "#feccb7"],
    'kinetic': ["#184339", "#287a68", "#36c8a7", "#79e1ca", "#c2f4e9"],
    'lunar': ["#10274b", "#18448a", "#1968e5", "#639cf6", "#b7d3fe"],
    'void': ["#26104b", "#43188a", "#6719e5", "#9b63f6", "#d2b7fe"],
    'acid': ["#2f4417", "#557c26", "#85cb33", "#b1e377", "#ddf5c0"],
}
def C(t):
    n, i = t.split('.'); return hx(R[n][int(i)]) + (255,)

def noise(x, y, seed=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) % 97 / 97.0

class Skin:
    def __init__(self, w, h): self.im = Image.new('RGBA', (w, h), (0, 0, 0, 0)); self.w, self.h = w, h
    def put(self, x, y, t):
        if t and 0 <= x < self.w and 0 <= y < self.h: self.im.putpixel((x, y), C(t))
    def box(self, u, v, w, h, d, paint):
        """paint(face, x, y, fw, fh) -> token. faces: up, down, right, front, left, back (vanilla layout)"""
        rects = {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
                 'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
        for f, (x0, y0, fw, fh) in rects.items():
            for y in range(fh):
                for x in range(fw): self.put(x0 + x, y0 + y, paint(f, x, y, fw, fh))

def mottled(ramp, x, y, lo=1, hi=3, seed=0):
    n = noise(x, y, seed)
    return f'{ramp}.{lo if n < .14 else hi if n > .88 else (lo + hi) // 2}'

def chip_px(x, y, cx, cy, chip):
    """4x4 chip at (cx, cy) on a face: gold corner pins, pcb, glowing core"""
    dx, dy = x - cx, y - cy
    if not (0 <= dx < 4 and 0 <= dy < 4): return None
    if (dx in (0, 3)) and (dy in (0, 3)): return 'gold.2' if dy == 0 else 'gold.1'
    if dx in (0, 3) or dy in (0, 3): return 'pcb.1'
    return f'{chip}.4' if (dx, dy) == (1, 1) else f'{chip}.2'

def crack(x, y, seed, density=.06):
    return noise(x * 3 + 1, y * 5 + 2, seed) < density

# ------------------------------------------------------------------ skins

def zombie(chip):
    s = Skin(64, 64)
    def head(f, x, y, fw, fh):
        t = mottled('zskin', x, y, 2, 3, 1)
        if f == 'up': t = mottled('zskin', x, y, 2, 4, 2)
        if f == 'front':
            if y in (3, 4) and x in (1, 2, 5, 6): t = 'out.0' if y == 3 else f'{chip}.3' if x in (2, 5) else 'zskin.0'
            if y == 3 and x in (2, 5): t = f'{chip}.4'
            if y == 6 and 2 <= x <= 5: t = 'zskin.0'
            if y == 5 and x in (3, 4): t = 'zskin.1'
        return t
    s.box(0, 0, 8, 8, 8, head)
    def body(f, x, y, fw, fh):
        t = mottled('shirt', x, y, 1, 3, 3)
        if y >= 10: t = 'pants.1'
        if f == 'front':
            c = chip_px(x, y, 2, 3, chip)
            if c: return c
            if crack(x, y, 5, .08) and y > 6: return f'{chip}.3'
            if y == 1 and x in (0, 7): t = 'shirt.0'
        if f == 'back' and crack(x, y, 6, .05): t = 'shirt.0'
        return t
    s.box(16, 16, 8, 12, 4, body)
    def arm(f, x, y, fw, fh):
        if y < 4 and f != 'down': return mottled('shirt', x, y, 1, 3, 7)
        t = mottled('zskin', x, y, 1, 3, 8)
        if f in ('front', 'right', 'left') and crack(x, y, 9, .045): t = f'{chip}.3'
        if f == 'down': t = 'zskin.1'
        return t
    s.box(40, 16, 4, 12, 4, arm)
    s.box(32, 48, 4, 12, 4, arm)
    def leg(f, x, y, fw, fh):
        t = mottled('pants', x, y, 0, 2, 10)
        if y >= 10 or f == 'down': t = 'st.1' if y < 11 else 'st.0'
        if f == 'front' and crack(x, y, 11, .07) and y < 9: t = f'{chip}.2'
        return t
    s.box(0, 16, 4, 12, 4, leg)
    s.box(16, 48, 4, 12, 4, leg)
    return s.im

def skeleton(chip):
    s = Skin(64, 32)
    def head(f, x, y, fw, fh):
        t = mottled('bone', x, y, 1, 3, 1)
        if f == 'front':
            if y in (3, 4) and x in (1, 2, 5, 6): t = 'out.0'
            if y == 4 and x in (2, 5): t = f'{chip}.4'
            if y == 6 and 1 <= x <= 6: t = 'bone.0' if x % 2 else 'bone.3'
            if y == 5 and x in (3, 4): t = 'bone.0'
        return t
    s.box(0, 0, 8, 8, 8, head)
    def body(f, x, y, fw, fh):
        if f in ('front', 'back'):
            if x in (3, 4): t = 'bone.2'                       # spine
            elif y in (1, 3, 5, 7) and 0 < x < 7: t = 'bone.2' if f == 'back' else 'bone.3'   # ribs
            else: t = None
            if f == 'front':
                c = chip_px(x, y, 2, 2, chip)
                if c: return c
            if y >= 9 and 1 <= x <= 6: t = 'bone.1'            # pelvis
            return t
        return 'bone.1' if y in (1, 3, 5, 7) or y >= 9 else None
    s.box(16, 16, 8, 12, 4, body)
    def limb(f, x, y, fw, fh):
        t = mottled('bone', x, y, 1, 2, 4)
        if y in (5, 6) and f != 'up': t = 'bone.0'   # joint
        return t
    s.box(40, 16, 2, 12, 2, limb)
    s.box(0, 16, 2, 12, 2, limb)
    return s.im

def creeper(chip):
    s = Skin(64, 32)
    def head(f, x, y, fw, fh):
        t = mottled('creep', x, y, 2, 3, 1)
        if noise(x, y, 4) > .88: t = 'creep.4'
        if f == 'front':
            face = {(1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3), (3, 4), (4, 4), (2, 5), (3, 5), (4, 5), (5, 5), (2, 6), (5, 6), (3, 6), (4, 6)}
            if (x, y) in face: t = 'out.0'
            if (x, y) in ((2, 2), (6, 2)): t = f'{chip}.4'
        return t
    s.box(0, 0, 8, 8, 8, head)
    def body(f, x, y, fw, fh):
        t = mottled('creep', x, y, 1, 3, 2)
        if y < 2: t = 'st.3' if f != 'up' else 'st.4'           # iron collar
        if f == 'front':
            c = chip_px(x, y, 2, 4, chip)
            if c: return c
            if crack(x, y, 3, .1) and y > 8: t = f'{chip}.3'
        return t
    s.box(16, 16, 8, 12, 4, body)
    def leg(f, x, y, fw, fh):
        t = mottled('creep', x, y, 1, 2, 5)
        if f == 'front' and y == 4 and x == 1: t = f'{chip}.3'
        return t
    s.box(0, 16, 4, 6, 4, leg)
    return s.im

def enderman(chip):
    s = Skin(64, 32)
    def head(f, x, y, fw, fh):
        t = mottled('ender', x, y, 1, 2, 1)
        if f == 'front' and y == 4 and x in (0, 1, 2, 5, 6, 7):
            t = f'{chip}.3' if x not in (1, 6) else f'{chip}.4'
        return t
    s.box(0, 0, 8, 8, 8, head)
    s.box(0, 16, 8, 8, 8, lambda f, x, y, fw, fh: None)       # jaw layer left empty
    def body(f, x, y, fw, fh):
        t = mottled('ender', x, y, 1, 2, 2)
        if f == 'front':
            c = chip_px(x, y, 2, 2, chip)
            if c: return c
            if crack(x, y, 3, .07): t = f'{chip}.2'
        return t
    s.box(32, 16, 8, 12, 4, body)
    def limb(f, x, y, fw, fh):
        t = mottled('ender', x, y, 0, 1, 6)
        if crack(x, y, 7, .04): t = f'{chip}.1'
        return t
    s.box(56, 0, 2, 30, 2, limb)
    return s.im

def wither(phase2):
    s = Skin(64, 64)
    def headf(cx, chip, seed):
        def p(f, x, y, fw, fh):
            t = mottled('tu', x, y, 2, 3, seed)
            if f == 'front':
                ex = (1, fw - 3)
                if y == fh // 2 - 1 and (x in ex or x - 1 in ex):
                    t = 'out.0' if not phase2 else ('ember.4' if x in ex else 'ember.2')
                if y == fh - 2 and 1 <= x <= fw - 2: t = 'tu.0'
                if phase2 and chip:
                    c = chip_px(x, y, cx, 0, chip)
                    if c: return c
            if phase2 and f in ('right', 'left', 'up') and crack(x, y, seed + 9, .07): t = 'ember.3'
            return t
        return p
    s.box(0, 0, 8, 8, 8, headf(2, 'ember', 1))
    s.box(32, 0, 6, 6, 6, headf(1, 'acid', 2))
    def bar(f, x, y, fw, fh):
        t = mottled('tu', x, y, 1, 3, 3)
        if phase2 and f in ('front', 'up') and y == 0: t = 'kinetic.2' if x % 5 else 'kinetic.4'
        return t
    s.box(0, 16, 20, 3, 3, bar)
    def ribs(f, x, y, fw, fh):
        t = mottled('tu', x, y, 1, 3, 4)
        if phase2 and crack(x, y, 12, .12): t = 'acid.3'
        return t
    s.box(0, 22, 3, 10, 3, ribs)
    s.box(24, 22, 11, 2, 2, ribs)
    s.box(12, 22, 3, 6, 3, ribs)
    return s.im

# ------------------------------------------------------------------ models (vanilla box layouts)
# (u, v, x, y, z, w, h, d, pivot, (rotX, rotY, rotZ) in degrees, mirror)
HUM = lambda arm, leg, armforward=False: [
    (0, 0, -4, -8, -4, 8, 8, 8, (0, 0, 0), (0, 0, 0), False),
    (16, 16, -4, 0, -2, 8, 12, 4, (0, 0, 0), (0, 0, 0), False),
    (arm[0], arm[1], -arm[2] + 1 - arm[2] // 2 if False else -3 + (4 - arm[2]), -2, -arm[2] // 2, arm[2], arm[3], arm[2], (-5, 2, 0), (-90 if armforward else 0, 0, 0), False),
    (arm[0], arm[1], -1, -2, -arm[2] // 2, arm[2], arm[3], arm[2], (5, 2, 0), (-90 if armforward else 0, 0, 0), True),
    (leg[0], leg[1], -leg[2] // 2, 0, -leg[2] // 2, leg[2], leg[3], leg[2], (-1.9, 12, 0), (0, 0, 0), False),
    (leg[0], leg[1], -leg[2] // 2, 0, -leg[2] // 2, leg[2], leg[3], leg[2], (1.9, 12, 0), (0, 0, 0), True),
]
MODELS = {
    'zombie': [(0, 0, -4, -8, -4, 8, 8, 8, (0, 0, 0), (0, 0, 0), False),
               (16, 16, -4, 0, -2, 8, 12, 4, (0, 0, 0), (0, 0, 0), False),
               (40, 16, -3, -2, -2, 4, 12, 4, (-5, 2, 0), (-85, 0, 0), False),
               (40, 16, -1, -2, -2, 4, 12, 4, (5, 2, 0), (-95, 0, 0), True),
               (0, 16, -2, 0, -2, 4, 12, 4, (-1.9, 12, 0), (0, 0, 0), False),
               (0, 16, -2, 0, -2, 4, 12, 4, (1.9, 12, 0), (0, 0, 0), True)],
    'skeleton': [(0, 0, -4, -8, -4, 8, 8, 8, (0, 0, 0), (0, 0, 0), False),
                 (16, 16, -4, 0, -2, 8, 12, 4, (0, 0, 0), (0, 0, 0), False),
                 (40, 16, -1, -2, -1, 2, 12, 2, (-5, 2, 0), (-80, 0, 0), False),
                 (40, 16, -1, -2, -1, 2, 12, 2, (5, 2, 0), (0, 0, -8), True),
                 (0, 16, -1, 0, -1, 2, 12, 2, (-2, 12, 0), (0, 0, 0), False),
                 (0, 16, -1, 0, -1, 2, 12, 2, (2, 12, 0), (0, 0, 0), True)],
    'creeper': [(0, 0, -4, -8, -4, 8, 8, 8, (0, 6, 0), (0, 0, 0), False),
                (16, 16, -4, 0, -2, 8, 12, 4, (0, 6, 0), (0, 0, 0), False),
                (0, 16, -2, 0, -2, 4, 6, 4, (-2, 18, 4), (0, 0, 0), False),
                (0, 16, -2, 0, -2, 4, 6, 4, (2, 18, 4), (0, 0, 0), False),
                (0, 16, -2, 0, -2, 4, 6, 4, (-2, 18, -4), (0, 0, 0), False),
                (0, 16, -2, 0, -2, 4, 6, 4, (2, 18, -4), (0, 0, 0), False)],
    'enderman': [(0, 0, -4, -8, -4, 8, 8, 8, (0, -14, 0), (0, 0, 0), False),
                 (32, 16, -4, 0, -2, 8, 12, 4, (0, -14, 0), (0, 0, 0), False),
                 (56, 0, -1, -2, -1, 2, 30, 2, (-5, -12, 0), (0, 0, 4), False),
                 (56, 0, -1, -2, -1, 2, 30, 2, (5, -12, 0), (0, 0, -4), True),
                 (56, 0, -1, 0, -1, 2, 30, 2, (-2, -5, 0), (0, 0, 0), False),
                 (56, 0, -1, 0, -1, 2, 30, 2, (2, -5, 0), (0, 0, 0), True)],
    'wither': [(0, 16, -10, 3.9, -0.5, 20, 3, 3, (0, 0, 0), (0, 0, 0), False),
               (0, 22, 0, 0, 0, 3, 10, 3, (-2, 6.9, -0.5), (10, 0, 0), False),
               (24, 22, -4, 1.5, 0.5, 11, 2, 2, (-2, 6.9, -0.5), (10, 0, 0), False),
               (24, 22, -4, 4, 0.5, 11, 2, 2, (-2, 6.9, -0.5), (10, 0, 0), False),
               (24, 22, -4, 6.5, 0.5, 11, 2, 2, (-2, 6.9, -0.5), (10, 0, 0), False),
               (12, 22, 0, 0, 0, 3, 6, 3, (-2, 16.6, 1.2), (30, 0, 0), False),
               (0, 0, -4, -4, -4, 8, 8, 8, (0, 0, 0), (0, 0, 0), False),
               (32, 0, -4, -4, -4, 6, 6, 6, (-8, 4, 0), (0, 0, 0), False),
               (32, 0, -4, -4, -4, 6, 6, 6, (10, 4, 0), (0, 0, 0), False)],
}

def rot(p, r):
    x, y, z = p
    ax, ay, az = (math.radians(a) for a in r)
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return x, y, z

def render(model, skin, yaw=-30, pitch=12, scale=9, size=(300, 400)):
    tex = np.array(skin); th = skin.height
    quads = []
    for (u, v, x, y, z, w, h, d, piv, r, mirror) in MODELS[model]:
        x0, y0, z0, x1, y1, z1 = x, y, z, x + w, y + h, z + d
        faces = [  # (uv rect, point(s,t), normal)
            ((u + d, v + d, w, h), lambda s, t: (x0 + s * w, y0 + t * h, z0), (0, 0, -1)),
            ((u + 2 * d + w, v + d, w, h), lambda s, t: (x1 - s * w, y0 + t * h, z1), (0, 0, 1)),
            ((u, v + d, d, h), lambda s, t: (x1, y0 + t * h, z1 - s * d), (1, 0, 0)),
            ((u + d + w, v + d, d, h), lambda s, t: (x0, y0 + t * h, z0 + s * d), (-1, 0, 0)),
            ((u + d, v, w, d), lambda s, t: (x0 + s * w, y0, z1 - t * d), (0, -1, 0)),
            ((u + d + w, v, w, d), lambda s, t: (x0 + s * w, y1, z0 + t * d), (0, 1, 0)),
        ]
        for (fu, fv, fw, fh), pt, nrm in faces:
            fw_i, fh_i = max(1, int(round(fw))), max(1, int(round(fh)))
            for i in range(fw_i):
                for j in range(fh_i):
                    tu = int(fu + (fw_i - 1 - i if mirror else i)); tv = int(fv + j)
                    if not (0 <= tu < tex.shape[1] and 0 <= tv < tex.shape[0]): continue
                    c = tex[tv, tu]
                    if c[3] < 128: continue
                    corners = [pt(i / fw_i, j / fh_i), pt((i + 1) / fw_i, j / fh_i), pt((i + 1) / fw_i, (j + 1) / fh_i), pt(i / fw_i, (j + 1) / fh_i)]
                    world = [tuple(a + b for a, b in zip(rot(p, r), piv)) for p in corners]
                    n = rot(nrm, r)
                    quads.append((world, c, n))
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    def proj(p):
        x, y, z = p
        x, z = x * cy - z * sy, x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z
    light = np.array([-0.5, -0.8, -0.6]); light /= np.linalg.norm(light)
    out = []
    for world, c, n in quads:
        P = [proj(p) for p in world]
        nn = proj(n)
        if nn[2] > 0.01: continue   # back face
        lum = 0.55 + 0.45 * max(0, -(nn[0] * light[0] + nn[1] * light[1] + nn[2] * light[2]))
        glow = int(c[0]) + int(c[1]) + int(c[2]) > 520 or (max(c[:3]) - min(c[:3]) > 120 and max(c[:3]) > 190)
        col = tuple(int(min(255, ch * (1.0 if glow else lum))) for ch in c[:3])
        out.append((sum(p[2] for p in P) / 4, P, col))
    out.sort(key=lambda q: -q[0])
    from PIL import ImageDraw
    img = Image.new('RGBA', size, (0, 0, 0, 0)); dr = ImageDraw.Draw(img)
    ys = [p[1] for _, P, _ in out for p in P]; xs = [p[0] for _, P, _ in out for p in P]
    cx = size[0] / 2 - (min(xs) + max(xs)) / 2 * scale; cyy = size[1] / 2 - (min(ys) + max(ys)) / 2 * scale
    for _, P, col in out:
        dr.polygon([(cx + p[0] * scale, cyy + p[1] * scale) for p in P], fill=col + (255,), outline=col + (255,))
    return img

if __name__ == '__main__':
    out = '/tmp/claude-0/-home-claude/dbb0d9ba-f1cd-5617-86e0-38bb24bf8c87/scratchpad/mob3d/'
    import os; os.makedirs(out, exist_ok=True)
    cast = [('zombie', 'ember', zombie('ember'), 'Ember Zombie'), ('skeleton', 'kinetic', skeleton('kinetic'), 'Kinetic Skeleton'),
            ('creeper', 'lunar', creeper('lunar'), 'Lunar Creeper'), ('enderman', 'void', enderman('void'), 'Void Enderman'),
            ('zombie', 'acid', zombie('acid'), 'Acid Zombie'), ('wither', None, wither(False), 'Wither'), ('wither', None, wither(True), 'Wither, phase 2')]
    tiles = []
    for model, chip, skin, name in cast:
        skin.save(out + f'{model}_{chip or name.split()[-1]}.png')
        sc = 6 if model == 'enderman' else 9 if model != 'wither' else 8
        tiles.append((name, render(model, skin, scale=sc), skin))
    from PIL import ImageDraw, ImageFont
    W = 300 * 4; Hh = 470 * 2
    sheet = Image.new('RGBA', (W, Hh), (34, 36, 48, 255)); d = ImageDraw.Draw(sheet)
    for i, (name, img, skin) in enumerate(tiles):
        x, y = (i % 4) * 300, (i // 4) * 470
        sheet.alpha_composite(img, (x, y))
        sk = skin.resize((skin.width * 2, skin.height * 2), Image.NEAREST)
        sheet.alpha_composite(sk, (x + 300 - sk.width - 8, y + 470 - sk.height - 30))
        d.text((x + 10, y + 470 - 26), name, fill=(230, 220, 190, 255))
    sheet.save(out + 'sheet.png'); print('ok')
