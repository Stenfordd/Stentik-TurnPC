package dev.stentik.ctccore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Void Herald: the 2.0 End boss, called with a Void Sigil in the outer End islands (1000+ blocks from the center).
 * Phase 1 teleports to its target and throws homing void orbs, phase 2 (below 2/3) also calls endermites and an
 * enderman, phase 3 (below 1/3) sends out pulses that throw players back. Every player who fought it gets a Void Heart.
 */
public class VoidHerald extends Monster {
    private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(VoidHerald.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.ctccore.void_herald"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<UUID> fighters = new HashSet<>();
    private int phase = 1;

    public VoidHerald(EntityType<? extends VoidHerald> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, false);
    }

    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
        float ratio = getHealth() / getMaxHealth();
        int now = ratio > 0.66f ? 1 : ratio > 0.33f ? 2 : 3;
        if (now != phase) {
            phase = now;
            bar.setName(Component.translatable("entity.ctccore.void_herald").append(" · ").append(Component.translatable("ctccore.boss.phase", phase)));
            level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0f, 0.6f);
        }
        LivingEntity target = getTarget();
        entityData.set(CASTING, tickCount % 40 > 30 && target != null);
        if (target == null || !(level() instanceof ServerLevel level)) return;
        if (tickCount % 40 == 0) {                         // homing void orb
            level.addFreshEntity(new ShulkerBullet(level, this, target, Direction.Axis.Y));
            playSound(SoundEvents.SHULKER_SHOOT, 2.0f, 0.5f);
        }
        if (tickCount % 70 == 0 && distanceToSqr(target) > 36) teleportNear(target);
        if (phase >= 2 && tickCount % 240 == 0) {          // calls help
            for (int i = 0; i < 2; i++) {
                Endermite m = EntityType.ENDERMITE.create(level);
                if (m != null) { m.moveTo(getX() + random.nextGaussian() * 3, getY(), getZ() + random.nextGaussian() * 3); m.setTarget(target); level.addFreshEntity(m); }
            }
            EnderMan man = EntityType.ENDERMAN.create(level);
            if (man != null) { man.moveTo(getX() + random.nextGaussian() * 4, getY(), getZ() + random.nextGaussian() * 4); man.setTarget(target); level.addFreshEntity(man); }
        }
        if (phase >= 3 && tickCount % 100 == 50) pulse(level);
    }

    private void teleportNear(LivingEntity target) {
        for (int i = 0; i < 12; i++) {
            Vec3 to = target.position().add(random.nextGaussian() * 4, 0, random.nextGaussian() * 4);
            if (randomTeleport(to.x, to.y + 1, to.z, true)) {
                playSound(SoundEvents.ENDERMAN_TELEPORT, 2.0f, 0.6f);
                return;
            }
        }
    }

    /** Phase 3: a ring of void that hurts and throws back everyone within 7 blocks. */
    private void pulse(ServerLevel level) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 160, 3.5, 0.5, 3.5, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.7f);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(7.0))) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.hurt(damageSources().magic(), 6.0f);
            Vec3 push = p.position().subtract(position()).normalize().scale(1.6);
            p.push(push.x, 0.6, push.z);
            p.hurtMarked = true;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) return;
        if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
        for (UUID id : fighters) {
            Player p = level.getPlayerByUUID(id);
            ItemStack heart = new ItemStack(Materials.VOID_HEART.get());
            if (p != null && p.distanceToSqr(this) < 96 * 96) {
                if (!p.getInventory().add(heart)) p.drop(heart, false);
                p.sendSystemMessage(Component.translatable("ctccore.boss.trophy", heart.getHoverName()));
            }
        }
        if (fighters.isEmpty()) spawnAtLocation(new ItemStack(Materials.VOID_HEART.get()));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (UUID id : fighters) list.add(NbtUtils.createUUID(id));
        tag.put("Fighters", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (Tag t : tag.getList("Fighters", Tag.TAG_INT_ARRAY)) fighters.add(NbtUtils.loadUUID(t));
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    // ---------- the sigil that calls it ----------

    public static class SigilItem extends Item {
        public static final int MIN_DISTANCE = 1000;

        public SigilItem(Properties props) {
            super(props);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (level.isClientSide) return InteractionResult.SUCCESS;
            BlockPos at = ctx.getClickedPos().above();
            if (level.dimension() != Level.END || Math.hypot(at.getX(), at.getZ()) < MIN_DISTANCE) {
                if (player != null) player.displayClientMessage(Component.translatable("ctccore.void_sigil.where"), true);
                return InteractionResult.FAIL;
            }
            ServerLevel server = (ServerLevel) level;
            if (!server.getEntitiesOfClass(VoidHerald.class, new net.minecraft.world.phys.AABB(at).inflate(128)).isEmpty()) {
                if (player != null) player.displayClientMessage(Component.translatable("ctccore.void_sigil.busy"), true);
                return InteractionResult.FAIL;
            }
            VoidHerald boss = CtcCore.VOID_HERALD.get().create(server);
            if (boss == null) return InteractionResult.FAIL;
            boss.moveTo(at.getX() + 0.5, at.getY() + 2, at.getZ() + 0.5, ctx.getRotation() + 180, 0);
            boss.finalizeSpawn(server, server.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null);
            server.addFreshEntity(boss);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) { bolt.moveTo(Vec3.atBottomCenterOf(at)); bolt.setVisualOnly(true); server.addFreshEntity(bolt); }
            server.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0f, 0.6f);
            if (player != null && !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            lines.add(Component.translatable("ctccore.void_sigil.tip").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    }
}
