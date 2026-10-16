package dev.afteros.blaster.item;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.SubspaceLance;
import dev.afteros.blaster.entity.VoidCollapse;
import dev.afteros.blaster.entity.VoidCrystalGrowth;
import dev.afteros.blaster.entity.VoidPortal;
import dev.afteros.blaster.entity.VoidWalker;
import dev.afteros.blaster.gameplay.ImaginaryTravel;
import dev.afteros.blaster.gameplay.VoidEnergy;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Herrscher Core of the Void (Sirin). Every ability runs on Honkai energy that the Core absorbs from the air.
 * Shift + right-click changes ability, right-click uses it:
 * Void Step, Portal Gate, Portal Lances, Spatial Collapse, Imaginary Space, Spatial Walk, Void Crystals.
 */
public class VoidCoreItem extends Item {
    private static final String MODE_KEY = "afteros_void_mode";
    private static final String[] MODES = {
            "mode.afteros_blaster.void_step", "mode.afteros_blaster.void_portal", "mode.afteros_blaster.void_lances",
            "mode.afteros_blaster.void_collapse", "mode.afteros_blaster.void_imaginary", "mode.afteros_blaster.void_walk",
            "mode.afteros_blaster.void_crystal"};
    private static final int[] COST = {10, 15, 30, 60, 25, 5, 35};
    private static final int[] COOLDOWN = {25, 30, 80, 300, 100, 20, 120};
    private static final DustParticleOptions HONKAI = new DustParticleOptions(new Vector3f(1.0F, 0.5F, 0.1F), 0.7F);

    public VoidCoreItem(Item.Properties properties) {
        super(properties);
    }

    private static int mode(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(MODE_KEY) ? Mth.clamp(tag.getInt(MODE_KEY), 0, MODES.length - 1) : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (sp.isShiftKeyDown()) {
            int next = (mode(stack) + 1) % MODES.length;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, next));
            sp.displayClientMessage(Component.translatable("message.afteros_blaster.void_mode", Component.translatable(MODES[next])), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        int mode = mode(stack);
        boolean ok = switch (mode) {
            case 0 -> voidStep(server, sp);
            case 1 -> portalGate(server, sp);
            case 2 -> portalLances(server, sp);
            case 3 -> spatialCollapse(server, sp);
            case 4 -> imaginarySpace(sp);
            case 5 -> spatialWalk(server, sp);
            case 6 -> voidCrystals(server, sp);
            default -> false;
        };
        if (ok) {
            sp.getCooldowns().addCooldown(this, COOLDOWN[mode]);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // ------------------------------------------------------------------ abilities

    /** Blink through space, even through walls: the farthest free spot along your line of sight. */
    private boolean voidStep(ServerLevel server, ServerPlayer p) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double range = BlasterConfig.VOID_BLINK_RANGE.get();
        Vec3 dest = null;
        for (double t = range; t >= 2.0D && dest == null; t -= 0.5D) {
            Vec3 feet = start.add(look.scale(t)).subtract(0.0D, p.getEyeHeight(), 0.0D);
            if (!server.hasChunkAt(BlockPos.containing(feet))) {
                continue;
            }
            for (int up = 0; up <= 1 && dest == null; up++) {
                Vec3 f = feet.add(0.0D, up, 0.0D);
                if (server.noCollision(p, p.getBoundingBox().move(f.subtract(p.position())))
                        && !server.getFluidState(BlockPos.containing(f)).is(FluidTags.LAVA)) {
                    dest = f;
                }
            }
        }
        if (dest == null) {
            p.displayClientMessage(Component.translatable("message.afteros_blaster.void_blocked"), true);
            return false;
        }
        if (!VoidEnergy.spend(p, COST[0])) {
            return false;
        }
        Vec3 from = p.position();
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0D, from.z, 40, 0.4D, 0.8D, 0.4D, 0.2D);
        server.playSound(null, from.x, from.y, from.z, AfterOSBlaster.VOID_OPEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        p.connection.teleport(dest.x, dest.y, dest.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0.0F;
        server.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0D, dest.z, 60, 0.4D, 0.8D, 0.4D, 0.5D);
        server.playSound(null, dest.x, dest.y, dest.z, AfterOSBlaster.VOID_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** A linked pair of walk-through portals: one in front of you, the other where you look. Arrows and fireballs get redirected too. */
    private boolean portalGate(ServerLevel server, ServerPlayer p) {
        if (!VoidEnergy.spend(p, COST[1])) {
            return false;
        }
        Vec3 look = p.getLookAngle();
        Vec3 h = new Vec3(look.x, 0.0D, look.z);
        h = h.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : h.normalize();
        Vec3 entrance = VoidPortal.settle(server, p.position().add(h.scale(3.0D)));
        Vec3 target = Aim.point(p, 64.0D).subtract(look.scale(1.2D)).add(0.0D, 0.2D, 0.0D);
        Vec3 exit = VoidPortal.settle(server, target);
        VoidPortal.openPair(server, p, entrance, VoidPortal.yawToward(h.scale(-1.0D)), exit, VoidPortal.yawToward(h),
                BlasterConfig.VOID_PORTAL_SECONDS.get() * 20);
        server.playSound(null, p.getX(), p.getY(), p.getZ(), AfterOSBlaster.VOID_OPEN.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
        return true;
    }

    /** Opens a ring of portals around the target; every one fires an incorporeal Subspace Lance that solidifies and detonates on the target. */
    private boolean portalLances(ServerLevel server, ServerPlayer p) {
        if (!VoidEnergy.spend(p, COST[2])) {
            return false;
        }
        boolean resonance = p.getOffhandItem().is(AfterOSBlaster.FLAME_CORE.get()); // both Herrscher Cores: the lances burn
        if (resonance) {
            if (!VoidEnergy.spend(p, 10.0D)) {
                return false;
            }
            p.displayClientMessage(Component.translatable("message.afteros_blaster.resonance"), true);
        }
        Vec3 target = Aim.point(p, 64.0D);
        int n = BlasterConfig.VOID_LANCE_COUNT.get();
        RandomSource rnd = server.getRandom();
        for (int i = 0; i < n; i++) {
            double a = i * 2.399963D;
            double elev = 0.25D + 0.7D * ((i + 0.5D) / n);
            double flat = Math.sqrt(1.0D - elev * elev);
            double r = 9.0D + rnd.nextDouble() * 5.0D;
            Vec3 pos = target.add(Math.cos(a) * flat * r, elev * r, Math.sin(a) * flat * r);
            if (!server.hasChunkAt(BlockPos.containing(pos))) {
                continue;
            }
            float yaw = VoidPortal.yawToward(target.subtract(pos));
            VoidPortal.decorative(server, p, pos.subtract(0.0D, 1.5D, 0.0D), yaw, 70);
            SubspaceLance lance = SubspaceLance.fromPortal(server, p, pos, target, 6 + (i * 40) / Math.max(1, n));
            if (resonance) {
                lance.flaming();
            }
            server.addFreshEntity(lance);
        }
        server.playSound(null, p.getX(), p.getY(), p.getZ(), AfterOSBlaster.VOID_LANCE.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        return true;
    }

    /** Collapse the space around the target: a black hole that drags everything in, then detonates. */
    private boolean spatialCollapse(ServerLevel server, ServerPlayer p) {
        if (!VoidEnergy.spend(p, COST[3])) {
            return false;
        }
        server.addFreshEntity(VoidCollapse.open(server, p, Aim.point(p, 48.0D)));
        return true;
    }

    private boolean imaginarySpace(ServerPlayer p) {
        if (ImaginaryTravel.isInside(p)) {
            return ImaginaryTravel.toggle(p); // leaving is free
        }
        if (!VoidEnergy.spend(p, COST[4])) {
            return false;
        }
        return ImaginaryTravel.toggle(p);
    }

    /** Toggle: hover and fly with purple discs of space under your feet. */
    private boolean spatialWalk(ServerLevel server, ServerPlayer p) {
        List<? extends VoidWalker> mine = server.getEntities(EntityTypeTest.forClass(VoidWalker.class),
                w -> w.getOwner() != null && w.getOwner().getUUID().equals(p.getUUID()));
        if (!mine.isEmpty()) {
            mine.forEach(Entity::discard);
            p.displayClientMessage(Component.translatable("message.afteros_blaster.void_walk_off"), true);
            return true;
        }
        if (!VoidEnergy.spend(p, COST[5])) {
            return false;
        }
        server.addFreshEntity(VoidWalker.start(server, p));
        p.displayClientMessage(Component.translatable("message.afteros_blaster.void_walk_on"), true);
        return true;
    }

    /** Raise a cluster of black crystal spires (the tallest up to the configured height, max 40). */
    private boolean voidCrystals(ServerLevel server, ServerPlayer p) {
        if (!VoidEnergy.spend(p, COST[6])) {
            return false;
        }
        Vec3 t = Aim.point(p, 48.0D);
        BlockPos base = BlockPos.containing(t.x, t.y + 0.01D, t.z);
        int height = BlasterConfig.VOID_CRYSTAL_HEIGHT.get();
        RandomSource rnd = server.getRandom();
        server.addFreshEntity(VoidCrystalGrowth.grow(server, base, height, 3.2D, (rnd.nextDouble() - 0.5D) * 0.12D, (rnd.nextDouble() - 0.5D) * 0.12D));
        for (int i = 0; i < 3; i++) {
            BlockPos at = base.offset(rnd.nextInt(9) - 4, 0, rnd.nextInt(9) - 4);
            server.addFreshEntity(VoidCrystalGrowth.grow(server, at, (int) (height * (0.35D + rnd.nextDouble() * 0.35D)), 1.9D,
                    (rnd.nextDouble() - 0.5D) * 0.3D, (rnd.nextDouble() - 0.5D) * 0.3D));
        }
        server.playSound(null, base.getX(), base.getY(), base.getZ(), AfterOSBlaster.SOUL_SLAM.get(), SoundSource.PLAYERS, 1.5F, 0.6F);
        return true;
    }

    // ------------------------------------------------------------------ glow + energy

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !(level instanceof ServerLevel server)) {
            return;
        }
        boolean held = isSelected || p.getOffhandItem() == stack;
        if (!held) {
            VoidEnergy.tickIdle(p);
            return;
        }
        VoidEnergy.absorb(p, 0.15D); // radiates and absorbs Honkai energy: refills faster while held
        if (p.tickCount % 8 == 0) {
            Vec3 look = p.getLookAngle();
            server.sendParticles(HONKAI, p.getX() + look.x * 0.5D + (server.getRandom().nextDouble() - 0.5D) * 0.5D, p.getEyeY() - 0.45D,
                    p.getZ() + look.z * 0.5D + (server.getRandom().nextDouble() - 0.5D) * 0.5D, 1, 0.1D, 0.1D, 0.1D, 0.0D);
        }
        if (p.tickCount % 5 == 0) {
            VoidEnergy.showBar(p, Component.translatable("message.afteros_blaster.void_status", Component.translatable(MODES[mode(stack)]),
                    (int) VoidEnergy.get(p)), BossEvent.BossBarColor.PURPLE);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_blaster.void.flavor").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.void.energy").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.void.modes").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.afteros_blaster.void.list").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
