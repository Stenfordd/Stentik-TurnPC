#!/usr/bin/env python3
"""Custom boss bars (Ender Dragon, Wither, Void Herald): base = background row + fill row (182x5 each),
overlay = a 256x32 frame with the bar slot at (37,11). Run from ctc-core/."""
import math, os, random
from PIL import Image, ImageDraw
OUT = 'src/main/resources/assets/ctccore/textures/gui/boss_bar/'
os.makedirs(OUT, exist_ok=True)
SX, SY, W, H = 37, 11, 182, 5

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def base(name, bg, f0, f1, hi):
    im = Image.new('RGBA', (256, 16), (0, 0, 0, 0)); p = im.load()
    for x in range(W):
        for y in range(H):
            p[x, y] = bg + (255,)
            c = lerp(f0, f1, y / (H - 1))
            if y == 0: c = hi
            if (x * 7 + y * 3) % 23 == 0: c = lerp(c, hi, 0.5)
            p[x, 5 + y] = c + (255,)
    im.save(OUT + name + '_base.png')

def frame(name, dark, mid, light, accent, deco):
    im = Image.new('RGBA', (256, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    # body around the slot
    d.rectangle([SX - 3, SY - 3, SX + W + 2, SY + H + 2], fill=dark + (255,))
    d.rectangle([SX - 2, SY - 2, SX + W + 1, SY - 2], fill=light + (255,))
    d.rectangle([SX - 2, SY + H + 1, SX + W + 1, SY + H + 1], fill=mid + (255,))
    d.rectangle([SX, SY, SX + W - 1, SY + H - 1], fill=(0, 0, 0, 0))
    deco(d, im)
    # keep the slot clear
    a = im.load()
    for x in range(SX, SX + W):
        for y in range(SY, SY + H): a[x, y] = (0, 0, 0, 0)
    im.save(OUT + name + '_overlay.png')

# ---------- Ender Dragon: obsidian frame, wing spikes at the ends, a dragon eye in the middle
def dragon(d, im):
    D, M, L, A = (20, 12, 30), (70, 38, 100), (150, 90, 200), (230, 80, 230)
    for side in (-1, 1):
        cx = SX - 4 if side < 0 else SX + W + 3
        for k, (dx, h) in enumerate([(0, 9), (4, 7), (8, 5), (12, 3)]):
            x = cx + side * dx
            d.polygon([(x, SY - h), (x + side * 4, SY + 2), (x, SY + H + h - 4)], fill=D + (255,), outline=M + (255,))
        d.line([(cx, SY - 9), (cx + side * 14, SY + 2)], fill=L + (255,))
    for x in range(SX + 12, SX + W - 12, 16):   # scales on the top rim
        d.polygon([(x, SY - 3), (x + 4, SY - 6), (x + 8, SY - 3)], fill=M + (255,), outline=D + (255,))
    cx = SX + W // 2
    d.polygon([(cx - 11, SY - 4), (cx, SY - 11), (cx + 11, SY - 4), (cx, SY + 1)], fill=D + (255,), outline=L + (255,))
    d.polygon([(cx - 6, SY - 4), (cx, SY - 8), (cx + 6, SY - 4), (cx, SY - 1)], fill=A + (255,))
    d.line([(cx, SY - 8), (cx, SY - 1)], fill=(10, 0, 10, 255), width=2)

# ---------- Wither: bone-grey frame, three skulls in the middle, small skulls at the ends
def skull(d, x, y, s, bone=(70, 70, 76), eye=(140, 220, 255)):
    d.rectangle([x - s, y - s, x + s, y + s - 1], fill=(28, 28, 32, 255))
    d.rectangle([x - s + 1, y - s + 1, x + s - 1, y + s - 2], fill=bone + (255,))
    d.rectangle([x - s + 2, y - 1, x - 1, y], fill=eye + (255,))
    d.rectangle([x + 1, y - 1, x + s - 2, y], fill=eye + (255,))
    d.rectangle([x - 1, y + s - 2, x, y + s], fill=(28, 28, 32, 255))

def wither(d, im):
    cx = SX + W // 2
    for x in range(SX - 2, SX + W + 2, 6):
        d.point((x, SY - 4), fill=(90, 90, 96, 255)); d.point((x + 3, SY + H + 3), fill=(90, 90, 96, 255))
    skull(d, cx, SY - 5, 6); skull(d, cx - 13, SY - 4, 4); skull(d, cx + 13, SY - 4, 4)
    skull(d, SX - 7, SY + 2, 5); skull(d, SX + W + 6, SY + 2, 5)

# ---------- Void Herald: violet frame with crystal shards and a floating heart crystal
def herald(d, im):
    C, C2, S = (230, 179, 255), (176, 76, 255), (40, 22, 60)
    for side in (-1, 1):
        cx = SX - 5 if side < 0 else SX + W + 4
        for k, (dx, h) in enumerate([(0, 10), (5, 7), (9, 4)]):
            x = cx + side * dx
            d.polygon([(x - 2, SY + 3), (x, SY - h), (x + 2, SY + 3), (x, SY + 7)], fill=C2 + (255,), outline=C + (255,))
    for x in range(SX + 20, SX + W - 20, 22):
        d.polygon([(x - 2, SY - 3), (x, SY - 7), (x + 2, SY - 3)], fill=C2 + (255,))
    cx = SX + W // 2
    d.polygon([(cx - 8, SY - 4), (cx, SY - 11), (cx + 8, SY - 4), (cx, SY + 1)], fill=S + (255,), outline=C + (255,))
    d.polygon([(cx - 4, SY - 4), (cx, SY - 8), (cx + 4, SY - 4), (cx, SY - 1)], fill=C + (255,))

base('ender_dragon', (24, 10, 30), (232, 90, 232), (140, 30, 170), (255, 170, 255))
frame('ender_dragon', (18, 10, 26), (60, 30, 85), (120, 70, 165), (230, 80, 230), dragon)
base('wither', (12, 12, 16), (110, 130, 170), (40, 48, 70), (170, 220, 255))
frame('wither', (24, 24, 28), (60, 60, 66), (110, 110, 118), (140, 220, 255), wither)
base('void_herald', (20, 10, 30), (196, 110, 255), (110, 40, 190), (240, 200, 255))
frame('void_herald', (26, 14, 38), (70, 40, 100), (150, 100, 200), (230, 179, 255), herald)

# preview
pv = Image.new('RGBA', (256 * 3, 34 * 3 * 3), (60, 90, 120, 255))
for i, n in enumerate(['ender_dragon', 'wither', 'void_herald']):
    b = Image.open(OUT + n + '_base.png'); o = Image.open(OUT + n + '_overlay.png')
    t = Image.new('RGBA', (256, 32), (0, 0, 0, 0))
    t.alpha_composite(b.crop((0, 0, W, H)), (SX, SY)); t.alpha_composite(b.crop((0, 5, int(W * 0.65), 10)), (SX, SY))
    t.alpha_composite(o)
    pv.alpha_composite(t.resize((768, 96), Image.NEAREST), (0, i * 102))
pv.save('/tmp/claude-0/-home-claude/dbb0d9ba-f1cd-5617-86e0-38bb24bf8c87/scratchpad/bossbars.png')
