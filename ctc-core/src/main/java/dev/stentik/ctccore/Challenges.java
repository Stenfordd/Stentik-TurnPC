package dev.stentik.ctccore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Quest-book challenges that need watching (the quests use these advancements, so they complete by themselves):
 * Voidwalker (full Voidforged armor), Fully Chipped (a diamond+ weapon with 3 chips), Maxed Out (a chip at level IV),
 * Flying Base (a whole day spent standing on a ship or contraption), Iron Will (the Wither killed in iron armor).
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class Challenges {
    private Challenges() {
    }

    public static void award(ServerPlayer p, String name) {
        var adv = p.server.getAdvancements().get(CtcCore.id("challenge/" + name));
        if (adv != null && !p.getAdvancements().getOrStartProgress(adv).isDone()) p.getAdvancements().award(adv, "done");
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        // Flying Base: on the ground, but nothing of the world under the feet = a ship (Aeronautics) or a contraption
        if (p.onGround() && !p.isPassenger() && onShip(p)) {
            var data = p.getPersistentData();
            int t = data.getInt("ctcShipTicks") + 20;
            data.putInt("ctcShipTicks", t);
            if (t >= 24000) award(p, "flying_base");
        }
        if (p.tickCount % 100 != 0) return;
        boolean voidforged = true;
        for (EquipmentSlot slot : Chips.ARMOR) if (!Voidforge.is(p.getItemBySlot(slot))) voidforged = false;
        if (voidforged) award(p, "voidwalker");
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (Gear.of(s) == Gear.NONE) continue;
            if (Gear.of(s) == Gear.WEAPON && Gear.base(s) >= 2 && ChipType.of(s).size() >= 3) award(p, "fully_chipped");
            for (int lvl : Chips.levels(s).values()) if (lvl >= Chips.MAX_LEVEL) award(p, "maxed_out");
        }
    }

    private static boolean onShip(ServerPlayer p) {
        Level level = p.level();
        var box = p.getBoundingBox();
        double y = box.minY - 0.3;
        for (double x : new double[] { box.minX + 0.05, box.maxX - 0.05 })
            for (double z : new double[] { box.minZ + 0.05, box.maxZ - 0.05 }) {
                BlockPos pos = BlockPos.containing(x, y, z);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return false;
            }
        return true;
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof WitherBoss) || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        for (EquipmentSlot slot : Chips.ARMOR) {
            ItemStack s = p.getItemBySlot(slot);
            if (!(s.getItem() instanceof ArmorItem a) || !a.getMaterial().is(ArmorMaterials.IRON.unwrapKey().get())) return;
        }
        award(p, "iron_will");
    }

    static String id(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }
}
