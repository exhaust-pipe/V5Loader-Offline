package com.chattriggers.ctjs.api.render

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.Context
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
//? if >=26.3 {
/*import com.chattriggers.ctjs.internal.mixins.PictureInPictureRendererAccessor
*///?}
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
    //? if >=26.3 {
    /*private val surface = NanoVGSurface()
    *///?}
    override fun getTranslateY(height: Int, guiScale: Int) = height / 2f
    override fun getRenderStateClass() = NanoVGPIP.State::class.java
    override fun getTextureLabel() = "V5 NanoVG"
    override fun renderToTexture(state: NanoVGPIP.State, poseStack: PoseStack, collector: SubmitNodeCollector) =
        //? if >=26.3 {
        /*surface.render((this as PictureInPictureRendererAccessor).`ctjs$getTexture`(), state.guiScale) {
            state.callback.run()
        }

    override fun close() {
        surface.close()
        super.close()
    }
    *///?} else {
        NanoVGPIP.render(state)
    //?}
}
//?}
