package com.chattriggers.ctjs.api.render

internal object GL14C {
    fun glBlendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
        org.lwjgl.opengl.GL14C.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
    }
}
