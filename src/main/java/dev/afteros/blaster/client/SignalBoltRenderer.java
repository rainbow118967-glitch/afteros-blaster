package dev.afteros.blaster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.afteros.blaster.entity.SignalBolt;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

public class SignalBoltRenderer extends EntityRenderer<SignalBolt> {
    private final BlockRenderDispatcher blocks;

    public SignalBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SignalBolt bolt, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float t = bolt.tickCount + partial;
        pose.mulPose(Axis.YP.rotationDegrees(t * 24.0F));
        pose.mulPose(Axis.XP.rotationDegrees(t * 17.0F));
        float size = (0.35F + 0.25F * bolt.getPower()) * (0.92F + 0.08F * Mth.sin(t * 0.8F));
        pose.scale(size, size, size);
        pose.translate(-0.5D, -0.5D, -0.5D);
        this.blocks.renderSingleBlock(Blocks.SEA_LANTERN.defaultBlockState(), pose, buffers,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(bolt, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SignalBolt bolt) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
