package dev.stentik.ctccore;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ChipItem extends Item {
    public final ChipType type;

    public ChipItem(ChipType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(type.basic() ? "ctccore.chip.hint_basic" : "ctccore.chip.hint").withStyle(ChatFormatting.DARK_GRAY));
        if (type == ChipType.LUMBER || type == ChipType.VEIN || type == ChipType.HARVEST) {
            String g = type == ChipType.LUMBER ? "weapon" : "tool";
            lines.add(Component.translatable("ctccore.chip." + type.id + "." + g).withStyle(s -> s.withColor(type.color)));
            return;
        }
        String[] parts = type.leveled() ? new String[] { "weapon", "helmet", "chest", "legs", "boots", "tool" } : new String[] { "weapon", "armor", "tool" };
        for (String part : parts) {
            lines.add(Component.translatable("ctccore.gear." + part).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(": "))
                    .append(Component.translatable("ctccore.chip." + type.id + "." + part).withStyle(s -> s.withColor(type.color))));
        }
        if (type.leveled()) lines.add(Component.translatable("ctccore.chip.stack_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type.boss;
    }
}
