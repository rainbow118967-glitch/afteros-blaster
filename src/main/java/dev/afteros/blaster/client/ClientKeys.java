package dev.afteros.blaster.client;

import dev.afteros.blaster.AfterOSBlaster;
import dev.afteros.blaster.network.ActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AfterOSBlaster.MOD_ID, value = Dist.CLIENT)
public final class ClientKeys {
    private ClientKeys() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        for (var key : KeyDefs.ALL) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null) {
            KeyDefs.poll(action -> PacketDistributor.sendToServer(new ActionPayload(action)));
        }
    }
}
