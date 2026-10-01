package dev.overprotocol.network;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.HonorGuardBlock;
import dev.overprotocol.block.HonorGuardBlockEntity;
import dev.overprotocol.vehicle.B2Entity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serves the statue's control panel. Every request is re-checked server side. */
@EventBusSubscriber(modid = Overprotocol.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private static final double REACH = 8.0;

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(GuardSkinPayload.TYPE, GuardSkinPayload.STREAM_CODEC, ModNetwork::onGuardSkin);
        event.registrar("3").playToServer(B2ControlPayload.TYPE, B2ControlPayload.STREAM_CODEC, (payload, context) ->
            context.enqueueWork(() -> {
                var player = context.player();
                if (player.getVehicle() instanceof B2Entity plane && plane.getId() == payload.entityId())
                    plane.acceptControls(player, payload.input(), payload.steering(), payload.elevator(), payload.scroll());
            }));
    }

    private static void onGuardSkin(GuardSkinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.getAbilities().mayBuild) return;
            if (!player.canInteractWithBlock(payload.pos(), REACH)) return;
            var level = player.level();
            var state = level.getBlockState(payload.pos());
            if (!(state.getBlock() instanceof HonorGuardBlock)) return;
            if (payload.action() == GuardSkinPayload.TURN) {
                HonorGuardBlock.turn(level, payload.pos(), state);
                return;
            }
            if (payload.action() == GuardSkinPayload.POSE) {
                HonorGuardBlock.cyclePose(level, payload.pos(), state);
                return;
            }
            var lower = state.getValue(HonorGuardBlock.HALF) == DoubleBlockHalf.LOWER ? payload.pos() : payload.pos().below();
            if (!(level.getBlockEntity(lower) instanceof HonorGuardBlockEntity guard)) return;
            if (payload.action() == GuardSkinPayload.TAKE) {
                var taken = guard.takeItem();
                if (!taken.isEmpty() && !player.getInventory().add(taken)) player.drop(taken, false);
                return;
            }
            switch (payload.action()) {
                case GuardSkinPayload.NAME -> guard.setProfileName(payload.value());
                case GuardSkinPayload.SELF -> guard.setProfile(player.getGameProfile());
                case GuardSkinPayload.FILE -> guard.setLocalSkin(payload.value());
                case GuardSkinPayload.DEFAULT -> guard.clearSkin();
                default -> { }
            }
        });
    }

    private ModNetwork() {}
}
