package com.chattriggers.ctjs

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.KeyBind
import com.chattriggers.ctjs.api.client.Sound
import com.chattriggers.ctjs.api.commands.DynamicCommands
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.render.Image
import com.chattriggers.ctjs.api.render.Render2D
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.api.world.pathfinding.PathManager
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.engine.Register
import com.chattriggers.ctjs.internal.commands.StaticCommand
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.utils.Initializer
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import kotlin.concurrent.thread

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Client.referenceSystemTime = System.nanoTime()
        Initializer.initializers.forEach(Initializer::init)
        Config.loadData()

        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            Render2D.destroy()
            TriggerType.GAME_UNLOAD.triggerAll()
            Console.close()
        }
    }

    companion object {
        const val MOD_ID = "ctjs"
        const val WEBSITE_ROOT = "https://www.chattriggers.com"
        val MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().metadata.version.friendlyString
        const val MODULES_FOLDER = "./config/ChatTriggers/modules"

        val configLocation = File("./config")
        val assetsDir = File(configLocation, "ChatTriggers/assets/").apply { mkdirs() }

        @JvmStatic
        @Volatile
        var isLoaded = true
            private set

        @JvmStatic
        @Volatile
        var scriptGeneration = 1L
            private set

        @JvmStatic
        fun isScriptGenerationCurrent(generation: Long): Boolean =
            isLoaded && generation == scriptGeneration

        @Volatile
        private var isReloading = false

        internal val images = mutableListOf<Image>()
        internal val sounds = mutableListOf<Sound>()
        internal val isDevelopment = FabricLoader.getInstance().isDevelopmentEnvironment

        internal val json = Json {
            useAlternativeNames = true
            ignoreUnknownKeys = true
        }

        private data class Resources(val images: List<Image>, val sounds: List<Sound>)

        private fun resetScriptRuntimeState() {
            Client.isFreecam = false
            Client.isFreelook = false
            Client.isUngrabbed = false
            Client.isInputLocked = false
            Client.isMacroEnabled = false
            Client.isForcePerspective = false
            Client.limitFps = false
            Client.muteGame = false
            Client.renderLimiter = Client.RenderLimiter.OFF
            Client.clearCameraRotation()
            Client.cameraPosition = null
            Client.freelookDistance = 4.0
            Client.spectatedEntity = null
            Client.setNameProcessor(null)
            Client.setNameReplacement(null, null)

            PathManager.cancelSearch()
            PathManager.clear()
        }

        private fun teardown(asCommand: Boolean): Resources {
            val resources = Resources(images.toList(), sounds.toList())

            Client.unpressKeys()
            TriggerType.WORLD_UNLOAD.triggerAll()
            TriggerType.GAME_UNLOAD.triggerAll()
            Scoreboard.clearCustom()
            isLoaded = false

            // Invalidate async work from the previous script generation before loading replacements.
            scriptGeneration++
            resetScriptRuntimeState()

            ModuleManager.teardown()
            KeyBind.clearKeyBinds()
            Register.clearCustomTriggers()
            StaticCommand.unregisterAll()
            DynamicCommands.unregisterAll()
            Render2D.clearCallbacks()

            if (Config.clearConsoleOnLoad) Console.clear()
            if (asCommand) ChatLib.chat("&7Unloaded ChatTriggers")

            return resources
        }

        private fun destroyResources(resources: Resources) {
            Render2D.destroy()
            resources.images.forEach(Image::destroy)
            resources.sounds.forEach(Sound::destroy)
        }

        @JvmStatic
        fun unload(asCommand: Boolean = true) {
            val resources = teardown(asCommand)
            Client.scheduleTask { destroyResources(resources) }
        }

        @JvmStatic
        @Synchronized
        fun load(asCommand: Boolean = true) {
            if (isReloading) return
            isReloading = true

            Client.getMinecraft().options.save()
            val resources = teardown(asCommand = false)
            if (asCommand) ChatLib.chat("&cReloading ChatTriggers...")

            // Complete destruction of the previous generation on the client thread before
            // replacement modules are allowed to allocate images, sounds, or renderer caches.
            Client.scheduleTask {
                destroyResources(resources)
                thread(name = "CTJS reload") {
                    try {
                        ModuleManager.setup()
                        Client.getMinecraft().options.load()
                        isLoaded = true
                        ModuleManager.entryPass()

                        if (asCommand) ChatLib.chat("&aDone reloading!")
                        TriggerType.GAME_LOAD.triggerAll()
                        if (World.isLoaded()) TriggerType.WORLD_LOAD.triggerAll()
                    } finally {
                        isReloading = false
                    }
                }
            }
        }
    }
}
