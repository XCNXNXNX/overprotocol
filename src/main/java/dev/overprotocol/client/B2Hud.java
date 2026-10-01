package dev.overprotocol.client;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.vehicle.B2Entity;
import dev.overprotocol.vehicle.B2Flight;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Compact corner readout; leaves the aircraft, horizon and hotbar clear. */
@EventBusSubscriber(modid=Overprotocol.MOD_ID,value=Dist.CLIENT)
public final class B2Hud {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.options.hideGui || mc.screen!=null || mc.getOverlay()!=null
            || mc.getDebugOverlay().showDebugScreen() || !(mc.player.getVehicle() instanceof B2Entity plane)) return;
        int speed=(int)Math.round(Math.min(B2Flight.MAX_KMH,Math.hypot(plane.speed(),plane.getDeltaMovement().y)*72));
        var status=Component.translatable("hud.overprotocol.b2.compact",speed,Math.round(plane.throttle()*100),
            Component.translatable(plane.gearDown()?"hud.overprotocol.b2.gear_down":"hud.overprotocol.b2.gear_up"));
        boolean pilot=plane.getControllingPassenger()==mc.player;
        var lines=pilot ? List.of(status,
            Component.translatable("hud.overprotocol.b2.mouse",mc.options.keyUp.getTranslatedKeyMessage(),mc.options.keyDown.getTranslatedKeyMessage()),
            Component.translatable("hud.overprotocol.b2.actions",B2Controls.GEAR.getTranslatedKeyMessage(),mc.options.keyShift.getTranslatedKeyMessage()))
            :List.of(status,Component.translatable("hud.overprotocol.b2.rider",mc.options.keyShift.getTranslatedKeyMessage()));
        int width=lines.stream().mapToInt(mc.font::width).max().orElse(0);
        float scale=Math.min(1F,(event.getGuiGraphics().guiWidth()-24F)/(width+12F));
        var graphics=event.getGuiGraphics();var pose=graphics.pose();pose.pushPose();pose.translate(8,8,0);pose.scale(scale,scale,1);
        int height=lines.size()*12+8;
        graphics.fill(0,0,width+12,height,0x780D1720);
        graphics.fill(0,0,2,height,0xFFE0BE70);
        for(int i=0;i<lines.size();i++)graphics.drawString(mc.font,lines.get(i),7,5+i*12,i==0?0xFFEAD39A:0xFFE4E9ED,true);
        pose.popPose();
    }
    private B2Hud() {}
}
