"""Pack items in structure chests (global loot modifiers), structure tags for expedition maps. Called by gen_assets.py."""
import os, json
R = 'src/main/resources/'; A = R + 'assets/ctccore/'; D = R + 'data/'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=2, ensure_ascii=False)
def it(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e
EARLY = ["simple_dungeon", "abandoned_mineshaft", "desert_pyramid", "jungle_temple", "igloo_chest", "pillager_outpost", "ruined_portal",
         "shipwreck_supply", "shipwreck_treasure", "buried_treasure", "village/village_weaponsmith", "village/village_toolsmith",
         "village/village_armorer", "trail_ruins"]
LATE = ["stronghold_corridor", "stronghold_crossing", "stronghold_library", "ancient_city", "woodland_mansion", "nether_bridge",
        "bastion_treasure", "bastion_other", "end_city_treasure"]
w(D + 'ctccore/loot_table/chests/extra_early.json', {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [
        it("create:andesite_alloy", 20, 4, 12), it("create:cogwheel", 10, 2, 6), it("create:shaft", 10, 4, 12), it("create:brass_ingot", 8, 2, 6),
        it("create:zinc_ingot", 10, 2, 8), it("ctccore:copper_chip", 6), it("ctccore:iron_chip", 4), it("ctccore:kinetic_chip", 3),
        it("ctccore:lumber_chip", 3), it("ctccore:vein_chip", 3), it("create:precision_mechanism", 2), {"type": "minecraft:empty", "weight": 25}]}]})
w(D + 'ctccore/loot_table/chests/extra_late.json', {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 3}, "entries": [
        it("create:precision_mechanism", 12, 1, 3), it("create:brass_ingot", 12, 4, 10), it("ctccore:blank_chip", 10, 1, 3),
        it("ctccore:ember_chip", 5), it("ctccore:terra_chip", 5), it("ctccore:kinetic_chip", 5), it("ctccore:harvest_chip", 4),
        it("ctccore:chip_socket", 2), {"type": "minecraft:empty", "weight": 20}]},
    {"rolls": 1, "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/end_city_treasure"}],
     "entries": [it("ctccore:voidforge_template", 1), {"type": "minecraft:empty", "weight": 6}]}]})
def ids(names): return [{"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/" + n} for n in names]
w(D + 'ctccore/loot_modifiers/extra_early.json', {"type": "neoforge:add_table", "conditions": [{"condition": "minecraft:any_of", "terms": ids(EARLY)}],
                                                   "table": "ctccore:chests/extra_early"})
w(D + 'ctccore/loot_modifiers/extra_late.json', {"type": "neoforge:add_table", "conditions": [{"condition": "minecraft:any_of", "terms": ids(LATE)}],
                                                  "table": "ctccore:chests/extra_late"})
w(D + 'neoforge/loot_modifiers/global_loot_modifiers.json', {"replace": False, "entries": ["ctccore:extra_early", "ctccore:extra_late"]})
for name in ("ancient_factory", "cursed_pyramid", "frosted_prison", "sunken_city"):
    w(D + f'ctccore/tags/worldgen/structure/map/{name}.json', {"replace": False, "values": [{"id": f"cataclysm:{name}", "required": False}]})
lp = A + 'lang/en_us.json'; lang = json.load(open(lp))
lang.update({"filled_map.ctccore.ancient_factory": "Expedition Map: Ancient Factory", "filled_map.ctccore.cursed_pyramid": "Expedition Map: Cursed Pyramid",
             "filled_map.ctccore.frosted_prison": "Expedition Map: Frosted Prison", "filled_map.ctccore.sunken_city": "Expedition Map: Sunken City"})
json.dump(lang, open(lp, 'w'), indent=2, ensure_ascii=False)
print('loot ok')

