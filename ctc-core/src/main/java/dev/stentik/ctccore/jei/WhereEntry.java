package dev.stentik.ctccore.jei;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One line of where_to_find.json: an ore or a deposit, where it is, how deep and what you need. */
public record WhereEntry(List<ItemStack> items, String place, String where, Integer ymin, Integer ymax, Integer best,
        String how, String note, boolean deposit) {

    private static Integer num(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsInt();
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    public static List<WhereEntry> load() {
        List<WhereEntry> out = new ArrayList<>();
        try (var in = WhereEntry.class.getResourceAsStream("/assets/ctccore/where_to_find.json")) {
            if (in == null) return out;
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                List<ItemStack> stacks = new ArrayList<>();
                for (JsonElement id : o.getAsJsonArray("items")) {
                    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.getAsString()));
                    if (item != Items.AIR) stacks.add(new ItemStack(item));
                }
                if (stacks.isEmpty()) continue;      // that mod is not installed
                out.add(new WhereEntry(stacks, str(o, "place"), str(o, "where"), num(o, "ymin"), num(o, "ymax"), num(o, "best"),
                        str(o, "how"), str(o, "note"), o.get("deposit").getAsBoolean()));
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("ctccore").warn("where_to_find.json could not be read", e);
        }
        return out;
    }
}
