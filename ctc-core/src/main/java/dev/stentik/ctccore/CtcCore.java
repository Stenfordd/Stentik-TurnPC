package dev.stentik.ctccore;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CtcCore.MODID)
public class CtcCore {
    public static final String MODID = "ctccore";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<ResearchStationBlock> STATION = BLOCKS.register("research_station",
            () -> new ResearchStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 9.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredItem<?> STATION_ITEM = ITEMS.registerSimpleBlockItem(STATION);

    public static final Supplier<BlockEntityType<ResearchStationBlockEntity>> STATION_BE = BLOCK_ENTITIES.register("research_station",
            () -> BlockEntityType.Builder.of(ResearchStationBlockEntity::new, STATION.get()).build(null));

    public static final Supplier<MenuType<StationMenu>> STATION_MENU = MENUS.register("research_station",
            () -> IMenuTypeExtension.create(StationMenu::new));

    /** Chips implanted into a piece of gear, oldest first (ids of {@link ChipType}). */
    public static final Supplier<DataComponentType<List<String>>> CHIPS = COMPONENTS.register("chips",
            () -> DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

    public static final DeferredItem<Item> BLANK_CHIP = ITEMS.registerSimpleItem("blank_chip");
    public static final DeferredItem<Item> CHIP_SCRAP = ITEMS.registerSimpleItem("chip_scrap");
    public static final Map<ChipType, DeferredItem<ChipItem>> CHIP_ITEMS = new EnumMap<>(ChipType.class);

    static {
        for (ChipType type : ChipType.values()) {
            CHIP_ITEMS.put(type, ITEMS.register(type.id + "_chip", () -> new ChipItem(type, new Item.Properties().stacksTo(16))));
        }
    }

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ctccore"))
            .icon(() -> new ItemStack(STATION_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(STATION_ITEM.get());
                out.accept(BLANK_CHIP.get());
                out.accept(CHIP_SCRAP.get());
                for (ChipType type : ChipType.values()) out.accept(CHIP_ITEMS.get(type).get());
            }).build());

    public CtcCore(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(CtcCore::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, STATION_BE.get(), (be, side) -> be.energy);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STATION_BE.get(), (be, side) -> be.items);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
