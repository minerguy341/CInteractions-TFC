package io.github.minerguy341.cinteractionstfc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CInteractionsTfc.MOD_ID)
public final class CInteractionsTfc {
    public static final String MOD_ID = "cinteractions_tfc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CInteractionsTfc(IEventBus modBus) {
        modBus.addListener(this::common);
        if (FMLEnvironment.dist.isClient()) modBus.addListener(this::client);
    }

    private void common(FMLCommonSetupEvent event) {
        // AddonStations.register(...) for TFC blocks goes here (both sides).
        LOGGER.info("Common setup");
    }

    private void client(FMLClientSetupEvent event) {
        // AddonScenes.register(...) for the matching scenes goes here (client only).
        LOGGER.info("Client setup");
    }
}
