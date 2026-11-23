"""Split the hand-drawn elite overlays into the two textures the game uses:
<mob>_frame.png (gold pins, circuit board - lit normally) and <mob>_glow.png (white, tinted with the chip colour, full bright).
The Wither's phase-2 glow keeps its own colours."""
import sys
sys.argv = [sys.argv[0]]
exec(open('overlays.py').read().split("\nif __name__")[0])
OUT = '/home/claude/stentik-turnpc/ctc-core/src/main/resources/assets/ctccore/textures/entity/elite/'
import os; os.makedirs(OUT, exist_ok=True)
MARK = 'lunar'
GREY = {4: 255, 3: 214, 2: 168, 1: 120, 0: 80}

def split(im, tinted=True):
    frame = Image.new('RGBA', im.size, (0, 0, 0, 0)); glow = Image.new('RGBA', im.size, (0, 0, 0, 0))
    lun = [hx(c) for c in R[MARK]]
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] == 0: continue
            if tinted and p[:3] in lun:
                g = GREY[lun.index(p[:3])]; glow.putpixel((x, y), (g, g, g, 255))
            elif not tinted and max(p[:3]) - min(p[:3]) > 60 and p[:3] not in [hx(c) for c in R['gold']]:
                glow.putpixel((x, y), p)
            else:
                frame.putpixel((x, y), p)
    return frame, glow

for name, fn in (('zombie', zombie_ov), ('skeleton', skeleton_ov), ('creeper', creeper_ov), ('enderman', enderman_ov)):
    f, g = split(fn(MARK))
    f.save(OUT + name + '_frame.png'); g.save(OUT + name + '_glow.png')
f, g = split(wither_ov(), tinted=False)
f.save(OUT + 'wither_frame.png'); g.save(OUT + 'wither_glow.png')
print(sorted(os.listdir(OUT)))
