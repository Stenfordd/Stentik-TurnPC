package dev.stentik.ctccore;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Cartographers sell expedition maps to the Cataclysm boss lairs of the Overworld. */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class Expeditions {
    private static TagKey<Structure> tag(String name) {
        return TagKey.create(Registries.STRUCTURE, CtcCore.id("map/" + name));
    }

    @SubscribeEvent
    public static void trades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CARTOGRAPHER) return;
        var trades = event.getTrades();
        trades.get(2).add(new VillagerTrades.TreasureMapForEmeralds(14, tag("ancient_factory"), "filled_map.ctccore.ancient_factory",
                MapDecorationTypes.RED_X, 2, 10));
        trades.get(3).add(new VillagerTrades.TreasureMapForEmeralds(18, tag("cursed_pyramid"), "filled_map.ctccore.cursed_pyramid",
                MapDecorationTypes.RED_X, 2, 15));
        trades.get(3).add(new VillagerTrades.TreasureMapForEmeralds(18, tag("frosted_prison"), "filled_map.ctccore.frosted_prison",
                MapDecorationTypes.RED_X, 2, 15));
        trades.get(4).add(new VillagerTrades.TreasureMapForEmeralds(22, tag("sunken_city"), "filled_map.ctccore.sunken_city",
                MapDecorationTypes.RED_X, 2, 20));
    }
}
