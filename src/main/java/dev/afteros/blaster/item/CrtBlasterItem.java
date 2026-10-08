package dev.afteros.blaster.item;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.BlasterConfig;
import dev.afteros.blaster.entity.SignalBeam;
import dev.afteros.blaster.entity.SignalBolt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The handheld CRT. Normal variant: bolt / beam modes. Overclocked variant: one huge beam. */
public class CrtBlasterItem extends Item {
    public static final String MODE_KEY = "afteros_fire_mode";
    private final boolean overclocked;

    public CrtBlasterItem(Item.Properties properties, boolean overclocked) {
        super(properties);
        this.overclocked = overclocked;
    }

    // ------------------------------------------------------------------ mode

    /** 0 = bolt, 1 = beam, 2 = scatter. */
    public static int getMode(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(MODE_KEY) ? Mth.clamp(tag.getInt(MODE_KEY), 0, 2) : 0;
    }

    public static Component modeName(ItemStack stack) {
        String key = switch (getMode(stack)) {
            case 1 -> "mode.afteros_blaster.beam";
            case 2 -> "mode.afteros_blaster.scatter";
            default -> "mode.afteros_blaster.bolt";
        };
        return Component.translatable(key);
    }

    // ------------------------------------------------------------------ ammo

    public static int countCells(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(AfterOSBlaster.PHOSPHOR_CELL.get())) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** Takes cells from the inventory. Always succeeds in creative or when ammo is not required. */
    public static boolean consumeCells(ServerPlayer player, int amount, boolean required) {
        if (!required || player.isCreative() || amount <= 0) {
            return true;
        }
        if (countCells(player) < amount) {
            return false;
        }
        int left = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(AfterOSBlaster.PHOSPHOR_CELL.get())) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ heat (server side, per player, not stored on the stack)
    private static final Map<UUID, long[]> HEAT = new HashMap<>();

    public static int heatOf(ServerPlayer player) {
        long[] h = HEAT.get(player.getUUID());
        long now = player.level().getGameTime();
        if (h == null || now < h[1]) {
            return 0;
        }
        return (int) Math.max(0.0D, h[0] - (now - h[1]) * BlasterConfig.HEAT_COOL_PER_SECOND.get() / 20.0D);
    }

    private static void setHeat(ServerPlayer player, int heat) {
        HEAT.put(player.getUUID(), new long[] {heat, player.level().getGameTime()});
    }

    /** Action-bar charge meter. */
    public static void meter(ServerPlayer player, String label, int charged, int full, ChatFormatting color, int cells, String extra) {
        int pct = Math.min(100, charged * 100 / Math.max(1, full));
        int filled = pct / 10;
        String bar = "\u2588".repeat(filled) + "\u2591".repeat(10 - filled);
        String text = label + " " + bar + " " + pct + "%" + (pct >= 100 ? "  READY" : "") + "   CELLS " + cells + extra;
        player.displayClientMessage(Component.literal(text).withStyle(pct >= 100 ? ChatFormatting.GREEN : color), true);
    }

    // ------------------------------------------------------------------ use

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.overclocked && player.isShiftKeyDown()) {
            int nextMode = (getMode(stack) + 1) % 3;
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, nextMode));
                player.displayClientMessage(Component.translatable("message.afteros_blaster.mode", modeName(stack)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int charged = getUseDuration(stack, user) - remaining;
        if (level.isClientSide) {
            RandomSource r = level.getRandom();
            Vec3 p = user.getEyePosition().add(user.getLookAngle().scale(1.0D));
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    p.x + (r.nextDouble() - 0.5D) * 0.7D, p.y - 0.3D + (r.nextDouble() - 0.5D) * 0.5D, p.z + (r.nextDouble() - 0.5D) * 0.7D,
                    0.0D, 0.0D, 0.0D);
        } else if (user instanceof ServerPlayer sp) {
            if (charged % 4 == 0) {
                int full = this.overclocked ? BlasterConfig.SUPER_CHARGE_TICKS.get() : BlasterConfig.CHARGE_TICKS.get();
                meter(sp, this.overclocked ? "OVERLOAD" : "CHARGE", charged, full,
                        this.overclocked ? ChatFormatting.RED : ChatFormatting.AQUA, countCells(sp),
                        !this.overclocked && BlasterConfig.HEAT_ENABLED.get() ? "   HEAT " + heatOf(sp) + "%" : "");
            }
            if (charged % 8 == 1) level.playSound(null, user.getX(), user.getY(), user.getZ(), AfterOSBlaster.CHARGE.get(), SoundSource.PLAYERS,
                    0.6F, 0.8F + Math.min(charged, 60) / 60.0F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int charged = getUseDuration(stack, user) - timeLeft;
        int needed = this.overclocked
                ? BlasterConfig.SUPER_CHARGE_TICKS.get()
                : Math.max(4, BlasterConfig.CHARGE_TICKS.get() / 4);
        if (charged < needed) {
            player.displayClientMessage(Component.translatable(
                    this.overclocked ? "message.afteros_blaster.super_not_charged" : "message.afteros_blaster.not_charged"), true);
            return;
        }

        int heatAdd = 0;
        if (!this.overclocked && BlasterConfig.HEAT_ENABLED.get()) {
            int m = getMode(stack);
            int full = BlasterConfig.CHARGE_TICKS.get();
            heatAdd = m == 1 ? 35 : m == 2 ? 30 : 15 + Math.min(15, charged * 15 / Math.max(1, full));
            if (heatOf(player) + heatAdd > 100) { // overheat: lock the weapon, vent steam
                setHeat(player, 70);
                player.displayClientMessage(Component.literal("OVERHEAT // venting...").withStyle(ChatFormatting.RED), true);
                server.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getEyeY(), player.getZ(), 12, 0.3D, 0.2D, 0.3D, 0.02D);
                server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.IMPACT.get(), SoundSource.PLAYERS, 0.8F, 0.5F);
                player.getCooldowns().addCooldown(this, 90);
                return;
            }
        }
        int cost = this.overclocked ? BlasterConfig.SUPER_CELL_COST.get() : 1;
        if (!consumeCells(player, cost, BlasterConfig.REQUIRE_AMMO.get())) {
            player.displayClientMessage(cost > 1
                    ? Component.translatable("message.afteros_blaster.need_cells", cost)
                    : Component.translatable("message.afteros_blaster.no_ammo"), true);
            return;
        }

        if (heatAdd > 0) {
            setHeat(player, heatOf(player) + heatAdd);
        }
        if (this.overclocked) {
            server.addFreshEntity(SignalBeam.held(server, player, 4.0F, BlasterConfig.SUPER_BEAM_TICKS.get(), true));
            server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.SUPER_FIRE.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
            Vec3 recoil = player.getLookAngle().scale(-0.5D);
            player.setDeltaMovement(player.getDeltaMovement().add(recoil.x, 0.1D, recoil.z));
            player.hurtMarked = true;
            player.getCooldowns().addCooldown(this, BlasterConfig.SUPER_COOLDOWN_TICKS.get());
            return;
        }

        float power = Mth.clamp(charged / (float) BlasterConfig.CHARGE_TICKS.get(), 0.25F, 1.0F);
        int mode = getMode(stack);
        int cooldown = BlasterConfig.COOLDOWN_TICKS.get();
        if (mode == 1) {
            server.addFreshEntity(SignalBeam.held(server, player, power, BlasterConfig.BEAM_TICKS.get(), false));
        } else if (mode == 2) { // scatter: a short-range cone of small bolts
            RandomSource rnd = server.getRandom();
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < 5; i++) {
                Vec3 dir = look.add((rnd.nextDouble() - 0.5D) * 0.24D, (rnd.nextDouble() - 0.5D) * 0.24D, (rnd.nextDouble() - 0.5D) * 0.24D).normalize();
                server.addFreshEntity(new SignalBolt(server, player, 0.25F, dir).withMaxAge(10));
            }
            cooldown += 6;
        } else {
            server.addFreshEntity(new SignalBolt(server, player, power));
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), AfterOSBlaster.FIRE.get(), SoundSource.PLAYERS, 1.0F, 0.9F + power * 0.3F);
        player.getCooldowns().addCooldown(this, cooldown);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        // NOTE: no config reads here - this runs on the client, where server config is not loaded.
        if (this.overclocked) {
            lines.add(Component.translatable("tooltip.afteros_blaster.super.flavor").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            lines.add(Component.translatable("tooltip.afteros_blaster.super.charge").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.afteros_blaster.super.power").withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.terrain").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.safety").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.flavor").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.charge").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.mode", modeName(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.switch_mode").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.ammo").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.terrain").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.safety").withStyle(ChatFormatting.DARK_GRAY));
    }
}
