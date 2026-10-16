package dev.afteros.crt.entity;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.gameplay.VoidEnergy;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Spatial Walk: while it lives its owner can hover and fly, with purple discs of space under the feet. Drains Void energy. */
public class VoidWalker extends Projectile {
    private static final DustParticleOptions DISC = new DustParticleOptions(new Vector3f(0.65F, 0.3F, 1.0F), 1.4F);
    private boolean granted;

    public VoidWalker(EntityType<? extends VoidWalker> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static VoidWalker start(ServerLevel level, ServerPlayer owner) {
        VoidWalker walker = new VoidWalker(AfterOSBlaster.VOID_WALKER.get(), level);
        walker.setOwner(owner);
        walker.setPos(owner.position());
        return walker;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (!(this.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != this.level()) {
            this.discard();
            return;
        }
        this.setPos(owner.position());
        if (!owner.getAbilities().mayfly && !owner.isCreative() && !owner.isSpectator()) {
            owner.getAbilities().mayfly = true;
            owner.onUpdateAbilities();
            this.granted = true;
        }
        if (this.tickCount % 20 == 0 && !VoidEnergy.spend(owner, 2.0D)) {
            owner.displayClientMessage(Component.translatable("message.afteros_crt.void_walk_off"), true);
            this.discard();
            return;
        }
        if (!owner.onGround() && this.tickCount % 2 == 0) { // semi-transparent purple circle under the feet
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2.0D * i / 12.0D;
                server.sendParticles(DISC, owner.getX() + Math.cos(a) * 0.55D, owner.getY() - 0.05D, owner.getZ() + Math.sin(a) * 0.55D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.granted && this.getOwner() instanceof ServerPlayer owner && !owner.isCreative() && !owner.isSpectator()) {
            owner.getAbilities().mayfly = false;
            owner.getAbilities().flying = false;
            owner.onUpdateAbilities();
            this.granted = false;
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
