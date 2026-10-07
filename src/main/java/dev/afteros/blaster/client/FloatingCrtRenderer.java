package dev.afteros.blaster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.entity.FloatingCrt;
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

/** Draws the hovering CRT using its own item model (green variant). */
public class FloatingCrtRenderer extends EntityRenderer<FloatingCrt> {
    private final ItemRenderer items;
    private ItemStack stack;

    public FloatingCrtRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(FloatingCrt crt, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (this.stack == null) {
            this.stack = new ItemStack(AfterOSBlaster.FLOATING_CRT.get());
        }
        float age = crt.tickCount + partial;
        float recoil = 1.0F - Math.min(crt.getFireAge(), 6) / 6.0F; // 1 right after a shot, 0 after 6 ticks
        float facing = Mth.rotLerp(partial, crt.yRotO, crt.getYRot());

        pose.pushPose();
        pose.translate(0.0D, 0.45D + Mth.sin(age * 0.12F) * 0.04D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing)); // model screen faces north (-z)
        pose.translate(0.0D, 0.0D, 0.15D * recoil);
        float scale = 1.6F + 0.1F * recoil;
        pose.scale(scale, scale, scale);
        this.items.renderStatic(this.stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                pose, buffers, crt.level(), crt.getId());
        pose.popPose();
        super.render(crt, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingCrt crt) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
