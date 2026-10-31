package io.github.minerguy341.cinteractionstfc.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;

/**
 * Anvils whose scene is drawing right now. The scene marks its block every frame, so the mark expires by itself if a
 * scene ends without telling us and the real block's items are never hidden for good.
 */
public final class SceneVisibility {
    private static final long FRESH_MS = 250;
    private static final Map<BlockPos, Long> DRAWING = new ConcurrentHashMap<>();

    private SceneVisibility() {}

    static void mark(BlockPos pos) {
        DRAWING.put(pos.immutable(), Util.getMillis());
    }

    /** True while a scene is drawing the items of this anvil itself, so the block should not draw them too. */
    public static boolean sceneIsDrawing(BlockPos pos) {
        Long at = DRAWING.get(pos);
        if (at == null) return false;
        if (Util.getMillis() - at > FRESH_MS) {
            DRAWING.remove(pos);
            return false;
        }
        return true;
    }
}
