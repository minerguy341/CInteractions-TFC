package io.github.minerguy341.cinteractionstfc.client;

import dev.dyslexic.cinematic.api.client.AddonScenes;
import io.github.minerguy341.cinteractionstfc.AnvilStations;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only registration; kept in its own class so a dedicated server never loads the scene. */
public final class CInteractionsTfcClient {
    private CInteractionsTfcClient() {}

    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (ResourceLocation block : AnvilStations.anvilBlocks()) {
                AddonScenes.register(AnvilStations.sceneId(block), (pos, state, player, side) -> new AnvilScene(pos, side));
            }
        });
    }
}
