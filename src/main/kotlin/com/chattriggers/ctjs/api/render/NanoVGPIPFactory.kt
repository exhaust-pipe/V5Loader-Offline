package com.chattriggers.ctjs.api.render

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.Context
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
//? if <26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?} else {
import net.minecraft.client.renderer.SubmitNodeCollector
//?}

//? if <26.2 {
/*internal fun createNanoVGPIP(context: Context): PictureInPictureRenderer<*> {
    val buffers = context.bufferSource()
    return NanoVGPIPRenderer(buffers)
}

private class NanoVGPIPRenderer(buffers: MultiBufferSource.BufferSource) :
    PictureInPictureRenderer<NanoVGPIP.State>(buffers) {
    override fun getTranslateY(height: Int, guiScale: Int) = height / 2f
    override fun getRenderStateClass() = NanoVGPIP.State::class.java
    override fun getTextureLabel() = "V5 NanoVG"
    override fun renderToTexture(state: NanoVGPIP.State, poseStack: PoseStack) = NanoVGPIP.render(state)
}
*///?} else {
internal fun createNanoVGPIP(context: Context): PictureInPictureRenderer<*> = NanoVGPIPRenderer()

private class NanoVGPIPRenderer : PictureInPictureRenderer<NanoVGPIP.State>() {
    override fun getTranslateY(height: Int, guiScale: Int) = height / 2f
    override fun getRenderStateClass() = NanoVGPIP.State::class.java
    override fun getTextureLabel() = "V5 NanoVG"
    override fun renderToTexture(state: NanoVGPIP.State, poseStack: PoseStack, collector: SubmitNodeCollector) =
        NanoVGPIP.render(state)
}
//?}
