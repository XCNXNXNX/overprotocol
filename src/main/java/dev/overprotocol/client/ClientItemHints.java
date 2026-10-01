package dev.overprotocol.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client keyboard access stays out of the server tooltip path. */
@OnlyIn(Dist.CLIENT)
public final class ClientItemHints {
    public static boolean shiftDown() { return Screen.hasShiftDown(); }
    private ClientItemHints() {}
}
