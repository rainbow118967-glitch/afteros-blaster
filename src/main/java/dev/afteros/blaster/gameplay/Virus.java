package dev.afteros.blaster.gameplay;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Infection helpers for the Data Corruption effect. */
public final class Virus {
    private Virus() {}

    /** Infects the entity. Returns false if it is immune, already infected, or infection is disabled. */
    public static boolean infect(LivingEntity target) {
        if (!BlasterConfig.CORRUPTION_ENABLED.get() || !BlasterConfig.VIRUS_ENABLED.get()) {
            return false;
        }
        if (!target.isAlive() || target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Player && !BlasterConfig.CORRUPT_PLAYERS.get()) {
            return false; // players are immune by default
        }
        Holder<MobEffect> effect = AfterOSBlaster.dataCorruption();
        if (target.hasEffect(effect)) {
            return false;
        }
        return target.addEffect(new MobEffectInstance(effect, BlasterConfig.VIRUS_SECONDS.get() * 20, 0, false, true));
    }

    /** Cures everything within radius. Returns how many were cured. */
    public static int cureAround(ServerLevel level, Vec3 at, double radius) {
        Holder<MobEffect> effect = AfterOSBlaster.dataCorruption();
        int cured = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius), x -> x.hasEffect(effect))) {
            e.removeEffect(effect);
            cured++;
        }
        return cured;
    }
}
