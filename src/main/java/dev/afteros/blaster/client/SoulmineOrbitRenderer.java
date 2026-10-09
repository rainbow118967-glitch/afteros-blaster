package dev.afteros.blaster.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.entity.SoulmineOrbit;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Two Soulmine blades circling the owner. */
public class SoulmineOrbitRenderer extends EntityRenderer<SoulmineOrbit> {
    private final ItemRenderer items;
    private ItemStack stack;

    public SoulmineOrbitRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(SoulmineOrbit rig, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (this.stack == null) {
            this.stack = new ItemStack(AfterOSBlaster.SOULMINE_BLADES.get());
        }
        float age = rig.tickCount + partial;
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(age * 18.0F + i * 180.0F));
            pose.translate(1.7D, 0.1D * Math.sin(age * 0.2D + i), 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-35.0F));
            pose.scale(1.5F, 1.5F, 1.5F);
            this.items.renderStatic(this.stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    pose, buffers, rig.level(), rig.getId() + i);
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SoulmineOrbit rig) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
