package com.chattriggers.ctjs.internal.utils

import com.chattriggers.ctjs.api.render.createNanoVGPIP
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry

internal object NanoVGPIPInitializer : Initializer {
    override fun init() {
        PictureInPictureRendererRegistry.register { context -> createNanoVGPIP(context) }
    }
}
