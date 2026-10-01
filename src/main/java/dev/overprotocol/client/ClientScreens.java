package dev.overprotocol.client;

import dev.overprotocol.block.HonorGuardBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The single place common code is allowed to touch a screen. The caller guards on
 * {@code level.isClientSide}, so a dedicated server never loads this class.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientScreens {
    public static void openGuardSkin(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        // resolve again here: whichever half was clicked, the panel must talk to the half that
        // actually holds the statue's data
        var owner = level == null ? pos : HonorGuardBlock.ownerPos(level, pos);
        Minecraft.getInstance().setScreen(new GuardSkinScreen(owner));
    }

    private ClientScreens() {}
}
