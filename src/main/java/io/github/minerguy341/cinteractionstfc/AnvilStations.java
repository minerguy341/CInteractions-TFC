package io.github.minerguy341.cinteractionstfc;

import dev.dyslexic.cinematic.api.AddonStations;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.dries007.tfc.common.container.AnvilContainer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Which TFC blocks get an anvil scene, and the scene id each one uses. */
public final class AnvilStations {
    private static final Pattern ANVIL = Pattern.compile("(rock|metal)/anvil/.+");
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private AnvilStations() {}

    /** Every registered TFC anvil block (stone and metal). Call after registries are filled. */
    public static List<ResourceLocation> anvilBlocks() {
        List<ResourceLocation> blocks = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if (id.getNamespace().equals("tfc") && ANVIL.matcher(id.getPath()).matches()) blocks.add(id);
        }
        return blocks;
    }

    public static ResourceLocation sceneId(ResourceLocation block) {
        return ResourceLocation.fromNamespaceAndPath(CInteractionsTfc.MOD_ID, block.getPath().replace('/', '_'));
    }

    /** Both sides. */
    public static void registerCommon() {
        for (ResourceLocation block : anvilBlocks()) {
            AddonStations.register(new AddonStations.Definition(sceneId(block), block, AnvilContainer.class, state -> SIDES));
        }
    }
}
