package dev.stentik.ctccore.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** JEI tab "Where to find": place, height bar, what you need, in plain words. */
public class WhereCategory implements IRecipeCategory<WhereEntry> {
    public static final RecipeType<WhereEntry> TYPE = RecipeType.create("ctccore", "where_to_find", WhereEntry.class);
    private static final int W = 170, H = 92, BOTTOM = -64, TOP = 320;
    private final IDrawable icon;

    public WhereCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(Items.FILLED_MAP));
    }

    @Override public RecipeType<WhereEntry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Where to find"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return W; }
    @Override public int getHeight() { return H; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WhereEntry entry, IFocusGroup focuses) {
        int x = 0;
        for (ItemStack stack : entry.items()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, x + 1, 1).addItemStack(stack);
            x += 19;
        }
    }

    private static int placeColor(String place) {
        return switch (place) {
            case "nether" -> 0xFF6A4A;
            case "end" -> 0xC08CFF;
            case "moon" -> 0xC8D4E8;
            case "mars" -> 0xFF8A4A;
            case "venus" -> 0xFFD84A;
            case "mercury" -> 0x9FB4D0;
            default -> 0x7CD67C;
        };
    }

    private static String placeName(String place) {
        return switch (place) {
            case "nether" -> "Nether";
            case "end" -> "The End";
            case "moon" -> "Moon";
            case "mars" -> "Mars";
            case "venus" -> "Venus";
            case "mercury" -> "Mercury";
            default -> "Overworld";
        };
    }

    private int lines(GuiGraphics g, Font font, String text, int y, int color) {
        List<FormattedCharSequence> parts = font.split(Component.literal(text), W - 2);
        for (FormattedCharSequence line : parts) {
            g.drawString(font, line, 1, y, color, false);
            y += 9;
        }
        return y;
    }

    @Override
    public void draw(WhereEntry e, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int y = 21;
        String head = e.deposit() ? "Deposit · drill it, do not mine it" : placeName(e.place());
        g.drawString(font, head, 1, y, e.deposit() ? 0xFFC84A : placeColor(e.place()), false);
        y += 10;
        y = lines(g, font, e.where(), y, 0x404040);
        if (e.ymin() != null && e.ymax() != null) {
            // height bar: whole world -64..320, the ore's range filled, the best height marked
            int bx = 1, bw = W - 2, by = y + 1;
            g.fill(bx, by, bx + bw, by + 6, 0xFF2A2F3A);
            int a = bx + (e.ymin() - BOTTOM) * bw / (TOP - BOTTOM), b = bx + (e.ymax() - BOTTOM) * bw / (TOP - BOTTOM);
            g.fill(a, by + 1, Math.max(a + 1, b), by + 5, 0xFF4FB3E8);
            if (e.best() != null) {
                int m = bx + (e.best() - BOTTOM) * bw / (TOP - BOTTOM);
                g.fill(m - 1, by - 1, m + 1, by + 7, 0xFFFFD84A);
            }
            int sea = bx + (63 - BOTTOM) * bw / (TOP - BOTTOM);
            g.fill(sea, by + 6, sea + 1, by + 8, 0xFF3060C0);
            y += 10;
            String range = "Y " + e.ymin() + " to " + e.ymax() + (e.best() != null ? "   most at Y " + e.best() : "   evenly");
            g.drawString(font, range, 1, y, 0x404040, false);
            y += 10;
        }
        y = lines(g, font, "Need: " + e.how(), y, 0x404040);
        if (e.note() != null) lines(g, font, e.note(), y, 0x806020);
    }
}
