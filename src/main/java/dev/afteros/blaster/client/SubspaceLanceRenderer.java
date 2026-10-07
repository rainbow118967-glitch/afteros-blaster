package dev.afteros.blaster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.entity.SubspaceLance;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SubspaceLanceRenderer extends EntityRenderer<SubspaceLance> {
    private final ItemRenderer items;
    private ItemStack stack;

    public SubspaceLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(SubspaceLance lance, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!lance.isArmed()) {
            return;
        }
        if (this.stack == null) {
            this.stack = new ItemStack(AfterOSBlaster.SUBSPACE_LANCE.get());
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partial, lance.yRotO, lance.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, lance.xRotO, lance.getXRot())));
        float scale = lance.isCore() ? 2.4F : 1.8F;
        pose.scale(scale, scale, scale);
        this.items.renderStatic(this.stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                pose, buffers, lance.level(), lance.getId());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SubspaceLance lance) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
