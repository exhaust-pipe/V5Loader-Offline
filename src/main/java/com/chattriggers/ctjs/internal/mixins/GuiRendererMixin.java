package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.render.Render2D;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the NanoVG-backed Render2D callbacks in the actual GUI render pass. */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        Render2D.INSTANCE.runPreDrawables();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void afterRender(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        Render2D.INSTANCE.runDrawables();
    }
}
