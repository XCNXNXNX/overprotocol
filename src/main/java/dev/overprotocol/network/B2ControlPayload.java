package dev.overprotocol.network;

import dev.overprotocol.Overprotocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record B2ControlPayload(int entityId, int input, float steering, float elevator, float scroll) implements CustomPacketPayload {
    public static final Type<B2ControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "b2_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, B2ControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, B2ControlPayload::entityId, ByteBufCodecs.VAR_INT, B2ControlPayload::input,
        ByteBufCodecs.FLOAT, B2ControlPayload::steering, ByteBufCodecs.FLOAT, B2ControlPayload::elevator,
        ByteBufCodecs.FLOAT, B2ControlPayload::scroll, B2ControlPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
