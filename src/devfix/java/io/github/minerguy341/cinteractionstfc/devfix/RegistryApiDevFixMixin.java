package io.github.minerguy341.cinteractionstfc.devfix;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development only; never part of the release jar.
 *
 * TFC Registry API (required by Hot or Not) scans every loaded mod by opening its file path as a zip filesystem. In a
 * Gradle dev run NeoForge, Minecraft and this project are not jar files, so that throws ProviderNotFoundException and
 * the game crashes during mod construction. Skip anything that is not a plain jar on the default filesystem.
 */
@Mixin(targets = "tfcregistryapi.util.Helpers", remap = false)
public abstract class RegistryApiDevFixMixin {
    @Inject(method = "lambda$discoverRegistryEnums$2", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void devfix$skipNonJarMods(Class<?> type, Map<?, ?> found, IModInfo mod, CallbackInfo ci) {
        IModFileInfo file = ModList.get().getModFileById(mod.getModId());
        Path path = file == null ? null : file.getFile().getFilePath();
        if (path == null || path.getFileSystem() != FileSystems.getDefault() || !Files.isRegularFile(path)) {
            ci.cancel();
        }
    }
}
