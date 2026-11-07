package io.github.minerguy341.cinteractionstfc.client;

import dev.dyslexic.cinematic.api.client.AddonScenes;
import io.github.minerguy341.cinteractionstfc.AnvilStations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Client-only registration; kept in its own class so a dedicated server never loads the scene. */
public final class CInteractionsTfcClient {
    private CInteractionsTfcClient() {}

    /** Reloading resources (F3+T) re-reads the tool models and the colours taken from their textures. */
    public static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            ToolModel.clearCache();
            ToolPalette.clearCache();
            ToolModel.get("tongs"); // load now, so a broken model is reported at startup and not in the middle of a scene
            ToolModel.get("hammer");
            ToolModel.get("hammer_alt");
        });
    }

    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (ResourceLocation block : AnvilStations.anvilBlocks()) {
                AddonScenes.register(AnvilStations.sceneId(block), (pos, state, player, side) -> new AnvilScene(pos, side));
            }
        });
    }
}
