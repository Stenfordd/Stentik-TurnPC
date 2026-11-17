"""where_to_find.json: where every important ore and deposit is, for the JEI 'Where to find' tab. Called by gen_assets.py."""
import json, os
E = []
def ore(items, place, where, ymin, ymax, best, how, note=None):
    E.append({"items": items, "place": place, "where": where, "ymin": ymin, "ymax": ymax, "best": best, "how": how, "note": note, "deposit": False})
def dep(items, where, how, note=None):
    E.append({"items": items, "place": "overworld", "where": where, "ymin": None, "ymax": None, "best": None, "how": how, "note": note, "deposit": True})
P = "Any pickaxe"; S = "Stone pickaxe"; I = "Iron pickaxe"; D = "Diamond pickaxe"
# ---------- Overworld ----------
ore(["minecraft:coal_ore", "minecraft:deepslate_coal_ore", "minecraft:coal"], "overworld", "Everywhere", 0, 320, 96, P, "Also high up in mountains")
ore(["minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:raw_iron"], "overworld", "Everywhere", -64, 320, 16, S, "Huge amounts on mountain tops (Y 230+)")
ore(["minecraft:copper_ore", "minecraft:deepslate_copper_ore", "minecraft:raw_copper"], "overworld", "Everywhere", -16, 112, 48, S, "Big veins in Dripstone Caves")
ore(["create:zinc_ore", "create:deepslate_zinc_ore", "create:raw_zinc"], "overworld", "Everywhere", -63, 70, None, S, "Big deposits in mountains: see Zinc Deposit")
ore(["minecraft:andesite"], "overworld", "Everywhere, in blobs", 0, 128, None, "Any pickaxe", "Also grey stripes in mountain cliffs")
ore(["minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:raw_gold"], "overworld", "Everywhere", -64, 32, -16, I, "Badlands: lots of gold from Y 32 to 256")
ore(["minecraft:redstone_ore", "minecraft:deepslate_redstone_ore", "minecraft:redstone"], "overworld", "Everywhere", -64, 15, -59, I)
ore(["minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:lapis_lazuli"], "overworld", "Everywhere", -64, 64, 0, S)
ore(["minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:diamond"], "overworld", "Everywhere", -64, 16, -59, I)
ore(["minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:emerald"], "overworld", "Mountains only", -16, 320, 236, I, "The higher, the more")
ore(["createnuclear:uranium_ore", "createnuclear:deepslate_uranium_ore", "createnuclear:raw_uranium"], "overworld", "Hot lands: badlands, desert", -63, 16, -24, I, "Radioactive! Big amounts: Uranium Deposit")
ore(["createnuclear:thorium_ore", "createnuclear:deepslate_thorium_ore", "createnuclear:raw_thorium"], "overworld", "Tropical: jungle, savanna", -16, 55, 20, I)
ore(["createnuclear:lead_ore", "createnuclear:deepslate_lead_ore", "createnuclear:raw_lead"], "overworld", "Cold lands: taiga, snowy biomes", -16, 60, 22, S)
ore(["createnuclear:nitrate_ore", "createnuclear:deepslate_nitrate_ore", "createnuclear:nitrate"], "overworld", "Hot lands: badlands, desert", -63, 64, 0, S)
# ---------- Nether / End ----------
ore(["minecraft:nether_quartz_ore", "minecraft:quartz"], "nether", "Everywhere in the Nether", 10, 117, None, P)
ore(["minecraft:ancient_debris", "minecraft:netherite_scrap"], "nether", "Everywhere in the Nether", 8, 119, 15, D, "Explode it out with TNT or beds")
ore(["ctc:voidsteel_ore", "ctc:raw_voidsteel"], "end", "Outer End islands only (not the dragon island)", 0, 320, None, D,
    "Does not smelt: melt it in a superheated mixer (Ponder)")
# ---------- Space ----------
ore(["ctccore:selenite_ore", "ctccore:selenite"], "moon", "Everywhere on the Moon", -64, 160, None, D, "For the Chip Socket")
ore(["northstar:moon_titanium_ore", "northstar:moon_deep_titanium_ore", "northstar:raw_titanium_ore"], "moon", "Everywhere on the Moon", -64, 48, -8, D)
ore(["northstar:lunar_sapphire_shard"], "moon", "Geodes deep under the Moon", -58, 5, None, D)
ore(["ctccore:rubrium_ore", "ctccore:raw_rubrium"], "mars", "Everywhere on Mars", -64, 160, None, D, "Smelt into Rubrium Ingots, for AE2")
ore(["northstar:mars_titanium_ore", "northstar:mars_deep_titanium_ore", "northstar:raw_titanium_ore"], "mars", "Everywhere on Mars", -64, 36, -64, D)
ore(["ctccore:sulfurite_ore", "ctccore:sulfurite"], "venus", "Everywhere on Venus", -64, 160, None, D, "For the AE2 Inscriber")
ore(["northstar:venus_titanium_ore", "northstar:venus_deep_titanium_ore", "northstar:raw_titanium_ore"], "venus", "Everywhere on Venus", -63, 60, None, D, "Lots of titanium")
ore(["ctccore:solarite_ore", "ctccore:solarite"], "mercury", "Everywhere on Mercury", -64, 160, None, D, "For the Chip Socket")
ore(["northstar:mercury_tungsten_ore", "northstar:mercury_deep_tungsten_ore", "northstar:raw_tungsten_ore"], "mercury", "Only on Mercury, deep down", -64, 46, -40, D,
    "The only tungsten in the game")
# ---------- Deposits (Create Ore Excavation) ----------
ADV = "Drilling Machine with a diamond or netherite drill"
NRM = "Drilling Machine with any drill"
dep(["createnuclear:raw_uranium"], "Hot lands: badlands, desert", ADV, "Uranium Deposit, needs water")
dep(["createnuclear:raw_thorium"], "Tropical: jungle, savanna", ADV, "Thorium Deposit, needs water")
dep(["createnuclear:raw_lead"], "Cold lands: taiga, snowy biomes", ADV, "Lead Deposit, needs water")
dep(["createnuclear:nitrate"], "Hot lands: badlands, desert", ADV, "Nitrate Deposit, needs water")
dep(["minecraft:clay_ball"], "Wetlands: swamp, river, beach", NRM, "Clay Deposit")
dep(["minecraft:raw_gold"], "Hot lands: badlands, desert", NRM, "Gold Deposit")
dep(["create:raw_zinc"], "Highlands: mountains, hills", NRM, "Zinc Deposit")
dep(["createoreexcavation:raw_diamond"], "Highlands: mountains, hills", NRM, "Diamond Deposit")
dep(["createoreexcavation:raw_emerald"], "Highlands: mountains, hills", NRM, "Emerald Deposit")
dep(["minecraft:lapis_lazuli"], "Tropical: jungle, savanna", NRM, "Lapis Deposit")
dep(["createoreexcavation:raw_redstone"], "Tropical: jungle, savanna", NRM, "Redstone Deposit")
os.makedirs('src/main/resources/assets/ctccore', exist_ok=True)
json.dump(E, open('src/main/resources/assets/ctccore/where_to_find.json', 'w'), indent=1)
print('where', len(E))
