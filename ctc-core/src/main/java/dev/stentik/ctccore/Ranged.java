package dev.stentik.ctccore;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bows and special arrows.
 * Titanium Bow: draws 1.6x faster, and while held fully drawn it keeps firing (auto-fire, like the Meka-Bow).
 * VoidSteel Bow: fires a burst of 3 arrows; plain arrows are never used up (Infinity), special arrows are.
 * Diamond Tipped Arrow: +50% damage. Void Tipped Arrow: +25% damage and ricochets to up to two more enemies nearby.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class Ranged {
    private Ranged() {}

    public static final String TAG = "ctc_arrow", BOUNCES = "ctc_bounces";

    public static final DeferredItem<TitaniumBow> TITANIUM_BOW = CtcCore.ITEMS.register("titanium_bow",
            () -> new TitaniumBow(new Item.Properties().durability(1200)));
    public static final DeferredItem<VoidsteelBow> VOIDSTEEL_BOW = CtcCore.ITEMS.register("voidsteel_bow",
            () -> new VoidsteelBow(new Item.Properties().durability(2400).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<SpecialArrow> DIAMOND_ARROW = CtcCore.ITEMS.register("diamond_tipped_arrow",
            () -> new SpecialArrow("diamond", new Item.Properties()));
    public static final DeferredItem<SpecialArrow> VOID_ARROW = CtcCore.ITEMS.register("void_tipped_arrow",
            () -> new SpecialArrow("void", new Item.Properties()));

    static {
        Materials.ALL.add(TITANIUM_BOW);
        Materials.ALL.add(VOIDSTEEL_BOW);
        Materials.ALL.add(DIAMOND_ARROW);
        Materials.ALL.add(VOID_ARROW);
    }

    static void init() {}

    public static class SpecialArrow extends ArrowItem {
        final String kind;

        public SpecialArrow(String kind, Properties props) {
            super(props);
            this.kind = kind;
        }

        @Override
        public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
            Arrow arrow = new Arrow(level, shooter, ammo.copyWithCount(1), weapon);
            arrow.getPersistentData().putString(TAG, kind);
            return arrow;
        }

        @Override
        public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
            Arrow arrow = new Arrow(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
            arrow.getPersistentData().putString(TAG, kind);
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;
            return arrow;
        }
    }

    /** Draw time scaled down: the bow reports more charge than real ticks. */
    public static class TitaniumBow extends BowItem {
        public static final float SPEED = 1.6f;

        public TitaniumBow(Properties props) {
            super(props);
        }

        @Override
        public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
            int used = getUseDuration(stack, entity) - timeLeft;
            if (used >= 0 && used < 2) return;
            int scaled = Math.min(getUseDuration(stack, entity), (int) (used * SPEED));
            super.releaseUsing(stack, level, entity, getUseDuration(stack, entity) - scaled);
        }

        /** Auto-fire: once fully drawn, keep shooting every 12 ticks while the button is held. */
        @Override
        public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
            int used = getUseDuration(stack, entity) - remaining;
            if (level.isClientSide || used < 13 || used % 12 != 1) return;
            if (!(entity instanceof Player player) || player.getProjectile(stack).isEmpty()) return;
            super.releaseUsing(stack, level, entity, getUseDuration(stack, entity) - 20);
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }
    }

    public static class VoidsteelBow extends BowItem {
        public VoidsteelBow(Properties props) {
            super(props);
        }

        @Override
        public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
            if (!(entity instanceof Player player)) return;
            ItemStack ammo = player.getProjectile(stack);
            if (ammo.isEmpty()) return;
            int used = getUseDuration(stack, entity) - timeLeft;
            float power = getPowerForTime(used);
            if (power < 0.1f) return;
            if (level instanceof ServerLevel server) {
                boolean plain = ammo.is(Items.ARROW) || player.getAbilities().instabuild;
                for (int i = 0; i < 3; i++) {
                    ItemStack one = ammo.copyWithCount(1);
                    ArrowItem item = one.getItem() instanceof ArrowItem a ? a : (ArrowItem) Items.ARROW;
                    AbstractArrow arrow = item.createArrow(server, one, player, stack);
                    arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + (i - 1) * 2.5f, 0.0f, power * 3.0f, 0.6f);
                    if (power >= 1.0f) arrow.setCritArrow(true);
                    arrow.pickup = plain || i > 0 ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
                    server.addFreshEntity(arrow);
                }
                if (!plain) ammo.shrink(1);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f,
                    1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + power * 0.5f);
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }
    }

    // ---------- special arrow effects ----------

    @SubscribeEvent
    public static void hit(LivingIncomingDamageEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof AbstractArrow arrow)) return;
        String kind = arrow.getPersistentData().getString(TAG);
        if (kind.isEmpty()) return;
        LivingEntity target = event.getEntity();
        if (kind.equals("diamond")) {
            event.setAmount(event.getAmount() * 1.5f);
        } else if (kind.equals("void")) {
            event.setAmount(event.getAmount() * 1.25f);
            int bounces = arrow.getPersistentData().getInt(BOUNCES);
            if (bounces < 2 && target.level() instanceof ServerLevel level) ricochet(level, arrow, target, bounces + 1);
        }
    }

    /** A new void arrow flies from the struck enemy to the nearest other enemy within 8 blocks. */
    private static void ricochet(ServerLevel level, AbstractArrow from, LivingEntity hit, int bounces) {
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, hit.getBoundingBox().inflate(8.0),
                e -> e != hit && e.isAlive() && e instanceof Enemy && e != from.getOwner());
        LivingEntity next = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : near) {
            double d = e.distanceToSqr(hit);
            if (d < best) {
                best = d;
                next = e;
            }
        }
        if (next == null) return;
        Arrow arrow = new Arrow(level, hit.getX(), hit.getEyeY(), hit.getZ(), new ItemStack(VOID_ARROW.get()), null);
        if (from.getOwner() != null) arrow.setOwner(from.getOwner());
        arrow.getPersistentData().putString(TAG, "void");
        arrow.getPersistentData().putInt(BOUNCES, bounces);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(from.getBaseDamage() * 0.75);
        double dx = next.getX() - hit.getX(), dy = next.getY(0.5) - hit.getEyeY(), dz = next.getZ() - hit.getZ();
        arrow.setPos(hit.getX() + dx * 0.15, hit.getEyeY() + dy * 0.15, hit.getZ() + dz * 0.15);
        arrow.shoot(dx, dy, dz, 2.2f, 0.0f);
        level.addFreshEntity(arrow);
        level.playSound(null, hit.getX(), hit.getY(), hit.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    static InteractionHand hand(LivingEntity e) {
        return e.getUsedItemHand();
    }
}
