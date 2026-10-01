package dev.overprotocol.client;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.vehicle.B2Entity;
import dev.overprotocol.vehicle.B2Flight;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Vehicle-only chase view. The player's preferences and invisibility state are never overwritten. */
@EventBusSubscriber(modid=Overprotocol.MOD_ID,value=Dist.CLIENT)
public final class B2Camera {
    private static CameraType savedPerspective;
    private static B2Entity tracked;
    private static float yaw, pitch, roll, speed;
    private static float oldYaw, oldPitch, oldRoll, oldSpeed;

    private static void select(B2Entity plane) {
        if (tracked == plane) return;
        tracked = plane;
        oldYaw = yaw = plane.getYRot();
        oldPitch = pitch = 12F + plane.getXRot() * .30F;
        oldRoll = roll = plane.bank() * .18F;
        oldSpeed = speed = plane.speed() / B2Flight.MAX_SPEED;
    }

    @SubscribeEvent public static void onTick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.player!=null && mc.player.getVehicle() instanceof B2Entity plane) {
            if(savedPerspective==null) savedPerspective=mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            select(plane);
            if (mc.isPaused()) return;
            oldYaw = yaw; oldPitch = pitch; oldRoll = roll; oldSpeed = speed;
            // Fixed-tick damping plus render interpolation avoids frame-rate-dependent camera lag.
            yaw = Mth.rotLerp(.32F, yaw, plane.getYRot());
            pitch = Mth.lerp(.22F, pitch, 12F + plane.getXRot() * .30F);
            roll = Mth.lerp(.18F, roll, plane.bank() * .18F);
            speed = Mth.lerp(.12F, speed, plane.speed() / B2Flight.MAX_SPEED);
        } else {
            if(savedPerspective!=null) mc.options.setCameraType(savedPerspective);
            savedPerspective=null;
            tracked=null;
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if(event.getCamera().getEntity()==null || !(event.getCamera().getEntity().getVehicle() instanceof B2Entity plane))return;
        select(plane);
        float partial=(float)event.getPartialTick();
        event.setYaw(Mth.rotLerp(partial,oldYaw,yaw));
        event.setPitch(Mth.lerp(partial,oldPitch,pitch));
        event.setRoll(Mth.lerp(partial,oldRoll,roll));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onDistance(CalculateDetachedCameraDistanceEvent event) {
        if(event.getCamera().getEntity()!=null && event.getCamera().getEntity().getVehicle() instanceof B2Entity plane) {
            select(plane);
            event.setDistance(38F+8F*Mth.lerp(event.getCamera().getPartialTickTime(),oldSpeed,speed));
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onFov(ViewportEvent.ComputeFov event) {
        if(event.usedConfiguredFov() && event.getCamera().getEntity()!=null
            && event.getCamera().getEntity().getVehicle() instanceof B2Entity plane) {
            var mc=Minecraft.getInstance();
            select(plane);
            event.setFOV(Mth.clamp(Math.max(82,mc.options.fov().get()+12)
                +14*Mth.lerp((float)event.getPartialTick(),oldSpeed,speed),82,112));
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void hideOccupant(RenderPlayerEvent.Pre event) {
        if(event.getEntity().getVehicle() instanceof B2Entity) event.setCanceled(true);
    }
    private B2Camera() {}
}
