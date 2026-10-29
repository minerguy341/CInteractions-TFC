package io.github.minerguy341.cinteractionstfc.mixin;

import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.container.AnvilContainer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cinematic Interactions opens a station's menu through the block's own {@code getMenuProvider}, with no extra data.
 * TFC's anvil has no menu provider on the block (its block entity opens the menu itself) and its client menu reads
 * the block position from the open-screen data, so the stock path fails and the session is closed straight away.
 * Open it the way TFC does instead, for TFC anvils only.
 */
@Mixin(targets = "dev.dyslexic.cinematic.station.MenuActions", remap = false)
public abstract class MenuActionsMixin {
    @Inject(method = "open", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinteractionsTfc$openAnvil(ServerPlayer player, Sessions.Session session, CallbackInfoReturnable<Boolean> cir) {
        if (session.kind() != StationKind.ADDON) return;
        if (!(player.level().getBlockEntity(session.pos()) instanceof AnvilBlockEntity anvil)) return;
        boolean opened = player.openMenu(anvil, session.pos()).isPresent() && player.containerMenu instanceof AnvilContainer;
        cir.setReturnValue(opened);
    }
}
