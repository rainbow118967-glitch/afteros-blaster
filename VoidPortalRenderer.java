package dev.afteros.crt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.entity.VoidPortal;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a Void portal: a big oval with a bright purple rim, a near-black centre, swirling dimensional energy at the edge
 * and a glowing purple star in the middle. The swirl and star are textures whose UVs are rotated each frame.
 */
public class VoidPortalRenderer extends EntityRenderer<VoidPortal> {
    private static final ResourceLocation RING = AfterOSBlaster.id("textures/entity/void_portal_ring.png");
    private static final ResourceLocation SWIRL = AfterOSBlaster.id("textures/entity/void_portal_swirl.png");
    private static final ResourceLocation STAR = AfterOSBlaster.id("textures/entity/void_portal_star.png");
    private static final float HALF_W = 1.05F;
    private static final float HALF_H = 1.55F;

    public VoidPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(VoidPortal portal, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(VoidPortal portal, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = portal.tickCount + partial;
        float open = Mth.clamp(age / 8.0F, 0.0F, 1.0F); // pops open
        float pulse = 0.9F + 0.1F * Mth.sin(age * 0.25F);
        pose.pushPose();
        pose.translate(0.0D, HALF_H, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - portal.getYRot()));
        pose.scale(open, open, open);
        quad(buffers.getBuffer(RenderType.entityTranslucent(RING)), pose, HALF_W, HALF_H, 0.0F, 0.0F, 255);
        quad(buffers.getBuffer(RenderType.entityTranslucent(SWIRL)), pose, HALF_W * 1.12F, HALF_H * 1.1F, 0.004F, age * 0.045F, 235);
        quad(buffers.getBuffer(RenderType.entityTranslucent(STAR)), pose, HALF_W * 0.62F * pulse, HALF_H * 0.62F * pulse, 0.008F, -age * 0.03F, 255);
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, PoseStack pose, float halfW, float halfH, float z, float spin, int alpha) {
        float cos = Mth.cos(spin);
        float sin = Mth.sin(spin);
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < 4; i++) {
            float cx = (i == 0 || i == 3) ? -1.0F : 1.0F;
            float cy = (i < 2) ? -1.0F : 1.0F;
            float u = 0.5F + (cx * cos - cy * sin) * 0.5F;   // rotate the texture, not the oval
            float v = 0.5F + (cx * sin + cy * cos) * 0.5F;
            vc.addVertex(p, cx * halfW, cy * halfH, z)
                    .setColor(255, 255, 255, alpha)
                    .setUv(u, 1.0F - v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(p, 0.0F, 0.0F, 1.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VoidPortal portal) {
        return RING;
    }
}
