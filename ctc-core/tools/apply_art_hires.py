#!/usr/bin/env python3
"""2.0 art, take 2: Gemini sheets -> 128px textures that keep the drawing (no re-gridding).
python3 tools/apply_art_hires.py <gemini dir> <kubejs assets/ctc dir>"""
import os, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from hires_sprites import blobs, icon

G, KUBE = sys.argv[1], sys.argv[2]
ITEM = 'src/main/resources/assets/ctccore/textures/item'
ART = 'tools/art'

def lighten(im, f=1.15):
    a = np.array(im).astype(float); a[..., :3] = np.clip(a[..., :3] * f + 6, 0, 255); return Image.fromarray(a.astype(np.uint8))

def cracked(im):
    a = np.array(im).astype(float)
    lum = a[..., :3] @ [0.3, 0.59, 0.11]
    g = np.clip(lum * 0.8 + 22, 0, 200)
    a[..., 0] = g; a[..., 1] = g; a[..., 2] = np.clip(g * 1.05, 0, 215)
    out = Image.fromarray(a.astype(np.uint8))
    from PIL import ImageDraw
    d = ImageDraw.Draw(out)
    al = np.array(im)[..., 3] > 0
    ys, xs = np.nonzero(al)
    cx, y0, y1 = int(xs.mean()), ys.min() + 10, ys.max() - 10
    pts, x = [], cx - 6
    for y in range(y0, y1, 9):
        x += 7 if (y // 9) % 2 else -7; pts.append((x, y))
    d.line(pts, fill=(30, 28, 34, 255), width=4)
    a2 = np.array(out); a2[..., 3] = np.array(im)[..., 3]
    return Image.fromarray(a2)

def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)

T = ['pickaxe', 'axe', 'shovel', 'hoe']
sheets = {'voidsteel': blobs(f'{G}/tools/D_voidsteel_tools.png', gap=1),
          'steel': [lighten(b) for b in blobs(f'{G}/tools/Fa2_steel_tools.png', gap=1)],
          'titanium': blobs(f'{G}/tools/Fb_titanium.png', gap=1),
          'tungsten': blobs(f'{G}/tools/Fc_tungsten.png', gap=1)}
for tier, sp in sheets.items():
    for i, t in enumerate(T):
        ic = icon(sp[i + 1]); save(ic, f'{ITEM}/{tier}_{t}.png'); save(ic, f'{ART}/{tier}_{t}.png')
for name, img in (('steel_saber', sheets['steel'][0]), ('titanium_blade', sheets['titanium'][0]),
                  ('voidsteel_sword', sheets['voidsteel'][0]), ('voidsteel_battle_axe', sheets['voidsteel'][5])):
    save(icon(img), f'{KUBE}/textures/item/{name}.png')
# bows: mirrored so the arc points forward like the vanilla bow (arrow tip to the top right)
tb = blobs(f'{G}/tools/E_titanium_bow.png', gap=1)
save(icon(tb[0], flip=True), f'{ITEM}/titanium_bow.png')
for k in range(3): save(icon(tb[k + 1], flip=True), f'{ITEM}/titanium_bow_pulling_{k}.png')
vb = icon(sheets['voidsteel'][6], flip=True)
save(vb, f'{ITEM}/voidsteel_bow.png')
for k in range(3): save(vb, f'{ITEM}/voidsteel_bow_pulling_{k}.png')
ar = blobs(f'{G}/arrows/arrows.png', gap=1)
save(icon(ar[0]), f'{ITEM}/diamond_tipped_arrow.png'); save(icon(ar[1]), f'{ITEM}/void_tipped_arrow.png')
tr = blobs(f'{G}/trophies.png', gap=1)
for img, n in zip(tr, ['dragon_scale', 'void_heart', 'wither_core', 'tidal_pearl', 'infernal_rivet']): save(icon(img), f'{ITEM}/{n}.png')
ai = blobs(f'{G}/icons.png', gap=1, merge=((3, 4),))
for img, p in zip(ai, ['helmet', 'chestplate', 'leggings', 'boots']):
    ic = icon(img); save(ic, f'{ITEM}/voidsteel_{p}.png'); save(ic, f'{ART}/armor/voidsteel_{p}.png')
    save(cracked(ic), f'{ITEM}/cracked_voidsteel_{p}.png')
print('art ok')
