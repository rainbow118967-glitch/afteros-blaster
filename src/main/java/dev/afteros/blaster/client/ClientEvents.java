package dev.afteros.blaster.client;

import dev.afteros.blaster.AfterOSBlaster;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AfterOSBlaster.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AfterOSBlaster.SIGNAL_BOLT.get(), SignalBoltRenderer::new);
        event.registerEntityRenderer(AfterOSBlaster.SIGNAL_DEBRIS.get(), SignalDebrisRenderer::new);
        event.registerEntityRenderer(AfterOSBlaster.SIGNAL_BEAM.get(), SignalBeamRenderer::new);
        event.registerEntityRenderer(AfterOSBlaster.FLOATING_CRT_ENTITY.get(), FloatingCrtRenderer::new);
        event.registerEntityRenderer(AfterOSBlaster.SUBSPACE_LANCE_ENTITY.get(), SubspaceLanceRenderer::new);
        event.registerEntityRenderer(AfterOSBlaster.IMAGINARY_SPACE.get(), NoopRenderer::new);
    }
}
