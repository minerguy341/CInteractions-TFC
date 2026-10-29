package io.github.minerguy341.cinteractionstfc.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Finds TFC Hot or Not tongs in either hand. Only the item registry id is checked, so this never touches Hot or Not
 * classes and is safe when the mod is absent (no tongs are found, so none are drawn).
 */
final class Tongs {
    private Tongs() {}

    static ItemStack inHands(Player player) {
        if (player == null) return ItemStack.EMPTY;
        if (is(player.getMainHandItem())) return player.getMainHandItem();
        if (is(player.getOffhandItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    static boolean is(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("tfchotornot") && id.getPath().contains("tongs");
    }
}
