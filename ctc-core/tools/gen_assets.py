"""Generates the mod's resources (textures, models, lang, recipes). Run from ctc-core: python3 tools/gen_assets.py"""
import os, json, random, math, shutil
from PIL import Image, ImageDraw
R = 'src/main/resources/'; A = R + 'assets/ctccore/'; D = R + 'data/'
for d in (A, D):
    shutil.rmtree(d, ignore_errors=True)
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2, ensure_ascii=False)
def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)
def shade(c, f): return tuple(max(0, min(255, int(v * f))) for v in c) + (255,)

CH = {'lunar': ('Lunar', (159, 196, 255)), 'martian': ('Martian', (232, 104, 60)), 'mercury': ('Mercury', (143, 163, 192)),
      'void': ('Void', (154, 92, 255)), 'harbinger': ('Harbinger', (192, 48, 58)), 'ignis': ('Ignis', (255, 154, 46)),
      'leviathan': ('Leviathan', (60, 200, 216)), 'remnant': ('Remnant', (216, 194, 122)), 'maledictus': ('Maledictus', (158, 230, 255)),
      'guardian': ('Guardian', (199, 125, 255)), 'ember': ('Ember', (255, 106, 43)), 'terra': ('Terra', (127, 211, 107))}
BOSS = ('harbinger', 'ignis', 'leviathan', 'remnant', 'maledictus', 'guardian')
FX = {'lunar': ('Launches the target into the air', 'Speed, Jump Boost, 60% less fall damage', 'Night Vision while held'),
      'martian': ('Sets the target on fire, +10% damage', 'Attackers catch fire', 'Haste II (a red-hot tool)'),
      'mercury': ('+25% damage', '15% less damage taken', 'Haste I'),
      'void': ('+3 damage', '15% chance to phase through a hit', 'Luck II'),
      'harbinger': ('Wither II for 5 s', 'Attackers wither', 'Haste I'),
      'ignis': ('Burns for 8 s, +15% damage', 'Fire Resistance, attackers catch fire', 'Haste II'),
      'leviathan': ('Slows and weakens the target', "Water Breathing and Dolphin's Grace", 'Conduit Power (mine fast under water)'),
      'remnant': ('Heals you for 15% of the damage', 'Regeneration', 'Slowly repairs itself'),
      'maledictus': ('Freezes the target', 'Attackers freeze', 'Haste I'),
      'guardian': ('+35% damage', '25% less damage taken', 'Haste III'),
      'ember': ('Sets the target on fire for 3 s', 'Attackers catch fire', 'Haste I'),
      'terra': ('+1.5 damage', '8% less damage taken', 'Luck I')}

# ---------------- lang ----------------
lang = {"itemGroup.ctccore": "CtC Core", "block.ctccore.research_station": "Research Station", "block.ctccore.research_casing": "Research Casing",
        "item.ctccore.blank_chip": "Blank Chip", "item.ctccore.chip_scrap": "Chip Scrap",
        "ctccore.chip.hint": "Implant it into gear at a Research Station", "ctccore.gear.weapon": "Weapon", "ctccore.gear.armor": "Armor",
        "ctccore.gear.tool": "Tool", "ctccore.chips": "Chips %s/%s", "ctccore.station.implant": "Implant",
        "ctccore.station.replaces": "Replaces %s, returns %s",
        "ctccore.station.status.0": "Ready", "ctccore.station.status.1": "Insert a weapon, armor or tool", "ctccore.station.status.2": "Insert a chip",
        "ctccore.station.status.3": "Already has this chip", "ctccore.station.status.4": "Needs a Uranium Rod",
        "ctccore.station.status.5": "Needs 60,000 FE", "ctccore.station.status.6": "Empty the output slots",
        "ctccore.station.status.7": "Implanting...", "ctccore.station.status.8": "Frame incomplete: %s blocks missing",
        "ctccore.station.build.short": "3x3 casing floor, 4 corner pillars, 3x3 casing roof with glass in the middle.",
        "ctccore.station.build.1": "Core of a 3x3x3 multiblock:",
        "ctccore.station.build.2": " Floor: 3x3 Research Casing",
        "ctccore.station.build.3": " Middle: this block, casing pillars at the 4 corners",
        "ctccore.station.build.4": " Roof: 3x3 Research Casing, glass in the centre",
        "ctccore.station.build.5": " Cables, hoppers and pipes connect to any frame block",
        "ctccore.station.build.6": " Redstone signal on the core = automatic"}
PONDER = {"research_station": ("Building the Research Station", [
    "Start with a 3x3 floor of Research Casing",
    "The Research Station goes in the middle, on top of the floor",
    "Put a casing pillar on each of the four corners. The middle of each side stays open",
    "Finish with a 3x3 casing roof. The block in the middle must be glass",
    "When the frame is complete, it lights up and the station is ready",
    "Every block of the frame is a port: cables, hoppers and pipes can connect anywhere on it",
    "Take one block away and the whole station switches off"]),
    "research_station_use": ("Implanting chips", [
    "Open the station and put in a weapon, armor or a tool, a chip and a Uranium Rod",
    "Every implant also needs 60,000 FE. The panel in the middle shows what the chip will do",
    "Each piece of gear holds 2 chips (3 with a Chip Socket). One more replaces the oldest and gives part of its material back",
    "A redstone signal next to the core makes it automatic: it starts as soon as everything is inside"])}
for scene, (header, texts) in PONDER.items():
    lang[f"ctccore.ponder.{scene}.header"] = header
    for i, t in enumerate(texts, 1): lang[f"ctccore.ponder.{scene}.text_{i}"] = t
for k, (n, c) in CH.items():
    lang[f"item.ctccore.{k}_chip"] = f"{n} Chip"
    for g, t in zip(('weapon', 'armor', 'tool'), FX[k]): lang[f"ctccore.chip.{k}.{g}"] = t
w(A + 'lang/en_us.json', lang)

# ---------------- item textures ----------------
def chip(path, col, boss=False):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y in range(3, 13):
        for x in range(3, 13): p[x, y] = shade((38, 44, 58), .7 if x in (3, 12) or y in (3, 12) else 1.0)
    for i in (5, 7, 9, 10):
        for (x, y) in ((i, 1), (i, 2), (i, 13), (i, 14), (1, i), (2, i), (13, i), (14, i)): p[x, y] = (212, 176, 84, 255)
    for y in range(5, 11):
        for x in range(5, 11): p[x, y] = shade(col, 1.25 if (x + y) < 14 else .85)
    p[6, 6] = shade(col, 1.6); p[9, 9] = shade(col, .6)
    if boss:
        for (x, y) in ((7, 7), (8, 8), (7, 8), (8, 7)): p[x, y] = (255, 255, 255, 255)
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)): p[x, y] = (120, 130, 150, 255)
    save(im, path)
chip(A + 'textures/item/blank_chip.png', (70, 78, 96))
for k, (n, c) in CH.items(): chip(A + f'textures/item/{k}_chip.png', c, k in BOSS)
im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load(); r = random.Random(3)
for (x, y) in [(5, 6), (6, 5), (6, 6), (7, 6), (7, 7), (8, 7), (9, 8), (8, 9), (9, 9), (10, 9), (6, 9), (5, 9), (6, 10), (10, 6), (11, 7), (4, 7), (7, 10), (9, 5)]:
    p[x, y] = shade(r.choice([(60, 66, 82), (92, 100, 120), (212, 176, 84)]), r.choice([.8, 1, 1.2]))
save(im, A + 'textures/item/chip_scrap.png')

# ---------------- casing texture ----------------
STEEL = (62, 68, 92)
im = Image.new('RGBA', (16, 16)); p = im.load(); r = random.Random(9)
for y in range(16):
    for x in range(16):
        f = r.choice([.95, 1, 1, 1.05])
        if x in (0, 15) or y in (0, 15): f = .62
        elif x in (1, 14) or y in (1, 14): f = 1.22 if (x == 1 or y == 1) else .8
        p[x, y] = shade(STEEL, f)
for i in range(4, 12):
    p[i, 4] = shade(STEEL, .7); p[i, 11] = shade(STEEL, 1.2); p[4, i] = shade(STEEL, .7); p[11, i] = shade(STEEL, 1.2)
for i in range(6, 10): p[i, 7] = (150, 96, 250, 255); p[i, 8] = (96, 56, 180, 255)
for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): p[x, y] = (214, 180, 92, 255)
save(im, A + 'textures/block/research_casing.png')
# formed frame: seamless plating (no border, so the blocks read as one machine) and a glowing band that runs around it
im = Image.new('RGBA', (16, 16)); p = im.load(); r = random.Random(12)
for y in range(16):
    for x in range(16):
        f = r.choice([.96, 1, 1, 1.04]) * (1.06 - 0.008 * y)
        if y in (3, 12): f *= .8                       # horizontal panel seams, they line up across blocks
        if y in (4, 13): f *= 1.12
        p[x, y] = shade((46, 50, 74), f)
for (x, y) in ((1, 1), (9, 1), (5, 14), (13, 14)): p[x, y] = (214, 180, 92, 255)
save(im, A + 'textures/block/research_casing_on.png')
FR = 8; band = Image.new('RGBA', (16, 16 * FR), (0, 0, 0, 0)); bp = band.load()
for f in range(FR):
    for x in range(16):
        v = 0.75 + 0.45 * math.sin((x / 16 - f / FR) * 2 * math.pi)
        bp[x, f * 16 + 7] = shade((150, 96, 250), v + .25); bp[x, f * 16 + 8] = shade((150, 96, 250), v)
        bp[x, f * 16 + 6] = shade((60, 34, 120), 1); bp[x, f * 16 + 9] = shade((60, 34, 120), 1)
        if (x + f * 2) % 16 == 0: bp[x, f * 16 + 7] = (84, 236, 214, 255); bp[x, f * 16 + 8] = (84, 236, 214, 255)
save(band, A + 'textures/block/research_casing_band.png')
json.dump({"animation": {"frametime": 2, "interpolate": True}}, open(A + 'textures/block/research_casing_band.png.mcmeta', 'w'))

# ---------------- station: a real 3D model ----------------
F = 8; TILE = 16; TEX = 64
MATS = {'steel': (70, 78, 104), 'dark': (34, 38, 56), 'gold': (214, 176, 84), 'screen': (150, 96, 250), 'teal': (84, 236, 214),
        'off': (40, 36, 60), 'glass': (120, 90, 200), 'red': (232, 72, 60)}
GLOW = ('screen', 'teal', 'red')
def hsh(*a):
    v = 2166136261
    for x in a:
        for ch in str(x): v = ((v ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return v
def station_texture():
    im = Image.new('RGBA', (TEX, TEX * F)); px = im.load(); tile = {}
    for i, (k, base) in enumerate(MATS.items()):
        tx, ty = (i % 4) * TILE, (i // 4) * TILE; tile[k] = (tx, ty)
        for f in range(F):
            for y in range(TILE):
                for x in range(TILE):
                    n = 0.94 + (hsh(k, x, y) % 13) / 100
                    if k in GLOW:
                        v = 1.0 + 0.3 * math.sin((y / TILE - f / F) * 2 * math.pi) + (0.3 if hsh(k, x, y, f // 2) % 23 == 0 else 0)
                        if k == 'screen' and (x % 4 == 0 or y % 4 == 0): v *= 0.55          # grid lines on the hologram table
                    else:
                        v = n * (1.1 - 0.01 * (x + y) / 2)
                        if (x + y) % 16 == 5: v *= 1.15
                        if hsh(k, x, y, 'p') % 41 == 0: v *= 0.8
                    px[tx + x, f * TEX + ty + y] = shade(base, v)
    save(im, A + 'textures/block/research_station.png')
    json.dump({"animation": {"frametime": 2, "interpolate": True}}, open(A + 'textures/block/research_station.png.mcmeta', 'w'))
    return tile
def station_model(tile, on):
    boxes = []
    def box(f, t, c): boxes.append((f, t, c))
    box((0, 0, 0), (16, 3, 16), 'steel')                                    # base plate
    for (x0, x1) in ((0, 16),):
        box((0, 3, 0), (16, 3.6, 1), 'gold'); box((0, 3, 15), (16, 3.6, 16), 'gold'); box((0, 3, 1), (1, 3.6, 15), 'gold'); box((15, 3, 1), (16, 3.6, 15), 'gold')
    box((4, 3, 4), (12, 9, 12), 'dark')                                     # column
    box((3.5, 5, 6), (4, 7, 10), 'red' if on else 'off'); box((12, 5, 6), (12.5, 7, 10), 'red' if on else 'off')
    box((6, 5, 3.5), (10, 7, 4), 'teal' if on else 'off'); box((6, 5, 12), (10, 7, 12.5), 'teal' if on else 'off')
    box((1, 9, 1), (15, 11, 15), 'steel')                                   # table
    box((2.5, 11, 2.5), (13.5, 11.3, 13.5), 'screen' if on else 'off')      # hologram surface
    for (x, z) in ((1, 1), (13, 1), (1, 13), (13, 13)):                      # four posts
        box((x, 11, z), (x + 2, 15, z + 2), 'dark')
        box((x + 0.5, 15, z + 0.5), (x + 1.5, 16, z + 1.5), 'gold')
    box((7, 12.2, 7), (9, 14.2, 9), 'teal' if on else 'off')                # the floating chip
    box((6.5, 11.3, 6.5), (9.5, 11.8, 9.5), 'glass')
    els = []; dims = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2), "down": (0, 2)}
    for bi, (f, t, c) in enumerate(boxes):
        tx, ty = tile[c]; faces = {}
        for face, (a0, a1) in dims.items():
            ww = max(1, min(TILE, round(abs(t[a0] - f[a0])))); hh = max(1, min(TILE, round(abs(t[a1] - f[a1]))))
            ox = hsh(bi, face, 'x') % (TILE - ww + 1); oy = hsh(bi, face, 'y') % (TILE - hh + 1)
            u0, v0 = (tx + ox) * 16 / TEX, (ty + oy) * 16 / TEX
            faces[face] = {"uv": [round(u0, 3), round(v0, 3), round(u0 + ww * 16 / TEX, 3), round(v0 + hh * 16 / TEX, 3)], "texture": "#0"}
        e = {"from": list(f), "to": list(t), "faces": faces}
        if c in GLOW:
            e["shade"] = False; e["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        els.append(e)
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"0": "ctccore:block/research_station", "particle": "ctccore:block/research_casing"}, "elements": els}
tile = station_texture()
w(A + 'models/block/research_station.json', station_model(tile, False))
w(A + 'models/block/research_station_on.json', station_model(tile, True))
w(A + 'blockstates/research_station.json', {"variants": {"formed=false": {"model": "ctccore:block/research_station"},
                                                          "formed=true": {"model": "ctccore:block/research_station_on"}}})
w(A + 'models/item/research_station.json', {"parent": "ctccore:block/research_station_on"})
w(A + 'blockstates/research_casing.json', {"variants": {"formed=false": {"model": "ctccore:block/research_casing"},
                                                         "formed=true": {"model": "ctccore:block/research_casing_on"}}})
def cube_faces(tex, sides_only=False):
    fs = {d: {"texture": tex, "cullface": d} for d in ("north", "south", "west", "east")}
    if not sides_only:
        fs.update({d: {"texture": tex, "cullface": d} for d in ("up", "down")})
    return fs
w(A + 'models/block/research_casing_on.json', {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
    "textures": {"particle": "ctccore:block/research_casing_on", "base": "ctccore:block/research_casing_on", "band": "ctccore:block/research_casing_band"},
    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": cube_faces("#base")},
                 {"from": [0, 0, 0], "to": [16, 16, 16], "shade": False, "neoforge_data": {"block_light": 15, "sky_light": 15},
                  "faces": cube_faces("#band", True)}]})
w(A + 'models/block/research_casing.json', {"parent": "minecraft:block/cube_all", "textures": {"all": "ctccore:block/research_casing"}})
w(A + 'models/item/research_casing.json', {"parent": "ctccore:block/research_casing"})
for n in ['blank_chip', 'chip_scrap'] + [k + '_chip' for k in CH]:
    w(A + f'models/item/{n}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"ctccore:item/{n}"}})

# ---------------- GUI (176 x 206, dark panel) ----------------
g = Image.new('RGBA', (256, 256), (0, 0, 0, 0)); d = ImageDraw.Draw(g); W, H = 176, 206
BG, BG2, LINE, HI, PURPLE = (30, 33, 58, 255), (22, 24, 44, 255), (70, 76, 120, 255), (112, 120, 176, 255), (150, 96, 250, 255)
d.rectangle((0, 0, W - 1, H - 1), fill=BG)
d.rectangle((0, 0, W - 1, H - 1), outline=(10, 10, 20, 255)); d.rectangle((1, 1, W - 2, H - 2), outline=HI)
d.line((1, H - 2, W - 2, H - 2), fill=LINE); d.line((W - 2, 1, W - 2, H - 2), fill=LINE)
d.rectangle((4, 15, W - 5, 15), fill=PURPLE); d.rectangle((4, 108, W - 5, 108), fill=LINE)
def slot(x, y, accent=None):
    d.rectangle((x - 1, y - 1, x + 16, y + 16), fill=(14, 15, 30, 255), outline=LINE)
    d.line((x - 1, y + 16, x + 16, y + 16), fill=HI); d.line((x + 16, y - 1, x + 16, y + 16), fill=HI)
    if accent: d.rectangle((x - 2, y - 2, x + 17, y + 17), outline=accent)
slot(26, 20, PURPLE); slot(26, 44, (84, 236, 214, 255)); slot(26, 68, (232, 72, 60, 255)); slot(150, 20, (111, 227, 155, 255)); slot(150, 68, (214, 176, 84, 255))
for row in range(3):
    for col in range(9): slot(8 + col * 18, 124 + row * 18)
for col in range(9): slot(8 + col * 18, 182)
d.rectangle((8, 19, 17, 92), fill=(12, 12, 24, 255), outline=LINE)                       # energy well
d.rectangle((47, 17, 144, 78), fill=BG2, outline=LINE)                                   # info panel
d.rectangle((47, 79, 144, 84), fill=(12, 12, 24, 255), outline=LINE)                     # progress well
for y in (28, 52, 76): d.line((44, y, 46, y), fill=LINE)                                 # inputs feed the panel
d.line((145, 28, 148, 28), fill=LINE); d.line((145, 76, 148, 76), fill=LINE)
save(g, A + 'textures/gui/research_station.png')

# ---------------- data ----------------
for b in ('research_station', 'research_casing'):
    w(D + f'ctccore/loot_table/blocks/{b}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"ctccore:{b}"}],
                                                                                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
w(D + 'minecraft/tags/block/mineable/pickaxe.json', {"values": ["ctccore:research_station", "ctccore:research_casing"]})
w(D + 'minecraft/tags/block/needs_iron_tool.json', {"values": ["ctccore:research_station", "ctccore:research_casing"]})
def shaped(name, pattern, key, result, count=1):
    w(D + f'ctccore/recipe/{name}.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key, "result": {"id": result, "count": count}})
shaped('blank_chip', [" G ", "RCR", " G "], {"G": {"item": "minecraft:gold_ingot"}, "R": {"item": "minecraft:redstone"}, "C": {"item": "northstar:circuit"}}, 'ctccore:blank_chip', 2)
for k, mat in (('lunar', 'northstar:polished_lunar_sapphire'), ('martian', 'northstar:martian_steel_ingot'), ('mercury', 'northstar:tungsten_ingot'), ('void', 'ctc:voidsteel_ingot')):
    shaped(k + '_chip', [" M ", "MCM", " M "], {"M": {"item": mat}, "C": {"item": "ctccore:blank_chip"}}, f'ctccore:{k}_chip')
shaped('research_station', ["TCT", "PSP", "RRR"], {"T": {"item": "northstar:titanium_sheet"}, "C": {"item": "northstar:advanced_circuit"}, "P": {"item": "create:precision_mechanism"},
                                                    "S": {"item": "minecraft:smithing_table"}, "R": {"item": "createnuclear:reactor_casing"}}, 'ctccore:research_station')
shaped('research_casing', [" T ", "TAT", " T "], {"T": {"item": "northstar:titanium_sheet"}, "A": {"item": "create:andesite_casing"}}, 'ctccore:research_casing', 4)
w(D + 'ctccore/recipe/netherite_scrap_from_chip_scrap.json', {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "ctccore:chip_scrap"}] * 4,
                                                             "result": {"id": "minecraft:netherite_scrap", "count": 1}})
print('assets ok')


# ---------------- Ponder schematic (vanilla structure NBT, gzipped) ----------------
import gzip, struct
def nbt_str(v): b = v.encode(); return struct.pack('>H', len(b)) + b
def tag(t, name, payload): return bytes([t]) + nbt_str(name) + payload
def compound(items): return b''.join(items) + b'\x00'
def int_list(vals): return bytes([3]) + struct.pack('>i', len(vals)) + b''.join(struct.pack('>i', v) for v in vals)
def comp_list(comps): return bytes([10]) + struct.pack('>i', len(comps)) + b''.join(compound(c) for c in comps)
palette, blocks = [], []
def state(name, props=None):
    key = (name, tuple(sorted((props or {}).items())))
    for i, k in enumerate(palette):
        if k == key: return i
    palette.append(key); return len(palette) - 1
def put(x, y, z, name, props=None): blocks.append((x, y, z, state(name, props)))
for x in range(5):
    for z in range(5): put(x, 0, z, 'minecraft:snow_block' if (x + z) % 2 else 'minecraft:smooth_stone')
for x in range(1, 4):
    for z in range(1, 4):
        put(x, 1, z, 'ctccore:research_casing', {'formed': 'false'})
        put(x, 3, z, 'minecraft:glass' if (x, z) == (2, 2) else 'ctccore:research_casing', None if (x, z) == (2, 2) else {'formed': 'false'})
for (x, z) in ((1, 1), (3, 1), (1, 3), (3, 3)): put(x, 2, z, 'ctccore:research_casing', {'formed': 'false'})
put(2, 2, 2, 'ctccore:research_station', {'formed': 'false'})
pal = []
for name, props in palette:
    items = [tag(8, 'Name', nbt_str(name))]
    if props: items.append(tag(10, 'Properties', compound([tag(8, k, nbt_str(v)) for k, v in props])))
    pal.append(items)
root = compound([tag(3, 'DataVersion', struct.pack('>i', 3955)),
                 tag(9, 'size', int_list([5, 4, 5])),
                 tag(9, 'palette', comp_list(pal)),
                 tag(9, 'blocks', comp_list([[tag(9, 'pos', int_list([x, y, z])), tag(3, 'state', struct.pack('>i', st))] for x, y, z, st in blocks])),
                 tag(9, 'entities', bytes([0]) + struct.pack('>i', 0))])
os.makedirs(A + 'ponder', exist_ok=True)
with gzip.open(A + 'ponder/research_station.nbt', 'wb') as f: f.write(bytes([10]) + nbt_str('') + root)
print('ponder schematic', len(blocks), 'blocks')

exec(open('tools/gen_materials.py').read())
