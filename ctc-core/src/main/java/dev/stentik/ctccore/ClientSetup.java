package dev.stentik.ctccore;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ClientSetup {
    @EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent event) {
            event.register(CtcCore.STATION_MENU.get(), StationScreen::new);
        }

        @SubscribeEvent
        public static void layers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(dev.stentik.ctccore.client.VoidHeraldRenderer.LAYER, dev.stentik.ctccore.client.VoidHeraldModel::createBodyLayer);
            for (String tier : dev.stentik.ctccore.client.ArmorModels.TIERS)
                for (int layer = 1; layer <= 2; layer++) {
                    final int l = layer;
                    event.registerLayerDefinition(dev.stentik.ctccore.client.ArmorModels.layer(tier, l), () -> dev.stentik.ctccore.client.ArmorModels.create(tier, l));
                }
        }

        @SubscribeEvent
        public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(CtcCore.VOID_HERALD.get(), dev.stentik.ctccore.client.VoidHeraldRenderer::new);
        }

        private static final java.util.Map<String, net.minecraft.client.model.HumanoidModel<?>> ARMOR_MODELS = new java.util.HashMap<>();

        /** 2.0: steel, titanium, tungsten and VoidSteel armor render with their 3D parts (client/ArmorModels). */
        @SubscribeEvent
        public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
            for (String tier : dev.stentik.ctccore.client.ArmorModels.TIERS) {
                var items = Armors.SETS.get(tier);
                if (items == null) continue;
                event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                    @Override
                    public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity entity,
                            net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot,
                            net.minecraft.client.model.HumanoidModel<?> original) {
                        int layer = slot == net.minecraft.world.entity.EquipmentSlot.LEGS ? 2 : 1;
                        return ARMOR_MODELS.computeIfAbsent(tier + layer, k -> new net.minecraft.client.model.HumanoidModel<>(
                                net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(dev.stentik.ctccore.client.ArmorModels.layer(tier, layer))));
                    }
                }, items[0].get(), items[1].get(), items[2].get(), items[3].get());
            }
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                for (var bow : new net.minecraft.world.item.Item[] { Ranged.TITANIUM_BOW.get(), Ranged.VOIDSTEEL_BOW.get() }) {
                    net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),
                            (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0.0f
                                    : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0f
                                            * (bow == Ranged.TITANIUM_BOW.get() ? Ranged.TitaniumBow.SPEED : 1.0f));
                    net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),
                            (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
                }
            });
            // Ponder comes with Create; without it the pack still works, just without the scenes
            if (ModList.get().isLoaded("ponder")) event.enqueueWork(dev.stentik.ctccore.ponder.CtcPonderPlugin::register);
        }
    }

    @EventBusSubscriber(modid = CtcCore.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameBus {
        /** Gear shows its chips and what they do in it. */
        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
            List<ChipType> chips = ChipType.of(event.getItemStack());
            String path = VoidsteelGear.path(event.getItemStack());
            if (path.startsWith("cracked_voidsteel_")) {
                event.getToolTip().add(Component.translatable("ctccore.cracked.tip").withStyle(ChatFormatting.GRAY));
                event.getToolTip().add(Component.translatable("ctccore.cracked.needs." + path.substring(18)).withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (VoidsteelGear.hasBuiltins(event.getItemStack())) {
                event.getToolTip().add(Component.translatable("ctccore.builtin." + path).withStyle(ChatFormatting.LIGHT_PURPLE));
                event.getToolTip().add(VoidsteelGear.modeLine(event.getItemStack()));
            }
            Gear gear = Gear.of(event.getItemStack());
            if (gear == Gear.NONE) return;
            List<Component> lines = event.getToolTip();
            Voidforge.tooltip(event.getItemStack(), lines);
            boolean gathering = false;
            for (ChipType t : ChipType.of(event.getItemStack())) if (t.gathering()) gathering = true;
            if (gathering) lines.add(GatherEvents.off(event.getItemStack())
                    ? Component.translatable("ctccore.chips.off").withStyle(ChatFormatting.RED)
                    : Component.translatable("ctccore.chips.on").withStyle(ChatFormatting.GREEN));
            String bonus = Armors.bonusKey(event.getItemStack());
            if (bonus != null) lines.add(Component.translatable("ctccore.armor.set", Component.translatable(bonus)).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("ctccore.chips", chips.size(), Gear.slots(event.getItemStack())).withStyle(ChatFormatting.GRAY));
            if (VoidRapier.is(event.getItemStack())) lines.add(VoidRapier.info(event.getItemStack()).withStyle(ChatFormatting.LIGHT_PURPLE));
            java.util.Map<ChipType, Integer> raw = new java.util.EnumMap<>(ChipType.class);
            for (ChipType c : chips) raw.merge(c, 1, Integer::sum);
            for (var en : raw.entrySet()) {
                ChipType chip = en.getKey();
                int n = en.getValue();
                String mult = chip.cap() > 1 ? " ×" + n + "/" + chip.cap() : "";
                lines.add(Component.literal(" ◆ ").withStyle(s -> s.withColor(chip.color))
                        .append(Component.translatable("item.ctccore." + chip.id + "_chip").append(mult).withStyle(s -> s.withColor(chip.color)))
                        .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable("ctccore.chip." + chip.id + "." + gear.name().toLowerCase()).withStyle(ChatFormatting.GRAY)));
            }
        }
    }
}
