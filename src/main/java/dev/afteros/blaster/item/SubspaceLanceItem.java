package dev.afteros.blaster.item;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.ImaginarySpace;
import dev.afteros.blaster.entity.SubspaceLance;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Herrscher-of-the-Void style Subspace Lance (fan version, original art).
 * Tap: pull a lance from Imaginary Space and throw it (every 4th makes a Space Core burst).
 * Hold + release: a barrage of lances falls around the target.
 * Shift + right-click: open Imaginary Space - time crawls, lances rain.
 */
public class SubspaceLanceItem extends Item {
    private static final String COMBO_KEY = "afteros_lance_combo";
    private static final double RANGE = 64.0D;

    public SubspaceLanceItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                if (!CrtBlasterItem.consumeCells(sp, 2, BlasterConfig.REQUIRE_AMMO.get())) {
                    sp.displayClientMessage(Component.translatable("message.afteros_blaster.need_cells", 2), true);
                    return InteractionResultHolder.fail(stack);
                }
                Vec3 target = aimPoint(sp);
                server.addFreshEntity(ImaginarySpace.open(server, sp, target));
                sp.getCooldowns().addCooldown(this, BlasterConfig.SPACE_COOLDOWN_TICKS.get());
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int charged = getUseDuration(stack, user) - remaining;
        if (!level.isClientSide && user instanceof ServerPlayer sp && charged % 4 == 0) {
            CrtBlasterItem.meter(sp, charged >= 25 ? "BARRAGE" : "LANCE", Math.min(charged, 25), 25,
                    ChatFormatting.LIGHT_PURPLE, CrtBlasterItem.countCells(sp), "");
        }
        if (level.isClientSide) {
            if (charged > 10) {
                Vec3 p = user.getEyePosition().add(user.getLookAngle().scale(1.2D));
                level.addParticle(ParticleTypes.REVERSE_PORTAL, p.x, p.y - 0.4D, p.z, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int charged = getUseDuration(stack, user) - timeLeft;
        Vec3 target = aimPoint(player);

        if (charged >= 25) { // Merciless Vengeance: a barrage around the target
            if (!CrtBlasterItem.consumeCells(player, 1, BlasterConfig.REQUIRE_AMMO.get())) {
                player.displayClientMessage(Component.translatable("message.afteros_blaster.no_ammo"), true);
                return;
            }
            int n = BlasterConfig.LANCE_BARRAGE_COUNT.get();
            for (int i = 0; i < n; i++) {
                double a = server.getRandom().nextDouble() * Math.PI * 2.0D;
                double r = i == 0 ? 0.0D : 1.5D + server.getRandom().nextDouble() * 3.0D;
                Vec3 point = new Vec3(target.x + Math.cos(a) * r, target.y, target.z + Math.sin(a) * r);
                server.addFreshEntity(SubspaceLance.skyfall(server, player, point, 6 + i * 4));
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.getCooldowns().addCooldown(this, BlasterConfig.BARRAGE_COOLDOWN_TICKS.get());
            return;
        }

        // basic Subspace Lance, every 4th summons a Space Core
        int combo = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(COMBO_KEY) + 1;
        boolean core = combo >= 4;
        int next = core ? 0 : combo;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(COMBO_KEY, next));
        server.addFreshEntity(SubspaceLance.straight(server, player, target, core));
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, core ? 0.8F : 1.4F);
        player.getCooldowns().addCooldown(this, BlasterConfig.LANCE_COOLDOWN_TICKS.get());
    }

    /** Where the player is looking: the first living entity or block within range. */
    private static Vec3 aimPoint(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(RANGE));
        BlockHitResult block = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB box = player.getBoundingBox().expandTowards(stop.subtract(start)).inflate(1.0D);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player.level(), player, start, stop, box,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable() && e != player);
        if (entity != null) {
            Entity hit = entity.getEntity();
            return hit.position();
        }
        return stop;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_blaster.lance.flavor").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.lance.tap").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.lance.hold").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.lance.space").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.translatable("tooltip.afteros_blaster.lance.fan").withStyle(ChatFormatting.DARK_GRAY));
    }
}
