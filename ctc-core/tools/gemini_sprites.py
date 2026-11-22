#!/usr/bin/env python3
"""Turn Gemini pixel-art sheets (sprites on flat magenta) into clean Minecraft item textures.

    sprites(path) -> list of RGBA sprites in left-to-right order, each snapped to its own pixel grid
    to_icon(sprite, size=16) -> size x size RGBA icon (centered, palette cleaned, magenta fringe removed)
"""
import sys
from collections import Counter

import numpy as np
from PIL import Image


def magenta_mask(a):
    r, g, b = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
    return (r > 170) & (b > 150) & (g < 110) & (abs(r - b) < 90)


def components(mask, min_px=400, gap=6):
    """Bounding boxes of foreground blobs; blobs closer than `gap` px are merged (bow + string, arrow + feathers)."""
    from scipy import ndimage
    fg = ~mask
    grown = ndimage.binary_dilation(fg, iterations=gap)
    lab, n = ndimage.label(grown)
    boxes = []
    for sl in ndimage.find_objects(lab):
        ys, xs = sl
        sub = fg[ys, xs]
        if sub.sum() < min_px:
            continue
        yy, xx = np.nonzero(sub)
        boxes.append((xs.start + xx.min(), ys.start + yy.min(), xs.start + xx.max() + 1, ys.start + yy.max() + 1))
    return sorted(boxes, key=lambda b: (b[0] + b[2]) / 2)


def cell_size(a, mask):
    """Pixel-art cell size: the period that best explains where colors change along rows and columns."""
    rgb = a[..., :3].astype(int)
    edges = []
    dx = np.abs(np.diff(rgb, axis=1)).sum(-1) > 60
    dy = np.abs(np.diff(rgb, axis=0)).sum(-1) > 60
    xs = np.nonzero(dx.any(axis=0))[0] + 1
    ys = np.nonzero(dy.any(axis=1))[0] + 1
    best, best_s = -1, 8
    for s in np.arange(5.0, 48.0, 0.25):
        score = 0.0
        for pos in (xs, ys):
            if len(pos) == 0:
                continue
            ph = (pos % s) / s
            # concentration of phases (circular mean length)
            c = np.abs(np.exp(2j * np.pi * ph).mean())
            score += c
        score *= min(1.0, s / 8)  # avoid tiny periods that fit noise
        if score > best:
            best, best_s = score, s
    return best_s


def snap(sprite_rgba, s):
    """Resample a sprite to its native grid: majority color per cell (magenta/transparent cells drop out)."""
    a = sprite_rgba
    h, w = a.shape[:2]
    nx, ny = max(1, round(w / s)), max(1, round(h / s))
    out = np.zeros((ny, nx, 4), np.uint8)
    for j in range(ny):
        for i in range(nx):
            y0, y1 = int(j * h / ny), int((j + 1) * h / ny)
            x0, x1 = int(i * w / nx), int((i + 1) * w / nx)
            # sample the inner 60% of the cell to dodge blurry borders
            my, mx = max(1, (y1 - y0) // 5), max(1, (x1 - x0) // 5)
            cell = a[y0 + my:y1 - my or y1, x0 + mx:x1 - mx or x1].reshape(-1, 4)
            cell = cell[cell[:, 3] > 0]
            if len(cell) < max(1, ((y1 - y0) * (x1 - x0)) // 8):
                continue
            q = (cell[:, :3] // 8) * 8
            col, _ = Counter(map(tuple, q)).most_common(1)[0]
            pick = cell[(q == col).all(1)][:, :3].mean(0)
            out[j, i, :3] = pick.astype(np.uint8)
            out[j, i, 3] = 255
    return out


def sprites(path, gap=6, min_px=400):
    im = Image.open(path).convert("RGBA")
    a = np.array(im)
    mask = magenta_mask(a)
    a[mask] = 0
    s = cell_size(a, mask)
    out = []
    for (x0, y0, x1, y1) in components(mask, min_px=min_px, gap=gap):
        out.append(snap(a[y0:y1, x0:x1], s))
    return out, s


def clean(icon):
    """Drop isolated magenta-ish fringe pixels and lone specks."""
    a = icon.copy()
    r, g, b = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
    fringe = (r > 150) & (b > 120) & (g < 90) & (a[..., 3] > 0)
    a[fringe] = 0
    return a


def to_icon(sp, size=16):
    sp = clean(sp)
    ys, xs = np.nonzero(sp[..., 3])
    if len(xs) == 0:
        return np.zeros((size, size, 4), np.uint8)
    sp = sp[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    h, w = sp.shape[:2]
    if h > size or w > size:  # too many cells: nearest-neighbor shrink keeping the aspect
        k = size / max(h, w)
        im = Image.fromarray(sp).resize((max(1, round(w * k)), max(1, round(h * k))), Image.NEAREST)
        sp = np.array(im)
        h, w = sp.shape[:2]
    out = np.zeros((size, size, 4), np.uint8)
    oy, ox = (size - h) // 2, (size - w) // 2
    out[oy:oy + h, ox:ox + w] = sp
    return out


if __name__ == "__main__":
    sps, s = sprites(sys.argv[1])
    print("cell", s, [sp.shape[:2] for sp in sps])


def icons(path, n=16, gap=2, min_px=600, merge=()):
    """Simpler and steadier for icon rows: each blob's bounding square is split into n x n cells (majority color).
    Only the blob's own pixels are used, so neighbors never leak in. merge=((i, j),) joins blobs i..j into one."""
    from scipy import ndimage
    a0 = np.array(Image.open(path).convert("RGBA"))
    mask = magenta_mask(a0)
    a0[mask] = 0
    fg = ~mask
    lab, _ = ndimage.label(ndimage.binary_dilation(fg, iterations=gap))
    items = []
    for k, sl in enumerate(ndimage.find_objects(lab), start=1):
        own = fg & (lab == k)
        if own.sum() < min_px:
            continue
        yy, xx = np.nonzero(own)
        items.append([xx.min(), yy.min(), xx.max() + 1, yy.max() + 1, own])
    items.sort(key=lambda b: (b[0] + b[2]) / 2)
    for i, j in sorted(merge, reverse=True):
        grp = items[i:j + 1]
        m = np.zeros_like(fg)
        for g in grp:
            m |= g[4]
        items[i:j + 1] = [[min(g[0] for g in grp), min(g[1] for g in grp), max(g[2] for g in grp), max(g[3] for g in grp), m]]
    out = []
    for (x0, y0, x1, y1, own) in items:
        a = a0.copy()
        a[~own] = 0
        side = max(x1 - x0, y1 - y0)
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        X0, Y0 = int(round(cx - side / 2)), int(round(cy - side / 2))
        pad = np.zeros((side, side, 4), np.uint8)
        sx0, sy0 = max(0, X0), max(0, Y0)
        sx1, sy1 = min(a.shape[1], X0 + side), min(a.shape[0], Y0 + side)
        pad[sy0 - Y0:sy1 - Y0, sx0 - X0:sx1 - X0] = a[sy0:sy1, sx0:sx1]
        out.append(clean(snap(pad, side / n)))
    return out


def sheet(icons_, scale=8):
    """Preview strip for checking by eye."""
    n = icons_[0].shape[0]
    im = Image.new("RGBA", (len(icons_) * (n + 2) * scale, (n + 2) * scale), (60, 60, 70, 255))
    for i, ic in enumerate(icons_):
        im.alpha_composite(Image.fromarray(ic).resize((n * scale, n * scale), Image.NEAREST), (i * (n + 2) * scale + scale, scale))
    return im
