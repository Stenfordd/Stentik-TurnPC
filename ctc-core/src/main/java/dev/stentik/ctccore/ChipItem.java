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
        for (Gear gear : new Gear[] { Gear.WEAPON, Gear.ARMOR, Gear.TOOL }) {
            lines.add(Component.translatable("ctccore.gear." + gear.name().toLowerCase()).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(": "))
                    .append(Component.translatable("ctccore.chip." + type.id + "." + gear.name().toLowerCase())
                            .withStyle(s -> s.withColor(type.color))));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type.boss;
    }
}
