package dev.overprotocol.client;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.block.HonorGuardBlock;
import dev.overprotocol.compat.TaczItems;
import dev.overprotocol.vehicle.B2Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/** Prioritize statue/aircraft interaction before TaCZ turns the same mouse press into aiming. */
@EventBusSubscriber(modid = Overprotocol.MOD_ID, value = Dist.CLIENT)
public final class GuardGunInteraction {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePress(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || minecraft.getOverlay() != null || !minecraft.mouseHandler.isMouseGrabbed()
            || player == null || minecraft.level == null || minecraft.gameMode == null
            || player.isSpectator() || !player.getAbilities().mayBuild) return;
        if (!minecraft.options.keyUse.matchesMouse(event.getButton())
            || !TaczItems.isGun(player.getMainHandItem())) return;
        if (minecraft.hitResult instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof B2Entity plane && player.canInteractWithEntity(plane, 0)) {
            event.setCanceled(true);
            var result = minecraft.gameMode.interact(player, plane, InteractionHand.MAIN_HAND);
            if (result.shouldSwing()) player.swing(InteractionHand.MAIN_HAND);
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
            || !(minecraft.level.getBlockState(hit.getBlockPos()).getBlock() instanceof HonorGuardBlock)
            || !player.canInteractWithBlock(hit.getBlockPos(), 0.0)
            || player.isSecondaryUseActive()) return;

        // Cancelling Pre also suppresses MouseButton.Post, where TaCZ toggles aim.
        // Use the normal prediction/packet path: the server still validates and owns the transfer.
        event.setCanceled(true);
        var result = minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        if (result.shouldSwing()) player.swing(InteractionHand.MAIN_HAND);
    }

    private GuardGunInteraction() {}
}
