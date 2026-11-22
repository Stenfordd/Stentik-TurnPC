#!/usr/bin/env python3
"""2.0 art: Gemini sheets (by David) -> textures. Run from ctc-core/: python3 tools/apply_art.py <gemini dir> <kubejs assets dir>
Writes CtC Core art into tools/art (gen_armor.py / gen_assets.py copy it into resources) and the KubeJS weapon icons."""
import os, sys, shutil
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gemini_sprites import icons

G, KUBE = sys.argv[1], sys.argv[2]
ART, ARMOR = 'tools/art', 'tools/art/armor'
ITEM = 'src/main/resources/assets/ctccore/textures/item'
os.makedirs(ARMOR, exist_ok=True); os.makedirs(ITEM, exist_ok=True)

def save(a, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); Image.fromarray(a).save(path)

def lighten(a, f=1.17):
    b = a.copy(); b[..., :3] = np.clip(b[..., :3].astype(float) * f + 6, 0, 255).astype(np.uint8); return b

def cracked(a):
    """Grey, glow-less, cracked version of an icon (Cracked VoidSteel)."""
    b = a.copy(); rgb = b[..., :3].astype(float)
    lum = rgb @ [0.3, 0.59, 0.11]
    g = np.clip(lum * 0.85 + 18, 0, 200)
    b[..., 0] = g; b[..., 1] = g; b[..., 2] = np.clip(g * 1.04, 0, 210)
    op = b[..., 3] > 0
    ys, xs = np.nonzero(op)
    if len(xs):  # a dark zigzag crack across the middle
        cx, y0, y1 = int(xs.mean()), ys.min() + 2, ys.max() - 2
        x = cx - 2
        for y in range(y0, y1):
            x += (1 if (y // 3) % 2 else -1)
            for dx in (0, 1):
                if 0 <= x + dx < b.shape[1] and op[y, x + dx]:
                    b[y, x + dx, :3] = (38, 36, 42)
    return b

T = ['pickaxe', 'axe', 'shovel', 'hoe']
sheets = {
    'voidsteel': icons(f'{G}/tools/D_voidsteel_tools.png', n=32),           # sword pick axe shovel hoe battleaxe bow
    'steel': [lighten(x) for x in icons(f'{G}/tools/Fa2_steel_tools.png', n=32)],
    'titanium': icons(f'{G}/tools/Fb_titanium.png', n=32),
    'tungsten': icons(f'{G}/tools/Fc_tungsten.png', n=32),
}
for tier, sp in sheets.items():
    assert len(sp) >= 5, (tier, len(sp))
    for i, t in enumerate(T):
        save(sp[i + 1], f'{ART}/{tier}_{t}.png')
# KubeJS weapons: flat Gemini icons (tungsten keeps its 3D hammer)
for name, img in (('steel_saber', sheets['steel'][0]), ('titanium_blade', sheets['titanium'][0]),
                  ('voidsteel_rapier', sheets['voidsteel'][0]), ('voidsteel_battle_axe', sheets['voidsteel'][5])):
    save(img, f'{KUBE}/textures/item/{name}.png')
# bows: titanium has 4 drawn frames, voidsteel only the resting one
tb = icons(f'{G}/tools/E_titanium_bow.png', n=32, gap=8)
save(tb[0], f'{ITEM}/titanium_bow.png')
for k in range(3):
    save(tb[min(k + 1, len(tb) - 1)], f'{ITEM}/titanium_bow_pulling_{k}.png')
vb = sheets['voidsteel'][6]
save(vb, f'{ITEM}/voidsteel_bow.png')
for k in range(3):
    save(vb, f'{ITEM}/voidsteel_bow_pulling_{k}.png')
# arrows and boss trophies
ar = icons(f'{G}/arrows/arrows.png', n=32)
save(ar[0], f'{ITEM}/diamond_tipped_arrow.png'); save(ar[1], f'{ITEM}/void_tipped_arrow.png')
tr = icons(f'{G}/trophies.png', n=32)
for img, n in zip(tr, ['dragon_scale', 'void_heart', 'wither_core', 'tidal_pearl', 'infernal_rivet']):
    save(img, f'{ITEM}/{n}.png')
# VoidSteel armor icons (+ cracked versions)
ai = icons(f'{G}/icons.png', n=32, merge=((3, 4),))
for img, p in zip(ai, ['helmet', 'chestplate', 'leggings', 'boots']):
    save(img, f'{ARMOR}/voidsteel_{p}.png')
    save(cracked(img), f'{ITEM}/cracked_voidsteel_{p}.png')
# worn layers (tools/worn, made by worn_from_reference.py + fix_layers.py)
for tier in ('voidsteel', 'steel', 'titanium', 'tungsten'):
    for k in (1, 2):
        src = f'tools/worn/{tier}_layer_{k}.png'
        a = np.array(Image.open(src).convert('RGBA'))
        if tier == 'steel':
            a = lighten(a, 1.12)
        save(a, f'{ARMOR}/{tier}_layer_{k}.png')
print('art written')
