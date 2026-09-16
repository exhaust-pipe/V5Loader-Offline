package com.chattriggers.ctjs.api.render

import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * NanoVG-backed compatibility layer for the JavaScript-facing [Render2D] API.
 *
 * The 5.2 script API remains unchanged while the Skija/PIP renderer is removed.
 */
open class GuiRendererBackend {
    private val callbacks = CopyOnWriteArrayList<Runnable>()
    private val preCallbacks = CopyOnWriteArrayList<Runnable>()
    private val deferredRenderWork = ConcurrentLinkedQueue<Runnable>()
    private var backendDrawing = false

    @JvmField
    val defaultFont = NVGRenderer.defaultFont ?: Font("Default", "/assets/v5/font.otf")

    @JvmField val ALIGN_CENTER = 2
    @JvmField val ALIGN_MIDDLE = 16

    data class GifData(val width: Int, val height: Int, val frameCount: Int, val delays: IntArray) {
        override fun equals(other: Any?) = other is GifData && width == other.width && height == other.height &&
            frameCount == other.frameCount && delays.contentEquals(other.delays)
        override fun hashCode() = 31 * (31 * (31 * width + height) + frameCount) + delays.contentHashCode()
    }

    fun registerV5Render(callback: Runnable) = callback.also(callbacks::add)
    fun unregisterV5Render(callback: Runnable) { callbacks -= callback }
    fun registerV5PreRender(callback: Runnable) = callback.also(preCallbacks::add)
    fun unregisterV5PreRender(callback: Runnable) { preCallbacks -= callback }
    fun clearCallbacks() { callbacks.clear(); preCallbacks.clear() }

    fun runPreDrawables() = runFrame(preCallbacks)
    fun runDrawables() = runFrame(callbacks)

    private fun runFrame(list: List<Runnable>) {
        // GuiRenderer invokes this on Minecraft's render thread. Drain GL/NanoVG cleanup queued
        // by the CTJS loader thread before deciding whether there is anything to draw.
        while (true) {
            val work = deferredRenderWork.poll() ?: break
            try { work.run() } catch (error: Exception) { error.printStackTrace() }
        }

        if (list.isEmpty()) return
        val window = Minecraft.getInstance().window
        backendDrawing = true
        NVGRenderer.beginFrame(window.guiScaledWidth.toFloat(), window.guiScaledHeight.toFloat())
        try {
            list.forEach { callback ->
                try { callback.run() } catch (error: Exception) { error.printStackTrace() }
            }
        } finally {
            NVGRenderer.endFrame()
            backendDrawing = false
        }
    }

    // Keep these names so Render2D does not need a broad API rewrite in this experiment.
    internal fun translateSkija(x: Float, y: Float): Boolean {
        if (!backendDrawing) return false
        NVGRenderer.translate(x, y)
        return true
    }

    internal fun scaleSkija(x: Float, y: Float): Boolean {
        if (!backendDrawing) return false
        NVGRenderer.scale(x, y)
        return true
    }

    internal fun rotateSkija(degrees: Float): Boolean {
        if (!backendDrawing) return false
        NVGRenderer.rotate(degrees)
        return true
    }

    fun blurBackground() = Unit
    fun save() = NVGRenderer.save()
    fun restore() = NVGRenderer.restore()
    fun globalAlpha(value: Float) = NVGRenderer.globalAlpha(value)
    fun scissor(x: Float, y: Float, width: Float, height: Float) = NVGRenderer.scissor(x, y, width, height)
    fun resetScissor() = NVGRenderer.resetScissor()
    fun pushScissor(x: Float, y: Float, width: Float, height: Float) = NVGRenderer.pushScissor(x, y, width, height)
    fun popScissor() = NVGRenderer.popScissor()

    fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Int) =
        NVGRenderer.drawRect(x, y, width, height, color)

    fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Int) =
        NVGRenderer.drawRoundedRect(x, y, width, height, radius, color)

    fun drawRoundedRectVaried(x: Float, y: Float, width: Float, height: Float, color: Int, tl: Float, tr: Float, br: Float, bl: Float) =
        NVGRenderer.drawRoundedRectVaried(x, y, width, height, color, tl, tr, br, bl)

    fun drawCircle(x: Float, y: Float, radius: Float, color: Int) = NVGRenderer.drawCircle(x, y, radius, color)

    @JvmOverloads
    fun drawHollowRect(x: Float, y: Float, width: Float, height: Float, thickness: Float, color: Int, radius: Float = 0f) =
        NVGRenderer.drawHollowRect(x, y, width, height, thickness, color, radius)

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) =
        NVGRenderer.drawLine(x1, y1, x2, y2, thickness, color)

    fun drawDropShadow(x: Float, y: Float, width: Float, height: Float, radius: Float, blur: Float, spread: Float, color: Int) =
        NVGRenderer.drawDropShadow(x, y, width, height, radius, blur, spread, color)

    @JvmOverloads
    fun drawGradientRect(x: Float, y: Float, width: Float, height: Float, color1: Int, color2: Int, direction: Any, radius: Float = 0f) =
        NVGRenderer.drawGradientRect(x, y, width, height, color1, color2, direction, radius)

    @JvmOverloads
    fun drawHollowGradientRect(x: Float, y: Float, width: Float, height: Float, thickness: Float, color1: Int, color2: Int, direction: Any, radius: Float = 0f) =
        NVGRenderer.drawHollowGradientRect(x, y, width, height, thickness, color1, color2, direction, radius)

    @JvmOverloads
    fun drawCheckerboard(x: Float, y: Float, width: Float, height: Float, radius: Float, size: Float = 4f) =
        NVGRenderer.drawCheckerboard(x, y, width, height, radius, size)

    fun drawHueBar(x: Float, y: Float, width: Float, height: Float, radius: Float) =
        NVGRenderer.drawHueBar(x, y, width, height, radius)

    fun getDefaultFont() = defaultFont

    @JvmOverloads
    fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: Font? = defaultFont, align: Int) =
        NVGRenderer.text(text, x, y, size, color, font, align)

    @JvmOverloads
    fun textWidth(text: String, size: Float, font: Font? = defaultFont): Float {
        // Module construction happens on the CTJS loader thread. Neither NanoVG nor Minecraft's
        // Font.width() is safe there: Minecraft may lazily bake/upload glyphs and touch RenderSystem.
        if (backendDrawing && NVGRenderer.isDrawing()) return NVGRenderer.textWidth(text, size, font)
        return estimateTextWidth(text, size)
    }

    /**
     * Pure CPU approximation used only before the first GUI render pass. The exact NanoVG metrics
     * are used while rendering. Keeping this deliberately simple is preferable to touching any
     * Minecraft font/glyph API from the CTJS loader thread.
     */
    private fun estimateTextWidth(text: String, size: Float): Float {
        var units = 0f
        var skipFormattingCode = false
        for (char in text) {
            if (skipFormattingCode) {
                skipFormattingCode = false
                continue
            }
            if (char == '\u00a7') {
                skipFormattingCode = true
                continue
            }

            units += when {
                char == ' ' -> 0.33f
                char in "ilI.,'`:;!|" -> 0.28f
                char in "mwMW@#%&" -> 0.82f
                char.code >= 0x2E80 -> 1.0f
                char.isUpperCase() -> 0.62f
                char.isDigit() -> 0.56f
                else -> 0.54f
            }
        }
        return units * size.coerceAtLeast(0f)
    }

    // Loading script modules must not allocate OpenGL/NanoVG textures. drawImage() already
    // performs a lazy load while a NanoVG frame is active, so keep loadImage() as a handle-only
    // compatibility call.
    fun loadImage(path: String) = path

    // Resource destruction can call nvgDelete* and must therefore run on the render thread too.
    // Queue cleanup requested by /ct load or other script-loader paths for the next render hook.
    fun unloadImage(path: String) { deferredRenderWork.add(Runnable { NVGRenderer.unloadImage(path) }) }
    fun isImageLoaded(path: String) = NVGRenderer.isImageLoaded(path)

    @JvmOverloads
    fun drawImage(path: String, x: Float, y: Float, width: Float, height: Float, radius: Float = 0f, imageAlpha: Float = 1f) =
        NVGRenderer.drawImage(path, x, y, width, height, radius, imageAlpha)

    @JvmOverloads
    fun drawImageFromUrl(url: String, x: Float, y: Float, width: Float, height: Float, radius: Float = 0f, imageAlpha: Float = 1f) =
        NVGRenderer.drawImageFromUrl(url, x, y, width, height, radius, imageAlpha)

    fun loadGif(path: String): GifData? = NVGRenderer.loadGif(path)?.let { GifData(it.width, it.height, it.frameCount, it.delays) }
    fun unloadGif(path: String) { deferredRenderWork.add(Runnable { NVGRenderer.unloadGif(path) }) }

    @JvmOverloads
    fun drawGif(path: String, x: Float, y: Float, width: Float, height: Float, frameIndex: Int, radius: Float = 0f, imageAlpha: Float = 1f) =
        NVGRenderer.drawGif(path, x, y, width, height, frameIndex, radius, imageAlpha)

    fun clearImageCache() { deferredRenderWork.add(Runnable { NVGRenderer.clearImageCache() }) }
    fun getCacheStats() = NVGRenderer.getCacheStats()
    fun destroy() { deferredRenderWork.add(Runnable { NVGRenderer.destroy() }) }
}
