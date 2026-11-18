package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;

/** Pickaxe, axe, shovel and hoe from every tier metal, next to the weapons and armor. */
public final class Tools {
    public static final List<DeferredItem<?>> ALL = new ArrayList<>();

    private static Ingredient repair(String id) {
        if (id.startsWith("#")) return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(id.substring(1))));
        return Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static void set(String name, TagKey<Block> incorrect, int uses, float speed, float damage, String repairId) {
        Tier tier = new SimpleTier(incorrect, uses, speed, damage, 14, () -> repair(repairId));
        ALL.add(CtcCore.ITEMS.register(name + "_pickaxe", () -> new PickaxeItem(tier, new Item.Properties().attributes(PickaxeItem.createAttributes(tier, 1.0f, -2.8f)))));
        ALL.add(CtcCore.ITEMS.register(name + "_axe", () -> new AxeItem(tier, new Item.Properties().attributes(AxeItem.createAttributes(tier, 5.0f, -3.0f)))));
        ALL.add(CtcCore.ITEMS.register(name + "_shovel", () -> new ShovelItem(tier, new Item.Properties().attributes(ShovelItem.createAttributes(tier, 1.5f, -3.0f)))));
        ALL.add(CtcCore.ITEMS.register(name + "_hoe", () -> new HoeItem(tier, new Item.Properties().attributes(HoeItem.createAttributes(tier, -3.0f, 0.0f)))));
    }

    static {
        set("steel", BlockTags.INCORRECT_FOR_IRON_TOOL, 600, 7.0f, 2.5f, "createnuclear:steel_ingot");
        set("titanium", BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1300, 8.5f, 3.0f, "#c:ingots/titanium");
        set("tungsten", BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2300, 9.5f, 4.0f, "northstar:tungsten_ingot");
        set("voidsteel", BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 11.0f, 4.5f, "ctc:voidsteel_ingot");
    }

    static void init() {}
}
