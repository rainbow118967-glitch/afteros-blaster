package dev.afteros.crt.network;

import dev.afteros.crt.AfterOSBlaster;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: "the player pressed key action N". */
public record ActionPayload(int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionPayload> TYPE = new CustomPacketPayload.Type<>(AfterOSBlaster.id("action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC = ByteBufCodecs.VAR_INT.map(ActionPayload::new, ActionPayload::action);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
