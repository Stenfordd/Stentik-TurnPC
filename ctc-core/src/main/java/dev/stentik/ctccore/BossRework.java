package dev.stentik.ctccore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 2.0 boss rework.
 * Ender Dragon and Wither: twice the health, +50% for every extra player nearby, and a second half with new moves.
 * Dragon below 50%: two crystals come back once, arrows do half damage, volleys of three fireballs.
 * Wither: three Wither Skeleton guards at 75% and 25%, a shockwave at 50%, blue skulls in the second half.
 * Every player who hit the boss gets its trophy: Dragon Scale, Wither Core; Scylla gives a Tidal Pearl and the
 * Netherite Monstrosity an Infernal Rivet. Trophies awaken Cracked VoidSteel at a Smithing Table.
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BossRework {
    private BossRework() {}

    private static final String SCALED = "ctc_scaled", FIGHTERS = "ctc_fighters", DONE = "ctc_stages";
    private static final Map<String, Supplier<Item>> TROPHIES = Map.of(
            "minecraft:ender_dragon", () -> Materials.DRAGON_SCALE.get(),
            "minecraft:wither", () -> Materials.WITHER_CORE.get(),
            "cataclysm:scylla", () -> Materials.TIDAL_PEARL.get(),
            "cataclysm:netherite_monstrosity", () -> Materials.INFERNAL_RIVET.get());

    private static String id(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
    }

    private static int playersNear(LivingEntity boss, double range) {
        return Math.max(1, boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(range), p -> !p.isSpectator()).size());
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity boss)) return;
        if (!(boss instanceof EnderDragon || boss instanceof WitherBoss)) return;
        CompoundTag data = boss.getPersistentData();
        if (data.getBoolean(SCALED)) return;
        data.putBoolean(SCALED, true);
        int players = playersNear(boss, boss instanceof EnderDragon ? 200 : 64);
        double mult = 2.0 * (1.0 + 0.5 * (players - 1));
        var hp = boss.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(CtcCore.id("boss_scale"));
            hp.addPermanentModifier(new AttributeModifier(CtcCore.id("boss_scale"), mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            boss.setHealth(boss.getMaxHealth());
        }
        // 2.0: the bosses fight with chips (Elites.hit through ChipEvents); a third one at half health
        Elites.equip(boss, boss instanceof EnderDragon ? List.of(ChipType.VOID, ChipType.LUNAR) : List.of(ChipType.EMBER, ChipType.ACID), false);
    }

    private static void equipMore(LivingEntity boss, ChipType chip, String who) {
        Elites.equip(boss, List.of(chip), false);
        for (Player p : boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(300)))
            p.sendSystemMessage(Component.translatable("ctccore.boss.equips", Component.translatable(who),
                    Component.translatable("item.ctccore." + chip.id + "_chip")).withStyle(s -> s.withColor(chip.color)));
        boss.level().playSound(null, boss.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4.0f, 0.6f);
    }

    /** Remember who hit a trophy boss (stored on the boss, so it survives restarts). */
    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        LivingEntity boss = event.getEntity();
        if (!TROPHIES.containsKey(id(boss)) || !(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        ListTag list = boss.getPersistentData().getList(FIGHTERS, Tag.TAG_STRING);
        String uuid = p.getStringUUID();
        for (Tag t : list) if (t.getAsString().equals(uuid)) return;
        list.add(StringTag.valueOf(uuid));
        boss.getPersistentData().put(FIGHTERS, list);
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        LivingEntity boss = event.getEntity();
        Supplier<Item> trophy = TROPHIES.get(id(boss));
        if (trophy == null || !(boss.level() instanceof ServerLevel level)) return;
        List<ServerPlayer> got = new ArrayList<>();
        for (Tag t : boss.getPersistentData().getList(FIGHTERS, Tag.TAG_STRING)) {
            Player p = level.getPlayerByUUID(java.util.UUID.fromString(t.getAsString()));
            if (p instanceof ServerPlayer sp && sp.distanceToSqr(boss) < 256 * 256) got.add(sp);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer && !got.contains(killer)) got.add(killer);
        var bossChips = new ArrayList<>(Elites.chips(boss).keySet());
        for (ServerPlayer p : got) {
            if (!bossChips.isEmpty() && boss.getRandom().nextFloat() < 0.15f) {
                ChipType c = bossChips.get(boss.getRandom().nextInt(bossChips.size()));
                ItemStack chip = new ItemStack(CtcCore.CHIP_ITEMS.get(c).get());
                p.sendSystemMessage(Component.translatable("ctccore.boss.chip", chip.getHoverName()).withStyle(st -> st.withColor(c.color)));
                if (!p.getInventory().add(chip)) p.drop(chip, false);
            }
            ItemStack stack = new ItemStack(trophy.get());
            p.sendSystemMessage(Component.translatable("ctccore.boss.trophy", stack.getHoverName()));
            if (!p.getInventory().add(stack)) p.drop(stack, false);
        }
        if (got.isEmpty()) boss.spawnAtLocation(new ItemStack(trophy.get()));
    }

    private static boolean stage(LivingEntity boss, String name) {
        CompoundTag data = boss.getPersistentData();
        CompoundTag done = data.getCompound(DONE);
        if (done.getBoolean(name)) return false;
        done.putBoolean(name, true);
        data.put(DONE, done);
        return true;
    }

    private static void tell(LivingEntity boss, String key, double range) {
        for (Player p : boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(range)))
            p.displayClientMessage(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity boss) || !(boss.level() instanceof ServerLevel level)) return;
        if (boss instanceof EnderDragon dragon) dragonTick(level, dragon);
        else if (boss instanceof WitherBoss wither) witherTick(level, wither);
    }

    private static void dragonTick(ServerLevel level, EnderDragon dragon) {
        if (dragon.getHealth() > dragon.getMaxHealth() * 0.5f || dragon.isDeadOrDying()) return;
        if (stage(dragon, "half")) {
            List<SpikeFeature.EndSpike> spikes = new ArrayList<>(SpikeFeature.getSpikesForLevel(level));
            java.util.Collections.shuffle(spikes, new java.util.Random(dragon.getRandom().nextLong()));
            for (int i = 0; i < Math.min(2, spikes.size()); i++) {
                SpikeFeature.EndSpike s = spikes.get(i);
                EndCrystal crystal = new EndCrystal(level, s.getCenterX() + 0.5, s.getHeight() + 1, s.getCenterZ() + 0.5);
                crystal.setShowBottom(true);
                level.setBlockAndUpdate(new net.minecraft.core.BlockPos(s.getCenterX(), s.getHeight(), s.getCenterZ()),
                        net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState());
                level.addFreshEntity(crystal);
            }
            level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0f, 0.7f);
            tell(dragon, "ctccore.boss.dragon.half", 300);
            equipMore(dragon, ChipType.GRAVITY, "entity.minecraft.ender_dragon");
        }
        if (dragon.tickCount % 200 == 0) {
            Player target = level.getNearestPlayer(dragon, 128);
            if (target == null || target.isCreative()) return;
            Vec3 from = dragon.getViewVector(1.0f).scale(4).add(dragon.position()).add(0, 2, 0);
            for (int i = 0; i < 3; i++) {
                Vec3 dir = target.position().add(dragon.getRandom().nextGaussian() * 3, 0, dragon.getRandom().nextGaussian() * 3).subtract(from).normalize();
                DragonFireball ball = new DragonFireball(level, dragon, dir);
                ball.setPos(from.x, from.y, from.z);
                level.addFreshEntity(ball);
            }
        }
    }

    private static void witherTick(ServerLevel level, WitherBoss wither) {
        if (wither.getInvulnerableTicks() > 0 || wither.isDeadOrDying()) return;
        float ratio = wither.getHealth() / wither.getMaxHealth();
        if (ratio <= 0.75f && stage(wither, "guards1")) guards(level, wither);
        if (ratio <= 0.25f && stage(wither, "guards2")) guards(level, wither);
        if (ratio <= 0.5f && stage(wither, "shock")) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, wither.getX(), wither.getY(), wither.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, wither.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 3.0f, 0.6f);
            for (Player p : level.getEntitiesOfClass(Player.class, wither.getBoundingBox().inflate(8.0))) {
                if (p.isCreative() || p.isSpectator()) continue;
                p.hurt(wither.damageSources().mobAttack(wither), 6.0f);
                Vec3 push = p.position().subtract(wither.position()).normalize().scale(1.8);
                p.push(push.x, 0.7, push.z);
                p.hurtMarked = true;
            }
            tell(wither, "ctccore.boss.wither.half", 64);
            equipMore(wither, ChipType.KINETIC, "entity.minecraft.wither");
        }
        if (ratio <= 0.5f && wither.tickCount % 50 == 0 && wither.getTarget() != null) {
            LivingEntity t = wither.getTarget();
            Vec3 from = wither.position().add(0, 3, 0);
            Vec3 dir = t.position().add(0, t.getBbHeight() / 2, 0).subtract(from).normalize();
            WitherSkull skull = new WitherSkull(level, wither, dir);
            skull.setDangerous(true);
            skull.setPos(from.x, from.y, from.z);
            level.addFreshEntity(skull);
        }
    }

    private static void guards(ServerLevel level, WitherBoss wither) {
        for (int i = 0; i < 3; i++) {
            WitherSkeleton s = EntityType.WITHER_SKELETON.create(level);
            if (s == null) continue;
            s.moveTo(wither.getX() + wither.getRandom().nextGaussian() * 3, wither.getY(), wither.getZ() + wither.getRandom().nextGaussian() * 3);
            s.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            if (wither.getTarget() != null) s.setTarget(wither.getTarget());
            level.addFreshEntity(s);
        }
        tell(wither, "ctccore.boss.wither.guards", 64);
    }

    /** Dragon second half: arrows only do half damage. */
    @SubscribeEvent
    public static void arrows(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && event.getSource().getDirectEntity() instanceof AbstractArrow
                && dragon.getHealth() <= dragon.getMaxHealth() * 0.5f) {
            event.setAmount(event.getAmount() * 0.5f);
        }
    }
}
