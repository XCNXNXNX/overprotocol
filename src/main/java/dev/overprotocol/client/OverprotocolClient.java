package dev.overprotocol.client;

import dev.overprotocol.Overprotocol;
import dev.overprotocol.registry.ModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.world.item.DyeColor;

/** Client-only wiring. Kept in its own class so a dedicated server never loads it. */
@EventBusSubscriber(modid = Overprotocol.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OverprotocolClient {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.HONOR_GUARD_BE.get(), HonorGuardRenderer::new);
        event.registerEntityRenderer(ModContent.B2.get(), B2Renderer::new);
        event.registerEntityRenderer(ModContent.RED_CARPET_ROLL.get(),RedCarpetRollRenderer::new);
        event.registerEntityRenderer(ModContent.CEREMONIAL_SEAT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    @SubscribeEvent public static void onKeys(RegisterKeyMappingsEvent event) { event.register(B2Controls.GEAR); }

    @SubscribeEvent public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        for(var color:DyeColor.values())event.register((state,level,pos,index)->tableColor(color),ModContent.TABLES.get(color).get());
        for(var color:DyeColor.values())event.register((state,level,pos,index)->tableColor(color),ModContent.CHAIRS.get(color).get());
    }
    @SubscribeEvent public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        for(var color:DyeColor.values())event.register((stack,index)->tableColor(color),ModContent.TABLE_ITEMS.get(color).get());
        for(var color:DyeColor.values())event.register((stack,index)->tableColor(color),ModContent.CHAIR_ITEMS.get(color).get());
    }
    /** Retain the established cloth/carpet palette while tinting the new neutral fabric textures. */
    private static int tableColor(DyeColor color) {
        return switch(color) {
            case WHITE -> 0xE9ECEC; case ORANGE -> 0xF07613; case MAGENTA -> 0xBD44B3; case LIGHT_BLUE -> 0x3AAFD9;
            case YELLOW -> 0xF8C627; case LIME -> 0x70B919; case PINK -> 0xED8DAC; case GRAY -> 0x3E4447;
            case LIGHT_GRAY -> 0x8E8E86; case CYAN -> 0x158991; case PURPLE -> 0x792AAC; case BLUE -> 0x35399D;
            case BROWN -> 0x724728; case GREEN -> 0x546D1B; case RED -> 0x9B2034; case BLACK -> 0x252529;
        };
    }

    private OverprotocolClient() {}
}
