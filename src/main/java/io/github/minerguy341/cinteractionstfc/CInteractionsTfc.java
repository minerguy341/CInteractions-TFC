package io.github.minerguy341.cinteractionstfc;

import io.github.minerguy341.cinteractionstfc.client.CInteractionsTfcClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CInteractionsTfc.MOD_ID)
public final class CInteractionsTfc {
    public static final String MOD_ID = "cinteractions_tfc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CInteractionsTfc(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AnvilConfig.SPEC);
        modBus.addListener(this::common);
        if (FMLEnvironment.dist.isClient()) modBus.addListener(CInteractionsTfcClient::setup);
    }

    private void common(FMLCommonSetupEvent event) {
        event.enqueueWork(AnvilStations::registerCommon);
    }
}
