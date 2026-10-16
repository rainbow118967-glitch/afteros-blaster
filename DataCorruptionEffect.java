package dev.afteros.crt.effect;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.BlasterConfig;
import dev.afteros.crt.entity.CorruptionFront;
import dev.afteros.crt.gameplay.Virus;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Data Corruption: an infection. Infected mobs are slowed, hit harder (attribute modifiers), take damage over time,
 * leave a trail of corrupted ground behind them and infect other creatures that stand close.
 * Note: this never adds or removes other effects from inside the tick (that would crash the effect iterator).
 */
public class DataCorruptionEffect extends MobEffect {
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.8F), 1.0F);

    public DataCorruptionEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF46C8);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, AfterOSBlaster.id("effect.data_corruption.slow"), -0.3D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, AfterOSBlaster.id("effect.data_corruption.rage"), 0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        if (entity instanceof Player && !BlasterConfig.CORRUPT_PLAYERS.get()) {
            return true; // immune players take no symptoms (the effect simply runs out)
        }
        if ((level.getGameTime() / 10L) % 2L == 0L) {
            entity.hurt(level.damageSources().magic(), 1.0F);
        }
        double y = entity.getY() + entity.getBbHeight() * 0.6D;
        level.sendParticles(PINK, entity.getX(), y, entity.getZ(), 5, 0.3D, 0.4D, 0.3D, 0.02D);
        level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), y + 0.4D, entity.getZ(), 2, 0.3D, 0.3D, 0.3D, 0.3D);

        if (!BlasterConfig.VIRUS_ENABLED.get()) {
            return true;
        }
        CorruptionFront.startVirus(level, entity.position(), 3); // a trail of corruption wherever it walks

        double chance = BlasterConfig.VIRUS_CHANCE.get();
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(BlasterConfig.VIRUS_RANGE.get()),
                o -> o != entity && o.isAlive())) {
            if (level.getRandom().nextDouble() < chance) {
                Virus.infect(other);
            }
        }
        return true;
    }
}
