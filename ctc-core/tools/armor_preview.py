#!/usr/bin/env python3
"""Front/back flat preview of an armor set on a player silhouette (layer_1 = helmet/chest/arms/boots, layer_2 = legs).
armor_preview.py out.png layer1 layer2 [layer1 layer2 ...]"""
import sys
from PIL import Image
def faces(l1, l2, back=False):
    s = l1.width // 64
    def f(img, x, y, w, h): return img.crop((x * s, y * s, (x + w) * s, (y + h) * s))
    o = Image.new('RGBA', (16 * s, 32 * s), (0, 0, 0, 0))
    skin = Image.new('RGBA', (16 * s, 32 * s), (0, 0, 0, 0))
    from PIL import ImageDraw
    d = ImageDraw.Draw(skin)
    for (x, y, w, h) in [(4, 0, 8, 8), (4, 8, 8, 12), (0, 8, 4, 12), (12, 8, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12)]:
        d.rectangle([x * s, y * s, (x + w) * s - 1, (y + h) * s - 1], fill=(190, 150, 120, 255))
    o.alpha_composite(skin)
    if not back:
        parts = [(l2, 4, 20, 4, 12, 4, 20), (l2, 4, 20, 4, 12, 8, 20), (l1, 4, 20, 4, 12, 4, 20), (l1, 4, 20, 4, 12, 8, 20),
                 (l1, 20, 20, 8, 12, 4, 8), (l2, 20, 20, 8, 12, 4, 8), (l1, 44, 20, 4, 12, 0, 8), (l1, 44, 20, 4, 12, 12, 8), (l1, 8, 8, 8, 8, 4, 0)]
    else:
        parts = [(l2, 12, 20, 4, 12, 4, 20), (l2, 12, 20, 4, 12, 8, 20), (l1, 12, 20, 4, 12, 4, 20), (l1, 12, 20, 4, 12, 8, 20),
                 (l1, 32, 20, 8, 12, 4, 8), (l2, 32, 20, 8, 12, 4, 8), (l1, 52, 20, 4, 12, 0, 8), (l1, 52, 20, 4, 12, 12, 8), (l1, 24, 8, 8, 8, 4, 0)]
    for img, x, y, w, h, ox, oy in parts:
        o.alpha_composite(f(img, x, y, w, h), (ox * s, oy * s))
    return o
out, rest = sys.argv[1], sys.argv[2:]
sets = [(Image.open(rest[i]).convert('RGBA'), Image.open(rest[i + 1]).convert('RGBA')) for i in range(0, len(rest), 2)]
tiles = []
for l1, l2 in sets:
    for b in (False, True):
        t = faces(l1, l2, b).resize((128, 256), Image.NEAREST); tiles.append(t)
W = len(tiles) * 140
im = Image.new('RGBA', (W, 270), (70, 70, 80, 255))
for i, t in enumerate(tiles): im.alpha_composite(t, (i * 140 + 6, 7))
im.save(out)
