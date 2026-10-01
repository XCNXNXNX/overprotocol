package dev.overprotocol;

import dev.overprotocol.data.ModData;
import dev.overprotocol.registry.ModContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Overprotocol.MOD_ID)
public final class Overprotocol {
    public static final String MOD_ID = "overprotocol";
    public static final Logger LOGGER = LoggerFactory.getLogger("Overprotocol");

    public Overprotocol(IEventBus modBus) {
        ModContent.register(modBus);
        modBus.addListener(ModData::gather);
    }
}
