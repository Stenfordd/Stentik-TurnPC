package dev.stentik.ctccore;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Elite mobs: a few naturally spawned hostile mobs carry chips. They glow in the chip's colour, have a name
 * ("Ember Kinetic Zombie"), more health and armor, hit with the chip's weapon effect, and rarely drop one of the chips.
 * What can spawn follows the progress of the nearest player: Overworld basics first, combos after the Nether,
 * the Void Chip after the End, planet chips on the planets. Spawners never make elites (no chip farms).
 */
@EventBusSubscriber(modid = CtcCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class Elites {
    private Elites() {}

    public static final String TAG = "ctc_elite", CHIPS = "ctcChips";
    private static final ResourceLocation HP = ResourceLocation.fromNamespaceAndPath(CtcCore.MODID, "elite_health");
    private static final ResourceLocation ARMOR = ResourceLocation.fromNamespaceAndPath(CtcCore.MODID, "elite_armor");

    private static final ChipType[] BASIC = { ChipType.COPPER, ChipType.IRON, ChipType.EMBER, ChipType.TERRA, ChipType.KINETIC };

    public static boolean is(Entity e) {
        return e.getTags().contains(TAG);
    }

    public static Map<ChipType, Integer> chips(LivingEntity e) {
        List<ChipType> list = new ArrayList<>();
        for (Tag t : e.getPersistentData().getList(CHIPS, Tag.TAG_STRING)) {
            ChipType c = ChipType.byId(t.getAsString());
            if (c != null) list.add(c);
        }
        return ChipType.counts(list);
    }

    /** Give a mob (or a boss) chips: tag, stored list, aura. Bosses keep their own health; mobs get tougher. */
    public static void equip(LivingEntity e, List<ChipType> chips, boolean buff) {
        ListTag list = e.getPersistentData().getList(CHIPS, Tag.TAG_STRING);
        for (ChipType c : chips) list.add(StringTag.valueOf(c.id));
        e.getPersistentData().put(CHIPS, list);
        e.addTag(TAG);
        if (!buff) return;   // bosses keep their own name and health
        int n = list.size();
        var hp = e.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(HP);
            hp.addPermanentModifier(new AttributeModifier(HP, 0.5 * n, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            e.setHealth(e.getMaxHealth());
        }
        var armor = e.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR);
            armor.addPermanentModifier(new AttributeModifier(ARMOR, 2.0 * n, AttributeModifier.Operation.ADD_VALUE));
        }
        MutableComponent name = Component.empty();
        for (ChipType c : chips(e).keySet()) name.append(Component.literal(cap(c.id) + " ").withStyle(s -> s.withColor(TextColor.fromRgb(c.color))));
        name.append(e.getType().getDescription().copy().withStyle(net.minecraft.ChatFormatting.WHITE));
        e.setCustomName(name);
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---------------------------------------------------------------- spawning

    /** 0 = Overworld start, 1 = has been to the Nether, 2 = has been to the End. */
    static int stage(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return 0;
        if (done(server, p, "story/enter_the_end")) return 2;
        if (done(server, p, "story/enter_the_nether")) return 1;
        return 0;
    }

    private static boolean done(MinecraftServer server, ServerPlayer p, String id) {
        var adv = server.getAdvancements().get(ResourceLocation.withDefaultNamespace(id));
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** Planet chips by dimension name (Northstar planets). */
    static ChipType[] planet(ServerLevel level) {
        String d = level.dimension().location().getPath();
        if (d.contains("moon")) return new ChipType[]{ ChipType.LUNAR, ChipType.GRAVITY };
        if (d.contains("mars")) return new ChipType[]{ ChipType.MARTIAN, ChipType.DUST };
        if (d.contains("mercury")) return new ChipType[]{ ChipType.MERCURY, ChipType.FLARE };
        if (d.contains("venus")) return new ChipType[]{ ChipType.ACID, ChipType.EMBER };
        return null;
    }

    @SubscribeEvent
    public static void spawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || mob instanceof EnderDragon || mob instanceof WitherBoss || mob.getMaxHealth() > 100) return;
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION && type != MobSpawnType.STRUCTURE
                && type != MobSpawnType.PATROL && type != MobSpawnType.REINFORCEMENT && type != MobSpawnType.JOCKEY) return;
        ServerLevel level = event.getLevel().getLevel();
        String ns = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace();
        if (!ns.equals("minecraft") && !ns.equals("northstar")) return;   // modded bosses/minibosses have their own design
        Player near = level.getNearestPlayer(mob, 96);
        int stage = near instanceof ServerPlayer sp ? stage(sp) : 0;
        ChipType[] planet = planet(level);
        RandomSource r = mob.getRandom();
        float chance = planet != null ? 0.12f : new float[]{ 0.06f, 0.08f, 0.10f }[stage];
        if (r.nextFloat() >= chance) return;

        int count = 1;
        float roll = r.nextFloat();
        if (planet != null || stage >= 2) count = roll < 0.12f ? 3 : roll < 0.45f ? 2 : 1;
        else if (stage == 1) count = roll < 0.25f ? 2 : 1;

        List<ChipType> pool = new ArrayList<>(List.of(BASIC));
        if (stage >= 2) pool.add(ChipType.VOID);
        List<ChipType> pick = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ChipType c = planet != null && (i == 0 || r.nextBoolean()) ? planet[r.nextInt(planet.length)] : pool.get(r.nextInt(pool.size()));
            if (pick.contains(c) && r.nextBoolean()) continue;   // doubles happen, but less often
            pick.add(c);
        }
        equip(mob, pick, true);
    }

    // ---------------------------------------------------------------- aura and the Void blink

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e.tickCount % 8 != 0 || !(e.level() instanceof ServerLevel level) || !(e instanceof LivingEntity living) || !is(e)) return;
        Map<ChipType, Integer> chips = chips(living);
        if (chips.isEmpty()) return;
        ChipType[] arr = chips.keySet().toArray(new ChipType[0]);
        ChipType c = arr[(e.tickCount / 8) % arr.length];
        int rgb = c.color;
        boolean big = living instanceof EnderDragon || living instanceof WitherBoss;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), big ? 2.5f : 1.0f);
        level.sendParticles(dust, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), big ? 12 : 3,
                e.getBbWidth() * 0.5, e.getBbHeight() * 0.4, e.getBbWidth() * 0.5, 0);
        // Void: blinks next to its target now and then
        if (chips.containsKey(ChipType.VOID) && !big && e.tickCount % 80 == 0 && living instanceof Mob mob && mob.getTarget() != null) {
            LivingEntity t = mob.getTarget();
            double d = mob.distanceTo(t);
            if (d > 6 && d < 24 && mob.getRandom().nextFloat() < 0.5f) {
                Vec3 to = t.position().add(mob.getRandom().nextGaussian() * 2, 0, mob.getRandom().nextGaussian() * 2);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                if (mob.randomTeleport(to.x, to.y, to.z, true))
                    level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 0.8f, 1.3f);
            }
        }
    }

    // ---------------------------------------------------------------- rewards

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!is(e) || e instanceof EnderDragon || e instanceof WitherBoss || !(event.getSource().getEntity() instanceof Player p)) return;
        int looting = 0;
        var reg = e.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var loot = reg.get(Enchantments.LOOTING);
        if (loot.isPresent()) looting = p.getMainHandItem().getEnchantmentLevel(loot.get());
        for (ChipType c : chips(e).keySet()) {
            if (e.getRandom().nextFloat() < 0.015f + 0.005f * looting) {
                Item chip = CtcCore.CHIP_ITEMS.get(c).get();
                event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), new ItemStack(chip)));
            }
        }
    }

    @SubscribeEvent
    public static void xp(LivingExperienceDropEvent event) {
        if (is(event.getEntity()) && !(event.getEntity() instanceof EnderDragon) && !(event.getEntity() instanceof WitherBoss)) {
            int n = 0;
            for (int v : chips(event.getEntity()).values()) n += v;
            event.setDroppedExperience(event.getDroppedExperience() * (2 + n));
        }
    }

    static EnumMap<ChipType, Integer> none() { return new EnumMap<>(ChipType.class); }
}
