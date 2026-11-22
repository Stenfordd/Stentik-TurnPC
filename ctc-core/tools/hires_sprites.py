#!/usr/bin/env python3
"""Gemini sheets -> high-resolution item textures (keeps the drawing as it is, no re-gridding).
Soft magenta keying: alpha from the distance to the key colour, then the magenta spill is removed from edge pixels."""
import numpy as np
from PIL import Image
from scipy import ndimage

KEY = np.array([255, 0, 255], float)

def matte(a, lo=38, hi=110):
    """Alpha from the colour distance to the sheet's key colour (taken from the border), then unmix the key."""
    rgb = a[..., :3].astype(float)
    border = np.concatenate([rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]])
    mag = border[(border[:, 0] > 120) & (border[:, 2] > 110) & (border[:, 1] < 110)]
    key = np.median(mag, axis=0) if len(mag) else KEY
    d = np.sqrt(((rgb - key) ** 2).sum(-1))
    alpha = np.clip((d - lo) / (hi - lo), 0, 1)
    with np.errstate(divide='ignore', invalid='ignore'):
        fg = (rgb - (1 - alpha)[..., None] * key) / np.maximum(alpha, 1e-3)[..., None]
    fg = np.clip(fg, 0, 255)
    # rim pixels that still lean to magenta: drop the tint
    rim = (alpha > 0) & (alpha < 1)
    lum = fg @ np.array([0.3, 0.59, 0.11])
    tint = rim & (np.minimum(fg[..., 0], fg[..., 2]) - fg[..., 1] > 35)
    fg[tint] = np.stack([lum[tint]] * 3, -1)
    return np.dstack([fg, alpha * 255]).astype(np.uint8)

def blobs(path, gap=4, min_px=600, merge=()):
    a0 = np.array(Image.open(path).convert('RGBA'))
    m = matte(a0)
    fg = m[..., 3] > 40
    lab, _ = ndimage.label(ndimage.binary_dilation(fg, iterations=gap))
    items = []
    for k, sl in enumerate(ndimage.find_objects(lab), start=1):
        own = (lab == k) & (m[..., 3] > 0)
        if (own & fg).sum() < min_px: continue
        yy, xx = np.nonzero(own)
        items.append([xx.min(), yy.min(), xx.max() + 1, yy.max() + 1, own])
    items.sort(key=lambda b: (b[0] + b[2]) / 2)
    for i, j in sorted(merge, reverse=True):
        grp = items[i:j + 1]; mm = np.zeros_like(fg)
        for g_ in grp: mm |= g_[4]
        items[i:j + 1] = [[min(g_[0] for g_ in grp), min(g_[1] for g_ in grp), max(g_[2] for g_ in grp), max(g_[3] for g_ in grp), mm]]
    out = []
    for x0, y0, x1, y1, own in items:
        s = m.copy(); s[~own] = 0
        out.append(Image.fromarray(s[y0:y1, x0:x1]))
    return out

def icon(sprite, size=128, fill=0.94, flip=False, rot=0):
    if flip: sprite = sprite.transpose(Image.FLIP_LEFT_RIGHT)
    if rot: sprite = sprite.rotate(rot, expand=True)
    w, h = sprite.size
    k = size * fill / max(w, h)
    sp = sprite.resize((max(1, round(w * k)), max(1, round(h * k))), Image.LANCZOS)
    # keep pixel-art edges crisp: snap alpha
    a = np.array(sp); al = a[..., 3]
    a[..., 3] = np.where(al > 110, 255, 0)
    # outline band (2 px in from the edge): magenta-leaning pixels become the dark outline colour
    op = a[..., 3] > 0
    inner = ndimage.binary_erosion(op, iterations=2)
    band = op & ~inner
    r, g, b = [a[..., i].astype(int) for i in range(3)]
    pink = band & (r - g > 22) & (b - g > 22) & (np.abs(r - b) < 70) & (g < 120)
    a[pink, :3] = (22, 17, 28)
    sp = Image.fromarray(a)
    out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    out.alpha_composite(sp, ((size - sp.width) // 2, (size - sp.height) // 2))
    return out
