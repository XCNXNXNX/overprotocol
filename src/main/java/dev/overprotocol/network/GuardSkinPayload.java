package dev.overprotocol.network;

import dev.overprotocol.Overprotocol;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A request from the statue's control panel. */
public record GuardSkinPayload(BlockPos pos, int action, String value) implements CustomPacketPayload {
    public static final int NAME = 0;
    public static final int DEFAULT = 1;
    public static final int SELF = 2;
    public static final int FILE = 3;
    public static final int TURN = 4;
    public static final int POSE = 5;
    public static final int TAKE = 6;

    public static final CustomPacketPayload.Type<GuardSkinPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "guard_skin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuardSkinPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, GuardSkinPayload::pos,
            ByteBufCodecs.VAR_INT, GuardSkinPayload::action,
            ByteBufCodecs.stringUtf8(64), GuardSkinPayload::value,
            GuardSkinPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
