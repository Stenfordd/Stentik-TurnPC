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

    // 2.0: trophies of the bosses that awaken Cracked VoidSteel, and the sigil that calls the Void Herald
    private static DeferredItem<Item> rare(String name, net.minecraft.world.item.Rarity rarity, int stack) {
        DeferredItem<Item> item = CtcCore.ITEMS.registerSimpleItem(name, new Item.Properties().rarity(rarity).stacksTo(stack));
        ALL.add(item);
        return item;
    }
    public static final DeferredItem<Item> DRAGON_SCALE = rare("dragon_scale", net.minecraft.world.item.Rarity.EPIC, 16);      // Ender Dragon
    public static final DeferredItem<Item> WITHER_CORE = rare("wither_core", net.minecraft.world.item.Rarity.EPIC, 16);        // Wither
    public static final DeferredItem<Item> TIDAL_PEARL = rare("tidal_pearl", net.minecraft.world.item.Rarity.RARE, 16);        // Scylla
    public static final DeferredItem<Item> INFERNAL_RIVET = rare("infernal_rivet", net.minecraft.world.item.Rarity.RARE, 16);  // Netherite Monstrosity
    public static final DeferredItem<Item> VOID_HEART = rare("void_heart", net.minecraft.world.item.Rarity.EPIC, 16);          // Void Herald
    public static final DeferredItem<VoidHerald.SigilItem> VOID_SIGIL = CtcCore.ITEMS.register("void_sigil",
            () -> new VoidHerald.SigilItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1)));
    public static final String[] PIECES = { "helmet", "chestplate", "leggings", "boots" };
    @SuppressWarnings("unchecked")
    public static final DeferredItem<Item>[] CRACKED = new DeferredItem[4];
    static {
        ALL.add(VOID_SIGIL);
        for (int i = 0; i < 4; i++) CRACKED[i] = rare("cracked_voidsteel_" + PIECES[i], net.minecraft.world.item.Rarity.UNCOMMON, 1);
    }

    // the third chip slot
    public static final DeferredItem<Item> CHIP_SOCKET = item("chip_socket");
    public static final DeferredItem<Item> VOIDFORGE_TEMPLATE = item("voidforge_template");

    static void init() {
        // touching the class registers everything above
    }
}
