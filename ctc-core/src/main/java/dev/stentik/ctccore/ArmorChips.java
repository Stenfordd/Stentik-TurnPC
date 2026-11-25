package dev.stentik.ctccore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 3.0 armor chips: every plain chip does something different in each armor piece, and its level (1 + Amplifiers)
 * counts per piece. Helmet = senses and immunities, chest = defence and payback, legs = power and speed, boots = movement.
 * Boss chips do their one thing from any piece.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArmorChips {
    private static final EquipmentSlot HEAD = EquipmentSlot.HEAD, CHEST = EquipmentSlot.CHEST, LEGS = EquipmentSlot.LEGS, FEET = EquipmentSlot.FEET;

    private ArmorChips() {
    }

    static int lvl(LivingEntity e, EquipmentSlot slot, ChipType t) {
        return Chips.worn(e, slot, t);
    }

    private static void fx(LivingEntity e, Holder<MobEffect> effect, int ticks, int amp) {
        ChipEvents.effect(e, effect, ticks, amp);
    }

    // ------------------------------------------------------------------ getting hurt

    /** Called from ChipEvents for every incoming hit; returns the new damage (may cancel the event). */
    static float hurt(LivingIncomingDamageEvent event, LivingEntity target, LivingEntity attacker, float amount) {
        DamageSource src = event.getSource();
        int n;
        // chest: Void dodges
        if ((n = lvl(target, CHEST, ChipType.VOID)) > 0 && target.getRandom().nextFloat() < 0.08f * n) {
            target.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4f, 1.8f);
            event.setCanceled(true);
            return 0;
        }
        // boots: Ember walks on magma and campfires
        if (lvl(target, FEET, ChipType.EMBER) > 0 && (src.is(DamageTypes.HOT_FLOOR) || src.is(DamageTypes.CAMPFIRE))) {
            event.setCanceled(true);
            return 0;
        }
        if (src.is(DamageTypeTags.IS_FALL) && (n = lvl(target, FEET, ChipType.LUNAR)) > 0) amount *= Math.max(0.1f, 1 - 0.3f * n);
        if ((n = lvl(target, HEAD, ChipType.IRON)) >= 2 && src.is(DamageTypeTags.IS_EXPLOSION)) amount *= 1 - 0.1f * n;
        if ((n = lvl(target, CHEST, ChipType.IRON)) > 0 && src.is(DamageTypeTags.IS_PROJECTILE)) amount *= Math.max(0.2f, 1 - 0.15f * n);
        if ((n = lvl(target, CHEST, ChipType.TERRA)) > 0) amount *= 1 - 0.08f * n;
        if ((n = lvl(target, CHEST, ChipType.MERCURY)) > 0
                && (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.DRAGON_BREATH)))
            amount *= Math.max(0.2f, 1 - 0.2f * n);
        if (Chips.anyArmor(target, ChipType.GUARDIAN)) amount *= 0.75f;

        // chest: payback on the attacker (only for real attackers, not for thorns loops)
        if (attacker != null && attacker != target && !src.is(DamageTypes.THORNS)) {
            if ((n = lvl(target, CHEST, ChipType.COPPER)) > 0) {
                attacker.hurt(target.damageSources().thorns(target), 1 + n);
                fx(attacker, MobEffects.MOVEMENT_SLOWDOWN, 40, 0);
                spark(target.level(), attacker);
            }
            if ((n = lvl(target, CHEST, ChipType.EMBER)) > 0) attacker.igniteForSeconds(3 + n);
            if ((n = lvl(target, CHEST, ChipType.KINETIC)) > 0)
                attacker.knockback(0.6 + 0.3 * n, target.getX() - attacker.getX(), target.getZ() - attacker.getZ());
            if ((n = lvl(target, CHEST, ChipType.GRAVITY)) > 0) {
                attacker.setDeltaMovement(attacker.getDeltaMovement().add(0, 0.5 + 0.2 * n, 0));
                attacker.hurtMarked = true;
            }
            if ((n = lvl(target, CHEST, ChipType.MARTIAN)) > 0) fx(attacker, MobEffects.WEAKNESS, 60, n - 1);
            if (lvl(target, CHEST, ChipType.DUST) > 0) fx(attacker, MobEffects.BLINDNESS, 40, 0);
            if ((n = lvl(target, CHEST, ChipType.ACID)) > 0) fx(attacker, MobEffects.POISON, 60, n - 1);
            if (Chips.anyArmor(target, ChipType.IGNIS)) attacker.igniteForSeconds(4);
            if (Chips.anyArmor(target, ChipType.HARBINGER)) fx(attacker, MobEffects.WITHER, 60, 0);
            if (Chips.anyArmor(target, ChipType.MALEDICTUS)) ChipEvents.freeze(attacker);
        }
        return amount;
    }

    private static void spark(Level level, LivingEntity at) {
        if (level instanceof ServerLevel s)
            s.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX(), at.getY() + at.getBbHeight() / 2, at.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
    }

    /** Helmet Kinetic: melee hits charge it, a full charge sends a spark into up to 3 monsters around the target. */
    static void onPlayerHit(Player player, LivingEntity target) {
        int n = lvl(player, HEAD, ChipType.KINETIC);
        if (n <= 0) return;
        var data = player.getPersistentData();
        int charge = data.getInt("ctcCharge") + 1;
        int need = Math.max(3, 10 - 2 * n);
        if (charge < need) {
            data.putInt("ctcCharge", charge);
            return;
        }
        data.putInt("ctcCharge", 0);
        Level level = player.level();
        int hit = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5),
                x -> x != target && x != player && x.isAlive() && x instanceof Enemy)) {
            e.hurt(player.damageSources().indirectMagic(player, player), 2 + n);
            if (level instanceof ServerLevel s) {
                Vec3 a = target.position().add(0, target.getBbHeight() / 2, 0), b = e.position().add(0, e.getBbHeight() / 2, 0);
                for (int i = 0; i <= 8; i++) {
                    Vec3 p = a.lerp(b, i / 8.0);
                    s.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
                }
            }
            if (++hit >= 3) break;
        }
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.35f, 1.8f);
    }

    /** Legs Void: dropping under 4 hearts blinks you away (60 s cooldown). */
    @SubscribeEvent
    public static void afterDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || lvl(p, LEGS, ChipType.VOID) <= 0) return;
        if (p.getHealth() > 8 || p.getHealth() <= 0) return;
        long now = p.level().getGameTime();
        if (now - p.getPersistentData().getLong("ctcBlink") < 1200) return;
        for (int i = 0; i < 16; i++) {
            double x = p.getX() + (p.getRandom().nextDouble() - 0.5) * 16, z = p.getZ() + (p.getRandom().nextDouble() - 0.5) * 16;
            double y = Math.max(p.level().getMinBuildHeight(), p.getY() + p.getRandom().nextInt(9) - 4);
            Vec3 from = p.position();
            if (p.randomTeleport(x, y, z, true)) {
                p.getPersistentData().putLong("ctcBlink", now);
                p.level().playSound(null, from.x, from.y, from.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                p.resetFallDistance();
                break;
            }
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        var data = e.getPersistentData();
        int n;
        if (data.getBoolean("ctcPound") && (n = lvl(e, FEET, ChipType.GRAVITY)) > 0) {
            data.remove("ctcPound");
            float dmg = Math.min(event.getDistance(), 20) * 0.5f + 2 * n;
            for (LivingEntity m : e.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(3 + n, 1, 3 + n),
                    x -> x != e && x.isAlive() && !(x instanceof Player))) {
                m.hurt(e instanceof Player p ? e.damageSources().playerAttack(p) : e.damageSources().mobAttack(e), dmg);
                m.setDeltaMovement(m.getDeltaMovement().add(0, 0.4, 0));
                m.hurtMarked = true;
            }
            if (e.level() instanceof ServerLevel s) {
                s.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
                s.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.1, e.getZ(), 30, 2, 0.1, 2, 0.05);
            }
            e.level().playSound(null, e.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0f, 0.9f);
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.5f);
        }
        if ((n = lvl(e, FEET, ChipType.ACID)) > 0 && event.getDistance() > 1.5f) {
            for (LivingEntity m : e.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(1.5, 0.5, 1.5),
                    x -> x != e && x.isAlive())) ChipEvents.effect(m, MobEffects.POISON, 60 + 20 * n, n - 1);
        }
    }

    // ------------------------------------------------------------------ immunities and mob behaviour

    private static final ThreadLocal<Boolean> READDING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void effects(MobEffectEvent.Applicable event) {
        LivingEntity e = event.getEntity();
        if (READDING.get()) return;
        Holder<MobEffect> eff = event.getEffectInstance().getEffect();
        boolean deny = false;
        if ((eff.is(MobEffects.BLINDNESS.unwrapKey().get()) || eff.is(MobEffects.DARKNESS.unwrapKey().get())) && lvl(e, HEAD, ChipType.IRON) > 0) deny = true;
        if ((eff.is(MobEffects.HUNGER.unwrapKey().get()) || eff.is(MobEffects.CONFUSION.unwrapKey().get())) && lvl(e, HEAD, ChipType.MARTIAN) > 0) deny = true;
        if (eff.is(MobEffects.DIG_SLOWDOWN.unwrapKey().get()) && lvl(e, HEAD, ChipType.DUST) > 0) deny = true;
        if (eff.is(MobEffects.POISON.unwrapKey().get()) && lvl(e, HEAD, ChipType.ACID) > 0) deny = true;
        if (eff.is(MobEffects.WITHER.unwrapKey().get()) && lvl(e, LEGS, ChipType.ACID) > 0) deny = true;
        int n;
        if (eff.is(MobEffects.MOVEMENT_SLOWDOWN.unwrapKey().get()) && (n = lvl(e, FEET, ChipType.IRON)) > 0) {
            deny = true;
            if (n == 1) {   // level 1: half as long
                MobEffectInstance in = event.getEffectInstance();
                READDING.set(true);
                try {
                    e.addEffect(new MobEffectInstance(in.getEffect(), Math.max(1, in.getDuration() / 2), in.getAmplifier(), in.isAmbient(), in.isVisible(), in.showIcon()));
                } finally {
                    READDING.set(false);
                }
            }
        }
        if (deny) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    /** Helmet Void: Endermen leave you alone unless you hit them. Helmet Lunar: Phantoms ignore you. */
    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player p)) return;
        LivingEntity mob = event.getEntity();
        if (mob instanceof EnderMan && mob.getLastHurtByMob() != p && lvl(p, HEAD, ChipType.VOID) > 0) event.setCanceled(true);
        if (mob instanceof Phantom && mob.getLastHurtByMob() != p && lvl(p, HEAD, ChipType.LUNAR) > 0) event.setCanceled(true);
    }

    // ------------------------------------------------------------------ attributes (legs and boots)

    private static AttributeModifier mod(String name, double v, AttributeModifier.Operation op) {
        return new AttributeModifier(CtcCore.id("chip_" + name), v, op);
    }

    @SubscribeEvent
    public static void attributes(ItemAttributeModifierEvent event) {
        ItemStack s = event.getItemStack();
        if (Gear.of(s) != Gear.ARMOR || !s.has(CtcCore.CHIPS.get())) return;
        String piece = Chips.pieceName(s);
        if (piece == null) return;
        var add = AttributeModifier.Operation.ADD_VALUE;
        var mul = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        for (var en : Chips.leveled(s).entrySet()) {
            int n = en.getValue();
            if (piece.equals("legs")) {
                EquipmentSlotGroup g = EquipmentSlotGroup.LEGS;
                switch (en.getKey()) {
                    case COPPER -> put(event, Attributes.MOVEMENT_SPEED, mod("copper_legs", 0.2 * n, mul), g);
                    case IRON -> put(event, Attributes.KNOCKBACK_RESISTANCE, mod("iron_legs", 0.1 * n, add), g);
                    case TERRA -> put(event, Attributes.MAX_HEALTH, mod("terra_legs", 2.0 * n, add), g);
                    case GRAVITY -> {
                        put(event, Attributes.BLOCK_INTERACTION_RANGE, mod("gravity_legs_block", 0.5 * n, add), g);
                        put(event, Attributes.ENTITY_INTERACTION_RANGE, mod("gravity_legs_entity", 0.5 * n, add), g);
                    }
                    case MARTIAN -> put(event, Attributes.ATTACK_DAMAGE, mod("martian_legs", 1.0 * n, add), g);
                    case DUST -> put(event, Attributes.SNEAKING_SPEED, mod("dust_legs", 0.2 * n, mul), g);
                    default -> { }
                }
            } else if (piece.equals("boots")) {
                EquipmentSlotGroup g = EquipmentSlotGroup.FEET;
                switch (en.getKey()) {
                    case COPPER -> {
                        put(event, Attributes.STEP_HEIGHT, mod("copper_boots_step", 0.5, add), g);
                        put(event, Attributes.MOVEMENT_SPEED, mod("copper_boots", 0.05 * n, mul), g);
                    }
                    case MERCURY -> put(event, Attributes.WATER_MOVEMENT_EFFICIENCY, mod("mercury_boots", Math.min(1.0, 0.34 * n), add), g);
                    default -> { }
                }
            }
        }
    }

    private static void put(ItemAttributeModifierEvent event, Holder<Attribute> a, AttributeModifier m, EquipmentSlotGroup g) {
        event.addModifier(a, m, g);
    }

    // ------------------------------------------------------------------ while worn

    /** Every 40 ticks (from ChipEvents): effects that last a few seconds. */
    static void tick40(Player p) {
        int n;
        Level level = p.level();
        // old (2.x) duplicates become levels
        for (EquipmentSlot slot : Chips.ARMOR) Chips.migrate(p.getItemBySlot(slot));
        Chips.migrate(p.getMainHandItem());
        Chips.migrate(p.getOffhandItem());

        if (lvl(p, HEAD, ChipType.TERRA) > 0 && p.getY() < 50 && !level.canSeeSky(p.blockPosition())) fx(p, MobEffects.NIGHT_VISION, 320, 0);
        if (lvl(p, HEAD, ChipType.MERCURY) > 0) fx(p, MobEffects.WATER_BREATHING, 100, 0);
        if ((n = lvl(p, HEAD, ChipType.EMBER)) > 0 && p.isInLava() && level.getGameTime() - p.getPersistentData().getLong("ctcLava") > 600) {
            p.getPersistentData().putLong("ctcLava", level.getGameTime());
            fx(p, MobEffects.FIRE_RESISTANCE, (2 + n) * 20, 0);
        }
        if (lvl(p, HEAD, ChipType.FLARE) > 0) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), x -> x instanceof Enemy && x.isAlive()))
                fx(m, MobEffects.GLOWING, 60, 0);
        }
        if ((n = lvl(p, CHEST, ChipType.FLARE)) > 0 && level.isDay() && level.canSeeSky(p.blockPosition())) fx(p, MobEffects.REGENERATION, 100, n - 1);
        if (lvl(p, LEGS, ChipType.EMBER) > 0 && p.isOnFire()) fx(p, MobEffects.DAMAGE_BOOST, 60, 0);
        if ((n = lvl(p, LEGS, ChipType.LUNAR)) > 0) fx(p, MobEffects.JUMP, 100, n - 1);
        if (lvl(p, LEGS, ChipType.MERCURY) > 0 && p.isInWater()) fx(p, MobEffects.DOLPHINS_GRACE, 60, 0);
        BlockState below = level.getBlockState(p.blockPosition().below());
        if ((n = lvl(p, FEET, ChipType.TERRA)) > 0 && below.is(BlockTags.DIRT) && p.getHealth() < p.getMaxHealth()) p.heal(0.5f * n);
        if ((n = lvl(p, FEET, ChipType.DUST)) > 0 && (below.is(BlockTags.SAND) || sandy(below))) fx(p, MobEffects.MOVEMENT_SPEED, 60, n - 1);

        // boss chips: one strong thing from any piece
        if (Chips.anyArmor(p, ChipType.IGNIS)) fx(p, MobEffects.FIRE_RESISTANCE, 100, 0);
        if (Chips.anyArmor(p, ChipType.LEVIATHAN)) { fx(p, MobEffects.WATER_BREATHING, 100, 0); fx(p, MobEffects.DOLPHINS_GRACE, 100, 0); }
        if (Chips.anyArmor(p, ChipType.REMNANT)) fx(p, MobEffects.REGENERATION, 100, 0);
    }

    private static boolean sandy(BlockState s) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return path.contains("sand") || path.contains("regolith") || path.contains("dust");
    }

    /** Every tick: movement chips. */
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) return;
        var data = p.getPersistentData();
        int n;
        boolean air = !p.onGround() && !p.isInWater() && !p.getAbilities().flying && !p.isPassenger() && !p.isFallFlying();

        if (lvl(p, CHEST, ChipType.LUNAR) > 0 && air && p.isShiftKeyDown() && p.getDeltaMovement().y < -0.1) fx(p, MobEffects.SLOW_FALLING, 10, 0);
        if (lvl(p, FEET, ChipType.GRAVITY) > 0) {
            if (p.onGround()) data.remove("ctcPound");
            else if (air && p.isShiftKeyDown() && p.fallDistance > 2 && !data.getBoolean("ctcPound")) {
                data.putBoolean("ctcPound", true);
                p.setDeltaMovement(p.getDeltaMovement().x * 0.2, -2.0, p.getDeltaMovement().z * 0.2);
                p.hurtMarked = true;
            }
        }
        // legs Kinetic: sprinting costs less food; legs Flare: no hunger in daylight
        var food = p.getFoodData();
        float ex = food.getExhaustionLevel(), last = data.getFloat("ctcEx");
        if ((n = lvl(p, LEGS, ChipType.KINETIC)) > 0 && p.isSprinting() && ex > last) {
            ex = ex - (ex - last) * Math.min(1f, 0.25f * n);
            food.setExhaustion(ex);
        }
        if (lvl(p, LEGS, ChipType.FLARE) > 0 && p.level().isDay() && p.level().canSeeSky(p.blockPosition()) && ex > last) {
            ex = last;
            food.setExhaustion(ex);
        }
        data.putFloat("ctcEx", ex);

        if (p.tickCount % 20 == 0) {
            if ((n = lvl(p, HEAD, ChipType.COPPER)) > 0) detector(p, n);
            if (lvl(p, FEET, ChipType.VOID) > 0 && p.onGround()) data.putLong("ctcSafe", p.blockPosition().asLong());
        }
        if (p.tickCount % 4 == 0 && (n = lvl(p, HEAD, ChipType.GRAVITY)) > 0) magnet(p, 3 + 2 * n);
        if (lvl(p, FEET, ChipType.VOID) > 0 && p.getY() < p.level().getMinBuildHeight() - 6 && data.contains("ctcSafe")
                && p.level().getGameTime() - data.getLong("ctcVoid") > 6000) {
            data.putLong("ctcVoid", p.level().getGameTime());
            BlockPos safe = BlockPos.of(data.getLong("ctcSafe"));
            p.teleportTo(safe.getX() + 0.5, safe.getY() + 0.1, safe.getZ() + 0.5);
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
            p.resetFallDistance();
            p.level().playSound(null, safe, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        if ((n = lvl(p, FEET, ChipType.MARTIAN)) > 0 && !p.isInLava()) coolLava(p, 1 + Math.min(n, 3));
        if (p.tickCount % 5 == 0 && lvl(p, FEET, ChipType.FLARE) > 0) lightTrail(p);
    }

    /** Helmet Copper: a monster that is after you and that you are not looking at clicks and lights up. */
    private static void detector(Player p, int n) {
        Vec3 look = p.getViewVector(1.0f);
        boolean any = false;
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8 + 4 * n), x -> x.getTarget() == p && x.isAlive())) {
            Vec3 to = m.position().subtract(p.position()).normalize();
            if (look.dot(to) > 0.5 && p.hasLineOfSight(m)) continue;   // you see it already
            if (!m.hasEffect(MobEffects.GLOWING)) any = true;
            fx(m, MobEffects.GLOWING, 40, 0);
        }
        if (any && p instanceof ServerPlayer sp)
            sp.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    private static void magnet(Player p, double r) {
        Vec3 c = p.position().add(0, 0.5, 0);
        for (ItemEntity it : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(r), x -> !x.hasPickUpDelay())) {
            Vec3 d = c.subtract(it.position());
            if (d.lengthSqr() < 0.5) continue;
            it.setDeltaMovement(it.getDeltaMovement().scale(0.5).add(d.normalize().scale(0.25)));
        }
    }

    // ------------------------------------------------------------------ blocks that go back after a while

    private record Temp(BlockState was, BlockState now, long until) {
    }

    private static final Map<ResourceKey<Level>, Map<BlockPos, Temp>> TEMP = new HashMap<>();

    private static void temp(Level level, BlockPos pos, BlockState now, int ticks) {
        Map<BlockPos, Temp> m = TEMP.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Temp old = m.get(pos);
        BlockState was = old != null ? old.was : level.getBlockState(pos);
        if (old == null) level.setBlock(pos, now, 3);
        m.put(pos.immutable(), new Temp(was, now, level.getGameTime() + ticks));
    }

    /** Boots Martian: lava under you hardens for a few seconds. */
    private static void coolLava(Player p, int r) {
        Level level = p.level();
        BlockPos feet = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        for (BlockPos q : BlockPos.betweenClosed(feet.offset(-r, -1, -r), feet.offset(r, -1, r))) {
            if (q.distSqr(feet.below()) > (r + 0.5) * (r + 0.5)) continue;
            BlockState s = level.getBlockState(q);
            if (s.getFluidState().is(Fluids.LAVA) && s.getFluidState().isSource() && level.getBlockState(q.above()).isAir())
                temp(level, q, Blocks.OBSIDIAN.defaultBlockState(), 100 + level.getRandom().nextInt(40));
            else if (TEMP.containsKey(level.dimension()) && TEMP.get(level.dimension()).containsKey(q))
                temp(level, q, Blocks.OBSIDIAN.defaultBlockState(), 100);
        }
    }

    /** Boots Flare: dark places light up behind you. */
    private static void lightTrail(Player p) {
        Level level = p.level();
        BlockPos at = p.blockPosition();
        if (!level.getBlockState(at).isAir() || level.getMaxLocalRawBrightness(at) >= 10) return;
        temp(level, at, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12), 120);
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide || level.getGameTime() % 10 != 0) return;
        Map<BlockPos, Temp> m = TEMP.get(level.dimension());
        if (m == null || m.isEmpty()) return;
        long now = level.getGameTime();
        for (Iterator<Map.Entry<BlockPos, Temp>> it = m.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            if (e.getValue().until > now) continue;
            it.remove();
            BlockPos pos = e.getKey();
            if (!level.isLoaded(pos)) continue;
            BlockState cur = level.getBlockState(pos);
            if (cur.getBlock() != e.getValue().now.getBlock()) continue;   // mined or replaced: leave it
            boolean someoneOn = !level.getEntitiesOfClass(Player.class, new AABB(pos.above()).inflate(0.2)).isEmpty();
            if (someoneOn && cur.is(Blocks.OBSIDIAN)) {   // still standing on it: a little longer
                m.put(pos, new Temp(e.getValue().was, cur, now + 20));
                break;   // the map changed under the iterator
            }
            level.setBlock(pos, e.getValue().was, 3);
        }
    }

    static DustParticleOptions dust(int color) {
        return new DustParticleOptions(new org.joml.Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f), 1.0f);
    }
}
