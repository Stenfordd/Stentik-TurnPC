package dev.stentik.ctccore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Creative world: a flat dimension (ctccore:creative) for trying builds out.
 * Rule that everything hangs on: a player inside it never has their survival things, a player outside never has
 * creative things. On the way in the whole survival state (inventory, armor, offhand, Curios, XP, health, food,
 * effects, game mode, position) is stored on the server and the player is emptied; on the way out (by any route:
 * command, death, another mod's teleport) it is put back and the creative things are kept for the next visit.
 * Nothing else crosses: entities can't travel in or out, ender chests and beds don't work there, and quests and
 * advancements don't count there (mixins in dev.stentik.ctccore.mixin.creative).
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CreativeWorld {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, CtcCore.id("creative"));
    private static final double SPAWN_Y = -60; // flat layers: bedrock, 2 dirt, grass from y -64

    private CreativeWorld() {}

    public static boolean isIn(Entity e) {
        return e != null && e.level().dimension() == DIM;
    }

    // ---------------------------------------------------------------- commands

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("ctc")
                .then(Commands.literal("creative").executes(ctx -> enter(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("survival").executes(ctx -> leave(ctx.getSource().getPlayerOrException()))));
    }

    private static int enter(ServerPlayer p) {
        if (isIn(p)) { p.sendSystemMessage(Component.literal("§eYou are already in the creative world. Back: /ctc survival")); return 0; }
        String why = cantPack(p);
        if (why != null) { p.sendSystemMessage(Component.literal("§c" + why)); return 0; }
        ServerLevel dim = p.server.getLevel(DIM);
        if (dim == null) { p.sendSystemMessage(Component.literal("§cThe creative world did not load.")); return 0; }
        p.stopRiding();
        if (!snapshot(p)) return 0;
        CompoundTag at = store(p.server).creativePos.get(p.getUUID());
        double x = 0.5, y = SPAWN_Y, z = 0.5; float yr = 0, xr = 0;
        if (at != null) { x = at.getDouble("x"); y = at.getDouble("y"); z = at.getDouble("z"); yr = at.getFloat("yr"); xr = at.getFloat("xr"); }
        p.teleportTo(dim, x, y, z, yr, xr);
        p.sendSystemMessage(Component.literal("§aCreative world. Your survival things are stored safely. Back: /ctc survival"));
        p.sendSystemMessage(Component.literal("§7Save a build with the Schematic and Quill, then build it in survival with a Schematicannon."));
        return 1;
    }

    private static int leave(ServerPlayer p) {
        if (!isIn(p)) { p.sendSystemMessage(Component.literal("§eYou are not in the creative world.")); return 0; }
        if (carrying(p)) { p.sendSystemMessage(Component.literal("§cPut down what you are carrying first (Carry On).")); return 0; }
        Store s = store(p.server);
        CompoundTag snap = s.snaps.get(p.getUUID());
        CompoundTag here = new CompoundTag();
        here.putDouble("x", p.getX()); here.putDouble("y", p.getY()); here.putDouble("z", p.getZ());
        here.putFloat("yr", p.getYRot()); here.putFloat("xr", p.getXRot());
        s.creativePos.put(p.getUUID(), here);
        s.setDirty();
        ServerLevel back = null;
        double x = 0, y = 0, z = 0; float yr = 0, xr = 0;
        if (snap != null && !snap.getString("dim").isEmpty()) {
            back =p.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(snap.getString("dim"))));
            x = snap.getDouble("x"); y = snap.getDouble("y"); z = snap.getDouble("z"); yr = snap.getFloat("yr"); xr = snap.getFloat("xr");
        }
        if (back == null) {
            back = p.server.overworld();
            var sp = back.getSharedSpawnPos();
            x = sp.getX() + 0.5; y = sp.getY(); z = sp.getZ() + 0.5;
        }
        p.stopRiding();
        restore(p); // before the trip, so nothing creative ever arrives in survival
        p.teleportTo(back, x, y, z, yr, xr);
        p.sendSystemMessage(Component.literal("§aBack in survival, your things are back."));
        return 1;
    }

    // ---------------------------------------------------------------- the rule, kept by events

    /** Before any trip: entities never cross, players are packed or unpacked. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void travel(EntityTravelToDimensionEvent event) {
        if (event.isCanceled()) return;
        Entity e = event.getEntity();
        boolean from = isIn(e), to = event.getDimension() == DIM;
        if (from == to) return;
        if (!(e instanceof ServerPlayer p)) { event.setCanceled(true); return; }
        if (to && !has(p)) {
            String why = cantPack(p);
            if (why != null) { p.sendSystemMessage(Component.literal("§c" + why)); event.setCanceled(true); return; }
            if (!snapshot(p)) event.setCanceled(true);
        } else if (from) {
            if (carrying(p)) { p.sendSystemMessage(Component.literal("§cPut down what you are carrying first (Carry On).")); event.setCanceled(true); return; }
            restore(p);
        }
    }

    @SubscribeEvent
    public static void changed(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) enforce(p);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) enforce(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) enforce(p);
    }

    /** Safety net for any route the events above missed. */
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) enforce(p);
    }

    private static void enforce(ServerPlayer p) {
        boolean in = isIn(p), has = has(p);
        if (in && !has) {
            // got here some other way (e.g. a teleport command): pack whatever they have, even if it must be forced
            if (carrying(p)) dropCarry(p);
            snapshot(p);
        } else if (!in && has) {
            if (carrying(p)) dropCarry(p);
            restore(p);
        }
        if (in && !p.isCreative() && !p.isSpectator()) p.setGameMode(GameType.CREATIVE);
    }

    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!isIn(event.getEntity())) return;
        Block b = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (b instanceof EnderChestBlock || b instanceof BedBlock || b instanceof RespawnAnchorBlock) {
            event.setCanceled(true);
            if (!event.getLevel().isClientSide) event.getEntity().sendSystemMessage(Component.literal("§cThat does not work in the creative world."));
        }
    }

    // ---------------------------------------------------------------- packing

    private static String cantPack(ServerPlayer p) {
        if (carrying(p)) return "Put down what you are carrying first (Carry On).";
        if (curios(p) == null)
            return "Curios slots can't be stored, so the creative world is locked for safety. Tell David.";
        if (p.isSleeping()) return "Get out of bed first.";
        return null;
    }

    /** Puts the cursor item and the 2x2 crafting grid back into the inventory, so they are packed (or wiped) too. */
    private static void tidy(ServerPlayer p) {
        p.closeContainer();
        ItemStack carried = p.inventoryMenu.getCarried();
        if (!carried.isEmpty()) { p.inventoryMenu.setCarried(ItemStack.EMPTY); p.getInventory().placeItemBackInInventory(carried); }
        CraftingContainer grid = p.inventoryMenu.getCraftSlots();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.removeItemNoUpdate(i);
            if (!s.isEmpty()) p.getInventory().placeItemBackInInventory(s);
        }
    }

    private static boolean snapshot(ServerPlayer p) {
        HolderLookup.Provider reg = p.registryAccess();
        tidy(p);
        CompoundTag t = new CompoundTag();
        t.put("inv", p.getInventory().save(new ListTag()));
        Map<String, IItemHandlerModifiable> cur = curios(p);
        if (cur == null) return false;
        ListTag curios = new ListTag();
        cur.forEach((key, h) -> {
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack st = h.getStackInSlot(i);
                if (st.isEmpty()) continue;
                CompoundTag e = (CompoundTag) st.save(reg, new CompoundTag());
                e.putString("Key", key);
                e.putInt("Slot", i);
                curios.add(e);
            }
        });
        t.put("curios", curios);
        ListTag fx = new ListTag();
        for (MobEffectInstance m : p.getActiveEffects()) fx.add(m.save());
        t.put("fx", fx);
        t.putInt("lvl", p.experienceLevel);
        t.putFloat("xpp", p.experienceProgress);
        t.putInt("xpt", p.totalExperience);
        t.putFloat("hp", p.getHealth());
        t.putInt("food", p.getFoodData().getFoodLevel());
        t.putFloat("sat", p.getFoodData().getSaturationLevel());
        t.putInt("mode", p.gameMode.getGameModeForPlayer().getId());
        boolean inside = isIn(p);
        // forced pack inside the creative world: no position from before, the way out goes to world spawn
        t.putString("dim", inside ? "" : p.level().dimension().location().toString());
        t.putDouble("x", p.getX()); t.putDouble("y", p.getY()); t.putDouble("z", p.getZ());
        t.putFloat("yr", p.getYRot()); t.putFloat("xr", p.getXRot());

        Store s = store(p.server);
        s.snaps.put(p.getUUID(), t);
        s.setDirty();
        s.saveNow(p.server); // written to disk before anything is taken away

        // empty the survival state, then hand back what they had in creative last time
        p.getInventory().clearContent();
        clearCurios(cur);
        p.removeAllEffects();
        CompoundTag kept = s.creativeInv.get(p.getUUID());
        if (kept != null) p.getInventory().load(kept.getList("inv", Tag.TAG_COMPOUND));
        p.setGameMode(GameType.CREATIVE);
        p.inventoryMenu.broadcastChanges();
        return true;
    }

    private static void restore(ServerPlayer p) {
        Store s = store(p.server);
        CompoundTag t = s.snaps.get(p.getUUID());
        if (t == null) return;
        HolderLookup.Provider reg = p.registryAccess();
        tidy(p);
        // keep the creative inventory for the next visit (it never leaves the server data)
        CompoundTag kept = new CompoundTag();
        kept.put("inv", p.getInventory().save(new ListTag()));
        s.creativeInv.put(p.getUUID(), kept);

        p.getInventory().load(t.getList("inv", Tag.TAG_COMPOUND)); // load() clears first
        Map<String, IItemHandlerModifiable> cur = curios(p);
        if (cur != null) clearCurios(cur); // creative curios are dropped, like the rest of the creative gear outside the inventory
        for (Tag raw : t.getList("curios", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) raw;
            ItemStack st = ItemStack.parseOptional(reg, e);
            int slot = e.getInt("Slot");
            IItemHandlerModifiable h = cur == null ? null : cur.get(e.getString("Key"));
            if (h != null && slot < h.getSlots() && h.getStackInSlot(slot).isEmpty()) h.setStackInSlot(slot, st);
            else p.getInventory().placeItemBackInInventory(st); // slot gone: never lose the item
        }
        p.removeAllEffects();
        for (Tag raw : t.getList("fx", Tag.TAG_COMPOUND)) {
            MobEffectInstance m = MobEffectInstance.load((CompoundTag) raw);
            if (m != null) p.addEffect(m);
        }
        p.experienceProgress = t.getFloat("xpp");
        p.totalExperience = t.getInt("xpt");
        p.setExperienceLevels(t.getInt("lvl"));
        p.setHealth(Math.max(1f, t.getFloat("hp")));
        p.getFoodData().setFoodLevel(t.getInt("food"));
        p.getFoodData().setSaturation(t.getFloat("sat"));
        GameType mode = GameType.byId(t.getInt("mode"));
        p.setGameMode(mode == GameType.CREATIVE ? GameType.SURVIVAL : mode);
        p.inventoryMenu.broadcastChanges();

        s.snaps.remove(p.getUUID());
        s.setDirty();
        s.saveNow(p.server);
    }

    /**
     * Every Curios slot (equipped and cosmetic) by "slotType/stacks|cosmetic", through the Curios API looked up by
     * name (no compile dependency). Empty map without Curios or without slots; null when it can't be read safely.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, IItemHandlerModifiable> curios(ServerPlayer p) {
        Map<String, IItemHandlerModifiable> out = new java.util.TreeMap<>();
        if (!ModList.get().isLoaded("curios")) return out;
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            Class<?> stacks = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            EntityCapability cap = EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "inventory"), api);
            Object inv = p.getCapability(cap);
            if (inv == null) return out;
            Map<String, ?> all = (Map<String, ?>) api.getMethod("getCurios").invoke(inv);
            for (var en : all.entrySet()) {
                for (String part : new String[]{"getStacks", "getCosmeticStacks"}) {
                    Object h = stacks.getMethod(part).invoke(en.getValue());
                    if (!(h instanceof IItemHandlerModifiable m)) return null;
                    out.put(en.getKey() + "/" + part, m);
                }
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void clearCurios(Map<String, IItemHandlerModifiable> cur) {
        for (IItemHandlerModifiable h : cur.values()) for (int i = 0; i < h.getSlots(); i++) h.setStackInSlot(i, ItemStack.EMPTY);
    }

    private static boolean has(ServerPlayer p) {
        return store(p.server).snaps.containsKey(p.getUUID());
    }

    // ---------------------------------------------------------------- Carry On (looked up by name, optional)

    private static Object carryData(Player p) throws ReflectiveOperationException {
        Class<?> m = Class.forName("tschipp.carryon.common.carry.CarryOnDataManager");
        return m.getMethod("getCarryData", Player.class).invoke(null, p);
    }

    private static boolean carrying(Player p) {
        if (!ModList.get().isLoaded("carryon")) return false;
        try {
            Object d = carryData(p);
            return d != null && (boolean) d.getClass().getMethod("isCarrying").invoke(d);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Only for the forced cases: whatever is carried across is dropped (it stays where it was picked up from). */
    private static void dropCarry(Player p) {
        try {
            Object d = carryData(p);
            d.getClass().getMethod("clear").invoke(d);
            Class.forName("tschipp.carryon.common.carry.CarryOnDataManager")
                    .getMethod("setCarryData", Player.class, d.getClass()).invoke(null, p, d);
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- storage

    private static Store store(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Store::new, Store::load, null), "ctccore_creative");
    }

    public static final class Store extends SavedData {
        final Map<UUID, CompoundTag> snaps = new HashMap<>();
        final Map<UUID, CompoundTag> creativeInv = new HashMap<>();
        final Map<UUID, CompoundTag> creativePos = new HashMap<>();

        static Store load(CompoundTag tag, HolderLookup.Provider reg) {
            Store s = new Store();
            read(tag.getCompound("snaps"), s.snaps);
            read(tag.getCompound("cinv"), s.creativeInv);
            read(tag.getCompound("cpos"), s.creativePos);
            return s;
        }

        private static void read(CompoundTag from, Map<UUID, CompoundTag> into) {
            for (String k : from.getAllKeys()) {
                try { into.put(UUID.fromString(k), from.getCompound(k)); } catch (IllegalArgumentException ignored) {}
            }
        }

        private static CompoundTag write(Map<UUID, CompoundTag> from) {
            CompoundTag t = new CompoundTag();
            from.forEach((k, v) -> t.put(k.toString(), v));
            return t;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
            tag.put("snaps", write(snaps));
            tag.put("cinv", write(creativeInv));
            tag.put("cpos", write(creativePos));
            return tag;
        }

        void saveNow(MinecraftServer server) {
            try { server.overworld().getDataStorage().save(); } catch (Throwable ignored) {}
        }
    }
}
