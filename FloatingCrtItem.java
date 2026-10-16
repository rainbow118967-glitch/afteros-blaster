package dev.afteros.crt.item;

import dev.afteros.crt.BlasterConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.util.Mth;
import dev.afteros.crt.entity.FloatingCrt;
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
            "mode.afteros_crt.drone_guard", "mode.afteros_crt.drone_hunt", "mode.afteros_crt.drone_hold", "mode.afteros_crt.drone_corrupt"};

    public FloatingCrtItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer owner) {
            if (owner.isShiftKeyDown()) { // cycle Guard -> Hunt -> Hold fire -> Corrupt, live on deployed drones too
                cycleMode(server, owner, stack);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (!drones(server, owner).isEmpty()) {
                recall(server, owner);
            } else if (!deploy(server, owner, stack)) {
                return InteractionResultHolder.fail(stack);
            }
            owner.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The owner's deployed Floating CRTs. */
    public static List<? extends FloatingCrt> drones(ServerLevel server, ServerPlayer owner) {
        return server.getEntities(EntityTypeTest.forClass(FloatingCrt.class),
                d -> d.getOwner() != null && d.getOwner().getUUID().equals(owner.getUUID()));
    }

    /** Dismisses the squad. Returns false if none was out. */
    public static boolean recall(ServerLevel server, ServerPlayer owner) {
        List<? extends FloatingCrt> mine = drones(server, owner);
        if (mine.isEmpty()) {
            return false;
        }
        mine.forEach(Entity::discard);
        owner.displayClientMessage(Component.translatable("message.afteros_crt.drone_recalled"), true);
        return true;
    }

    /** Spends a Phosphor Cell and deploys the squad using the mode stored on the stack. */
    public static boolean deploy(ServerLevel server, ServerPlayer owner, ItemStack stack) {
        if (!CrtBlasterItem.consumeCells(owner, 1, BlasterConfig.DRONE_REQUIRE_AMMO.get())) {
            owner.displayClientMessage(Component.translatable("message.afteros_crt.no_ammo"), true);
            return false;
        }
        int stored = storedMode(stack);
        int mode = stored >= 0 ? stored : (BlasterConfig.DRONE_TARGET_PASSIVE.get() ? 1 : 0);
        int squad = BlasterConfig.DRONE_COUNT.get();
        for (int i = 0; i < squad; i++) {
            server.addFreshEntity(FloatingCrt.deploy(server, owner, i, squad, mode));
        }
        owner.displayClientMessage(Component.translatable("message.afteros_crt.drone_deployed"), true);
        return true;
    }

    /** Guard -> Hunt -> Hold fire -> Corrupt. Applies to deployed drones immediately. */
    public static void cycleMode(ServerLevel server, ServerPlayer owner, ItemStack stack) {
        int stored = storedMode(stack);
        int mode = stored >= 0 ? stored : (BlasterConfig.DRONE_TARGET_PASSIVE.get() ? 1 : 0);
        int next = (mode + 1) % 4;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, next));
        drones(server, owner).forEach(d -> d.setMode(next));
        owner.displayClientMessage(Component.translatable("message.afteros_crt.drone_mode",
                Component.translatable(MODE_NAMES[next])), true);
    }

    private static int storedMode(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(MODE_KEY) ? Mth.clamp(tag.getInt(MODE_KEY), 0, 3) : -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_crt.floating.flavor").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_crt.floating.use").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_crt.floating.mode").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_crt.floating.ammo").withStyle(ChatFormatting.DARK_GREEN));
        lines.add(Component.translatable("tooltip.afteros_crt.crt_blaster.terrain").withStyle(ChatFormatting.DARK_GRAY));
    }
}
