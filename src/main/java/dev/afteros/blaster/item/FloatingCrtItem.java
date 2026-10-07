package dev.afteros.blaster.item;

import dev.afteros.blaster.BlasterConfig;
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
    public FloatingCrtItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer owner) {
            List<? extends FloatingCrt> mine = server.getEntities(EntityTypeTest.forClass(FloatingCrt.class),
                    d -> d.getOwner() != null && d.getOwner().getUUID().equals(owner.getUUID()));
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
                    server.addFreshEntity(FloatingCrt.deploy(server, owner, i, squad));
                }
                owner.displayClientMessage(Component.translatable("message.afteros_blaster.drone_deployed"), true);
            }
            owner.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.afteros_blaster.floating.flavor").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.afteros_blaster.floating.use").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.afteros_blaster.floating.ammo").withStyle(ChatFormatting.DARK_GREEN));
        lines.add(Component.translatable("tooltip.afteros_blaster.crt_blaster.terrain").withStyle(ChatFormatting.DARK_GRAY));
    }
}
