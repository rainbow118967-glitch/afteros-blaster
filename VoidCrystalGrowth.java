package dev.afteros.crt.entity;

import dev.afteros.crt.AfterOSBlaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** Grows a black crystal spire one layer at a time (up to 40 blocks tall). Only fills empty space, never removes anything. */
public class VoidCrystalGrowth extends Entity {
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.6F, 0.2F, 1.0F), 1.2F);
    private int height = 12;
    private int layer;
    private double radius = 3.0D;
    private double leanX;
    private double leanZ;

    public VoidCrystalGrowth(EntityType<? extends VoidCrystalGrowth> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static VoidCrystalGrowth grow(ServerLevel level, BlockPos base, int height, double radius, double leanX, double leanZ) {
        VoidCrystalGrowth g = new VoidCrystalGrowth(AfterOSBlaster.VOID_CRYSTAL.get(), level);
        g.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        g.height = height;
        g.radius = radius;
        g.leanX = leanX;
        g.leanZ = leanZ;
        return g;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server) || this.tickCount % 2 != 0) {
            return;
        }
        if (this.layer > this.height) {
            this.discard();
            return;
        }
        RandomSource rnd = server.getRandom();
        double frac = this.layer / (double) Math.max(1, this.height);
        double r = Math.max(0.0D, this.radius * (1.0D - Math.pow(frac, 1.3D)));
        int ri = (int) Math.ceil(r);
        BlockPos center = BlockPos.containing(this.getX() + this.leanX * this.layer, this.getY() + this.layer, this.getZ() + this.leanZ * this.layer);
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dz = -ri; dz <= ri; dz++) {
                double limit = r * r + (rnd.nextFloat() < 0.15F ? 0.9D : 0.0D);
                if (dx * dx + dz * dz > limit && !(dx == 0 && dz == 0)) {
                    continue;
                }
                BlockPos pos = center.offset(dx, 0, dz);
                if (!server.isInWorldBounds(pos) || !server.hasChunkAt(pos)) {
                    continue;
                }
                BlockState existing = server.getBlockState(pos);
                if (existing.isAir() || (existing.canBeReplaced() && existing.getFluidState().isEmpty() && !existing.hasBlockEntity())) {
                    float roll = rnd.nextFloat();
                    BlockState state = roll < 0.07F ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                            : roll < 0.13F ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                            : roll < 0.45F ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
                    server.setBlock(pos, state, Block.UPDATE_CLIENTS);
                }
            }
        }
        server.sendParticles(PURPLE, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, 4, r * 0.5D + 0.3D, 0.2D, r * 0.5D + 0.3D, 0.02D);
        this.layer++;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
