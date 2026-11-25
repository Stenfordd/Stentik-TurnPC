package dev.stentik.ctccore;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** What the chips actually do. Weapon chips act on hit, armor chips when hurt and while worn, tool chips while held. */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ChipEvents {
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    /** Bosses drop their chip on every kill. */
    private static final Map<String, ChipType> BOSS_CHIPS = Map.of(
            "cataclysm:the_harbinger", ChipType.HARBINGER,
            "cataclysm:ignis", ChipType.IGNIS,
            "cataclysm:the_leviathan", ChipType.LEVIATHAN,
            "cataclysm:ancient_remnant", ChipType.REMNANT,
            "cataclysm:maledictus", ChipType.MALEDICTUS,
            "cataclysm:ender_guardian", ChipType.GUARDIAN);

    /** Chips in all worn armor pieces (any level). */
    static Set<ChipType> armorChips(LivingEntity entity) {
        Set<ChipType> out = EnumSet.noneOf(ChipType.class);
        for (EquipmentSlot slot : ARMOR) out.addAll(ChipType.of(Chips.piece(entity, slot)));
        return out;
    }

    static void effect(LivingEntity entity, Holder<MobEffect> effect, int ticks, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false, true));
    }

    static void freeze(LivingEntity entity) {
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), 300));
        effect(entity, MobEffects.MOVEMENT_SLOWDOWN, 50, 3);
    }

    /** What weapon chips do on a hit; also used for elite mobs and bosses. Returns the new damage. */
    public static float hit(java.util.Map<ChipType, Integer> counts, LivingEntity attacker, LivingEntity target, float amount) {
        for (var e : counts.entrySet()) {
            int n = e.getValue();
            switch (e.getKey()) {
                case LUNAR -> effect(target, MobEffects.LEVITATION, 30 * n, 1);
                case MARTIAN -> { target.igniteForSeconds(5); amount *= 1 + 0.10f * n; }
                case MERCURY -> amount *= 1 + 0.20f * n;
                case VOID -> amount += 3.0f * n;
                case HARBINGER -> effect(target, MobEffects.WITHER, 100, n);
                case IGNIS -> { target.igniteForSeconds(8); amount *= 1 + 0.15f * n; }
                case LEVIATHAN -> { effect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 1 + n); effect(target, MobEffects.WEAKNESS, 60, n - 1); }
                case REMNANT -> attacker.heal(amount * 0.15f * n);
                case MALEDICTUS -> freeze(target);
                case GUARDIAN -> amount *= 1 + 0.35f * n;
                case EMBER -> target.igniteForSeconds(3 * n);
                case TERRA -> amount += 1.5f * n;
                case COPPER -> amount += 1.0f * n;
                case IRON -> amount *= 1 + 0.10f * n;
                case KINETIC -> target.knockback(1.2 * n, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                case GRAVITY -> { target.setDeltaMovement(target.getDeltaMovement().add(0, 0.7 * n, 0)); target.hurtMarked = true; }
                case DUST -> effect(target, MobEffects.BLINDNESS, 50 * n, 0);
                case ACID -> effect(target, MobEffects.POISON, 80, n);
                case FLARE -> { target.igniteForSeconds(5 * n); effect(target, MobEffects.GLOWING, 100, 0); }
                default -> { }
            }
        }
        return amount;
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        float amount = event.getAmount();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;

        // weapon chips: only real melee hits
        if (attacker != null && event.getSource().getDirectEntity() == attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (Gear.of(weapon) == Gear.WEAPON) amount = hit(Chips.levels(weapon), attacker, target, amount);
            if (attacker instanceof Player hitter && hitter != target) ArmorChips.onPlayerHit(hitter, target);
        }
        // elite mobs and the reworked bosses carry chips too (melee, arrows, fireballs)
        if (attacker != null && attacker != target && Elites.is(attacker)) {
            amount = hit(Elites.chips(attacker), attacker, target, amount);
            Elites.onHit(attacker, target);
        }

        // a Kinetic dash cushions the landing: up to 3 hearts of fall damage are absorbed for 3 s after a dash
        if (event.getSource().is(DamageTypeTags.IS_FALL) && target instanceof Player p
                && target.level().getGameTime() - p.getPersistentData().getLong("ctcDash") < 60) {
            amount = Math.max(0, amount - 6.0f);
            if (amount <= 0) { event.setCanceled(true); return; }
        }

        // armor chips of the one being hit (per piece since 3.0)
        amount = ArmorChips.hurt(event, target, attacker, amount);
        if (event.isCanceled()) return;
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 40 != 0) return;

        ArmorChips.tick40(player);
        Armors.setBonus(player);

        // boss chips in tools keep their old effects (the plain chips work through ToolChips)
        ItemStack held = player.getMainHandItem();
        if (Gear.of(held) != Gear.TOOL) return;
        int haste = -1;
        for (ChipType t : ChipType.of(held)) {
            switch (t) {
                case IGNIS -> haste = Math.max(haste, 1);
                case HARBINGER, MALEDICTUS -> haste = Math.max(haste, 0);
                case GUARDIAN -> haste = Math.max(haste, 2);
                case LEVIATHAN -> effect(player, MobEffects.CONDUIT_POWER, 100, 0);
                case REMNANT -> { if (held.isDamaged()) held.setDamageValue(Math.max(0, held.getDamageValue() - 1)); }
                default -> { }
            }
        }
        if (haste >= 0) effect(player, MobEffects.DIG_SPEED, 100, Math.min(haste, 3));
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        ChipType chip = BOSS_CHIPS.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        if (chip == null) chip = Outposts.chipOf(entity);
        if (chip == null) return;
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(),
                new ItemStack(CtcCore.CHIP_ITEMS.get(chip).get())));
        // the free bosses also guard a material the main line needs
        ItemStack trophy = switch (chip) {
            case LEVIATHAN -> new ItemStack(Materials.ABYSSAL_GILL.get(), 2);
            case REMNANT -> new ItemStack(Materials.DESERT_RELIC.get(), 2);
            case MALEDICTUS -> new ItemStack(Materials.CRYO_CORE.get(), 2);
            default -> ItemStack.EMPTY;
        };
        if (!trophy.isEmpty()) event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(), trophy));
    }
}
