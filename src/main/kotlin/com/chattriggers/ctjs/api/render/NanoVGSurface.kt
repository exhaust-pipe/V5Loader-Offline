//? if >=26.3 {
/*package com.chattriggers.ctjs.api.render

import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.backend.opengl.GlTexture
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C

internal class NanoVGSurface : AutoCloseable {
    private var framebuffer = 0
    private var stencil = 0
    private var width = 0
    private var height = 0

    fun render(texture: GpuTexture, guiScale: Float, draw: () -> Unit) {
        val color = texture as? GlTexture ?: return
        val previousRead = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val previousDraw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        val previousRenderbuffer = GL11C.glGetInteger(GL30C.GL_RENDERBUFFER_BINDING)
        val scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST)
        val stencilTest = GL11C.glIsEnabled(GL11C.GL_STENCIL_TEST)
        val clearColor = FloatArray(4).also { GL11C.glGetFloatv(GL11C.GL_COLOR_CLEAR_VALUE, it) }
        val colorMask = IntArray(4).also { GL11C.glGetIntegerv(GL11C.GL_COLOR_WRITEMASK, it) }
        val stencilMask = GL11C.glGetInteger(GL11C.GL_STENCIL_WRITEMASK)
        val stencilBackMask = GL11C.glGetInteger(GL20C.GL_STENCIL_BACK_WRITEMASK)
        val clearStencil = GL11C.glGetInteger(GL11C.GL_STENCIL_CLEAR_VALUE)
        try {
            if (framebuffer == 0) framebuffer = GL30C.glGenFramebuffers()
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
            GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, color.glId(), 0)
            if (stencil == 0 || width != texture.getWidth(0) || height != texture.getHeight(0)) {
                if (stencil != 0) GL30C.glDeleteRenderbuffers(stencil)
                width = texture.getWidth(0)
                height = texture.getHeight(0)
                stencil = GL30C.glGenRenderbuffers()
                GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, stencil)
                GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH24_STENCIL8, width, height)
                GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_STENCIL_ATTACHMENT, GL30C.GL_RENDERBUFFER, stencil)
            }
            check(GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE)
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST)
            GL11C.glClearColor(0f, 0f, 0f, 0f)
            GL11C.glColorMask(true, true, true, true)
            GL11C.glStencilMask(-1)
            GL11C.glClearStencil(0)
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT or GL11C.GL_STENCIL_BUFFER_BIT)
            NVGRenderer.beginFrame(width / guiScale, height / guiScale, width, height)
            try {
                draw()
            } finally {
                NVGRenderer.endFrame()
            }
        } finally {
            GL11C.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
            GL11C.glClearStencil(clearStencil)
            GL11C.glColorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0)
            GL20C.glStencilMaskSeparate(GL11C.GL_FRONT, stencilMask)
            GL20C.glStencilMaskSeparate(GL11C.GL_BACK, stencilBackMask)
            if (scissor) GL11C.glEnable(GL11C.GL_SCISSOR_TEST) else GL11C.glDisable(GL11C.GL_SCISSOR_TEST)
            if (stencilTest) GL11C.glEnable(GL11C.GL_STENCIL_TEST) else GL11C.glDisable(GL11C.GL_STENCIL_TEST)
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, previousRenderbuffer)
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousRead)
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDraw)
        }
    }

    override fun close() {
        if (stencil != 0) GL30C.glDeleteRenderbuffers(stencil)
        if (framebuffer != 0) GL30C.glDeleteFramebuffers(framebuffer)
        stencil = 0
        framebuffer = 0
    }
}
*///?}
