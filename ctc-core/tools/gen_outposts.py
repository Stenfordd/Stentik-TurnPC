"""Planet outposts: one structure per planet with loot and a miniboss. Called by gen_assets.py."""
import os, json, gzip, struct, math, random
R = 'src/main/resources/'; A = R + 'assets/ctccore/'; D = R + 'data/'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2, ensure_ascii=False)

# ---------- tiny NBT writer: int -> Int, str -> String, dict -> Compound, list -> List, ('b'|'s'|'f'|'d'|'l', v) typed ----------
def _s(v): b = v.encode(); return struct.pack('>H', len(b)) + b
def _tag(v):
    if isinstance(v, tuple):
        t, x = v
        code, fmt = {'b': (1, '>b'), 's': (2, '>h'), 'f': (5, '>f'), 'd': (6, '>d'), 'l': (4, '>q')}[t]
        return code, struct.pack(fmt, x)
    if isinstance(v, bool): return 1, struct.pack('>b', int(v))
    if isinstance(v, int): return 3, struct.pack('>i', v)
    if isinstance(v, float): return 6, struct.pack('>d', v)
    if isinstance(v, str): return 8, _s(v)
    if isinstance(v, dict): return 10, b''.join(bytes([_tag(x)[0]]) + _s(k) + _tag(x)[1] for k, x in v.items()) + b'\x00'
    if isinstance(v, list):
        if not v: return 9, bytes([10]) + struct.pack('>i', 0)
        t = _tag(v[0])[0]
        return 9, bytes([t]) + struct.pack('>i', len(v)) + b''.join(_tag(x)[1] for x in v)
    raise TypeError(v)
def write_nbt(path, root):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, 'wb') as f: f.write(bytes([10]) + _s('') + _tag(root)[1][:])

class Build:
    def __init__(s, sx, sy, sz): s.size = (sx, sy, sz); s.b = {}; s.be = {}; s.ents = []
    def set(s, x, y, z, block, props=None, nbt=None):
        if 0 <= x < s.size[0] and 0 <= y < s.size[1] and 0 <= z < s.size[2]:
            s.b[(x, y, z)] = (block, tuple(sorted((props or {}).items())))
            if nbt: s.be[(x, y, z)] = nbt
            else: s.be.pop((x, y, z), None)
    def fill(s, x0, y0, z0, x1, y1, z1, block, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1): s.set(x, y, z, block, props)
    def chest(s, x, y, z, table, facing='north'):
        s.set(x, y, z, 'minecraft:chest', {'facing': facing, 'type': 'single', 'waterlogged': 'false'}, {'id': 'minecraft:chest', 'LootTable': table})
    def boss(s, x, y, z, nbt): s.ents.append((x, y, z, nbt))
    def save(s, path):
        pal = []; blocks = []
        for (x, y, z), key in sorted(s.b.items()):
            if key not in pal: pal.append(key)
            e = {'pos': [x, y, z], 'state': pal.index(key)}
            if (x, y, z) in s.be: e['nbt'] = s.be[(x, y, z)]
            blocks.append(e)
        palette = []
        for name, props in pal:
            e = {'Name': name}
            if props: e['Properties'] = dict(props)
            palette.append(e)
        ents = [{'pos': [('d', x + 0.5), ('d', float(y)), ('d', z + 0.5)], 'blockPos': [x, y, z], 'nbt': n} for x, y, z, n in s.ents]
        write_nbt(path, {'DataVersion': 3955, 'size': list(s.size), 'palette': palette, 'blocks': blocks, 'entities': ents})

def mob(entity, name, color, tag, hp, hand=None, armor=None, effects=()):
    n = {'id': entity, 'CustomName': json.dumps({"text": name, "color": color, "bold": True}), 'CustomNameVisible': ('b', 1),
         'PersistenceRequired': ('b', 1), 'Tags': [tag], 'Health': ('f', float(hp)),
         'attributes': [{'id': 'minecraft:generic.max_health', 'base': float(hp)},
                        {'id': 'minecraft:generic.knockback_resistance', 'base': 0.6},
                        {'id': 'minecraft:generic.follow_range', 'base': 32.0}],
         'active_effects': [{'id': e, 'amplifier': ('b', a), 'duration': -1, 'show_particles': ('b', 0)} for e, a in effects],
         'HandDropChances': [('f', 0.0), ('f', 0.0)], 'ArmorDropChances': [('f', 0.0)] * 4}
    n['HandItems'] = [{'id': hand, 'count': 1} if hand else {}, {}]
    n['ArmorItems'] = [{'id': a, 'count': 1} if a else {} for a in (armor or [None] * 4)]
    return n

AIR = 'minecraft:air'
# ---------------- Moon: research dome ----------------
def moon():
    b = Build(17, 11, 17); c = 8
    for x in range(17):
        for z in range(17):
            d = math.hypot(x - c, z - c)
            if d <= 7.6:
                b.set(x, 0, z, 'northstar:polished_moon_stone'); b.set(x, 1, z, 'northstar:titanium_plating')
                for y in range(2, 11): b.set(x, y, z, AIR)
            if 6.6 < d <= 7.6:
                for y in range(2, 6): b.set(x, y, z, 'minecraft:light_gray_stained_glass' if y in (3, 4) and (x + z) % 3 else 'northstar:titanium_sheetmetal')
            r = math.sqrt(max(0, 7.6 ** 2 - d ** 2))
            if d <= 7.6:
                top = 6 + int(r * 0.55)
                if top <= 10: b.set(x, top, z, 'minecraft:light_gray_stained_glass' if d < 3 else 'northstar:titanium_plating')
    for (x, z) in ((c, c - 7), (c, c + 7), (c - 7, c), (c + 7, c)): b.fill(x, 2, z, x, 5, z, 'northstar:titanium_pillar', {'axis': 'y'})
    b.fill(c, 2, 1, c, 3, 1, AIR)                                   # door
    for (x, z) in ((4, 4), (12, 4), (4, 12), (12, 12)): b.set(x, 2, z, 'northstar:moon_stone_lamp')
    b.fill(3, 2, 8, 5, 2, 8, 'create:andesite_casing'); b.fill(11, 2, 8, 13, 2, 8, 'create:brass_casing')
    b.set(4, 3, 8, 'northstar:lunar_sapphire_block'); b.set(12, 3, 8, 'minecraft:brewing_stand')
    b.chest(c - 2, 2, 13, 'ctccore:chests/outpost_moon'); b.chest(c + 2, 2, 13, 'ctccore:chests/outpost_moon'); b.chest(14, 2, c, 'ctccore:chests/outpost_moon', 'west')
    b.boss(c, 2, c, mob('minecraft:stray', 'Lunar Sentinel', 'aqua', 'ctc_boss_moon', 140, 'minecraft:bow',
                        [None, None, 'minecraft:iron_chestplate', 'minecraft:iron_helmet'], [('minecraft:resistance', 0), ('minecraft:speed', 0)]))
    return b

# ---------------- Mars: crashed rocket ----------------
def mars():
    b = Build(23, 10, 13); cz = 6
    b.fill(0, 0, 0, 22, 0, 12, 'northstar:mars_sand')
    for x in range(1, 21):
        cy = 3 + (x - 1) * 0.12          # tilted body, nose buried at x=0
        for y in range(10):
            for z in range(13):
                d = math.hypot(y - cy, z - cz)
                if d <= 3.4: b.set(x, y, z, AIR if d <= 2.4 else ('northstar:martian_steel_plating' if (x + y) % 5 else 'northstar:martian_steel_sheetmetal'))
    for z in (1, 11):
        b.fill(17, 1, z, 21, 2, z, 'northstar:martian_steel_large_plating')   # fins
    b.fill(21, 2, cz - 1, 22, 5, cz + 1, AIR)                                      # broken engine = entrance
    b.set(21, 1, cz, 'minecraft:magma_block'); b.set(22, 1, cz + 1, 'minecraft:magma_block')
    b.fill(2, 1, cz - 1, 3, 1, cz + 1, 'northstar:martian_steel_grate')
    for x in (6, 12): b.set(x, 2, cz + 2, 'northstar:martian_steel_lamp')
    b.chest(4, 2, cz - 1, 'ctccore:chests/outpost_mars', 'east'); b.chest(4, 2, cz + 1, 'ctccore:chests/outpost_mars', 'east')
    b.chest(10, 2, cz + 2, 'ctccore:chests/outpost_mars', 'north')
    b.boss(9, 2, cz, mob('minecraft:vindicator', 'Martian Warden', 'red', 'ctc_boss_mars', 170, 'minecraft:netherite_axe',
                        [None, None, None, 'minecraft:chainmail_helmet'], [('minecraft:resistance', 0), ('minecraft:fire_resistance', 0)]))
    return b

# ---------------- Venus: stepped temple ----------------
def venus():
    b = Build(19, 12, 19)
    for lvl in range(5):
        a, z1 = lvl * 2, 18 - lvl * 2
        b.fill(a, lvl * 2, a, z1, lvl * 2 + 1, z1, 'northstar:venus_stone_bricks')
    b.fill(4, 1, 4, 14, 7, 14, AIR)                                           # inner hall
    b.fill(4, 0, 4, 14, 0, 14, 'northstar:polished_venus_stone')
    for (x, z) in ((5, 5), (13, 5), (5, 13), (13, 13)): b.fill(x, 1, z, x, 7, z, 'northstar:venus_stone_pillar', {'axis': 'y'})
    b.fill(9, 1, 0, 9, 3, 4, AIR); b.fill(8, 1, 0, 10, 3, 3, AIR)            # corridor
    for (x, z) in ((7, 9), (11, 9), (9, 7), (9, 11)): b.set(x, 1, z, 'northstar:venus_stone_lamp')
    b.set(9, 1, 9, 'minecraft:slime_block'); b.set(9, 2, 9, 'northstar:spike_fungus_block')
    b.chest(6, 1, 12, 'ctccore:chests/outpost_venus'); b.chest(12, 1, 12, 'ctccore:chests/outpost_venus'); b.chest(9, 1, 13, 'ctccore:chests/outpost_venus')
    b.boss(9, 1, 7, mob('minecraft:evoker', 'Venusian Oracle', 'green', 'ctc_boss_venus', 160, None, None,
                        [('minecraft:resistance', 0), ('minecraft:fire_resistance', 0)]))
    return b

# ---------------- Mercury: sun observatory ----------------
def mercury():
    b = Build(15, 16, 15); c = 7
    for x in range(15):
        for z in range(15):
            d = math.hypot(x - c, z - c)
            if d <= 6.5:
                b.set(x, 0, z, 'northstar:polished_mercury_stone')
                for y in range(1, 16): b.set(x, y, z, AIR)
            if 5.5 < d <= 6.5:
                for y in range(1, 11): b.set(x, y, z, 'northstar:mercury_stone_bricks' if y % 4 else 'northstar:tungsten_plating')
            if d <= 6.5: b.set(x, 11, z, 'northstar:tungsten_plating' if d > 3 else 'minecraft:orange_stained_glass')
            if d <= 3.5: b.set(x, 6, z, 'northstar:tungsten_grate')
    b.fill(c, 1, 1, c, 2, 1, AIR)
    for y in range(12, 15): b.set(c, y, c, 'minecraft:lightning_rod', {'facing': 'up', 'powered': 'false', 'waterlogged': 'false'})
    b.set(c, 15, c, 'minecraft:end_rod', {'facing': 'up'})
    for (x, z) in ((3, 3), (11, 3), (3, 11), (11, 11)): b.set(x, 1, z, 'northstar:mercury_stone_lamp')
    b.chest(3, 7, c, 'ctccore:chests/outpost_mercury', 'east'); b.chest(11, 7, c, 'ctccore:chests/outpost_mercury', 'west')
    b.chest(c, 1, 12, 'ctccore:chests/outpost_mercury')
    b.fill(1, 1, 3, 3, 6, 3, 'northstar:mercury_stone_brick_wall')            # stairs-ish column to the upper floor
    for y in range(1, 6): b.set(4, y, 12 - y, 'northstar:mercury_stone_bricks')
    b.boss(c, 1, c, mob('minecraft:blaze', 'Solar Herald', 'gold', 'ctc_boss_mercury', 150, None, None, [('minecraft:resistance', 0)]))
    return b

NS = [{"type": "neoforge:mod_loaded", "modid": "northstar"}]
PLANETS = {'moon': (moon, 'Lunar Research Outpost', 31), 'mars': (mars, 'Crashed Rocket', 32), 'venus': (venus, 'Venusian Temple', 33),
           'mercury': (mercury, 'Sun Observatory', 34)}
MAT = {'moon': 'ctccore:selenite', 'mars': 'ctccore:raw_rubrium', 'venus': 'ctccore:sulfurite', 'mercury': 'ctccore:solarite'}
CHIP = {'moon': 'gravity', 'mars': 'dust', 'venus': 'acid', 'mercury': 'flare'}
for p, (fn, title, salt) in PLANETS.items():
    fn().save(A.replace('assets', 'data') + f'structure/outpost/{p}.nbt')
    w(D + f'ctccore/worldgen/template_pool/outpost/{p}.json', {"neoforge:conditions": NS, "fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"ctccore:outpost/{p}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    w(D + f'ctccore/worldgen/structure/outpost_{p}.json', {"neoforge:conditions": NS, "type": "minecraft:jigsaw", "biomes": f"#ctccore:on_{p}",
        "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "beard_thin", "start_pool": f"ctccore:outpost/{p}", "size": 1,
        "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80, "use_expansion_hack": False})
    w(D + f'ctccore/worldgen/structure_set/outpost_{p}.json', {"neoforge:conditions": NS, "structures": [{"structure": f"ctccore:outpost_{p}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 26, "separation": 9, "salt": 7310000 + salt}})
    # loot: planet material, tech parts, basic chips, rare socket and a rare copy of the planet chip
    w(D + f'ctccore/loot_table/chests/outpost_{p}.json', {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            {"type": "minecraft:item", "name": MAT[p], "weight": 20, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 6}}]},
            {"type": "minecraft:item", "name": "create:precision_mechanism", "weight": 10, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": "create:brass_ingot", "weight": 12, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 4, "max": 12}}]},
            {"type": "minecraft:item", "name": "ctccore:blank_chip", "weight": 10, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": "minecraft:golden_apple", "weight": 6},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 6, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": "minecraft:experience_bottle", "weight": 8, "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 8}}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "ctccore:chip_socket", "weight": 1}, {"type": "minecraft:empty", "weight": 7}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"ctccore:{CHIP[p]}_chip", "weight": 1}, {"type": "minecraft:empty", "weight": 12}]}]})
print('outposts ok')
