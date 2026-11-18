"""Basic chips (Smithing Table), chip slot tags, and the five armor sets. Called at the end of gen_assets.py."""
import os, json, random
from PIL import Image
R = 'src/main/resources/'; A = R + 'assets/ctccore/'; D = R + 'data/'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2, ensure_ascii=False)
def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)
def shade(c, f): return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)
def rec(name, obj): w(D + f'ctccore/recipe/{name}.json', obj)
def ing(v): return {"tag": v[1:]} if v.startswith('#') else {"item": v}
def shaped(name, pattern, key, result, count=1, cat="misc"):
    rec(name, {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern, "key": {k: ing(v) for k, v in key.items()},
               "result": {"id": result, "count": count}})

SETS = {  # name: (display, colour, accent, ingot)
    'steel': ('Steel', (120, 128, 140), (200, 206, 214), 'createnuclear:steel_ingot'),
    'titanium': ('Titanium', (178, 186, 204), (110, 200, 230), '#c:ingots/titanium'),
    'tungsten': ('Tungsten', (78, 84, 98), (240, 190, 90), 'northstar:tungsten_ingot'),
    'voidsteel': ('Voidsteel', (52, 34, 84), (170, 110, 255), 'ctc:voidsteel_ingot'),
}
BONUS = {'steel': 'Haste I', 'titanium': 'Speed I', 'martian': 'Fire Resistance', 'tungsten': 'Resistance I', 'voidsteel': 'No fall damage'}
PIECES = ('helmet', 'chestplate', 'leggings', 'boots')
lp = A + 'lang/en_us.json'; lang = json.load(open(lp))
lang["ctccore.chip.hint_basic"] = "Smithing Table: this chip + gear + redstone (or a Research Station)"
lang["ctccore.armor.set"] = "Full set: %s"
for n, (disp, *_rest) in SETS.items():
    for p in PIECES: lang[f"item.ctccore.{n}_{p}"] = f"{disp} {p.capitalize()}"
    lang[f"ctccore.armor.{n}.bonus"] = BONUS[n]
lang["ctccore.armor.martian.bonus"] = BONUS['martian']
json.dump(lang, open(lp, 'w'), indent=2, ensure_ascii=False)

# ---------- basic chips: no blank chip, early materials ----------
shaped('copper_chip', [" C ", "CRC", " C "], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, 'ctccore:copper_chip')
shaped('iron_chip', [" I ", "IRI", " I "], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 'ctccore:iron_chip')
shaped('ember_chip', [" B ", "GRG", " B "], {"B": "minecraft:blaze_rod", "G": "minecraft:gold_ingot", "R": "minecraft:redstone"}, 'ctccore:ember_chip')
shaped('terra_chip', [" A ", "ERE", " A "], {"A": "minecraft:amethyst_shard", "E": "minecraft:emerald", "R": "minecraft:redstone"}, 'ctccore:terra_chip')
shaped('kinetic_chip', [" A ", "FRF", " A "], {"A": "create:andesite_alloy", "F": "minecraft:feather", "R": "minecraft:redstone"}, 'ctccore:kinetic_chip')
shaped('lumber_chip', [" L ", "IRI", " L "], {"L": "#minecraft:logs", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 'ctccore:lumber_chip')
shaped('vein_chip', [" Z ", "IRI", " Z "], {"Z": "create:zinc_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 'ctccore:vein_chip')
shaped('harvest_chip', [" W ", "BRB", " W "], {"W": "minecraft:wheat", "B": "minecraft:bone_meal", "R": "minecraft:redstone"}, 'ctccore:harvest_chip')
rec('chip_smithing', {"type": "ctccore:chip_smithing"})

# ---------- chip slot tags ----------
w(D + 'ctccore/tags/item/two_chip_slots.json', {"values": [{"id": f"ctc:{x}", "required": False} for x in
    ('steel_saber', 'titanium_blade', 'voidsteel_rapier', 'witherite_cleaver', 'martian_glaive', 'tungsten_hammer')]})
w(D + 'ctccore/tags/item/one_chip_slot.json', {"values": [f"ctccore:steel_{p}" for p in PIECES]})

# ---------- armor recipes + tags ----------
SHAPES = {'helmet': ["XXX", "X X"], 'chestplate': ["X X", "XXX", "XXX"], 'leggings': ["XXX", "X X", "X X"], 'boots': ["X X", "X X"]}
for n, (disp, col, acc, ingot) in SETS.items():
    for p in PIECES: shaped(f'{n}_{p}', SHAPES[p], {"X": ingot}, f'ctccore:{n}_{p}', cat="equipment")
TAGS = {'helmet': ['head_armor'], 'chestplate': ['chest_armor'], 'leggings': ['leg_armor'], 'boots': ['foot_armor']}
for p, tags in TAGS.items():
    ids = [f'ctccore:{n}_{p}' for n in SETS]
    for t in tags:
        w(D + f'minecraft/tags/item/{t}.json', {"values": ids})
        w(D + f'minecraft/tags/item/enchantable/{t}.json', {"values": ids})
for t in ('enchantable/armor', 'enchantable/equippable', 'enchantable/durability'):
    w(D + f'minecraft/tags/item/{t}.json', {"values": [f'ctccore:{n}_{p}' for n in SETS for p in PIECES]})
w(D + 'minecraft/tags/item/trimmable_armor.json', {"values": [f'ctccore:{n}_{p}' for n in SETS for p in PIECES]})

# ---------- textures: plates over the skin, not a full suit ----------
def paint(p, rect, rows, col, acc, seed, accent_rows=(), seam_rows=(), mask=None):
    """Fill the given rows of a face rect with metal plating. mask(x_in_face, y_in_face) -> False leaves a hole."""
    x0, y0, x1, y1 = rect; r = random.Random(seed)
    for y in range(y0, y1):
        fy = y - y0
        if fy not in rows: continue
        for x in range(x0, x1):
            fx = x - x0
            if mask and not mask(fx, fy, x1 - x0): continue
            f = r.choice([.92, 1, 1, 1.05]) * (1.15 - 0.3 * fy / max(1, y1 - y0))
            if fx == 0 or fx == x1 - x0 - 1: f *= .7
            if fy in seam_rows: f *= .6
            p[x, y] = shade(acc, 1.05) if fy in accent_rows else shade(col, f)
def faces(u, v, wd, h, d):   # right, front, left, back side faces + top + bottom of a box
    return {'top': (u + d, v, u + d + wd, v + d), 'bottom': (u + d + wd, v, u + d + 2 * wd, v + d),
            'right': (u, v + d, u + d, v + d + h), 'front': (u + d, v + d, u + d + wd, v + d + h),
            'left': (u + d + wd, v + d, u + 2 * d + wd, v + d + h), 'back': (u + 2 * d + wd, v + d, u + 2 * d + 2 * wd, v + d + h)}
SIDES = ('right', 'front', 'left', 'back')
for n, (disp, col, acc, ingot) in SETS.items():
    s0 = sum(map(ord, n))
    l1 = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); p1 = l1.load()
    head = faces(0, 0, 8, 8, 8)
    paint(p1, head['top'], range(8), col, acc, s0)
    paint(p1, head['back'], range(8), col, acc, s0 + 1, accent_rows=(6,))
    for side in ('right', 'left'): paint(p1, head[side], range(7), col, acc, s0 + 2, accent_rows=(1,))
    # open face: brow band, nose guard and cheek plates, the face shows through
    paint(p1, head['front'], range(8), col, acc, s0 + 3, accent_rows=(1,),
          mask=lambda fx, fy, w: fy <= 2 or fx in (0, 1, 6, 7) and fy <= 6 or fx in (3, 4) and fy <= 4)
    body = faces(16, 16, 8, 12, 4)
    paint(p1, body['front'], range(10), col, acc, s0 + 4, seam_rows=(4, 7),
          mask=lambda fx, fy, w: not (fy == 0 and 2 <= fx <= 5))      # neck opening
    for (x, y) in ((19 + 4, 21), (20 + 4, 21), (19 + 4, 22), (20 + 4, 22)): p1[x, y] = shade(acc, 1.35)   # core gem
    for side in ('right', 'left'): paint(p1, body[side], range(10), col, acc, s0 + 5, seam_rows=(4, 7))
    paint(p1, body['back'], range(10), col, acc, s0 + 6, seam_rows=(4, 7))
    arm = faces(40, 16, 4, 12, 4)
    paint(p1, arm['top'], range(4), col, acc, s0 + 7)
    for side in SIDES:                                     # pauldron and bracer, the sleeve shows between
        paint(p1, arm[side], (0, 1, 2, 3, 8, 9, 10, 11), col, acc, s0 + 8, accent_rows=(3, 8))
    leg = faces(0, 16, 4, 12, 4)
    for side in SIDES: paint(p1, leg[side], range(8, 12), col, acc, s0 + 9, accent_rows=(8,))
    paint(p1, leg['bottom'], range(4), col, acc, s0 + 10)
    save(l1, A + f'textures/models/armor/{n}_layer_1.png')
    l2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); p2 = l2.load()
    for side in SIDES: paint(p2, body[side], range(8, 12), col, acc, s0 + 11, accent_rows=(8,))   # belt
    for side in SIDES:                                     # thigh plates and a knee guard
        paint(p2, leg[side], range(0, 8), col, acc, s0 + 12, accent_rows=(5,) if side == 'front' else (),
              mask=lambda fx, fy, w: fy != 4 or side == 'front')
    save(l2, A + f'textures/models/armor/{n}_layer_2.png')

ICONS = {
 'helmet': ["................", "................", "....########....", "...#oooooooo#...", "..#oooooooooo#..", "..#oooooooooo#..",
            "..#oo#vvvv#oo#..", "..#oo#....#oo#..", "..#o#......#o#..", "..###......###..", "................", "................",
            "................", "................", "................", "................"],
 'chestplate': ["................", "..###......###..", ".#ooo#....#ooo#.", ".#oooo####oooo#.", ".#oooooooooooo#.", ".###oooccoooo###",
                "...#ooocccooo#..", "...#oooccooo#...", "...#oooooooo#...", "...#oooooooo#...", "...#oooooooo#...", "...#oooooooo#...",
                "...##########...", "................", "................", "................"],
 'leggings': ["................", "...##########...", "...#vvvvvvvv#...", "...#oooooooo#...", "...#oooooooo#...", "...#ooo##ooo#...",
              "...#ooo##ooo#...", "...#ooo##ooo#...", "...#ooo##ooo#...", "...#ooo##ooo#...", "...#ooo##ooo#...", "...#ooo##ooo#...",
              "...#####.####...", "................", "................", "................"],
 'boots': ["................", "................", "................", "................", "................", "...####..####...",
           "...#oo#..#oo#...", "...#oo#..#oo#...", "...#oo#..#oo#...", "..#ooo#..#ooo#..", ".#vvvv#..#vvvv#.", ".######..######.",
           "................", "................", "................", "................"],
}
for n, (disp, col, acc, ingot) in SETS.items():
    for piece, rows in ICONS.items():
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == '#': p[x, y] = shade(col, .5)
                elif ch == 'o': p[x, y] = shade(col, 1.25 - 0.04 * y + (0.15 if x < 7 else 0))
                elif ch in 'vc': p[x, y] = shade(acc, 1.2)
        save(im, A + f'textures/item/{n}_{piece}.png')
        w(A + f'models/item/{n}_{piece}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"ctccore:item/{n}_{piece}"}})
print('armor ok')

# ---------- Voidforging ----------
lang = json.load(open(lp))
lang["item.ctccore.voidforge_template"] = "Voidforge Template"
json.dump(lang, open(lp, 'w'), indent=2, ensure_ascii=False)
shaped('voidforge_template', [" V ", "VCV", " V "], {"V": "ctc:voidsteel_ingot", "C": "cataclysm:void_core"}, 'ctccore:voidforge_template')
shaped('voidforge_template_copy', ["VTV", "VOV", "VVV"], {"V": "ctc:voidsteel_ingot", "T": "ctccore:voidforge_template", "O": "minecraft:crying_obsidian"},
       'ctccore:voidforge_template', 2)
rec('voidforge', {"type": "ctccore:voidforge"})
im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
for y in range(1, 15):
    for x in range(3, 13):
        edge = x in (3, 12) or y in (1, 14)
        p[x, y] = (40, 22, 70, 255) if edge else shade((92, 50, 150), 1.25 - 0.03 * y)
for (x, y) in ((7, 4), (8, 4), (6, 5), (9, 5), (6, 6), (9, 6), (7, 7), (8, 7), (7, 9), (8, 10), (7, 11), (8, 12)): p[x, y] = (200, 150, 255, 255)
save(im, A + 'textures/item/voidforge_template.png')
w(A + 'models/item/voidforge_template.json', {"parent": "minecraft:item/generated", "textures": {"layer0": "ctccore:item/voidforge_template"}})
print('voidforge ok')
