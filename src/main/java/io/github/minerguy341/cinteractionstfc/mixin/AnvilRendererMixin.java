package io.github.minerguy341.cinteractionstfc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.minerguy341.cinteractionstfc.client.SceneVisibility;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFC draws the anvil's ingot and hammer on the block itself. While our first-person scene draws them (in its own,
 * interactive places) that copy would show twice, so skip the block's copy for that anvil.
 */
@Mixin(targets = "net.dries007.tfc.client.render.blockentity.AnvilBlockEntityRenderer", remap = false)
public abstract class AnvilRendererMixin {
    @Inject(method = "render(Lnet/dries007/tfc/common/blockentities/AnvilBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void cinteractionsTfc$hideWhileSceneDraws(AnvilBlockEntity anvil, float partialTick, PoseStack pose,
                                                      MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (SceneVisibility.sceneIsDrawing(anvil.getBlockPos())) ci.cancel();
    }
}
