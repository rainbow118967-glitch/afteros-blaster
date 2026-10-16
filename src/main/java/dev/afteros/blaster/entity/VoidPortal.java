package dev.afteros.blaster.entity;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A large oval Void portal (2 wide, 3 tall). Linked pairs carry anything that walks or flies into the front face
 * out of the other portal, so they also redirect arrows and fireballs. Unlinked portals are just launch points.
 */
public class VoidPortal extends Projectile {
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.7F, 0.3F, 1.0F), 1.1F);

    private UUID link;
    private int life = 600;

    public VoidPortal(EntityType<? extends VoidPortal> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    /** Direction the portal faces (its front side). */
    public Vec3 normal() {
        double r = Math.toRadians(this.getYRot());
        return new Vec3(-Math.sin(r), 0.0D, Math.cos(r));
    }

    public static float yawToward(Vec3 direction) {
        return (float) (Mth.atan2(-direction.x, direction.z) * (180.0D / Math.PI));
    }

    /** Standalone portal (launch point). pos is the bottom centre. */
    public static VoidPortal decorative(ServerLevel level, LivingEntity owner, Vec3 pos, float yaw, int life) {
        VoidPortal portal = new VoidPortal(AfterOSBlaster.VOID_PORTAL.get(), level);
        portal.setOwner(owner);
        portal.setPos(pos);
        portal.setYRot(yaw);
        portal.life = life;
        level.addFreshEntity(portal);
        return portal;
    }

    /** Two linked portals. Also trims the owner's oldest portals down to the configured maximum. */
    public static void openPair(ServerLevel level, LivingEntity owner, Vec3 posA, float yawA, Vec3 posB, float yawB, int life) {
        trim(level, owner, 2);
        VoidPortal a = decorative(level, owner, posA, yawA, life);
        VoidPortal b = decorative(level, owner, posB, yawB, life);
        a.link = b.getUUID();
        b.link = a.getUUID();
    }

    private static void trim(ServerLevel level, LivingEntity owner, int incoming) {
        List<VoidPortal> mine = level.getEntitiesOfClass(VoidPortal.class, owner.getBoundingBox().inflate(256.0D),
                p -> p.getOwner() != null && p.getOwner().getUUID().equals(owner.getUUID()) && p.link != null);
        int over = mine.size() + incoming - BlasterConfig.VOID_PORTAL_MAX.get();
        mine.sort((x, y) -> Integer.compare(y.tickCount, x.tickCount)); // oldest first
        for (int i = 0; i < over && i < mine.size(); i++) {
            mine.get(i).discard();
        }
    }

    /** Moves a spot to somewhere a 2x3 portal fits: up out of walls, then down onto the ground. */
    public static Vec3 settle(ServerLevel level, Vec3 start) {
        double x = start.x;
        double z = start.z;
        double y = start.y;
        for (int i = 0; i < 6 && !fits(level, x, y, z); i++) {
            y += 1.0D;
        }
        for (int i = 0; i < 16 && fits(level, x, y - 1.0D, z); i++) {
            y -= 1.0D;
        }
        return new Vec3(x, y, z);
    }

    private static boolean fits(ServerLevel level, double x, double y, double z) {
        return level.hasChunkAt(BlockPos.containing(x, y, z)) && level.noCollision(new AABB(x - 0.6D, y, z - 0.6D, x + 0.6D, y + 3.0D, z + 0.6D));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 n = this.normal();
            Vec3 right = new Vec3(-n.z, 0.0D, n.x);
            for (int i = 0; i < 2; i++) { // sparks drifting off the rim
                double a = this.random.nextDouble() * Math.PI * 2.0D;
                this.level().addParticle(PURPLE, this.getX() + right.x * Math.cos(a) * 1.05D, this.getY() + 1.5D + Math.sin(a) * 1.55D,
                        this.getZ() + right.z * Math.cos(a) * 1.05D, n.x * 0.02D, 0.01D, n.z * 0.02D);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (--this.life <= 0) {
            this.discard();
            return;
        }
        if (this.link == null) {
            return;
        }
        Entity found = server.getEntity(this.link);
        if (!(found instanceof VoidPortal other) || other.isRemoved()) {
            if (this.tickCount > 5) {
                this.discard();
            }
            return;
        }

        Vec3 n = this.normal();
        Vec3 right = new Vec3(-n.z, 0.0D, n.x);
        Vec3 center = this.position().add(0.0D, 1.5D, 0.0D);
        long now = server.getGameTime();
        for (Entity e : server.getEntities(this, this.getBoundingBox().inflate(0.3D), VoidPortal::canPass)) {
            Long until = COOLDOWN.get(e.getUUID());
            if (until != null && until > now) {
                continue;
            }
            Vec3 rel = e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D).subtract(center);
            double d = rel.dot(n);
            double u = rel.dot(right);
            double v = rel.y;
            if (Math.abs(d) > 0.9D || (u * u) / (1.15D * 1.15D) + (v * v) / (1.65D * 1.65D) > 1.0D) {
                continue;
            }
            Vec3 move = e.position().subtract(e.xOld, e.yOld, e.zOld);
            if (move.dot(n) >= -0.01D) {
                continue; // only things moving into the front face
            }
            carry(server, e, other, u, move.length(), now);
        }
        if (COOLDOWN.size() > 512) {
            COOLDOWN.values().removeIf(t -> t < now);
        }
    }

    private static boolean canPass(Entity e) {
        return (e instanceof LivingEntity || e instanceof AbstractArrow || e instanceof Fireball || e instanceof ThrowableProjectile) && !e.isSpectator();
    }

    private void carry(ServerLevel server, Entity e, VoidPortal exit, double lateral, double speed, long now) {
        Vec3 on = exit.normal();
        Vec3 oright = new Vec3(-on.z, 0.0D, on.x);
        double dy = Mth.clamp(e.getY() - this.getY(), 0.0D, 1.5D);
        Vec3 dest = exit.position().add(on.scale(1.2D)).add(oright.scale(-lateral)).add(0.0D, dy, 0.0D);
        float yaw = yawToward(on);
        Vec3 velocity = on.scale(Math.max(speed, 0.25D));
        server.playSound(null, e.getX(), e.getY(), e.getZ(), AfterOSBlaster.VOID_TRAVEL.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        if (e instanceof ServerPlayer player) {
            player.connection.teleport(dest.x, dest.y, dest.z, yaw, player.getXRot());
        } else {
            e.moveTo(dest.x, dest.y, dest.z, yaw, e.getXRot());
        }
        e.setDeltaMovement(velocity);
        e.hurtMarked = true;
        e.fallDistance = 0.0F;
        COOLDOWN.put(e.getUUID(), now + 15L);
        server.playSound(null, dest.x, dest.y, dest.z, AfterOSBlaster.VOID_TRAVEL.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
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
