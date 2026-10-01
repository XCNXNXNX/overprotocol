package dev.overprotocol.client;

import dev.overprotocol.Overprotocol;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Optional, client-only calls to TaCZ's public third-person animation API. */
final class TaczGuardCompat {
    private static final Bridge BRIDGE = discover();
    private static boolean warned;

    private record Bridge(Method gun, Method display, Method animationName, Method animation, Method hold) {}

    static void initialize() { /* Resolve the optional API once during renderer creation. */ }

    private static Bridge discover() {
        if (!ModList.get().isLoaded("tacz")) return null;
        try {
            var gun = Class.forName("com.tacz.guns.api.item.IGun");
            var api = Class.forName("com.tacz.guns.api.TimelessAPI");
            var display = Class.forName("com.tacz.guns.client.resource.GunDisplayInstance");
            var manager = Class.forName("com.tacz.guns.api.client.other.ThirdPersonManager");
            var animation = Class.forName("com.tacz.guns.api.client.other.IThirdPersonAnimation");
            var bridge = new Bridge(gun.getMethod("getIGunOrNull", ItemStack.class),
                api.getMethod("getGunDisplay", ItemStack.class), display.getMethod("getThirdPersonAnimation"),
                manager.getMethod("getAnimation", String.class), animation.getMethod("animateGunHold",
                    LivingEntity.class, ModelPart.class, ModelPart.class, ModelPart.class, ModelPart.class));
            Overprotocol.LOGGER.info("TaCZ statue third-person animation bridge ready");
            return bridge;
        } catch (ReflectiveOperationException | LinkageError error) {
            Overprotocol.LOGGER.warn("TaCZ statue animation API unavailable; keeping ordinary hand rendering", error);
            return null;
        }
    }

    /** Uses the gun pack's own hold animation, including two-arm and special weapon poses. */
    static boolean applyHold(LivingEntity actor, ItemStack stack, PlayerModel<LivingEntity> model) {
        if (BRIDGE == null || stack.isEmpty()) return false;
        try {
            if (BRIDGE.gun.invoke(null, stack) == null) return false;
            var display = (Optional<?>) BRIDGE.display.invoke(null, stack);
            if (display.isEmpty()) return false;
            var animationName = (String) BRIDGE.animationName.invoke(display.get());
            var animation = BRIDGE.animation.invoke(null, animationName);
            BRIDGE.hold.invoke(animation, actor, model.rightArm, model.leftArm, model.body, model.head);
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            if (!warned) {
                warned = true;
                Overprotocol.LOGGER.warn("Cannot apply TaCZ statue hold animation; keeping ceremonial pose", error);
            }
            return false;
        }
    }

    private TaczGuardCompat() {}
}
