"""Tungsten tools are made with Electrum Rods, and VoidSteel tools are smithed from them: give both an electrum
handle instead of the wooden one. Recolours the wood ramp in place (run once)."""
import os
from PIL import Image

D = os.path.join(os.path.dirname(__file__), '../../../src/main/resources/assets/ctccore/textures/item')
WOOD = {'3b2614': '5c4312', '5a3b20': '8c6a1c', '7a5230': 'b8902c', '9a6c40': 'dcb848', 'b98a55': 'f6e07e'}
OUT = '140d1c'
h = lambda s: tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
hx = lambda p: '%02x%02x%02x' % p[:3]

for tool in ('pickaxe', 'axe', 'shovel', 'hoe'):
    p = os.path.join(D, 'tungsten_' + tool + '.png')
    im = Image.open(p).convert('RGBA'); px = im.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] and hx(px[x, y]) in WOOD: px[x, y] = h(WOOD[hx(px[x, y])])
    im.save(p)

    p = os.path.join(D, 'voidsteel_' + tool + '.png')
    im = Image.open(p).convert('RGBA'); px = im.load(); src = im.copy().load()
    def c(x, y): return hx(src[x, y]) if 0 <= x < 16 and 0 <= y < 16 and src[x, y][3] else None
    for y in range(16):
        for x in range(16):
            k = c(x, y)
            if k == '3b2614':   # shaft: lit side up-left, shade down-right
                px[x, y] = h('7a5c18' if c(x + 1, y) == OUT or c(x, y + 1) == OUT or c(x + 1, y) is None else 'd0ac40')
            elif k in ('5a3b20', '7a5230'):   # the grip wrap turns violet
                px[x, y] = h('8440d0' if k == '7a5230' else '5a1f96')
    im.save(p)
print('ok')
