package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/** Smithing Table: basic chip (template) + gear (base) + redstone (addition) = the gear with that chip. Needs a free slot. The same chip may go in again up to its stack limit (ChipType.cap). */
public class ChipSmithingRecipe implements SmithingRecipe {
    public static final ChipSmithingRecipe INSTANCE = new ChipSmithingRecipe();

    private static boolean basicChip(ItemStack stack) {
        return stack.getItem() instanceof ChipItem chip && chip.type.basic();
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return basicChip(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return Gear.of(stack) != Gear.NONE;
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(Items.REDSTONE);
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (!basicChip(input.template()) || !input.addition().is(Items.REDSTONE)) return false;
        ItemStack gear = input.base();
        if (Gear.of(gear) == Gear.NONE) return false;
        ChipType type = ((ChipItem) input.template().getItem()).type;
        return type.fits(gear) && ChipType.canAdd(gear, type) && ChipType.of(gear).size() < Gear.slots(gear);
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack out = input.base().copyWithCount(1);
        List<String> chips = new ArrayList<>(out.getOrDefault(CtcCore.CHIPS.get(), List.of()));
        chips.add(((ChipItem) input.template().getItem()).type.id);
        out.set(CtcCore.CHIPS.get(), chips);
        return out;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CtcCore.CHIP_SMITHING.get();
    }

    public static class Serializer implements RecipeSerializer<ChipSmithingRecipe> {
        private static final MapCodec<ChipSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, ChipSmithingRecipe> STREAM = StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<ChipSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChipSmithingRecipe> streamCodec() {
            return STREAM;
        }
    }
}
