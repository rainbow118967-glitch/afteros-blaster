package dev.afteros.blaster.block;

import com.mojang.serialization.MapCodec;
import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.CorruptionFront;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Place it and a huge corruption wave starts, sized to finish in about five minutes (configurable).
 * Breaking the seed stops the spread. What it already corrupted stays until restored.
 */
public class CorruptionSeedBlock extends Block {
    public static final MapCodec<CorruptionSeedBlock> CODEC = simpleCodec(CorruptionSeedBlock::new);
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.8F), 1.4F);

    public CorruptionSeedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || !(level instanceof ServerLevel server) || oldState.is(state.getBlock())) {
            return;
        }
        int seconds = BlasterConfig.SEED_SECONDS.get();
        if (CorruptionFront.startTimed(server, Vec3.atCenterOf(pos), BlasterConfig.SEED_RADIUS.get(), seconds * 20)) {
            server.playSound(null, pos, AfterOSBlaster.VOID_OPEN.get(), SoundSource.BLOCKS, 2.0F, 0.5F);
            for (ServerPlayer player : server.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64.0D * 64.0D) {
                    player.displayClientMessage(Component.translatable("message.afteros_blaster.seed_armed", seconds), false);
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && level instanceof ServerLevel server && !state.is(newState.getBlock())) {
            if (CorruptionFront.cancelTimed(server, Vec3.atCenterOf(pos))) {
                for (ServerPlayer player : server.players()) {
                    if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64.0D * 64.0D) {
                        player.displayClientMessage(Component.translatable("message.afteros_blaster.seed_cancelled"), true);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.addParticle(PINK, x + (random.nextDouble() - 0.5D) * 1.4D, y + (random.nextDouble() - 0.5D) * 1.4D, z + (random.nextDouble() - 0.5D) * 1.4D, 0.0D, 0.02D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, x, y + 0.8D, z, (random.nextDouble() - 0.5D), random.nextDouble() * 0.5D, (random.nextDouble() - 0.5D));
        }
    }
}
