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
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final Supplier<ChipSmithingRecipe.Serializer> CHIP_SMITHING = SERIALIZERS.register("chip_smithing", ChipSmithingRecipe.Serializer::new);

    public static final DeferredBlock<ResearchStationBlock> STATION = BLOCKS.register("research_station",
            () -> new ResearchStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 9.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(ResearchStationBlock.FORMED) ? 10 : 0)));
    public static final DeferredItem<?> STATION_ITEM = ITEMS.registerSimpleBlockItem(STATION);
    /** The frame of the Research Station multiblock. */
    public static final DeferredBlock<ResearchCasingBlock> CASING = BLOCKS.register("research_casing",
            () -> new ResearchCasingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 9.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ResearchCasingBlock.FORMED) ? 7 : 0)));
    public static final DeferredItem<?> CASING_ITEM = ITEMS.registerSimpleBlockItem(CASING);

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

    /** Extra chip slots a piece of gear got from a Chip Socket (0 or 1). */
    public static final Supplier<DataComponentType<Integer>> SOCKETS = COMPONENTS.register("sockets",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    static {
        Materials.init();
        Armors.init();
    }

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ctccore"))
            .icon(() -> new ItemStack(STATION_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(STATION_ITEM.get());
                out.accept(CASING_ITEM.get());
                out.accept(BLANK_CHIP.get());
                out.accept(CHIP_SCRAP.get());
                for (ChipType type : ChipType.values()) out.accept(CHIP_ITEMS.get(type).get());
                for (var item : Materials.ALL) out.accept(item.get());
                for (var item : Armors.ALL) out.accept(item.get());
            }).build());

    public CtcCore(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        SERIALIZERS.register(modBus);
        Armors.MATERIALS.register(modBus);
        modBus.addListener(CtcCore::registerPayloads);
        modBus.addListener(CtcCore::registerCapabilities);
    }

    private static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DashPayload.TYPE, DashPayload.CODEC, DashPayload::handle);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, STATION_BE.get(), (be, side) -> be.energy);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STATION_BE.get(), (be, side) -> be.automation);
        // every block of a complete frame is a port: cables and hoppers on the frame reach the core
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            ResearchStationBlockEntity core = ResearchStationBlockEntity.findCore(level, pos, state);
            return core == null ? null : core.energy;
        }, CASING.get());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            ResearchStationBlockEntity core = ResearchStationBlockEntity.findCore(level, pos, state);
            return core == null ? null : core.automation;
        }, CASING.get());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
