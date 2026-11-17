"""Planet ores, boss trophies, Chip Socket, early chips, Voidsteel Ponder. Called at the end of gen_assets.py."""
import os, json, random, math, gzip, struct
from PIL import Image
R = 'src/main/resources/'; A = R + 'assets/ctccore/'; D = R + 'data/'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2, ensure_ascii=False)
def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)
def shade(c, f): return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)

# ---------------- lang ----------------
lp = A + 'lang/en_us.json'; lang = json.load(open(lp))
lang.update({
    "block.ctccore.selenite_ore": "Selenite Ore", "item.ctccore.selenite": "Selenite",
    "block.ctccore.rubrium_ore": "Rubrium Ore", "item.ctccore.raw_rubrium": "Raw Rubrium", "item.ctccore.rubrium_ingot": "Rubrium Ingot",
    "block.ctccore.sulfurite_ore": "Sulfurite Ore", "item.ctccore.sulfurite": "Sulfurite",
    "block.ctccore.solarite_ore": "Solarite Ore", "item.ctccore.solarite": "Solarite",
    "item.ctccore.abyssal_gill": "Abyssal Gill", "item.ctccore.desert_relic": "Desert Relic", "item.ctccore.cryo_core": "Cryo Core",
    "item.ctccore.chip_socket": "Chip Socket",
    "item.ctccore.ember_chip": "Ember Chip", "item.ctccore.terra_chip": "Terra Chip",
    "ctccore.chip.ember.weapon": "Sets the target on fire for 3 s", "ctccore.chip.ember.armor": "Attackers catch fire",
    "ctccore.chip.ember.tool": "Haste I",
    "ctccore.chip.terra.weapon": "+1.5 damage", "ctccore.chip.terra.armor": "8% less damage taken", "ctccore.chip.terra.tool": "Luck I",
    "ctccore.station.socket": "Adds a 3rd chip slot",
    "ctccore.ponder.voidsteel_melting.header": "Melting Voidsteel",
})
for i, t in enumerate([
        "Voidsteel does not melt in a furnace. It needs a Blaze Burner fed with Blaze Cake (superheated)",
        "Put a Basin on it and a Mechanical Mixer above. Drop in Voidsteel Ore or Raw Voidsteel",
        "Each piece becomes 250 mB of Molten Voidsteel. Crush the ore first: it gives two raw pieces",
        "Move the metal with pipes into a Basin under a Mechanical Press. Pressing needs no heat",
        "Every 250 mB is pressed into one Voidsteel Ingot"], 1):
    lang[f"ctccore.ponder.voidsteel_melting.text_{i}"] = t
json.dump(lang, open(lp, 'w'), indent=2, ensure_ascii=False)

# ---------------- ores: Northstar stone + crystals ----------------
ORES = {  # name: (planet, colour, drop, crystal style)
    'selenite': ('moon', (220, 236, 255), 'ctccore:selenite', 'shard'),
    'rubrium': ('mars', (214, 40, 56), 'ctccore:raw_rubrium', 'blob'),
    'sulfurite': ('venus', (246, 222, 64), 'ctccore:sulfurite', 'shard'),
    'solarite': ('mercury', (255, 150, 40), 'ctccore:solarite', 'shard'),
}
def ore_tex(base_png, col, style, seed):
    im = Image.open(base_png).convert('RGBA').resize((16, 16)); p = im.load(); r = random.Random(seed)
    spots = [(3, 4), (10, 3), (6, 10), (12, 11), (2, 12)]
    for (cx, cy) in spots:
        shape = [(0, 0), (1, 0), (0, 1), (1, 1)] if style == 'blob' else [(0, 0), (1, -1), (0, 1), (-1, 1)]
        for i, (dx, dy) in enumerate(shape + [(r.choice([-1, 2]), r.choice([0, 1]))]):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16: p[x, y] = shade(col, [1.35, 1.0, .85, .7, .6][i % 5])
        p[cx, cy] = shade(col, 1.6)
    return im
for name, (planet, col, drop, style) in ORES.items():
    for variant, src in (('', f'tools/ns/{planet}_stone.png'),):
        save(ore_tex(src, col, style, sum(map(ord, name))), A + f'textures/block/{name}_ore.png')
    # glowing overlay layer (just the crystals) so ores show up in dark caves
    ov = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); o = ov.load(); full = Image.open(A + f'textures/block/{name}_ore.png').load()
    base = Image.open(f'tools/ns/{planet}_stone.png').convert('RGBA').resize((16, 16)).load()
    for y in range(16):
        for x in range(16):
            if full[x, y] != base[x, y]: o[x, y] = full[x, y]
    save(ov, A + f'textures/block/{name}_ore_glow.png')
    w(A + f'models/block/{name}_ore.json', {"parent": "minecraft:block/block",
        "textures": {"particle": f"ctccore:block/{name}_ore", "base": f"ctccore:block/{name}_ore", "glow": f"ctccore:block/{name}_ore_glow"},
        "render_type": "minecraft:cutout",
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {d: {"texture": "#base", "cullface": d} for d in ("north", "south", "east", "west", "up", "down")}},
                     {"from": [0, 0, 0], "to": [16, 16, 16], "shade": False, "neoforge_data": {"block_light": 10, "sky_light": 15},
                      "faces": {d: {"texture": "#glow", "cullface": d} for d in ("north", "south", "east", "west", "up", "down")}}]})
    w(A + f'blockstates/{name}_ore.json', {"variants": {"": {"model": f"ctccore:block/{name}_ore"}}})
    w(A + f'models/item/{name}_ore.json', {"parent": f"ctccore:block/{name}_ore"})
    # loot: silk touch -> ore, else drop (fortune)
    w(D + f'ctccore/loot_table/blocks/{name}_ore.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": f"ctccore:{name}_ore", "conditions": [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
        {"type": "minecraft:item", "name": drop, "functions": [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                                                              {"function": "minecraft:explosion_decay"}]}]}]}]})
    # worldgen: only on its planet
    NS = [{"type": "neoforge:mod_loaded", "modid": "northstar"}]   # planet ores only exist with Northstar
    w(D + f'ctccore/worldgen/configured_feature/{name}_ore.json', {"neoforge:conditions": NS, "type": "minecraft:ore", "config": {"size": 7, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": f"northstar:{planet}_stone"}, "state": {"Name": f"ctccore:{name}_ore"}},
        {"target": {"predicate_type": "minecraft:block_match", "block": f"northstar:{planet}_deep_stone"}, "state": {"Name": f"ctccore:{name}_ore"}}]}})
    w(D + f'ctccore/worldgen/placed_feature/{name}_ore.json', {"neoforge:conditions": NS, "feature": f"ctccore:{name}_ore", "placement": [
        {"type": "minecraft:count", "count": 9}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -64}, "max_inclusive": {"absolute": 160}}},
        {"type": "minecraft:biome"}]})
    w(D + f'ctccore/neoforge/biome_modifier/{name}_ore.json', {"neoforge:conditions": NS, "type": "neoforge:add_features", "biomes": f"#ctccore:on_{planet}",
                                                              "features": f"ctccore:{name}_ore", "step": "underground_ores"})
BIOMES = {
    'moon': ['lunar_asurine_caves', 'lunar_cooled_lava_cave', 'lunar_crater_fields', 'lunar_glowstone_cavern', 'lunar_hills', 'lunar_ice_caves', 'lunar_plains'],
    'mars': ['martian_crimsite_caverns', 'martian_dunes', 'martian_highlands', 'martian_magmatic_caves', 'martian_overgrown_caverns', 'martian_peaks'],
    'mercury': ['mercury_basins', 'mercury_hills', 'mercury_icy_caverns', 'mercury_magmatic_caverns'],
    'venus': ['venus_fungal_caverns', 'venus_fungal_forest', 'venus_lava_caves', 'venus_sulfuric_caverns', 'venusian_plains', 'venusian_wastes']}
for planet, bs in BIOMES.items():
    w(D + f'ctccore/tags/worldgen/biome/on_{planet}.json', {"replace": False, "values": [{"id": f"northstar:{b}", "required": False} for b in bs]})
ores = [f'ctccore:{n}_ore' for n in ORES]
for t in ('mineable/pickaxe', 'needs_diamond_tool'):
    p = D + f'minecraft/tags/block/{t}.json'
    vals = json.load(open(p))['values'] if os.path.exists(p) else []
    w(p, {"values": vals + ores})
w(D + 'c/tags/block/ores.json', {"replace": False, "values": ores})
w(D + 'c/tags/item/ores.json', {"replace": False, "values": ores})

# ---------------- item textures ----------------
def gem(path, col, seed):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    rows = [(6, 9), (5, 10), (4, 11), (3, 12), (3, 12), (4, 11), (5, 10), (6, 9), (7, 8)]
    for i, (a, b) in enumerate(rows):
        y = 3 + i
        for x in range(a, b + 1):
            f = 1.45 if x - a < 2 and i < 4 else (.65 if x > b - 2 or i > 6 else 1.0)
            p[x, y] = shade(col, f)
    p[6, 5] = (255, 255, 255, 255); p[5, 6] = shade(col, 1.8)
    save(im, path)
def raw(path, col):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load(); r = random.Random(4)
    for y in range(3, 13):
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 8) ** 2 < 24 + r.randint(-6, 6): p[x, y] = shade(col, r.choice([.7, .85, 1, 1.15]))
    save(im, path)
def ingot(path, col):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y in range(5, 12):
        for x in range(2 + (11 - y) // 2, 14 - (y - 5) // 2):
            p[x, y] = shade(col, 1.4 if y == 5 else (.6 if y == 11 else 1.0 - (x - 2) * .02))
    save(im, path)
def gill(path):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y in range(3, 14):
        for x in range(4, 12):
            if abs(x - 7.5) < 4 - abs(y - 8) * .25:
                p[x, y] = shade((40, 150, 170), 1.3 if (y % 3 == 0) else (.75 if x > 9 else 1.0))
    for y in range(4, 13, 3): p[6, y] = (140, 240, 255, 255)
    save(im, path)
def relic(path):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y in range(2, 14):
        half = 1 + (y - 2) // 2
        for x in range(8 - half, 8 + half):
            p[x, y] = shade((216, 186, 110), 1.25 if x < 8 else .8)
    for (x, y) in ((7, 8), (8, 8), (7, 9), (8, 9)): p[x, y] = (60, 220, 200, 255)
    save(im, path)
def cryo(path):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y in range(16):
        for x in range(16):
            dd = abs(x - 7.5) + abs(y - 7.5)
            if dd < 6.5: p[x, y] = shade((150, 225, 255), 1.5 - dd * .12)
            elif dd < 7.5: p[x, y] = (70, 120, 170, 255)
    p[7, 7] = p[8, 8] = (255, 255, 255, 255)
    save(im, path)
def socket(path):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
    for y in range(2, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            inner = 5 <= x <= 10 and 5 <= y <= 10
            p[x, y] = (212, 176, 84, 255) if edge else ((18, 20, 34, 255) if inner else shade((70, 78, 104), 1.1 - (x + y) * .01))
    for (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)): p[x, y] = (84, 236, 214, 255)
    save(im, path)
T = A + 'textures/item/'
gem(T + 'selenite.png', (220, 236, 255), 1); gem(T + 'sulfurite.png', (246, 222, 64), 2); gem(T + 'solarite.png', (255, 150, 40), 3)
raw(T + 'raw_rubrium.png', (180, 40, 52)); ingot(T + 'rubrium_ingot.png', (214, 52, 66))
gill(T + 'abyssal_gill.png'); relic(T + 'desert_relic.png'); cryo(T + 'cryo_core.png'); socket(T + 'chip_socket.png')
# early chips reuse the chip painter from gen_assets
import importlib.util
for n in ['selenite', 'raw_rubrium', 'rubrium_ingot', 'sulfurite', 'solarite', 'abyssal_gill', 'desert_relic', 'cryo_core', 'chip_socket']:
    w(A + f'models/item/{n}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"ctccore:item/{n}"}})
for n in ('selenite', 'sulfurite', 'solarite'):
    pass

# ---------------- recipes ----------------
def rec(name, obj): w(D + f'ctccore/recipe/{name}.json', obj)
def shaped(name, pattern, key, result, count=1):
    rec(name, {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
               "key": {k: ({"tag": v[1:]} if v.startswith('#') else {"item": v}) for k, v in key.items()}, "result": {"id": result, "count": count}})
for kind, t in (('smelting', 200), ('blasting', 100)):
    rec(f'rubrium_ingot_from_{kind}', {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": "ctccore:raw_rubrium"},
                                       "result": {"id": "ctccore:rubrium_ingot"}, "experience": 1.0, "cookingtime": t})
shaped('chip_socket', ["SGS", "GBG", "SGS"], {"S": "ctccore:selenite", "G": "ctccore:solarite", "B": "ctccore:blank_chip"}, 'ctccore:chip_socket')
# cheaper station: reachable before space
shaped('blank_chip', [" G ", "RTR", " G "], {"G": "minecraft:gold_ingot", "R": "minecraft:redstone", "T": "create:electron_tube"}, 'ctccore:blank_chip', 2)
shaped('research_casing', [" I ", "IBI", " I "], {"I": "#c:plates/iron", "B": "create:brass_casing"}, 'ctccore:research_casing', 4)
shaped('research_station', ["BEB", "PSP", "CCC"], {"B": "#c:plates/brass", "E": "create:electron_tube", "P": "create:precision_mechanism",
                                                  "S": "minecraft:smithing_table", "C": "create:brass_casing"}, 'ctccore:research_station')

# ---------------- Ponder schematic: voidsteel_melting (7 x 6 x 7) ----------------
def nbt_str(v): b = v.encode(); return struct.pack('>H', len(b)) + b
def tag(t, name, payload): return bytes([t]) + nbt_str(name) + payload
def compound(items): return b''.join(items) + b'\x00'
def int_list(vals): return bytes([3]) + struct.pack('>i', len(vals)) + b''.join(struct.pack('>i', v) for v in vals)
def comp_list(comps): return bytes([10]) + struct.pack('>i', len(comps)) + b''.join(compound(c) for c in comps)
def schematic(path, size, blocks):
    palette = []
    def st(name, props):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in palette: palette.append(key)
        return palette.index(key)
    bl = [(x, y, z, st(n, pr)) for x, y, z, n, pr in blocks]
    pal = []
    for name, props in palette:
        items = [tag(8, 'Name', nbt_str(name))]
        if props: items.append(tag(10, 'Properties', compound([tag(8, k, nbt_str(v)) for k, v in props])))
        pal.append(items)
    root = compound([tag(3, 'DataVersion', struct.pack('>i', 3955)), tag(9, 'size', int_list(list(size))), tag(9, 'palette', comp_list(pal)),
                     tag(9, 'blocks', comp_list([[tag(9, 'pos', int_list([x, y, z])), tag(3, 'state', struct.pack('>i', s))] for x, y, z, s in bl])),
                     tag(9, 'entities', bytes([0]) + struct.pack('>i', 0))])
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, 'wb') as f: f.write(bytes([10]) + nbt_str('') + root)
b = [(x, 0, z, 'minecraft:snow_block' if (x + z) % 2 else 'minecraft:smooth_stone', None) for x in range(7) for z in range(7)]
b += [(2, 1, 3, 'create:blaze_burner', {'blaze': 'seething', 'facing': 'north'}), (2, 2, 3, 'create:basin', {'facing': 'down'}),
      (2, 4, 3, 'create:mechanical_mixer', None), (2, 5, 3, 'create:creative_motor', {'facing': 'down'}),
      (5, 1, 3, 'create:basin', {'facing': 'down'}), (5, 3, 3, 'create:mechanical_press', {'facing': 'north'}),
      (5, 3, 4, 'create:creative_motor', {'facing': 'north'})]
schematic(A + 'ponder/voidsteel_melting.nbt', (7, 6, 7), b)
print('materials ok')
