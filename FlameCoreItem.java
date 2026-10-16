package dev.afteros.crt.item;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.BlasterConfig;
import dev.afteros.crt.entity.FlameAura;
import dev.afteros.crt.entity.SignalBeam;
import dev.afteros.crt.entity.ThermalFront;
import dev.afteros.crt.gameplay.BlastProfile;
import dev.afteros.crt.gameplay.ImpactEffects;
import dev.afteros.crt.gameplay.VoidEnergy;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Herrscher Core of Flame (Gem of Destruction). Runs on the same Honkai energy as the Void Core. The user is immune to the fire.
 * Shift + right-click changes ability, right-click uses it:
 * Flame Wave, Flame Armor, Inferno, Heat Lance, Heat Sink, Nuclear Flare.
 */
public class FlameCoreItem extends Item {
    private static final String MODE_KEY = "afteros_flame_mode";
    private static final String[] MODES = {
            "mode.afteros_crt.flame_wave", "mode.afteros_crt.flame_armor", "mode.afteros_crt.flame_inferno",
            "mode.afteros_crt.flame_lance", "mode.afteros_crt.flame_sink", "mode.afteros_crt.flame_nuke"};
    private static final int[] COST = {12, 8, 25, 20, 0, 80};
    private static final int[] COOLDOWN = {40, 20, 120, 30, 60, 600};
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.9F, 0.1F, 0.1F), 0.8F);

    public FlameCoreItem(Item.Properties properties) {
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
            sp.displayClientMessage(Component.translatable("message.afteros_crt.flame_mode", Component.translatable(MODES[next])), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        int mode = mode(stack);
        boolean ok = switch (mode) {
            case 0 -> flameWave(server, sp);
            case 1 -> flameArmor(server, sp);
            case 2 -> inferno(server, sp);
            case 3 -> heatLance(server, sp);
            case 4 -> heatSink(server, sp);
            case 5 -> nuclearFlare(server, sp);
            default -> false;
        };
        if (ok) {
            sp.getCooldowns().addCooldown(this, COOLDOWN[mode]);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** The wearer is immune to their own flames. */
    private static void fireproof(ServerPlayer p, int ticks) {
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, false));
        p.clearFire();
    }

    // ------------------------------------------------------------------ abilities

    /** A wave of thermal energy rolls outward: burns, knocks back, and ignites the ground. */
    private boolean flameWave(ServerLevel server, ServerPlayer p) {
        if (!VoidEnergy.spend(p, COST[0])) {
            return false;
        }
        fireproof(p, 200);
        Vec3 c = p.position();
        BlastProfile wave = BlastProfile.thermal(16.0F, 2.6D);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(13.0D), t -> ImpactEffects.canDamage(p, t))) {
            if (target.distanceToSqr(p) <= 13.0D * 13.0D) {
                ImpactEffects.strike(server, p, target, wave, c, 1.0D);
            }
        }
        for (int i = 0; i < 90; i++) {
            double a = Math.PI * 2.0D * i / 90.0D;
            server.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5D, c.z, 0, Math.cos(a), 0.05D, Math.sin(a), 0.9D);
            if (i % 3 == 0) {
                server.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5D, c.z, 0, Math.cos(a), 0.2D, Math.sin(a), 0.6D);
            }
        }
        igniteGround(server, c, 12.0D, 70);
        server.playSound(null, c.x, c.y, c.z, AfterOSBlaster.BOOM.get(), SoundSource.PLAYERS, 2.5F, 1.4F);
        return true;
    }

    /** Toggle: surround yourself in flames. You cannot be burned; hostile things that come close are scorched. */
    private boolean flameArmor(ServerLevel server, ServerPlayer p) {
        List<? extends FlameAura> mine = server.getEntities(EntityTypeTest.forClass(FlameAura.class),
                a -> a.getOwner() != null && a.getOwner().getUUID().equals(p.getUUID()));
        if (!mine.isEmpty()) {
            mine.forEach(Entity::discard);
            p.displayClientMessage(Component.translatable("message.afteros_crt.flame_armor_off"), true);
            return true;
        }
        if (!VoidEnergy.spend(p, COST[1])) {
            return false;
        }
        fireproof(p, 100);
        server.addFreshEntity(FlameAura.start(server, p));
        p.displayClientMessage(Component.translatable("message.afteros_crt.flame_armor_on"), true);
        return true;
    }

    /** Generates enormous quantities of fire around the target and ignites everything in reach. */
    private boolean inferno(ServerLevel server, ServerPlayer p) {
        if (!VoidEnergy.spend(p, COST[2])) {
            return false;
        }
        fireproof(p, 600);
        Vec3 target = Aim.point(p, 48.0D);
        BlastProfile burn = BlastProfile.thermal(8.0F, 0.6D);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(target, target).inflate(15.0D), t -> ImpactEffects.canDamage(p, t))) {
            if (victim.position().distanceToSqr(target) <= 15.0D * 15.0D) {
                ImpactEffects.strike(server, p, victim, burn, target, 1.0D);
            }
        }
        igniteGround(server, target, 15.0D, 380);
        for (int i = 0; i < 60; i++) {
            RandomSource rnd = server.getRandom();
            server.sendParticles(ParticleTypes.FLAME, target.x + (rnd.nextDouble() - 0.5D) * 24.0D, target.y + rnd.nextDouble() * 6.0D,
                    target.z + (rnd.nextDouble() - 0.5D) * 24.0D, 2, 0.2D, 0.5D, 0.2D, 0.05D);
        }
        server.playSound(null, target.x, target.y, target.z, AfterOSBlaster.BOOM.get(), SoundSource.PLAYERS, 3.0F, 0.8F);
        return true;
    }

    /** Concentrates heat into a tiny point: incinerates a target, or bores a small molten pit into whatever it hits. */
    private boolean heatLance(ServerLevel server, ServerPlayer p) {
        if (!VoidEnergy.spend(p, COST[3])) {
            return false;
        }
        fireproof(p, 200);
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.5D)).add(0.0D, -0.3D, 0.0D);
        LivingEntity target = Aim.entity(p, 48.0D);
        Vec3 to;
        if (target != null) {
            to = target.getBoundingBox().getCenter();
            BlastProfile lance = BlastProfile.thermal(250.0F, 1.5D);
            ImpactEffects.strike(server, p, target, lance, p.position(), 1.0D);
            if (target.isAlive()) {
                target.setRemainingFireTicks(400);
            }
            ThermalFront.start(server, p, target.position(), 3, 12);
        } else {
            BlockHitResult hit = Aim.block(p, 48.0D);
            if (hit.getType() != HitResult.Type.BLOCK) {
                p.displayClientMessage(Component.translatable("message.afteros_crt.void_blocked"), true);
                return false;
            }
            to = hit.getLocation();
            ThermalFront.start(server, p, to, 5, 20);
        }
        server.addFreshEntity(SignalBeam.visual(server, p, from, to, 1.6F, 10, 1));
        server.playSound(null, p.getX(), p.getY(), p.getZ(), AfterOSBlaster.SUPER_FIRE.get(), SoundSource.PLAYERS, 1.2F, 1.6F);
        return true;
    }

    /** Absorbs thermal energy: puts out fires, cools lava to stone and magma to rock, and recharges the Core. */
    private boolean heatSink(ServerLevel server, ServerPlayer p) {
        int r = 10;
        BlockPos o = p.blockPosition();
        int cooled = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) {
                        continue;
                    }
                    BlockPos pos = o.offset(dx, dy, dz);
                    if (!server.hasChunkAt(pos)) {
                        continue;
                    }
                    BlockState s = server.getBlockState(pos);
                    if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) {
                        server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        cooled++;
                    } else if (s.is(Blocks.LAVA)) {
                        server.setBlock(pos, s.getFluidState().isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
                        cooled++;
                    } else if (s.is(Blocks.MAGMA_BLOCK)) {
                        server.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                        cooled++;
                    }
                }
            }
        }
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r))) {
            e.clearFire();
        }
        VoidEnergy.absorb(p, Math.min(40.0D, 8.0D + cooled * 0.5D));
        server.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.0D, p.getZ(), 40, 3.0D, 1.0D, 3.0D, 0.05D);
        server.playSound(null, p.getX(), p.getY(), p.getZ(), AfterOSBlaster.FORCE_PULL.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }

    /** The big one: a nuclear-scale thermal explosion at the target. Vaporizes, melts, fuses to glass, ashes. */
    private boolean nuclearFlare(ServerLevel server, ServerPlayer p) {
        if (!BlasterConfig.THERMAL_ENABLED.get()) {
            return false;
        }
        if (!VoidEnergy.spend(p, COST[5])) {
            return false;
        }
        fireproof(p, 1200);
        Vec3 target = Aim.point(p, 96.0D);
        ThermalFront.start(server, p, target, BlasterConfig.NUKE_RADIUS.get(), BlasterConfig.NUKE_SECONDS.get() * 20);
        return true;
    }

    private static void igniteGround(ServerLevel server, Vec3 center, double radius, int attempts) {
        RandomSource rnd = server.getRandom();
        for (int i = 0; i < attempts; i++) {
            double a = rnd.nextDouble() * Math.PI * 2.0D;
            double d = Math.sqrt(rnd.nextDouble()) * radius;
            int x = Mth.floor(center.x + Math.cos(a) * d);
            int z = Mth.floor(center.z + Math.sin(a) * d);
            if (!server.hasChunkAt(new BlockPos(x, 64, z))) {
                continue;
            }
            int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - center.y) < 8.0D && server.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(server, pos, Direction.UP)) {
                server.setBlock(pos, BaseFireBlock.getState(server, pos), Block.UPDATE_ALL);
            }
        }
    }

    // ------------------------------------------------------------------ glow + energy

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!(isSelected || p.getOffhandItem() == stack)) {
            VoidEnergy.tickIdle(p);
            return;
        }
        VoidEnergy.absorb(p, 0.15D);
        if (p.tickCount % 6 == 0) {
            Vec3 look = p.getLookAngle();
            RandomSource rnd = server.getRandom();
            server.sendParticles(rnd.nextBoolean() ? CRIMSON : DustParticleOptions.REDSTONE, p.getX() + look.x * 0.5D + (rnd.nextDouble() - 0.5D) * 0.5D,
                    p.getEyeY() - 0.45D, p.getZ() + look.z * 0.5D + (rnd.nextDouble() - 0.5D) * 0.5D, 1, 0.1D, 0.1D, 0.1D, 0.0D);
            if (p.tickCount % 18 == 0) {
                server.sendParticles(ParticleTypes.SMALL_FLAME, p.getX() + look.x * 0.5D, p.getEyeY() - 0.4D, p.getZ() + look.z * 0.5D, 1, 0.15D, 0.1D, 0.15D, 0.0D);
            }
        }
        if (p.tickCount % 5 == 0) {
            VoidEnergy.showBar(p, Component.translatable("message.afteros_crt.flame_status", Component.translatable(MODES[mode(stack)]),
                    (int) VoidEnergy.get(p)), BossEvent.BossBarColor.RED);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_crt.flame.flavor").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_crt.flame.energy").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_crt.flame.modes").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.afteros_crt.flame.list").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.afteros_crt.flame.warn").withStyle(ChatFormatting.DARK_GRAY));
    }
}
