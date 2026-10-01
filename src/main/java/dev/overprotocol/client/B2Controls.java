package dev.overprotocol.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overprotocol.Overprotocol;
import dev.overprotocol.network.B2ControlPayload;
import dev.overprotocol.vehicle.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Mth;

@EventBusSubscriber(modid = Overprotocol.MOD_ID, value = Dist.CLIENT)
public final class B2Controls {
    public static final KeyMapping GEAR = new KeyMapping("key.overprotocol.b2.gear", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.overprotocol.b2");
    private static int previousInput = -1, previousEntity = -1, heartbeat;
    private static float previousSteering, previousElevator;
    private static B2Entity inputPlane;
    private static final B2MouseInput MOUSE = new B2MouseInput();
    private static double pendingScroll;

    private static boolean inputActive(Minecraft mc) {
        return mc.screen == null && mc.getOverlay() == null && !mc.isPaused()
            && mc.isWindowActive() && mc.mouseHandler.isMouseGrabbed();
    }

    @SubscribeEvent public static void onMouse(CalculatePlayerTurnEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof B2Entity plane)
            || plane.getControllingPassenger() != mc.player || !inputActive(mc)) {
            MOUSE.reset();
            return;
        }
        selectPlane(plane);
        // This event runs before MouseHandler consumes its frame deltas. Match the user's normal sensitivity.
        double scale = mc.options.sensitivity().get() * .6 + .2;
        scale = scale * scale * scale * 8 * .15;
        MOUSE.add(mc.mouseHandler.getXVelocity() * scale,
            -mc.mouseHandler.getYVelocity() * scale * (mc.options.invertYMouse().get() ? -1 : 1));
    }

    private static void selectPlane(B2Entity plane) {
        if (inputPlane != plane) {
            inputPlane = plane;
            MOUSE.reset();
            previousInput = previousEntity = -1;
            heartbeat = 0;
            pendingScroll = 0;
        }
    }

    @SubscribeEvent public static void onScroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof B2Entity plane)
            || plane.getControllingPassenger() != mc.player || !inputActive(mc)) return;
        selectPlane(plane);
        if (Double.isFinite(event.getScrollDeltaY())) pendingScroll += event.getScrollDeltaY();
        event.setCanceled(true);
    }

    @SubscribeEvent public static void onTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof B2Entity plane)
            || plane.getControllingPassenger() != mc.player) {
            previousInput = -1; previousEntity = -1; heartbeat = 0;
            inputPlane = null; MOUSE.reset();
            pendingScroll = 0;
            return;
        }
        selectPlane(plane);
        int input = 0;
        if (inputActive(mc)) {
            if (mc.options.keyUp.isDown()) input |= B2Flight.THROTTLE_UP;
            if (mc.options.keyDown.isDown()) input |= B2Flight.THROTTLE_DOWN;
            if (mc.options.keyLeft.isDown()) input |= B2Flight.LEFT;
            if (mc.options.keyRight.isDown()) input |= B2Flight.RIGHT;
            if (mc.options.keyJump.isDown()) input |= B2Flight.CLIMB;
            if (mc.options.keySprint.isDown()) input |= B2Flight.DESCEND;
            if (GEAR.isDown()) input |= B2Flight.GEAR;
            MOUSE.tick();
        } else { MOUSE.reset(); pendingScroll = 0; }
        // Quantize tiny changes, but always transmit returning to neutral. At most one packet per tick.
        float steering = Math.round(MOUSE.turn() * 100) / 100F;
        float elevator = Math.round(MOUSE.pitch() * 100) / 100F;
        float scroll = (float)Mth.clamp(pendingScroll, -4, 4);
        if (input != previousInput || steering != previousSteering || elevator != previousElevator
            || scroll != 0 || previousEntity != plane.getId() || ++heartbeat >= 10) {
            PacketDistributor.sendToServer(new B2ControlPayload(plane.getId(), input, steering, elevator, scroll));
            previousInput = input; previousEntity = plane.getId(); heartbeat = 0;
            previousSteering = steering; previousElevator = elevator;
            pendingScroll -= scroll;
        }
    }

    private B2Controls() {}
}
