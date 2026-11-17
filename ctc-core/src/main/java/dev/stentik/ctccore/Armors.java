package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One armor set per tier metal, next to the weapons. Each full set has its own bonus. */
public final class Armors {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, CtcCore.MODID);
    public static final List<DeferredItem<?>> ALL = new ArrayList<>();
    public static final Map<String, DeferredItem<ArmorItem>[]> SETS = new java.util.LinkedHashMap<>();

    private static Ingredient repair(String id) {
        if (id.startsWith("#")) return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(id.substring(1))));
        return Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @SuppressWarnings("unchecked")
    private static void set(String name, int helmet, int chest, int legs, int boots, float toughness, float knockback, int durability, String repairId) {
        DeferredHolder<ArmorMaterial, ArmorMaterial> material = MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.HELMET, helmet); m.put(ArmorItem.Type.CHESTPLATE, chest);
                    m.put(ArmorItem.Type.LEGGINGS, legs); m.put(ArmorItem.Type.BOOTS, boots); m.put(ArmorItem.Type.BODY, chest);
                }), 12, SoundEvents.ARMOR_EQUIP_IRON, () -> repair(repairId),
                List.of(new ArmorMaterial.Layer(CtcCore.id(name))), toughness, knockback));
        ArmorItem.Type[] types = { ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS };
        String[] suffix = { "helmet", "chestplate", "leggings", "boots" };
        DeferredItem<ArmorItem>[] items = new DeferredItem[4];
        for (int i = 0; i < 4; i++) {
            ArmorItem.Type type = types[i];
            items[i] = CtcCore.ITEMS.register(name + "_" + suffix[i],
                    () -> new ArmorItem(material, type, new Item.Properties().durability(type.getDurability(durability))));
            ALL.add(items[i]);
        }
        SETS.put(name, items);
    }

    static {
        set("steel", 2, 7, 5, 2, 0.5f, 0.0f, 22, "createnuclear:steel_ingot");
        set("titanium", 3, 8, 6, 3, 2.0f, 0.0f, 30, "#c:ingots/titanium");
        set("martian", 3, 8, 6, 3, 2.5f, 0.05f, 33, "northstar:martian_steel_ingot");
        set("tungsten", 3, 9, 7, 3, 3.0f, 0.15f, 38, "northstar:tungsten_ingot");
        set("voidsteel", 4, 9, 7, 4, 3.5f, 0.1f, 40, "ctc:voidsteel_ingot");
    }

    static void init() {}

    private static String fullSet(Player player) {
        String found = null;
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        for (var e : SETS.entrySet()) {
            boolean all = true;
            for (int i = 0; i < 4; i++) if (!player.getItemBySlot(slots[i]).is(e.getValue()[i].get())) { all = false; break; }
            if (all) found = e.getKey();
        }
        return found;
    }

    /** Called every 2 seconds from the chip tick. */
    static void setBonus(Player player) {
        String set = fullSet(player);
        if (set == null) return;
        switch (set) {
            case "steel" -> player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 0, true, false, true));
            case "titanium" -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, true, false, true));
            case "martian" -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
            case "tungsten" -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, true, false, true));
            case "voidsteel" -> player.fallDistance = 0;
            default -> {}
        }
    }

    /** "Full set: ..." line for the tooltip, or null. */
    public static String bonusKey(ItemStack stack) {
        for (var e : SETS.entrySet()) {
            for (var item : e.getValue()) if (stack.is(item.get())) return "ctccore.armor." + e.getKey() + ".bonus";
        }
        return null;
    }

    public static Holder<ArmorMaterial> material(String name) {
        return BuiltInRegistries.ARMOR_MATERIAL.getHolder(CtcCore.id(name)).orElseThrow();
    }
}
