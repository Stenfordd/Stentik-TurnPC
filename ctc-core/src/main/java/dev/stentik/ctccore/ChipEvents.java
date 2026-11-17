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

    private static Set<ChipType> armorChips(LivingEntity entity) {
        Set<ChipType> out = EnumSet.noneOf(ChipType.class);
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (Gear.of(stack) == Gear.ARMOR) out.addAll(ChipType.of(stack));
        }
        return out;
    }

    private static void effect(LivingEntity entity, Holder<MobEffect> effect, int ticks, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false, true));
    }

    private static void freeze(LivingEntity entity) {
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), 300));
        effect(entity, MobEffects.MOVEMENT_SLOWDOWN, 50, 3);
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
            if (Gear.of(weapon) == Gear.WEAPON) {
                for (ChipType chip : ChipType.of(weapon)) {
                    switch (chip) {
                        case LUNAR -> effect(target, MobEffects.LEVITATION, 30, 1);
                        case MARTIAN -> { target.igniteForSeconds(5); amount *= 1.10f; }
                        case MERCURY -> amount *= 1.25f;
                        case VOID -> amount += 3.0f;
                        case HARBINGER -> effect(target, MobEffects.WITHER, 100, 1);
                        case IGNIS -> { target.igniteForSeconds(8); amount *= 1.15f; }
                        case LEVIATHAN -> { effect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 2); effect(target, MobEffects.WEAKNESS, 60, 0); }
                        case REMNANT -> attacker.heal(amount * 0.15f);
                        case MALEDICTUS -> freeze(target);
                        case GUARDIAN -> amount *= 1.35f;
                        case EMBER -> target.igniteForSeconds(3);
                        case TERRA -> amount += 1.5f;
                    }
                }
            }
        }

        // armor chips of the one being hit
        Set<ChipType> armor = armorChips(target);
        if (!armor.isEmpty()) {
            if (armor.contains(ChipType.VOID) && target.getRandom().nextFloat() < 0.15f) {
                event.setCanceled(true);
                return;
            }
            if (armor.contains(ChipType.LUNAR) && event.getSource().is(DamageTypeTags.IS_FALL)) amount *= 0.4f;
            if (armor.contains(ChipType.MERCURY)) amount *= 0.85f;
            if (armor.contains(ChipType.TERRA)) amount *= 0.92f;
            if (armor.contains(ChipType.GUARDIAN)) amount *= 0.75f;
            if (attacker != null && attacker != target) {
                if (armor.contains(ChipType.MARTIAN) || armor.contains(ChipType.IGNIS) || armor.contains(ChipType.EMBER)) attacker.igniteForSeconds(4);
                if (armor.contains(ChipType.HARBINGER)) effect(attacker, MobEffects.WITHER, 60, 0);
                if (armor.contains(ChipType.MALEDICTUS)) freeze(attacker);
            }
        }
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 40 != 0) return;

        Set<ChipType> armor = armorChips(player);
        if (armor.contains(ChipType.LUNAR)) { effect(player, MobEffects.MOVEMENT_SPEED, 100, 0); effect(player, MobEffects.JUMP, 100, 1); }
        if (armor.contains(ChipType.IGNIS)) effect(player, MobEffects.FIRE_RESISTANCE, 100, 0);
        if (armor.contains(ChipType.LEVIATHAN)) { effect(player, MobEffects.WATER_BREATHING, 100, 0); effect(player, MobEffects.DOLPHINS_GRACE, 100, 0); }
        if (armor.contains(ChipType.REMNANT)) effect(player, MobEffects.REGENERATION, 100, 0);

        ItemStack held = player.getMainHandItem();
        if (Gear.of(held) != Gear.TOOL) return;
        int haste = -1;
        for (ChipType chip : ChipType.of(held)) {
            switch (chip) {
                case LUNAR -> effect(player, MobEffects.NIGHT_VISION, 320, 0);
                case MARTIAN, IGNIS -> haste = Math.max(haste, 1);
                case MERCURY, HARBINGER, MALEDICTUS -> haste = Math.max(haste, 0);
                case GUARDIAN -> haste = Math.max(haste, 2);
                case VOID -> effect(player, MobEffects.LUCK, 100, 1);
                case EMBER -> haste = Math.max(haste, 0);
                case TERRA -> effect(player, MobEffects.LUCK, 100, 0);
                case LEVIATHAN -> effect(player, MobEffects.CONDUIT_POWER, 100, 0);
                case REMNANT -> { if (held.isDamaged()) held.setDamageValue(held.getDamageValue() - 1); }
            }
        }
        if (haste >= 0) effect(player, MobEffects.DIG_SPEED, 100, haste);
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        ChipType chip = BOSS_CHIPS.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
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
