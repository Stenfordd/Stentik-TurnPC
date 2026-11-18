package dev.stentik.ctccore;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Voidforging: Cataclysm armor + Voidforge Template + Voidsteel Ingot at a Smithing Table.
 * The armor keeps its own model and gets +2 armor, +2 toughness and one more chip slot. A full set trails void particles.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class Voidforge implements SmithingRecipe {
    public static final Voidforge INSTANCE = new Voidforge();
    private static final ResourceLocation VOIDSTEEL = ResourceLocation.parse("ctc:voidsteel_ingot");

    public static boolean is(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(CtcCore.VOIDFORGED.get()));
    }

    /** Any Cataclysm armor piece except the one with wings. */
    public static boolean forgeable(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem)) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("cataclysm") && !id.getPath().contains("elytra");
    }

    @Override public boolean isTemplateIngredient(ItemStack stack) { return stack.is(Materials.VOIDFORGE_TEMPLATE.get()); }
    @Override public boolean isBaseIngredient(ItemStack stack) { return forgeable(stack) && !is(stack); }
    @Override public boolean isAdditionIngredient(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(VOIDSTEEL); }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return isTemplateIngredient(input.template()) && isBaseIngredient(input.base()) && isAdditionIngredient(input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack out = input.base().copyWithCount(1);
        out.set(CtcCore.VOIDFORGED.get(), true);
        return out;
    }

    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return CtcCore.VOIDFORGE.get(); }

    public static class Serializer implements RecipeSerializer<Voidforge> {
        private static final MapCodec<Voidforge> CODEC = MapCodec.unit(INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, Voidforge> STREAM = StreamCodec.unit(INSTANCE);
        @Override public MapCodec<Voidforge> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, Voidforge> streamCodec() { return STREAM; }
    }

    @SubscribeEvent
    public static void attributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!is(stack) || !(stack.getItem() instanceof ArmorItem armor)) return;
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(armor.getEquipmentSlot());
        String slot = armor.getEquipmentSlot().getName();
        event.addModifier(Attributes.ARMOR, new AttributeModifier(CtcCore.id("voidforged_armor_" + slot), 2.0,
                AttributeModifier.Operation.ADD_VALUE), group);
        event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(CtcCore.id("voidforged_toughness_" + slot), 2.0,
                AttributeModifier.Operation.ADD_VALUE), group);
    }

    @SubscribeEvent
    public static void particles(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.tickCount % 6 != 0 || !(player.level() instanceof ServerLevel level)) return;
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            if (!is(player.getItemBySlot(slot))) return;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.35, 0.6, 0.35, 0.01);
        if (player.tickCount % 24 == 0)
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.3, 0.05, 0.3, 0.0);
    }

    /** Client tooltip: purple "Voidforged" name and what it adds. */
    public static void tooltip(ItemStack stack, java.util.List<Component> lines) {
        if (!is(stack) || lines.isEmpty()) return;
        lines.set(0, Component.literal("Voidforged ").append(lines.get(0).copy()).withStyle(s -> s.withColor(0xB77CFF)));
        lines.add(1, Component.literal("+2 armor, +2 toughness, +1 chip slot").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(2, Component.literal("Full set: trails of the Void").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
