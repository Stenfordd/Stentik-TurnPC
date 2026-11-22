#!/usr/bin/env python3
"""Gemini armor references (front/back + side/top, magenta background) -> armor layers that keep Gemini's shading.
Every model pixel becomes S x S texels resampled from the drawing (no re-gridding, no flat fills).
worn_hd.py <front_back.png> <side_top.png> <out_prefix> [S=4] [--nowings]"""
import sys, os
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from hires_sprites import matte

fb_path, st_path, out = sys.argv[1], sys.argv[2], sys.argv[3]
S = int(sys.argv[4]) if len(sys.argv) > 4 and sys.argv[4].isdigit() else 4
NOWINGS = '--nowings' in sys.argv

def load(p):
    a = matte(np.array(Image.open(p).convert('RGBA')))
    return a

def figures(a, n):
    """Bounding boxes of the n biggest figures, left to right."""
    from scipy import ndimage
    op = a[..., 3] > 128
    lab, k = ndimage.label(ndimage.binary_dilation(op, iterations=6))
    sizes = ndimage.sum(op, lab, range(1, k + 1))
    keep = np.argsort(sizes)[::-1][:n] + 1
    boxes = []
    for i in keep:
        ys, xs = np.nonzero((lab == i) & op)
        boxes.append((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
    return sorted(boxes)

FB, ST = load(fb_path), load(st_path)
front_b, back_b = figures(FB, 2)
side_b, side2_b, top_b = figures(ST, 3)

def grab(a, box, units_w, units_h, ux, uy, uw, uh, mirror=False, anchor='center', horns_cut=0):
    """Model-unit rectangle of a figure -> (uh*S, uw*S) RGBA, resampled with a box filter (keeps shading)."""
    x0, y0, x1, y1 = box
    ppu = (y1 - y0 - horns_cut) / units_h
    y0 = y0 + horns_cut
    cx = (x0 + x1) / 2
    fx0 = cx - units_w / 2 * ppu
    l, t = fx0 + ux * ppu, y0 + uy * ppu
    crop = Image.fromarray(a).crop((int(round(l)), int(round(t)), int(round(l + uw * ppu)), int(round(t + uh * ppu))))
    crop = crop.resize((uw * S, uh * S), Image.BOX)
    r = np.array(crop)
    r[..., 3] = np.where(r[..., 3] > 100, 255, 0)
    return r[:, ::-1] if mirror else r

# the front/back figure is 16 units wide and 32 high; horns stick out above the head on the VoidSteel sheet
def head_top(a, box):
    """Rows above the head that are horns only: find where the head is at least 6 units wide."""
    x0, y0, x1, y1 = box
    op = a[y0:y1, x0:x1, 3] > 0
    width = op.sum(1)
    est = (y1 - y0) / 32
    for y in range(len(width)):
        if width[y] > 6 * est: return y
    return 0

hc_f, hc_b = head_top(FB, front_b), head_top(FB, back_b)
F = lambda *r, **k: grab(FB, front_b, 16, 32, *r, horns_cut=hc_f, **k)
B = lambda *r, **k: grab(FB, back_b, 16, 32, *r, horns_cut=hc_b, **k)
hc_s = head_top(ST, side_b)
SD = lambda *r, **k: grab(ST, side_b, 8, 32, *r, horns_cut=hc_s, **k)       # side view: 8 units wide (the head depth)
x0, y0, x1, y1 = top_b
TOP = Image.fromarray(ST).crop(top_b)

L1 = np.zeros((32 * S, 64 * S, 4), np.uint8); L2 = np.zeros_like(L1)
def put(t, u, v, a):
    h, w = a.shape[:2]; t[v * S:v * S + h, u * S:u * S + w] = a

def solid(a):
    """Fill holes inside a face with the nearest drawn colour (armor faces are closed plates)."""
    from scipy import ndimage
    op = a[..., 3] > 0
    if not op.any(): return a
    idx = ndimage.distance_transform_edt(~op, return_distances=False, return_indices=True)
    return a[idx[0], idx[1]]

def dewing(a):
    """VoidSteel back: the purple wing panels Gemini drew become dark plate (David: no wings)."""
    a = a.copy(); rgb = a[..., :3].astype(int)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    wing = (b > r + 8) & (b - g > 35) & (rgb.max(2) < 170) & (rgb.max(2) > 60)
    lum = (r + g + b) // 3
    a[wing, 0] = (lum[wing] * 0.42).astype(np.uint8) + 10
    a[wing, 1] = (lum[wing] * 0.38).astype(np.uint8) + 8
    a[wing, 2] = (lum[wing] * 0.55).astype(np.uint8) + 14
    return a

def unhorn(a):
    """Horn tips that reach into the helmet's top rows: removed (the 3D horns are separate cubes)."""
    a = a.copy(); rgb = a[..., :3].astype(int)
    horn = (rgb[..., 0] > 150) & (rgb[..., 2] > 180) & (rgb[..., 1] > 110)
    horn[3 * S:] = False
    a[horn] = 0
    return solid(a)

def depink(t):
    """Magenta-tinted rim texels left by the background: take the nearest clean colour."""
    from scipy import ndimage
    rgb = t[..., :3].astype(int)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    if NOWINGS:   # VoidSteel is purple itself: only the obvious key colour
        pink = (t[..., 3] > 0) & (r - g > 45) & (b - g > 45) & (np.abs(r - b) < 60)
    else:
        pink = (t[..., 3] > 0) & (r > g * 1.35 + 8) & (b > g * 1.35 + 8) & (np.abs(r - b) < 0.35 * np.maximum(r, b) + 10)
    if not pink.any(): return t
    good = (t[..., 3] > 0) & ~pink
    idx = ndimage.distance_transform_edt(~good, return_distances=False, return_indices=True)
    t = t.copy(); t[pink] = t[idx[0], idx[1]][pink]
    return t

# ---- head (helmet)
put(L1, 8, 8, unhorn(F(4, 0, 8, 8)))
put(L1, 24, 8, unhorn(B(4, 0, 8, 8)))
side_head = unhorn(SD(0, 0, 8, 8))
put(L1, 0, 8, side_head); put(L1, 16, 8, side_head[:, ::-1])
tw = TOP.width; th = TOP.height
# top view: the head is the middle square; shoulders/arms stick out on both sides
hs = int(min(th, tw * 8 / 24))
cx = tw // 2
head_sq = TOP.crop((cx - hs // 2, 0, cx + hs // 2, hs)).resize((8 * S, 8 * S), Image.BOX)
hsq = np.array(head_sq); hsq[..., 3] = np.where(hsq[..., 3] > 100, 255, 0)
put(L1, 8, 0, unhorn(hsq))
put(L1, 16, 0, solid(hsq))                     # bottom of the head (barely seen)
# ---- body: chestplate rows 0-10 on layer 1, belt rows 8-12 on layer 2
bf, bb = solid(F(4, 8, 8, 12)), solid(B(4, 8, 8, 12))
if NOWINGS: bb = dewing(bb)
bs = solid(SD(2, 8, 4, 12))
c = lambda a, r0, r1: (lambda z: (z.__setitem__(slice(0, r0 * S), 0), z.__setitem__(slice(r1 * S, None), 0), z)[2])(a.copy())
put(L1, 20, 20, c(bf, 0, 10)); put(L1, 32, 20, c(bb, 0, 10)); put(L1, 16, 20, c(bs, 0, 10)); put(L1, 28, 20, c(bs, 0, 10)[:, ::-1])
top_body = np.array(Image.fromarray(bf[:4 * S]).resize((8 * S, 4 * S), Image.BOX)); put(L1, 20, 16, top_body)
put(L2, 20, 20, c(bf, 8, 12)); put(L2, 32, 20, c(bb, 8, 12)); put(L2, 16, 20, c(bs, 8, 12)); put(L2, 28, 20, c(bs, 8, 12)[:, ::-1])
# ---- arms (layer 1): front = viewer's left arm on the front view, back = viewer's right arm on the back view
af, ab = solid(F(0, 8, 4, 12)), solid(B(12, 8, 4, 12))
aside = solid(SD(2, 8, 4, 12))                 # in side view the arm covers the body side
put(L1, 44, 20, af); put(L1, 52, 20, ab); put(L1, 40, 20, aside); put(L1, 48, 20, aside[:, ::-1])
put(L1, 44, 16, af[:4 * S]); put(L1, 48, 16, af[-4 * S:])
# ---- legs: boots rows 8-12 on layer 1, leggings rows 0-8 on layer 2
lf, lb = solid(F(4, 20, 4, 12)), solid(B(8, 20, 4, 12))
ls = solid(SD(2, 20, 4, 12))
for layer, r0, r1 in ((L1, 8, 12), (L2, 0, 8)):
    for u, a in ((4, lf), (12, lb), (0, ls), (8, ls[:, ::-1])):
        put(layer, u, 20, c(a, r0, r1))
put(L1, 8, 16, lf[-4 * S:])                    # boot soles
if '--lighten' in sys.argv:
    for t in (L1, L2): t[..., :3] = np.clip(t[..., :3].astype(float) * 1.12 + 6, 0, 255).astype(np.uint8)
L1, L2 = depink(L1), depink(L2)
Image.fromarray(L1).save(out + '_layer_1.png'); Image.fromarray(L2).save(out + '_layer_2.png')
print('written', out)
