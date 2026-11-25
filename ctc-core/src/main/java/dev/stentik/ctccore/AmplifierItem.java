package dev.stentik.ctccore;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Raises one chip in a piece of gear by a level (Research Station). Tier 1 up to level 2, tier 2 up to 3, tier 3 up to 4. */
public class AmplifierItem extends Item {
    public final int tier;

    public AmplifierItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("ctccore.amp.tip", Chips.ROMAN[tier + 1]).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("ctccore.amp.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}
