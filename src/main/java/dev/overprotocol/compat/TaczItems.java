package dev.overprotocol.compat;

import dev.overprotocol.Overprotocol;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Optional gun detection, independent of TaCZ's client animation API. */
public final class TaczItems {
    private static final Class<?> GUN_TYPE = findGunType();

    private static Class<?> findGunType() {
        if (!ModList.get().isLoaded("tacz")) return null;
        try {
            return Class.forName("com.tacz.guns.api.item.IGun");
        } catch (ClassNotFoundException | LinkageError error) {
            Overprotocol.LOGGER.warn("TaCZ gun interaction API unavailable", error);
            return null;
        }
    }

    public static boolean isGun(ItemStack stack) {
        return GUN_TYPE != null && !stack.isEmpty() && GUN_TYPE.isInstance(stack.getItem());
    }

    private TaczItems() {}
}
