package dev.stentik.ctccore;

import net.minecraft.core.registries.BuiltInRegistries;

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
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final Supplier<net.minecraft.world.entity.EntityType<VoidHerald>> VOID_HERALD = ENTITY_TYPES.register("void_herald",
            () -> net.minecraft.world.entity.EntityType.Builder.<VoidHerald>of(VoidHerald::new, net.minecraft.world.entity.MobCategory.MONSTER)
                    .sized(1.1f, 4.2f).fireImmune().clientTrackingRange(10).build("void_herald"));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final Supplier<ChipSmithingRecipe.Serializer> CHIP_SMITHING = SERIALIZERS.register("chip_smithing", ChipSmithingRecipe.Serializer::new);
    public static final Supplier<Voidforge.Serializer> VOIDFORGE = SERIALIZERS.register("voidforge", Voidforge.Serializer::new);

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
    /** Stops creeper explosions from breaking blocks and items in the 16 x 16 chunks around it. */
    public static final DeferredBlock<CreeperTotem> CREEPER_TOTEM = BLOCKS.register("creeper_totem",
            () -> new CreeperTotem(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(3.0f, 1200.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().lightLevel(state -> 6)));
    public static final DeferredItem<?> CREEPER_TOTEM_ITEM = ITEMS.registerSimpleBlockItem(CREEPER_TOTEM);

    public static final Supplier<BlockEntityType<ResearchStationBlockEntity>> STATION_BE = BLOCK_ENTITIES.register("research_station",
            () -> BlockEntityType.Builder.of(ResearchStationBlockEntity::new, STATION.get()).build(null));

    public static final Supplier<MenuType<StationMenu>> STATION_MENU = MENUS.register("research_station",
            () -> IMenuTypeExtension.create(StationMenu::new));

    /** Chips implanted into a piece of gear, oldest first (ids of {@link ChipType}). */
    public static final Supplier<DataComponentType<List<String>>> CHIPS = COMPONENTS.register("chips",
            () -> DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

    /** 3.0: Amplifiers installed into the gear, one "chip:tier" entry each (see {@link Chips}). */
    public static final Supplier<DataComponentType<List<String>>> AMPS = COMPONENTS.register("amps",
            () -> DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

    /** Amplifiers: tier 1 Brass, 2 Titanium, 3 Voidsteel. */
    public static final DeferredItem<AmplifierItem> BRASS_AMP = ITEMS.register("brass_amplifier", () -> new AmplifierItem(1, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<AmplifierItem> TITANIUM_AMP = ITEMS.register("titanium_amplifier", () -> new AmplifierItem(2, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<AmplifierItem> VOIDSTEEL_AMP = ITEMS.register("voidsteel_amplifier",
            () -> new AmplifierItem(3, new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC)));

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

    /** Gathering chips (Lumber, Vein, Quarry, Harvest) of this tool switched off: right-click it in the inventory. */
    public static final Supplier<DataComponentType<Boolean>> CHIPS_OFF = COMPONENTS.register("chips_off",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** VoidSteel pickaxe: which built-in gathering mode is on (0 = Vein + Quarry, 1 = Vein, 2 = Quarry). */
    public static final Supplier<DataComponentType<Integer>> TOOL_MODE = COMPONENTS.register("tool_mode",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Cataclysm armor upgraded with Voidsteel at a Smithing Table. */
    public static final Supplier<DataComponentType<Boolean>> VOIDFORGED = COMPONENTS.register("voidforged",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    static {
        Materials.init();
        Armors.init();
        Tools.init();
        Ranged.init();
    }

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ctccore"))
            .icon(() -> new ItemStack(STATION_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(STATION_ITEM.get());
                out.accept(CASING_ITEM.get());
                out.accept(CREEPER_TOTEM_ITEM.get());
                out.accept(BLANK_CHIP.get());
                out.accept(CHIP_SCRAP.get());
                out.accept(BRASS_AMP.get());
                out.accept(TITANIUM_AMP.get());
                out.accept(VOIDSTEEL_AMP.get());
                for (ChipType type : ChipType.values()) out.accept(CHIP_ITEMS.get(type).get());
                for (var item : Materials.ALL) out.accept(item.get());
                for (var item : Armors.ALL) out.accept(item.get());
                for (var item : Tools.ALL) out.accept(item.get());
            }).build());

    public CtcCore(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        SERIALIZERS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) ->
                e.put(VOID_HERALD.get(), VoidHerald.attributes().build()));
        Armors.MATERIALS.register(modBus);
        modBus.addListener(CtcCore::registerPayloads);
        modBus.addListener(CtcCore::registerCapabilities);
        modBus.addListener(CtcCore::unbreakableVoidsteel);
        if (System.getenv("CTC_CI_BOOT") != null) {
            // CI only: the server came up with the mod, recipes and mixins loaded -> say so and shut down
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> {
                try {
                    Class.forName("net.minecraft.world.inventory.AnvilMenu");
                    var recipes = e.getServer().getRecipeManager();
                    long ours = recipes.getRecipes().stream().filter(r -> r.id().getNamespace().equals(MODID)).count();
                    if (e.getServer().getLevel(CreativeWorld.DIM) == null) throw new IllegalStateException("creative dimension missing");
                    org.slf4j.LoggerFactory.getLogger(MODID).info("CTC BOOT OK: {} ctccore recipes, creative world loaded", ours);
                } catch (Throwable t) {
                    org.slf4j.LoggerFactory.getLogger(MODID).error("CTC BOOT FAILED", t);
                }
                e.getServer().halt(false);
            });
        }
    }

    /** 2.0: all VoidSteel gear (CtC Core and the KubeJS weapons) never breaks and shows no durability bar. */
    private static void unbreakableVoidsteel(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event) {
        for (var item : BuiltInRegistries.ITEM) {
            var key = BuiltInRegistries.ITEM.getKey(item);
            if ((key.getNamespace().equals(MODID) || key.getNamespace().equals("ctc")) && key.getPath().startsWith("voidsteel_")
                    && item.components().has(net.minecraft.core.component.DataComponents.MAX_DAMAGE)) {
                event.modify(item, b -> b.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,
                        new net.minecraft.world.item.component.Unbreakable(false)));
            }
        }
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
