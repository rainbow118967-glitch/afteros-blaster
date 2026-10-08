package dev.afteros.blaster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.entity.SignalBeam;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Two crossed, layered quads from the entity position to its synced end point. */
public class SignalBeamRenderer extends EntityRenderer<SignalBeam> {
    private static final ResourceLocation[] TEXTURES = {
            AfterOSBlaster.id("textures/entity/signal_beam.png"),
            AfterOSBlaster.id("textures/entity/signal_beam_super.png"),
            AfterOSBlaster.id("textures/entity/signal_beam_drone.png")};

    private static ResourceLocation textureFor(SignalBeam beam) {
        return TEXTURES[Mth.clamp(beam.getStyle(), 0, TEXTURES.length - 1)];
    }

    public SignalBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(SignalBeam beam, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(SignalBeam beam, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 offset = beam.getBeamOffset();
        double length = offset.length();
        if (length < 0.05D) {
            return;
        }
        float age = beam.tickCount + partial;
        int duration = Math.max(1, beam.getDuration());
        float fade = Mth.clamp((duration - age) / 6.0F, 0.0F, 1.0F) * Mth.clamp(age / 2.0F + 0.2F, 0.0F, 1.0F);
        if (fade <= 0.0F) {
            return;
        }
        float pulse = 0.88F + 0.12F * Mth.sin(age * 1.7F);
        float half = (0.14F + 0.10F * beam.getPower()) * pulse * fade;

        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F,
                (float) (offset.x / length), (float) (offset.y / length), (float) (offset.z / length)));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(textureFor(beam)));
        float scroll = -age * 0.15F;
        layer(consumer, pose.last(), half * 2.4F, (float) length, (int) (90 * fade), scroll);
        layer(consumer, pose.last(), half * 1.3F, (float) length, (int) (170 * fade), scroll);
        layer(consumer, pose.last(), half * 0.55F, (float) length, (int) (255 * fade), scroll);
        pose.popPose();
    }

    private static void layer(VertexConsumer vc, PoseStack.Pose pose, float w, float length, int alpha, float scroll) {
        float u0 = scroll;
        float u1 = scroll + length * 0.25F;
        // plane in X
        vertex(vc, pose, -w, 0.0F, 0.0F, u0, 0.0F, alpha, 0.0F, 0.0F, 1.0F);
        vertex(vc, pose, w, 0.0F, 0.0F, u0, 1.0F, alpha, 0.0F, 0.0F, 1.0F);
        vertex(vc, pose, w, length, 0.0F, u1, 1.0F, alpha, 0.0F, 0.0F, 1.0F);
        vertex(vc, pose, -w, length, 0.0F, u1, 0.0F, alpha, 0.0F, 0.0F, 1.0F);
        // plane in Z
        vertex(vc, pose, 0.0F, 0.0F, -w, u0, 0.0F, alpha, 1.0F, 0.0F, 0.0F);
        vertex(vc, pose, 0.0F, 0.0F, w, u0, 1.0F, alpha, 1.0F, 0.0F, 0.0F);
        vertex(vc, pose, 0.0F, length, w, u1, 1.0F, alpha, 1.0F, 0.0F, 0.0F);
        vertex(vc, pose, 0.0F, length, -w, u1, 0.0F, alpha, 1.0F, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int alpha, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, Mth.clamp(alpha, 0, 255))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(SignalBeam beam) {
        return textureFor(beam);
    }
}
