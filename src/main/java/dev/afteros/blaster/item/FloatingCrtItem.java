package dev.afteros.blaster.item;

import dev.afteros.blaster.BlasterConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.util.Mth;
import dev.afteros.blaster.entity.FloatingCrt;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;

/** Right-click to deploy a hovering CRT turret; right-click again to recall it. */
public class FloatingCrtItem extends Item {
    private static final String MODE_KEY = "afteros_drone_mode";
    private static final String[] MODE_NAMES = {
            "mode.afteros_blaster.drone_guard", "mode.afteros_blaster.drone_hunt", "mode.afteros_blaster.drone_hold"};

    public FloatingCrtItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer owner) {
            List<? extends FloatingCrt> mine = server.getEntities(EntityTypeTest.forClass(FloatingCrt.class),
                    d -> d.getOwner() != null && d.getOwner().getUUID().equals(owner.getUUID()));
            int stored = storedMode(stack);
            int mode = stored >= 0 ? stored : (BlasterConfig.DRONE_TARGET_PASSIVE.get() ? 1 : 0);
            if (owner.isShiftKeyDown()) { // cycle Guard -> Hunt -> Hold fire, live on deployed drones too
                int next = (mode + 1) % 3;
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, next));
                mine.forEach(d -> d.setMode(next));
                owner.displayClientMessage(Component.translatable("message.afteros_blaster.drone_mode",
                        Component.translatable(MODE_NAMES[next])), true);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (!mine.isEmpty()) {
                mine.forEach(Entity::discard);
                owner.displayClientMessage(Component.translatable("message.afteros_blaster.drone_recalled"), true);
            } else {
                if (!CrtBlasterItem.consumeCells(owner, 1, BlasterConfig.DRONE_REQUIRE_AMMO.get())) {
                    owner.displayClientMessage(Component.translatable("message.afteros_blaster.no_ammo"), true);
                    return InteractionResultHolder.fail(stack);
                }
                int squad = BlasterConfig.DRONE_COUNT.get();
                for (int i = 0; i < squad; i++) {
                    server.addFreshEntity(FloatingCrt.deploy(server, owner, i, squad, mode));
                }
                owner.displayClientMessage(Component.translatable("message.afteros_blaster.drone_deployed"), true);
            }
            owner.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static int storedMode(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(MODE_KEY) ? Mth.clamp(tag.getInt(MODE_KEY), 0, 2) : -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_blaster.floating.flavor").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.floating.use").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.floating.mode").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.floating.ammo").withStyle(ChatFormatting.DARK_GREEN));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.terrain").withStyle(ChatFormatting.DARK_GRAY));
    }
}
