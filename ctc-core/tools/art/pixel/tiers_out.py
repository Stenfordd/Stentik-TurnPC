exec(open('tiers.py').read())
from PIL import Image
HEX = {'st': ["#2e3238","#474d56","#646b76","#848c97","#a7aeb8","#c6ccd4"], 'leather': ["#3a2414","#5c3a22","#7e5232","#a06c44"],
 'slit': ["#121418","#1e2126","#2c3036"], 'tu': ["#1f2126","#2f3238","#43474f","#5a5f69","#737985","#8e95a1"],
 'hot': ["#7a3108","#b45414","#e07a22","#ffa840","#ffd88a"], 'vo': ["#140d1c","#221632","#33224a","#47335f","#5e4a7c","#77649a"],
 'bone': ["#6e6452","#9a8e74","#c4b896","#e2d8bc"], 'violet': ["#5a1f96","#8440d0","#b278f6","#dcbcff"]}
def hx(tok):
    n, k = tok.split('.'); h = HEX[n][int(k)]; return tuple(int(h[i:i+2], 16) for i in (1, 3, 5)) + (255,)
out = {}
for tier in ('steel', 'tungsten', 'voidsteel'):
    c1, c2 = build(tier); T = TOK[tier]
    for k, c in ((1, c1), (2, c2)):
        im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        for y in range(64):
            for x in range(64):
                ch = c.g[y][x]
                if ch != '.': im.putpixel((x, y), hx(T[ch]))
        im.save(f'/home/claude/stentik-turnpc/ctc-core/tools/art/pixel/{tier}_layer_{k}.png')
        out[f'{tier}_{k}'] = c.rows(0, 0, 64, 64)
json.dump(out, open('tier_rows.json', 'w'))
print('ok')
