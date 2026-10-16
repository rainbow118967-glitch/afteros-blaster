package dev.afteros.blaster.block;

import com.mojang.serialization.MapCodec;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.gameplay.Virus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** A block that has been overwritten by CRT corruption. Slows and hurts whatever walks on it. */
public class CorruptedBlock extends Block {
    public static final MapCodec<CorruptedBlock> CODEC = simpleCodec(CorruptedBlock::new);

    public CorruptedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide || !(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (!BlasterConfig.CORRUPTION_MOB_EFFECTS.get()) {
            return;
        }
        if (living instanceof Player && !BlasterConfig.CORRUPT_PLAYERS.get()) {
            return; // players are immune by default
        }
        if (living.tickCount % 10 != 0) {
            return;
        }
        if (BlasterConfig.VIRUS_ENABLED.get()) {
            Virus.infect(living); // standing on corrupted ground infects you
        }
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
        if (living.tickCount % 20 == 0) {
            living.hurt(level.damageSources().magic(), 1.0F);
        }
        if (living instanceof Mob) { // corrupted mobs glow and hit harder
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1, false, false));
        }
        server.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.8F), 1.2F),
                living.getX(), living.getY() + 0.3D, living.getZ(), 6, 0.3D, 0.3D, 0.3D, 0.02D);
    }
}
