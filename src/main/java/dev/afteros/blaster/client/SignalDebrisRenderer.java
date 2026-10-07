package dev.afteros.blaster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.afteros.blaster.entity.SignalDebris;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class SignalDebrisRenderer extends EntityRenderer<SignalDebris> {
    private final BlockRenderDispatcher blocks;

    public SignalDebrisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SignalDebris debris, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0D, 0.25D, 0.0D);
        float spin = debris.onGround() ? 0.0F : (debris.tickCount + partial) * 22.0F;
        pose.mulPose(Axis.YP.rotationDegrees(debris.getId() * 37.0F + spin));
        pose.mulPose(Axis.XP.rotationDegrees(spin * 0.7F));
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(-0.5D, -0.5D, -0.5D);
        this.blocks.renderSingleBlock(debris.getBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(debris, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SignalDebris debris) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
