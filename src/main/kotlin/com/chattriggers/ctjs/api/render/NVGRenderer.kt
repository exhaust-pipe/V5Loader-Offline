package com.chattriggers.ctjs.api.render

import com.chattriggers.ctjs.api.client.MinecraftCompat
import net.minecraft.client.Minecraft
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.*
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryUtil
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode

/**
 * OpenGL NanoVG renderer used by the V5 GUI compatibility layer.
 *
 * It intentionally renders into the framebuffer already bound by Minecraft's GuiRenderer.
 * This avoids the version-specific RenderTarget/FBO internals that changed in 26.2 and also
 * keeps this backend OpenGL-only; Vulkan support is deliberately not provided.
 */
object NVGRenderer {
    private val mc = Minecraft.getInstance()
    private val nvgColor = NVGColor.malloc()
    private val nvgColor2 = NVGColor.malloc()
    private val nvgPaint = NVGPaint.malloc()

    private var checkTexId = 0
    private var hueTexId = 0

    val defaultFont by lazy {
        try {
            val stream = NVGRenderer::class.java.getResourceAsStream("/assets/v5/font.otf")
                ?: throw Exception("Could not find /assets/v5/font.otf in classpath")
            Font("Default", stream)
        } catch (e: Exception) {
            println("[V5] Failed to load font: ${e.message}")
            null
        }
    }

    private val fontMap = HashMap<Font, NVGFont>()
    private val fontBounds = FloatArray(4)
    private val imageCache = HashMap<String, CachedImage>()
    private val gifCache = ConcurrentHashMap<String, CachedGif>()
    private val glTextureCache = ConcurrentHashMap<Int, Int>()

    private var drawing = false
    @JvmField var vg = -1L

    private var savedReadFramebuffer = 0
    private var savedDrawFramebuffer = 0
    private var savedViewport = IntArray(4)
    private var savedProgram = 0
    private var savedActiveTexture = 0
    private var savedTexture = 0
    private var savedSampler = 0
    private var savedBlend = false
    private var savedCull = false
    private var savedDepth = false
    private var savedScissor = false
    private var savedBlendSrcRgb = 0
    private var savedBlendDstRgb = 0
    private var savedBlendSrcAlpha = 0
    private var savedBlendDstAlpha = 0

    data class GifData(
        val width: Int,
        val height: Int,
        val frameCount: Int,
        val delays: IntArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GifData) return false
            return width == other.width && height == other.height &&
                frameCount == other.frameCount && delays.contentEquals(other.delays)
        }

        override fun hashCode(): Int = 31 * (31 * (31 * width + height) + frameCount) + delays.contentHashCode()
    }

    private data class CachedGif(
        var frameIds: IntArray?,
        var rawFrames: ArrayList<ByteBuffer>?,
        val delays: IntArray,
        val width: Int,
        val height: Int,
        var refCount: Int,
    )

    private data class CachedImage(val nvgId: Int, var refCount: Int)
    private data class NVGFont(val id: Int, val buffer: ByteBuffer)

    private fun ensureInitialized() {
        if (vg != -1L) return
        vg = nvgCreate(NVG_ANTIALIAS or NVG_STENCIL_STROKES)
        if (vg == -1L) throw RuntimeException("Failed to initialize NanoVG")
    }

    @JvmStatic
    fun isDrawing(): Boolean = drawing

    @JvmStatic
    fun beginFrame(width: Float, height: Float) {
        if (mc.window.handle() == 0L || drawing) return
        ensureInitialized()

        savedReadFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        savedDrawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, savedViewport)
        savedProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM)
        savedActiveTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0)
        savedTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        savedSampler = GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING)
        savedBlend = GL11C.glIsEnabled(GL11C.GL_BLEND)
        savedCull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE)
        savedDepth = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST)
        savedScissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST)
        savedBlendSrcRgb = GL11C.glGetInteger(GL14_BLEND_SRC_RGB)
        savedBlendDstRgb = GL11C.glGetInteger(GL14_BLEND_DST_RGB)
        savedBlendSrcAlpha = GL11C.glGetInteger(GL14_BLEND_SRC_ALPHA)
        savedBlendDstAlpha = GL11C.glGetInteger(GL14_BLEND_DST_ALPHA)

        val target = MinecraftCompat.mainRenderTarget(mc)
        GL11C.glViewport(0, 0, target.width, target.height)
        GL33C.glBindSampler(0, 0)
        val pixelRatio = if (width > 0f) target.width.toFloat() / width else 1f

        nvgBeginFrame(vg, width, height, pixelRatio)
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        drawing = true
    }

    @JvmStatic
    fun endFrame() {
        if (!drawing || vg == -1L) return
        try {
            nvgEndFrame(vg)
        } finally {
            GL20C.glUseProgram(savedProgram)
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0)
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, savedTexture)
            GL33C.glBindSampler(0, savedSampler)
            GL13C.glActiveTexture(savedActiveTexture)
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, savedReadFramebuffer)
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, savedDrawFramebuffer)
            GL11C.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3])
            GL14C.glBlendFuncSeparate(savedBlendSrcRgb, savedBlendDstRgb, savedBlendSrcAlpha, savedBlendDstAlpha)
            setEnabled(GL11C.GL_BLEND, savedBlend)
            setEnabled(GL11C.GL_CULL_FACE, savedCull)
            setEnabled(GL11C.GL_DEPTH_TEST, savedDepth)
            setEnabled(GL11C.GL_SCISSOR_TEST, savedScissor)
            drawing = false
        }
    }

    private fun setEnabled(capability: Int, enabled: Boolean) {
        if (enabled) GL11C.glEnable(capability) else GL11C.glDisable(capability)
    }

    private fun applyColor(color: Int, target: NVGColor = nvgColor) {
        nvgRGBA(
            ((color shr 16) and 0xFF).toByte(),
            ((color shr 8) and 0xFF).toByte(),
            (color and 0xFF).toByte(),
            ((color shr 24) and 0xFF).toByte(),
            target,
        )
    }

    private fun applyGradient(x: Float, y: Float, w: Float, h: Float, color1: Int, color2: Int, direction: Any) {
        applyColor(color1, nvgColor)
        applyColor(color2, nvgColor2)
        when {
            direction.toString().contains("LeftToRight") -> nvgLinearGradient(vg, x, y, x + w, y, nvgColor, nvgColor2, nvgPaint)
            direction.toString().contains("TopToBottom") -> nvgLinearGradient(vg, x, y, x, y + h, nvgColor, nvgColor2, nvgPaint)
            direction.toString().contains("TopLeftToBottomRight") -> nvgLinearGradient(vg, x, y, x + w, y + h, nvgColor, nvgColor2, nvgPaint)
            direction.toString().contains("BottomLeftToTopRight") -> nvgLinearGradient(vg, x, y + h, x + w, y, nvgColor, nvgColor2, nvgPaint)
            else -> nvgLinearGradient(vg, x, y, x + w, y, nvgColor, nvgColor2, nvgPaint)
        }
    }

    @JvmStatic fun save() { if (drawing) nvgSave(vg) }
    @JvmStatic fun restore() { if (drawing) nvgRestore(vg) }
    @JvmStatic fun translate(x: Float, y: Float) { if (drawing) nvgTranslate(vg, x, y) }
    @JvmStatic fun rotate(angle: Float) { if (drawing) nvgRotate(vg, Math.toRadians(angle.toDouble()).toFloat()) }
    @JvmStatic fun scale(x: Float, y: Float) { if (drawing) nvgScale(vg, x, y) }
    @JvmStatic fun globalAlpha(alpha: Float) { if (drawing) nvgGlobalAlpha(vg, alpha.coerceIn(0f, 1f)) }

    @JvmStatic
    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        if (!drawing) return
        nvgBeginPath(vg); nvgRect(vg, x, y, w, h); applyColor(color); nvgFillColor(vg, nvgColor); nvgFill(vg)
    }

    @JvmStatic
    fun drawRoundedRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int) {
        if (!drawing) return
        nvgBeginPath(vg); nvgRoundedRect(vg, x, y, w, h, radius); applyColor(color); nvgFillColor(vg, nvgColor); nvgFill(vg)
    }

    @JvmStatic
    fun drawRoundedRectVaried(x: Float, y: Float, w: Float, h: Float, color: Int, tl: Float, tr: Float, br: Float, bl: Float) {
        if (!drawing) return
        nvgBeginPath(vg); nvgRoundedRectVarying(vg, x, y, w, h, tl, tr, br, bl); applyColor(color); nvgFillColor(vg, nvgColor); nvgFill(vg)
    }

    @JvmStatic
    fun drawCircle(x: Float, y: Float, radius: Float, color: Int) {
        if (!drawing) return
        nvgBeginPath(vg); nvgCircle(vg, x, y, radius); applyColor(color); nvgFillColor(vg, nvgColor); nvgFill(vg)
    }

    @JvmStatic
    fun drawDropShadow(x: Float, y: Float, w: Float, h: Float, radius: Float, blur: Float, spread: Float, color: Int) {
        if (!drawing) return
        applyColor(color, nvgColor); applyColor(0, nvgColor2)
        nvgBoxGradient(vg, x - spread, y - spread, w + spread * 2, h + spread * 2, radius + spread, blur, nvgColor, nvgColor2, nvgPaint)
        nvgBeginPath(vg)
        nvgRect(vg, x - spread - blur, y - spread - blur, w + spread * 2 + blur * 2, h + spread * 2 + blur * 2)
        nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    @JvmStatic
    @JvmOverloads
    fun drawHollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float = 0f) {
        if (!drawing) return
        nvgBeginPath(vg)
        if (radius > 0f) nvgRoundedRect(vg, x, y, w, h, radius) else nvgRect(vg, x, y, w, h)
        nvgStrokeWidth(vg, thickness); applyColor(color); nvgStrokeColor(vg, nvgColor); nvgStroke(vg)
    }

    @JvmStatic
    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
        if (!drawing) return
        nvgBeginPath(vg); nvgMoveTo(vg, x1, y1); nvgLineTo(vg, x2, y2); nvgStrokeWidth(vg, thickness)
        applyColor(color); nvgStrokeColor(vg, nvgColor); nvgStroke(vg)
    }

    @JvmStatic
    @JvmOverloads
    fun drawGradientRect(x: Float, y: Float, w: Float, h: Float, color1: Int, color2: Int, direction: Any, radius: Float = 0f) {
        if (!drawing) return
        nvgBeginPath(vg); if (radius > 0f) nvgRoundedRect(vg, x, y, w, h, radius) else nvgRect(vg, x, y, w, h)
        applyGradient(x, y, w, h, color1, color2, direction); nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    @JvmStatic
    @JvmOverloads
    fun drawHollowGradientRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color1: Int, color2: Int, direction: Any, radius: Float = 0f) {
        if (!drawing) return
        nvgBeginPath(vg); if (radius > 0f) nvgRoundedRect(vg, x, y, w, h, radius) else nvgRect(vg, x, y, w, h)
        nvgStrokeWidth(vg, thickness); applyGradient(x, y, w, h, color1, color2, direction); nvgStrokePaint(vg, nvgPaint); nvgStroke(vg)
    }

    @JvmStatic
    @JvmOverloads
    fun drawCheckerboard(x: Float, y: Float, w: Float, h: Float, radius: Float, size: Float = 4f) {
        if (!drawing) return
        if (checkTexId == 0) {
            val buffer = MemoryUtil.memAlloc(16)
            val c1 = 64.toByte(); val c2 = 115.toByte(); val a = 255.toByte()
            buffer.put(c1).put(c1).put(c1).put(a).put(c2).put(c2).put(c2).put(a)
            buffer.put(c2).put(c2).put(c2).put(a).put(c1).put(c1).put(c1).put(a).flip()
            checkTexId = nvgCreateImageRGBA(vg, 2, 2, NVG_IMAGE_REPEATX or NVG_IMAGE_REPEATY or NVG_IMAGE_NEAREST, buffer)
            MemoryUtil.memFree(buffer)
        }
        nvgImagePattern(vg, x, y, size * 2, size * 2, 0f, checkTexId, 1f, nvgPaint)
        nvgBeginPath(vg); nvgRoundedRect(vg, x, y, w, h, radius); nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    @JvmStatic
    fun drawHueBar(x: Float, y: Float, w: Float, h: Float, radius: Float) {
        if (!drawing) return
        if (hueTexId == 0) {
            val width = 128
            val buffer = MemoryUtil.memAlloc(width * 4)
            for (i in 0 until width) {
                val rgb = Color.HSBtoRGB(i.toFloat() / width, 1f, 1f)
                buffer.put((rgb shr 16 and 0xFF).toByte()).put((rgb shr 8 and 0xFF).toByte()).put((rgb and 0xFF).toByte()).put(255.toByte())
            }
            buffer.flip(); hueTexId = nvgCreateImageRGBA(vg, width, 1, 0, buffer); MemoryUtil.memFree(buffer)
        }
        nvgImagePattern(vg, x, y, w, 1f, 0f, hueTexId, 1f, nvgPaint)
        nvgBeginPath(vg); nvgRoundedRect(vg, x, y, w, h, radius); nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    @JvmStatic
    fun linearGradient(sx: Float, sy: Float, ex: Float, ey: Float, color1: Int, color2: Int) {
        if (!drawing) return
        applyColor(color1, nvgColor); applyColor(color2, nvgColor2); nvgLinearGradient(vg, sx, sy, ex, ey, nvgColor, nvgColor2, nvgPaint)
    }

    @JvmStatic
    fun setGlobalCompositeOperation(op: Int) { if (drawing) nvgGlobalCompositeOperation(vg, op) }

    @JvmStatic
    fun loadImage(path: String): String {
        ensureInitialized()
        synchronized(imageCache) { imageCache[path]?.let { it.refCount++; return path } }
        val bytes = readImage(path)
        val id = if (path.endsWith(".svg", true)) loadSvgImage(bytes, path) else loadRasterImage(bytes, path)
        synchronized(imageCache) { imageCache[path] = CachedImage(id, 1) }
        return path
    }

    @JvmStatic
    fun unloadImage(path: String) {
        synchronized(imageCache) {
            val cached = imageCache[path] ?: return
            if (--cached.refCount <= 0) { if (vg != -1L) nvgDeleteImage(vg, cached.nvgId); imageCache.remove(path) }
        }
    }

    @JvmStatic fun isImageLoaded(path: String): Boolean = synchronized(imageCache) { imageCache.containsKey(path) }

    private fun readImage(path: String): ByteArray {
        val trimmed = path.trim()
        val stream = when {
            trimmed.contains("://") -> throw IllegalArgumentException("Remote images are disabled")
            File(trimmed).isFile -> FileInputStream(trimmed)
            else -> NVGRenderer::class.java.getResourceAsStream(trimmed) ?: throw FileNotFoundException("Cannot find image: $trimmed")
        }
        return stream.use { it.readBytes() }
    }

    private fun loadRasterImage(bytes: ByteArray, identifier: String): Int {
        val w = IntArray(1); val h = IntArray(1); val channels = IntArray(1)
        val encoded = MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
        try {
            val pixels = stbi_load_from_memory(encoded, w, h, channels, 4) ?: throw RuntimeException("Failed to load image: $identifier")
            val id = nvgCreateImageRGBA(vg, w[0], h[0], 0, pixels)
            stbi_image_free(pixels)
            return id
        } finally { MemoryUtil.memFree(encoded) }
    }

    private fun loadSvgImage(bytes: ByteArray, identifier: String): Int {
        val svg = nsvgParse(String(bytes), "px", 96f) ?: throw RuntimeException("Failed to parse SVG: $identifier")
        val width = svg.width().toInt(); val height = svg.height().toInt()
        if (width <= 0 || height <= 0) { nsvgDelete(svg); throw RuntimeException("Invalid SVG dimensions: $identifier") }
        val buffer = MemoryUtil.memAlloc(width * height * 4)
        try {
            val rasterizer = nsvgCreateRasterizer()
            nsvgRasterize(rasterizer, svg, 0f, 0f, 1f, buffer, width, height, width * 4)
            val id = nvgCreateImageRGBA(vg, width, height, 0, buffer)
            nsvgDeleteRasterizer(rasterizer)
            return id
        } finally { nsvgDelete(svg); MemoryUtil.memFree(buffer) }
    }

    @JvmStatic
    @JvmOverloads
    fun drawImage(path: String, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, alpha: Float = 1f) {
        if (!drawing) return
        val cached = synchronized(imageCache) { imageCache[path] } ?: runCatching { loadImage(path) }.getOrNull()?.let { synchronized(imageCache) { imageCache[it] } } ?: return
        nvgImagePattern(vg, x, y, w, h, 0f, cached.nvgId, alpha.coerceIn(0f, 1f), nvgPaint)
        nvgBeginPath(vg); if (radius > 0f) nvgRoundedRect(vg, x, y, w, h, radius) else nvgRect(vg, x, y, w, h)
        nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    @JvmStatic
    @JvmOverloads
    fun drawImageFromUrl(url: String, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, alpha: Float = 1f) = Unit

    @JvmStatic
    fun loadGif(path: String): GifData? {
        gifCache[path]?.let { cached ->
            synchronized(cached) { cached.refCount++ }
            val count = cached.frameIds?.size ?: cached.rawFrames?.size ?: 0
            return GifData(cached.width, cached.height, count, cached.delays)
        }
        val file = File(path)
        if (!file.exists()) return null
        return try {
            FileInputStream(file).use { stream ->
                val readers = ImageIO.getImageReadersByFormatName("gif")
                if (!readers.hasNext()) return null
                val reader = readers.next(); reader.input = ImageIO.createImageInputStream(stream)
                val count = reader.getNumImages(true)
                val rawFrames = ArrayList<ByteBuffer>(count)
                val delays = IntArray(count)
                val first = reader.read(0)
                val master = BufferedImage(first.width, first.height, BufferedImage.TYPE_INT_ARGB)
                val graphics = master.createGraphics().apply { background = Color(0, 0, 0, 0) }
                repeat(count) { index ->
                    val frame = reader.read(index)
                    val metadata = reader.getImageMetadata(index)
                    val tree = metadata.getAsTree(metadata.nativeMetadataFormatName) as IIOMetadataNode
                    val control = tree.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
                    delays[index] = (control.getAttribute("delayTime").toInt() * 10).coerceAtLeast(10)
                    val descriptor = tree.getElementsByTagName("ImageDescriptor").item(0) as IIOMetadataNode
                    val fx = descriptor.getAttribute("imageLeftPosition").toInt(); val fy = descriptor.getAttribute("imageTopPosition").toInt()
                    graphics.drawImage(frame, fx, fy, null)
                    rawFrames += bufferedImageToByteBuffer(master)
                    if (control.getAttribute("disposalMethod") == "restoreToBackgroundColor") graphics.clearRect(fx, fy, frame.width, frame.height)
                }
                graphics.dispose(); reader.dispose()
                gifCache[path] = CachedGif(null, rawFrames, delays, first.width, first.height, 1)
                GifData(first.width, first.height, count, delays)
            }
        } catch (e: Exception) {
            println("[V5] Failed to load GIF $path: ${e.message}")
            null
        }
    }

    @JvmStatic
    fun unloadGif(path: String) {
        val cached = gifCache[path] ?: return
        if (synchronized(cached) { --cached.refCount <= 0 }) {
            if (vg != -1L) cached.frameIds?.forEach { nvgDeleteImage(vg, it) }
            cached.rawFrames?.forEach(MemoryUtil::memFree)
            gifCache.remove(path)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun drawGif(path: String, x: Float, y: Float, w: Float, h: Float, frameIndex: Int, radius: Float = 0f, alpha: Float = 1f) {
        if (!drawing) return
        val cached = gifCache[path] ?: return
        if (cached.frameIds == null) {
            val raw = cached.rawFrames ?: return
            cached.frameIds = IntArray(raw.size) { i -> nvgCreateImageRGBA(vg, cached.width, cached.height, 0, raw[i]).also { MemoryUtil.memFree(raw[i]) } }
            cached.rawFrames = null
        }
        val frames = cached.frameIds ?: return
        if (frames.isEmpty()) return
        nvgImagePattern(vg, x, y, w, h, 0f, frames[Math.floorMod(frameIndex, frames.size)], alpha.coerceIn(0f, 1f), nvgPaint)
        nvgBeginPath(vg); if (radius > 0f) nvgRoundedRect(vg, x, y, w, h, radius) else nvgRect(vg, x, y, w, h)
        nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    private fun bufferedImageToByteBuffer(image: BufferedImage): ByteBuffer {
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val buffer = MemoryUtil.memAlloc(pixels.size * 4)
        for (pixel in pixels) {
            buffer.put((pixel shr 16 and 0xFF).toByte()).put((pixel shr 8 and 0xFF).toByte()).put((pixel and 0xFF).toByte()).put((pixel shr 24 and 0xFF).toByte())
        }
        return buffer.flip() as ByteBuffer
    }

    @JvmStatic
    @JvmOverloads
    fun drawGLTexture(glTextureId: Int, texW: Int, texH: Int, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, alpha: Float = 1f) {
        if (!drawing) return
        val id = glTextureCache.getOrPut(glTextureId) { nvglCreateImageFromHandle(vg, glTextureId, texW, texH, NVG_IMAGE_NODELETE) }
        if (id == 0) return
        nvgImagePattern(vg, x, y, w, h, 0f, id, alpha, nvgPaint)
        nvgBeginPath(vg); if (radius > 0f) nvgRoundedRect(vg, x, y, w, h, radius) else nvgRect(vg, x, y, w, h); nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    @JvmStatic
    @JvmOverloads
    fun drawGLTextureRegion(glTextureId: Int, textureWidth: Int, textureHeight: Int, subX: Int, subY: Int, subW: Int, subH: Int, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, alpha: Float = 1f) {
        if (!drawing) return
        val id = glTextureCache.getOrPut(glTextureId) { nvglCreateImageFromHandle(vg, glTextureId, textureWidth, textureHeight, NVG_IMAGE_NODELETE) }
        if (id == 0) return
        val sw = subW.toFloat() / textureWidth; val sh = subH.toFloat() / textureHeight
        val iw = w / sw; val ih = h / sh
        val ix = x - iw * (subX.toFloat() / textureWidth); val iy = y - ih * (subY.toFloat() / textureHeight)
        nvgImagePattern(vg, ix, iy, iw, ih, 0f, id, alpha, nvgPaint)
        nvgBeginPath(vg); if (radius > 0f) nvgRoundedRect(vg, x, y, w, h, radius) else nvgRect(vg, x, y, w, h); nvgFillPaint(vg, nvgPaint); nvgFill(vg)
    }

    @JvmStatic
    fun invalidateGLTexture(glTextureId: Int) { glTextureCache.remove(glTextureId)?.takeIf { it != 0 && vg != -1L }?.let { nvgDeleteImage(vg, it) } }

    @JvmStatic fun scissor(x: Float, y: Float, w: Float, h: Float) { if (drawing) nvgIntersectScissor(vg, x, y, w, h) }
    @JvmStatic fun pushScissor(x: Float, y: Float, w: Float, h: Float) { if (drawing) { nvgSave(vg); nvgIntersectScissor(vg, x, y, w, h) } }
    @JvmStatic fun popScissor() { if (drawing) nvgRestore(vg) }
    @JvmStatic fun resetScissor() { if (drawing) nvgResetScissor(vg) }

    @JvmStatic
    @JvmOverloads
    fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: Font? = defaultFont, align: Int) {
        if (font == null || !drawing) return
        nvgFontSize(vg, size); nvgFontFaceId(vg, getFontId(font)); nvgTextAlign(vg, align); applyColor(color); nvgFillColor(vg, nvgColor); nvgText(vg, x, y, text)
    }

    @JvmStatic
    @JvmOverloads
    fun textWidth(text: String, size: Float, font: Font? = defaultFont): Float {
        if (font == null) return 0f
        ensureInitialized()
        nvgFontSize(vg, size); nvgFontFaceId(vg, getFontId(font)); return nvgTextBounds(vg, 0f, 0f, text, fontBounds)
    }

    private fun getFontId(font: Font): Int = fontMap.getOrPut(font) {
        val buffer = font.buffer()
        NVGFont(nvgCreateFontMem(vg, font.name, buffer, false), buffer)
    }.id

    @JvmStatic
    fun clearImageCache() {
        if (vg != -1L) synchronized(imageCache) { imageCache.values.forEach { nvgDeleteImage(vg, it.nvgId) }; imageCache.clear() }
        else synchronized(imageCache) { imageCache.clear() }
        gifCache.values.forEach { gif ->
            if (vg != -1L) gif.frameIds?.forEach { nvgDeleteImage(vg, it) }
            gif.rawFrames?.forEach(MemoryUtil::memFree)
        }
        gifCache.clear()
        if (vg != -1L) glTextureCache.values.filter { it != 0 }.forEach { nvgDeleteImage(vg, it) }
        glTextureCache.clear()
        if (vg != -1L && checkTexId != 0) nvgDeleteImage(vg, checkTexId)
        if (vg != -1L && hueTexId != 0) nvgDeleteImage(vg, hueTexId)
        checkTexId = 0; hueTexId = 0
    }

    @JvmStatic fun getCacheStats() = "Images: ${imageCache.size}, GIFs: ${gifCache.size}, GL: ${glTextureCache.size}"

    @JvmStatic
    fun destroy() {
        if (drawing) endFrame()
        clearImageCache()
        fontMap.clear()
        if (vg != -1L) { nvgDelete(vg); vg = -1L }
    }

    private const val GL14_BLEND_SRC_RGB = 0x80C9
    private const val GL14_BLEND_DST_RGB = 0x80C8
    private const val GL14_BLEND_SRC_ALPHA = 0x80CB
    private const val GL14_BLEND_DST_ALPHA = 0x80CA
}
