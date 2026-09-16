package com.chattriggers.ctjs.api.render

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.joml.Matrix3x2f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Inserts a NanoVG callback into Minecraft's ordered GUI render state.
 *
 * This keeps NanoVG-only visuals in the same ordering model as vanilla GUI elements without
 * bringing back Skija. The PIP target is full-screen and transparent, so a callback can use the
 * same logical GUI coordinates as normal Render2D code and then be composited at the exact point
 * where this state was submitted.
 */
object NanoVGPIP {
    private var framebufferWarningPrinted = false

    @JvmStatic
    fun draw(graphics: GuiGraphicsExtractor, callback: Runnable) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val pose = Matrix3x2f(graphics.pose())
        val screen = ScreenRectangle(0, 0, width, height).transformMaxBounds(pose)
        val scissor = graphics.scissorStack.peek()
        val bounds = scissor?.intersection(screen) ?: screen
        if (bounds.width <= 0 || bounds.height <= 0) return

        val scale = Minecraft.getInstance().window.guiScale.toFloat()
        graphics.guiRenderState.addPicturesInPictureState(
            State(width, height, scale, pose, scissor, bounds, callback),
        )
    }

    internal fun render(state: State) {
        val colorTarget = RenderSystem.outputColorTextureOverride ?: return
        val framebuffer = resolveFramebuffer(colorTarget) ?: return

        val previousReadFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val previousDrawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        try {
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, framebuffer)
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, framebuffer)

            NVGRenderer.beginFrame(state.width.toFloat(), state.height.toFloat())
            try {
                state.callback.run()
            } catch (error: Throwable) {
                error.printStackTrace()
            } finally {
                NVGRenderer.endFrame()
            }
        } finally {
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer)
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer)
        }
    }

    /**
     * Mojang moved FBO ownership between texture and texture-view implementations across 26.1/26.2.
     * Keep that version-specific OpenGL detail behind reflection while the public PIP API remains
     * stable. This experiment is intentionally OpenGL-only, so failure simply skips the layer.
     */
    private fun resolveFramebuffer(colorTarget: Any): Int? {
        return try {
            val device = RenderSystem.getDevice()
            val backendField = findField(device.javaClass, "backend")
                ?: throw IllegalStateException("GpuDevice backend field not found")
            val backend = backendField.apply { isAccessible = true }.get(device)
                ?: throw IllegalStateException("GpuDevice backend is null")
            val directStateAccessMethod = findMethod(backend.javaClass, "directStateAccess", 0)
                ?: throw IllegalStateException("OpenGL directStateAccess method not found")
            val directStateAccess = directStateAccessMethod.apply { isAccessible = true }.invoke(backend)

            val viewMethod = findMethod(colorTarget.javaClass, "getFbo", 2)
            if (viewMethod != null) {
                return (viewMethod.apply { isAccessible = true }.invoke(colorTarget, directStateAccess, null) as Number).toInt()
            }

            val textureMethod = findMethod(colorTarget.javaClass, "texture", 0)
                ?: throw IllegalStateException("Output texture accessor not found")
            val texture = textureMethod.apply { isAccessible = true }.invoke(colorTarget)
                ?: throw IllegalStateException("Output texture is null")
            val textureFboMethod = findMethod(texture.javaClass, "getFbo", 2)
                ?: throw IllegalStateException("OpenGL framebuffer accessor not found")
            (textureFboMethod.apply { isAccessible = true }.invoke(texture, directStateAccess, null) as Number).toInt()
        } catch (error: Throwable) {
            if (!framebufferWarningPrinted) {
                framebufferWarningPrinted = true
                println("[V5] Failed to resolve NanoVG PIP framebuffer: ${error.message}")
            }
            null
        }
    }

    private fun findField(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    private fun findMethod(type: Class<*>, name: String, parameterCount: Int): Method? {
        var current: Class<*>? = type
        while (current != null) {
            current.declaredMethods.firstOrNull { it.name == name && it.parameterCount == parameterCount }?.let { return it }
            current = current.superclass
        }
        return null
    }

    class State(
        val width: Int,
        val height: Int,
        val guiScale: Float,
        private val poseMatrix: Matrix3x2f,
        private val scissor: ScreenRectangle?,
        private val area: ScreenRectangle,
        val callback: Runnable,
    ) : PictureInPictureRenderState {
        override fun x0() = 0
        override fun y0() = 0
        override fun x1() = width
        override fun y1() = height
        override fun scale() = 1f
        override fun pose() = poseMatrix
        override fun scissorArea() = scissor
        override fun bounds() = area
    }
}
