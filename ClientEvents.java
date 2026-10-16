package dev.afteros.crt.client;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.network.ActionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class ClientEvents implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (KeyMapping key : KeyDefs.ALL) {
            KeyBindingHelper.registerKeyBinding(key);
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                KeyDefs.poll(action -> ClientPlayNetworking.send(new ActionPayload(action)));
            }
        });
        EntityRendererRegistry.register(AfterOSBlaster.SIGNAL_BOLT.get(), SignalBoltRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.SIGNAL_DEBRIS.get(), SignalDebrisRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.SIGNAL_BEAM.get(), SignalBeamRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.FLOATING_CRT_ENTITY.get(), FloatingCrtRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.SUBSPACE_LANCE_ENTITY.get(), SubspaceLanceRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.IMAGINARY_SPACE.get(), NoopRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.TELEKINESIS_GRIP.get(), NoopRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.VOID_COLLAPSE.get(), NoopRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.SOULMINE_ORBIT.get(), SoulmineOrbitRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.CORRUPTION_FRONT.get(), NoopRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.VOID_PORTAL.get(), VoidPortalRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.VOID_WALKER.get(), NoopRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.VOID_CRYSTAL.get(), NoopRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.FLAME_AURA.get(), NoopRenderer::new);
        EntityRendererRegistry.register(AfterOSBlaster.THERMAL_FRONT.get(), NoopRenderer::new);
    }
}
