#!/usr/bin/env python3
"""2.0 armor, pixel version: hand-designed 1:1 pixel art (like vanilla armor: one texel = one model pixel) with real
3D parts on whole pixels. Motifs follow David's Gemini sheets (visors, trims, crystal), colours are 5-step ramps.
Writes textures (64x64: top = normal armor layout, bottom = 3D parts) and client/ArmorModels.java.
Run from ctc-core/: python3 tools/pixel_armor.py"""
import numpy as np
from PIL import Image

OUT = 'src/main/resources/assets/ctccore/textures/models/armor/'

RAMPS = {
    'steel':     dict(m=[(46, 50, 58), (72, 78, 88), (104, 112, 124), (140, 148, 160), (182, 190, 200)],
                      a=[(56, 38, 26), (84, 58, 36), (114, 82, 50), (150, 112, 70), (180, 140, 90)],      # leather straps
                      g=[(24, 26, 30), (34, 36, 42), (20, 20, 24), (40, 42, 48), (60, 64, 70)]),        # dark slits
    'titanium':  dict(m=[(96, 108, 120), (134, 146, 158), (172, 184, 194), (206, 214, 222), (234, 239, 243)],
                      a=[(98, 50, 26), (136, 72, 38), (176, 100, 54), (208, 134, 76), (232, 170, 110)],    # copper
                      g=[(26, 92, 110), (40, 140, 160), (70, 196, 214), (128, 230, 240), (200, 250, 255)]), # cyan glow
    'tungsten':  dict(m=[(34, 36, 42), (52, 55, 62), (74, 78, 88), (100, 105, 116), (128, 134, 146)],
                      a=[(120, 52, 14), (170, 84, 20), (214, 118, 34), (244, 156, 56), (255, 196, 110)],   # hot orange
                      g=[(140, 60, 10), (200, 100, 20), (250, 150, 40), (255, 200, 90), (255, 236, 170)]),
    'voidsteel': dict(m=[(20, 13, 28), (34, 23, 47), (50, 36, 70), (72, 54, 98), (98, 80, 128)],
                      a=[(118, 108, 90), (150, 140, 118), (184, 174, 148), (212, 204, 180), (236, 230, 210)],  # bone trim
                      g=[(90, 30, 150), (130, 56, 200), (170, 96, 240), (210, 150, 255), (238, 206, 255)]),  # violet glow
}

def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}

class Tex:
    def __init__(self, tier):
        self.a = np.zeros((64, 64, 4), np.uint8); self.r = RAMPS[tier]; self.tier = tier
    def px(self, x, y, c):
        if 0 <= x < 64 and 0 <= y < 64: self.a[y, x, :3] = c; self.a[y, x, 3] = 255
    def clear(self, rect, rows=None):
        u, v, w, h = rect
        for y in range(h):
            if rows is None or y in rows:
                self.a[v + y, u:u + w] = 0
    def plate(self, rect, light=0, ramp='m', rows=None, cols=None, noise=True):
        """A metal plate: base tone, light top/left edge, dark bottom/right edge, a little texture."""
        u, v, w, h = rect; R = self.r[ramp]; rng = np.random.default_rng(u * 131 + v * 17 + w)
        rows = range(h) if rows is None else rows
        cols = range(w) if cols is None else cols
        b = 2 + light
        for y in rows:
            for x in cols:
                k = b
                if y == min(rows) or x == min(cols): k = b + 1
                if y == max(rows) or x == max(cols): k = b - 1
                if y == max(rows) and len(rows) > 2: k = b - 2
                if noise and 0 < y < h - 1 and rng.random() < 0.05: k += rng.choice([-1, 1])
                self.px(u + x, v + y, R[max(0, min(4, k))])
    def line(self, rect, y, ramp='a', k=2, cols=None, x0=0, x1=None):
        u, v, w, h = rect; R = self.r[ramp]
        for x in (range(x0, w if x1 is None else x1) if cols is None else cols):
            self.px(u + x, v + y, R[k])
    def vline(self, rect, x, ramp='m', k=0, y0=0, y1=None):
        u, v, w, h = rect; R = self.r[ramp]
        for y in range(y0, h if y1 is None else y1): self.px(u + x, v + y, R[k])
    def dot(self, rect, x, y, ramp='m', k=4):
        u, v, w, h = rect; self.px(u + x, v + y, self.r[ramp][k])

def box(t, u, v, w, h, d, light=0):
    f = faces(u, v, w, h, d)
    t.plate(f['top'], light + 1); t.plate(f['bottom'], light - 1, noise=False)
    t.plate(f['front'], light); t.plate(f['back'], light - 1)
    t.plate(f['right'], light - 1); t.plate(f['left'], light - 1)
    return f

# ---------------------------------------------------------------- layer 1: helmet, chestplate, arms, boots
def layer1(tier):
    t = Tex(tier)
    H = box(t, 0, 0, 8, 8, 8)
    fr = H['front']
    if tier == 'steel':          # great helm: two eye slits and breathing holes
        t.line(fr, 3, 'g', 0, x0=1, x1=7); t.line(fr, 4, 'm', 1, x0=1, x1=7)
        for x in (2, 5): t.dot(fr, x, 6, 'g', 0)
        t.vline(fr, 3, 'm', 4, 4, 8); t.vline(fr, 4, 'm', 3, 4, 8)
    elif tier == 'titanium':     # rounded visor with a cyan band
        t.line(fr, 3, 'g', 2, x0=1, x1=7); t.line(fr, 4, 'g', 1, x0=1, x1=7); t.dot(fr, 1, 3, 'g', 3); t.dot(fr, 6, 3, 'g', 3)
        t.line(fr, 7, 'a', 2)
        for side in (H['right'], H['left']): t.dot(side, 3, 4, 'a', 3); t.dot(side, 4, 4, 'a', 2); t.dot(side, 3, 5, 'a', 1); t.dot(side, 4, 5, 'a', 2)
    elif tier == 'tungsten':     # T-visor glowing orange
        t.line(fr, 2, 'g', 2, x0=1, x1=7); t.line(fr, 3, 'g', 1, x0=1, x1=7)
        t.vline(fr, 3, 'g', 2, 3, 7); t.vline(fr, 4, 'g', 1, 3, 7)
        t.line(fr, 0, 'a', 1)
        for side in (H['right'], H['left']): t.line(side, 6, 'a', 1)
    else:                        # VoidSteel: narrow violet slit, dark visor, bone brow
        for x in range(1, 7):
            for y in range(2, 7): t.dot(fr, x, y, 'm', 0)
        t.line(fr, 3, 'g', 3, x0=1, x1=7); t.dot(fr, 1, 3, 'g', 2); t.dot(fr, 6, 3, 'g', 2)
        t.line(fr, 1, 'a', 1, x0=1, x1=7)
        t.vline(fr, 0, 'm', 3, 1, 8); t.vline(fr, 7, 'm', 1, 1, 8)
    # chestplate
    B = faces(16, 16, 8, 12, 4)
    for k in ('top', 'bottom', 'right', 'left', 'back', 'front'):
        t.plate(B[k], 0 if k == 'front' else -1 if k != 'top' else 1)
    fr, bk = B['front'], B['back']
    for f in (fr, bk): t.clear(f, rows=range(10, 12))
    t.clear(B['right'], rows=range(10, 12)); t.clear(B['left'], rows=range(10, 12))
    t.plate(fr, 1, rows=range(1, 5), cols=range(1, 7))            # chest plate pectorals
    t.vline(fr, 3, 'm', 1, 1, 9); t.vline(fr, 4, 'm', 3, 1, 9)    # centre seam
    t.vline(bk, 3, 'm', 1, 0, 10); t.vline(bk, 4, 'm', 3, 0, 10)
    if tier == 'steel':
        t.line(fr, 0, 'a', 2, x0=2, x1=6); t.line(fr, 8, 'a', 1); t.line(fr, 9, 'a', 2)
        for x in (1, 6): t.dot(fr, x, 9, 'm', 4)
    elif tier == 'titanium':
        t.line(fr, 0, 'a', 2, x0=2, x1=6); t.line(fr, 1, 'a', 1, x0=3, x1=5)
        t.line(fr, 5, 'g', 2, x0=1, x1=7); t.line(fr, 7, 'g', 1, x0=2, x1=6)
        t.line(fr, 9, 'a', 2); t.line(bk, 9, 'a', 1)
    elif tier == 'tungsten':
        for y, k in ((0, 2), (6, 3), (9, 2)): t.line(fr, y, 'a', k)
        t.vline(fr, 0, 'a', 1, 0, 10); t.vline(fr, 7, 'a', 1, 0, 10)
        t.line(bk, 2, 'a', 1); t.line(bk, 8, 'a', 1)
    else:
        t.line(fr, 0, 'g', 1, x0=1, x1=7); t.line(fr, 8, 'g', 1); t.vline(fr, 0, 'g', 0, 1, 8); t.vline(fr, 7, 'g', 0, 1, 8)
        for y in range(3, 7):
            for x in (3, 4): t.dot(fr, x, y, 'g', 3 if y in (4, 5) else 2)
        t.dot(fr, 3, 4, 'g', 4)
    # arms
    A = faces(40, 16, 4, 12, 4)
    for k in A: t.plate(A[k], 0 if k in ('front', 'right') else -1)
    for k in ('front', 'back', 'right', 'left'):
        f = A[k]
        t.plate(f, 0, ramp='m', rows=range(5, 8), noise=True)
        t.line(f, 4, 'm', 0); t.line(f, 8, 'a' if tier != 'voidsteel' else 'g', 2 if tier != 'voidsteel' else 1)
        t.line(f, 11, 'm', 0)
        if tier == 'titanium': t.line(f, 9, 'g', 2)
        if tier == 'tungsten': t.line(f, 10, 'a', 2)
    # boots (rows 8..11 of the leg on layer 1)
    L = faces(0, 16, 4, 12, 4)
    for k in ('front', 'back', 'right', 'left'):
        f = L[k]; t.clear(f)
        t.plate(f, 0 if k == 'front' else -1, rows=range(8, 12))
        t.line(f, 11, 'm', 0); t.line(f, 8, 'a' if tier != 'voidsteel' else 'a', 1)
        if k == 'front': t.line(f, 10, 'm', 3, x0=1, x1=3)
    t.plate(L['bottom'], -2, noise=False)
    return t

# ---------------------------------------------------------------- layer 2: leggings
def layer2(tier):
    t = Tex(tier)
    B = faces(16, 16, 8, 12, 4)
    for k in ('front', 'back', 'right', 'left'):
        t.plate(B[k], -1, rows=range(8, 12))
        t.line(B[k], 8, 'a', 2 if k == 'front' else 1)
    t.dot(B['front'], 3, 9, 'a', 4); t.dot(B['front'], 4, 9, 'a', 3); t.dot(B['front'], 3, 10, 'a', 3); t.dot(B['front'], 4, 10, 'a', 2)
    L = faces(0, 16, 4, 12, 4)
    for k in ('front', 'back', 'right', 'left'):
        f = L[k]
        t.plate(f, 0 if k == 'front' else -1, rows=range(0, 9))
        t.line(f, 4, 'm', 0); t.line(f, 8, 'm', 0)
        if tier == 'titanium' and k == 'front': t.line(f, 6, 'g', 2)
        if tier == 'tungsten': t.line(f, 2, 'a', 1)
        if tier == 'voidsteel': t.line(f, 2, 'g', 0)
    t.plate(L['top'], 1, noise=False)
    return t

# ---------------------------------------------------------------- 3D parts (whole pixels)
# (name, part, layer, box (x, y, z, w, h, d) in pixels, grow, look)
PAUL = [('pauldron_r', 'right_arm', 1, (-5, -4, -4, 6, 3, 8), 0.0, 'pauldron'), ('pauldron_l', 'left_arm', 1, (-1, -4, -4, 6, 3, 8), 0.0, 'pauldron')]
PAUL2 = [('pauldron2_r', 'right_arm', 1, (-5, -1, -4, 5, 2, 8), -0.1, 'plate'), ('pauldron2_l', 'left_arm', 1, (0, -1, -4, 5, 2, 8), -0.1, 'plate')]
KNEE = [('knee_r', 'right_leg', 2, (-2, 4, -3, 4, 3, 1), 0.15, 'knee'), ('knee_l', 'left_leg', 2, (-2, 4, -3, 4, 3, 1), 0.15, 'knee')]
BREAST = ('breast', 'body', 1, (-3, 1, -4, 6, 6, 1), 0.0, 'breast')
CREST = ('crest', 'head', 1, (-1, -11, -5, 2, 2, 10), -0.25, 'crest')
BRIM = ('brim', 'head', 1, (-5, -7, -6, 10, 1, 1), 0.0, 'brim')
CUFF = [('cuff_r', 'right_leg', 1, (-3, 8, -3, 6, 1, 6), 0.3, 'cuff'), ('cuff_l', 'left_leg', 1, (-3, 8, -3, 6, 1, 6), 0.3, 'cuff')]
GEM = ('gem', 'body', 1, (-1, 2, -5, 2, 3, 1), 0.0, 'gem')
PARTS = {'steel': PAUL + PAUL2 + [('ridge', 'body', 1, (-1, 1, -4, 2, 8, 1), 0.0, 'ridge'), CREST] + KNEE,
         'titanium': PAUL + [BREAST, BRIM] + KNEE,
         'tungsten': PAUL + PAUL2 + [BREAST, CREST] + KNEE + CUFF,
         'voidsteel': PAUL + [BREAST, GEM] + KNEE}

def paint_part(t, look, u, v, w, h, d):
    f = box(t, u, v, w, h, d, light=0)
    fr = f['front']
    acc = 'g' if t.tier == 'voidsteel' and look in ('gem',) else 'a'
    if look == 'pauldron':
        for k in ('front', 'back', 'right', 'left'): t.line(f[k], h - 1, 'a', 2)
        tp = f['top']; t.plate(tp, 1)
        if t.tier == 'tungsten': t.line(tp, 0, 'a', 2); t.line(tp, d - 1, 'a', 1)
        if t.tier == 'voidsteel':
            for (x, y) in ((1, 2), (4, 5), (2, 6)): t.dot(tp, x, y, 'g', 2)
            for k in ('front', 'back'): t.dot(f[k], 1, 1, 'g', 2); t.dot(f[k], w - 2, 1, 'g', 3)
        if t.tier == 'titanium': t.line(tp, d // 2, 'a', 2)
    elif look == 'knee':
        t.dot(fr, 1, 1, 'a', 3); t.dot(fr, 2, 1, 'a', 2)
        if t.tier == 'voidsteel': t.dot(fr, 1, 1, 'g', 2); t.dot(fr, 2, 1, 'g', 3)
    elif look == 'breast':
        t.vline(fr, w // 2 - 1, 'm', 1); t.vline(fr, w // 2, 'm', 3)
        t.line(fr, 0, 'a', 3)
        if t.tier == 'titanium': t.line(fr, 3, 'g', 2, x0=1, x1=w - 1)
        if t.tier == 'tungsten': t.line(fr, 3, 'a', 3)
    elif look == 'gem':
        for k in f:
            uu, vv, ww, hh = f[k]
            for y in range(hh):
                for x in range(ww): t.px(uu + x, vv + y, t.r['g'][3 if (x + y) % 3 else 4])
        t.dot(fr, 0, 2, 'g', 1); t.dot(fr, 1, 2, 'g', 2)
    elif look in ('crest', 'ridge'):
        for k in f: t.line(f[k], 0, 'a' if t.tier != 'steel' else 'm', 3 if t.tier != 'steel' else 4)
    elif look == 'brim':
        t.line(fr, 0, 'a', 2)
    elif look == 'cuff':
        for k in ('front', 'back', 'right', 'left'): t.line(f[k], 0, 'a', 2)

def build(tier):
    t1, t2 = layer1(tier), layer2(tier)
    # VoidSteel horns: corner u 56..64, v 0..8 (crystal)
    if tier == 'voidsteel':
        for y in range(8):
            for x in range(8): t1.px(56 + x, y, RAMPS['voidsteel']['g'][4 if (x + y) % 4 == 0 else 3])
    cur = {1: [0, 32, 0], 2: [0, 32, 0]}
    java = {1: [], 2: []}
    placed = {}
    for name, part, layer, (x, y, z, w, h, d), grow, look in PARTS[tier]:
        tex = t1 if layer == 1 else t2
        twin = name[:-2] + '_r' if name.endswith('_l') else None
        if twin in placed:
            u, v = placed[twin]; mir = '.mirror()'
        else:
            span = 2 * (w + d)
            if cur[layer][0] + span > 64: cur[layer][0] = 0; cur[layer][1] += cur[layer][2]; cur[layer][2] = 0
            u, v = cur[layer][0], cur[layer][1]
            assert v + h + d <= 64, (tier, name)
            cur[layer][0] += span; cur[layer][2] = max(cur[layer][2], h + d)
            placed[name] = (u, v); mir = ''
            paint_part(tex, look, u, v, w, h, d)
        java[layer].append(f'            root.getChild("{part}").addOrReplaceChild("{name}", CubeListBuilder.create().texOffs({u}, {v}){mir}'
                           f'.addBox({x}f, {y}f, {z}f, {w}f, {h}f, {d}f, new CubeDeformation({grow}f)), PartPose.ZERO);')
    Image.fromarray(t1.a).save(f'{OUT}{tier}_layer_1.png'); Image.fromarray(t2.a).save(f'{OUT}{tier}_layer_2.png')
    return java

# Sets drawn by hand in Pixelwright (tools/art/pixel/<tier>_layer_N.png) replace the generated ones; their 3D parts:
HAND = {'titanium': {
    1: [('right_arm', 'pauldron_r', 0, 32, '', (-5, -4, -4, 6, 3, 8), 0.0), ('left_arm', 'pauldron_l', 0, 32, '.mirror()', (-1, -4, -4, 6, 3, 8), 0.0),
        ('body', 'breast', 28, 32, '', (-3, 1, -4, 6, 6, 1), 0.0), ('head', 'brim', 42, 32, '', (-5, -7, -6, 10, 1, 1), 0.0)],
    2: [('right_leg', 'knee_r', 0, 32, '', (-2, 4, -3, 4, 3, 1), 0.15), ('left_leg', 'knee_l', 0, 32, '.mirror()', (-2, 4, -3, 4, 3, 1), 0.15)]}}
import os, shutil

blocks = []
for tier in RAMPS:
    j = build(tier)
    if tier in HAND and os.path.exists(f'tools/art/pixel/{tier}_layer_1.png'):
        for k in (1, 2): shutil.copy(f'tools/art/pixel/{tier}_layer_{k}.png', f'{OUT}{tier}_layer_{k}.png')
        j = {k: [f'            root.getChild("{part}").addOrReplaceChild("{name}", CubeListBuilder.create().texOffs({u}, {v}){m}'
                 f'.addBox({x}f, {y}f, {z}f, {w}f, {h}f, {d}f, new CubeDeformation({g}f)), PartPose.ZERO);'
                 for part, name, u, v, m, (x, y, z, w, h, d), g in HAND[tier][k]] for k in (1, 2)}
    for layer in (1, 2):
        blocks.append(f'        case "{tier}_{layer}" -> {{\n' + '\n'.join(j[layer]) + '\n        }')
src = open('src/main/java/dev/stentik/ctccore/client/ArmorModels.java').read()
head, rest = src.split('        switch (tier + "_" + layer) {\n', 1)
tail = rest[rest.index('            default -> { }'):]
open('src/main/java/dev/stentik/ctccore/client/ArmorModels.java', 'w').write(head + '        switch (tier + "_" + layer) {\n' + '\n'.join(blocks) + '\n' + tail)
print('pixel armor ok')
