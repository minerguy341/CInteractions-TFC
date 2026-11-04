package io.github.minerguy341.cinteractionstfc.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Colours for a 3D tool, taken from the pixels of the item's own texture, so every metal (and any addon's tool) gets
 * its own look. The hammer's head is the top-right of its sprite and its handle the bottom-left; tongs use the whole
 * sprite.
 */
final class ToolPalette {
    private static final Map<ResourceLocation, ToolPalette> CACHE = new HashMap<>();
    private static final int[] GREY = {0xFF8A8A8A, 0xFF9A9A9A, 0xFF7A7A7A};

    private final int[] head;
    private final int[] handle;
    private final int[] all;

    private ToolPalette(int[] head, int[] handle, int[] all) {
        this.head = head;
        this.handle = handle;
        this.all = all;
    }

    static ToolPalette of(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);
        ResourceLocation sprite = model.getParticleIcon(ModelData.EMPTY).contents().name();
        return CACHE.computeIfAbsent(sprite, ToolPalette::load);
    }

    /** Six face colours (-X, +X, -Y, +Y, -Z, +Z) for a part of the hammer's head, drawn from the sprite's head pixels. */
    int[] headFaces() { return faces(head, 0); }

    int[] handleFaces() { return faces(handle, 2); }

    int[] allFaces() { return faces(all, 1); }

    private static int[] faces(int[] colours, int seed) {
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) out[i] = colours[(seed + i) % colours.length];
        return out;
    }

    private static ToolPalette load(ResourceLocation sprite) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
        List<Integer> head = new ArrayList<>();
        List<Integer> handle = new ArrayList<>();
        List<Integer> all = new ArrayList<>();
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(file).open();
             NativeImage image = NativeImage.read(in)) {
            int size = Math.min(image.getWidth(), image.getHeight()); // an animated texture is a tall strip: first frame only
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if (((abgr >>> 24) & 255) < 128) continue;
                    int argb = 0xFF000000 | (abgr & 255) << 16 | (abgr >> 8 & 255) << 8 | (abgr >> 16 & 255);
                    all.add(argb);
                    double score = (double) x / size + (1.0 - (double) y / size); // 2 at the top right, 0 at the bottom left
                    if (score > 1.15) head.add(argb);
                    else if (score < 0.85) handle.add(argb);
                }
            }
        } catch (Exception problem) {
            return new ToolPalette(GREY, GREY, GREY);
        }
        return new ToolPalette(spread(head, all), spread(handle, all), spread(all, null));
    }

    /** Up to six colours, darkest to lightest, evenly picked from {@code colours} (or from {@code fallback} when empty). */
    private static int[] spread(List<Integer> colours, List<Integer> fallback) {
        List<Integer> source = colours.isEmpty() && fallback != null ? fallback : colours;
        if (source.isEmpty()) return GREY;
        List<Integer> sorted = new ArrayList<>(source);
        sorted.sort(Comparator.comparingInt(ToolPalette::luma));
        int count = Math.min(6, sorted.size());
        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = sorted.get(count == 1 ? 0 : i * (sorted.size() - 1) / (count - 1));
        return out;
    }

    private static int luma(int argb) {
        return (argb >> 16 & 255) * 3 + (argb >> 8 & 255) * 6 + (argb & 255);
    }
}
