package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Materials that exist in one place only.
 * Planet ores (worldgen in data/ctccore/worldgen, biome lists in data/ctccore/tags/worldgen/biome) and boss trophies
 * (dropped by the Cataclysm bosses that guard the oceans, deserts and snow). Recipes that need them: kubejs ctc_recipes.js.
 */
public final class Materials {
    private Materials() {
    }

    public static final List<DeferredItem<?>> ALL = new ArrayList<>();

    private static DeferredBlock<Block> ore(String name, MapColor color) {
        DeferredBlock<Block> block = CtcCore.BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of().mapColor(color)
                .strength(4.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
        ALL.add(CtcCore.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    private static DeferredItem<Item> item(String name) {
        DeferredItem<Item> item = CtcCore.ITEMS.registerSimpleItem(name);
        ALL.add(item);
        return item;
    }

    // Moon, Mars, Venus, Mercury
    public static final DeferredBlock<Block> SELENITE_ORE = ore("selenite_ore", MapColor.SNOW);
    public static final DeferredItem<Item> SELENITE = item("selenite");
    public static final DeferredBlock<Block> RUBRIUM_ORE = ore("rubrium_ore", MapColor.COLOR_RED);
    public static final DeferredItem<Item> RAW_RUBRIUM = item("raw_rubrium");
    public static final DeferredItem<Item> RUBRIUM_INGOT = item("rubrium_ingot");
    public static final DeferredBlock<Block> SULFURITE_ORE = ore("sulfurite_ore", MapColor.COLOR_YELLOW);
    public static final DeferredItem<Item> SULFURITE = item("sulfurite");
    public static final DeferredBlock<Block> SOLARITE_ORE = ore("solarite_ore", MapColor.COLOR_ORANGE);
    public static final DeferredItem<Item> SOLARITE = item("solarite");

    // boss trophies
    public static final DeferredItem<Item> ABYSSAL_GILL = item("abyssal_gill");     // The Leviathan, Sunken City
    public static final DeferredItem<Item> DESERT_RELIC = item("desert_relic");     // Ancient Remnant, Cursed Pyramid
    public static final DeferredItem<Item> CRYO_CORE = item("cryo_core");           // Maledictus, Frosted Prison

    // the third chip slot
    public static final DeferredItem<Item> CHIP_SOCKET = item("chip_socket");

    static void init() {
        // touching the class registers everything above
    }
}
