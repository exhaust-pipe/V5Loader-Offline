package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.render.GuiRendererBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiRendererBackend.class, remap = false)
public abstract class GuiRendererBackendMixin {
    @Inject(method = "drawImageFromUrl(Ljava/lang/String;FFFFFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void ctjs$disableRemoteImages(String url, float x, float y, float width, float height, float radius, float imageAlpha, CallbackInfo ci) {
        ci.cancel();
    }
}
