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
    'martian': ('Martian', (176, 74, 48), (255, 160, 80), 'northstar:martian_steel_ingot'),
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
json.dump(lang, open(lp, 'w'), indent=2, ensure_ascii=False)

# ---------- basic chips: no blank chip, early materials ----------
shaped('copper_chip', [" C ", "CRC", " C "], {"C": "minecraft:copper_ingot", "R": "minecraft:redstone"}, 'ctccore:copper_chip')
shaped('iron_chip', [" I ", "IRI", " I "], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 'ctccore:iron_chip')
shaped('ember_chip', [" B ", "GRG", " B "], {"B": "minecraft:blaze_rod", "G": "minecraft:gold_ingot", "R": "minecraft:redstone"}, 'ctccore:ember_chip')
shaped('terra_chip', [" A ", "ERE", " A "], {"A": "minecraft:amethyst_shard", "E": "minecraft:emerald", "R": "minecraft:redstone"}, 'ctccore:terra_chip')
shaped('kinetic_chip', [" A ", "FRF", " A "], {"A": "create:andesite_alloy", "F": "minecraft:feather", "R": "minecraft:redstone"}, 'ctccore:kinetic_chip')
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

# ---------- textures ----------
def plate(p, x0, y0, x1, y1, col, acc, seed, trim=True):
    r = random.Random(seed)
    for y in range(y0, y1):
        for x in range(x0, x1):
            f = r.choice([.94, 1, 1, 1.06]) * (1.12 - 0.22 * (y - y0) / max(1, y1 - y0))
            if trim and (x in (x0, x1 - 1) or y in (y0, y1 - 1)): f *= .62
            p[x, y] = shade(col, f)
    if trim and x1 - x0 >= 4 and y1 - y0 >= 4:
        for x in range(x0 + 1, x1 - 1): p[x, y0 + 1] = shade(acc, 1.0)
# faces of a box at (u, v) with size (w, h, d): top/bottom row of height d, sides row of height h
def box_faces(u, v, wd, h, d):
    return [(u + d, v, u + d + wd, v + d), (u + d + wd, v, u + d + 2 * wd, v + d),                         # top, bottom
            (u, v + d, u + d, v + d + h), (u + d, v + d, u + d + wd, v + d + h),                        # right, front
            (u + d + wd, v + d, u + 2 * d + wd, v + d + h), (u + 2 * d + wd, v + d, u + 2 * d + 2 * wd, v + d + h)]  # left, back
for n, (disp, col, acc, ingot) in SETS.items():
    s = sum(map(ord, n))
    l1 = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); p1 = l1.load()
    for i, f in enumerate(box_faces(0, 0, 8, 8, 8)): plate(p1, *f, col, acc, s + i)             # helmet
    for x in range(10, 14): p1[x, 12] = shade(acc, 1.3); p1[x, 13] = shade(acc, .9)             # visor slit
    for x in range(11, 13): p1[x, 12] = (255, 255, 255, 255) if n != 'steel' else shade(acc, 1.4)
    for i, f in enumerate(box_faces(16, 16, 8, 12, 4)): plate(p1, *f, col, acc, s + 10 + i)     # chest
    for y in range(22, 26): p1[23, y] = shade(acc, 1.25); p1[24, y] = shade(acc, 1.25)         # chest core
    for i, f in enumerate(box_faces(40, 16, 4, 12, 4)): plate(p1, *f, col, acc, s + 20 + i)     # arms
    for (x0, y0, x1, y1) in box_faces(0, 16, 4, 12, 4)[2:]:                                     # boots: lower part of the legs
        plate(p1, x0, y1 - 5, x1, y1, col, acc, s + 30 + x0)
    plate(p1, *box_faces(0, 16, 4, 12, 4)[1], col, acc, s + 40)
    save(l1, A + f'textures/models/armor/{n}_layer_1.png')
    l2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); p2 = l2.load()
    for (x0, y0, x1, y1) in box_faces(16, 16, 8, 12, 4)[2:]: plate(p2, x0, y1 - 5, x1, y1, col, acc, s + 50 + x0)  # waist
    for i, (x0, y0, x1, y1) in enumerate(box_faces(0, 16, 4, 12, 4)):
        plate(p2, x0, y0, x1, y1 if i < 2 else y1 - 2, col, acc, s + 60 + i)
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
