package com.chattriggers.ctjs

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.client.KeyBind
import com.chattriggers.ctjs.api.client.Sound
import com.chattriggers.ctjs.api.commands.DynamicCommands
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.render.Image
import com.chattriggers.ctjs.api.render.NVGRenderer
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.api.world.Scoreboard
import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.engine.Console
import com.chattriggers.ctjs.engine.Register
import com.chattriggers.ctjs.internal.commands.StaticCommand
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.chattriggers.ctjs.internal.utils.Initializer
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import kotlin.concurrent.thread

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Client.referenceSystemTime = System.nanoTime()
        Initializer.initializers.forEach(Initializer::init)

        Runtime.getRuntime().addShutdownHook(Thread {
            TriggerType.GAME_UNLOAD.triggerAll()
            Console.close()
        })
    }

    companion object {
        const val MOD_ID = "ctjs"
        const val MOD_VERSION = "5.1.1"
        const val MODULES_FOLDER = "./config/ChatTriggers/modules"

        val configLocation = File("./config")
        val assetsDir = File(configLocation, "ChatTriggers/assets/").apply { mkdirs() }

        @JvmStatic
        var isLoaded = true
            private set

        internal val images = mutableListOf<Image>()
        internal val sounds = mutableListOf<Sound>()
        internal val isDevelopment = FabricLoader.getInstance().isDevelopmentEnvironment

        internal val json = Json {
            useAlternativeNames = true
            ignoreUnknownKeys = true
        }

        @JvmStatic
        fun unload(asCommand: Boolean = true) {
            Client.unpressKeys()
            TriggerType.WORLD_UNLOAD.triggerAll()
            TriggerType.GAME_UNLOAD.triggerAll()
            Scoreboard.clearCustom()

            isLoaded = false

            ModuleManager.teardown()
            KeyBind.clearKeyBinds()
            Register.clearCustomTriggers()
            StaticCommand.unregisterAll()
            DynamicCommands.unregisterAll()
            NVGRenderer.clearCallbacks()

            if (Config.clearConsoleOnLoad)
                Console.clear()

            Client.scheduleTask {
                images.forEach(Image::destroy)
                sounds.forEach(Sound::destroy)

                images.clear()
                sounds.clear()
            }

            if (asCommand)
                ChatLib.chat("&7Unloaded ChatTriggers")
        }

        @JvmStatic
        fun load(asCommand: Boolean = true) {
            Client.getMinecraft().options.save()
            unload(asCommand = false)

            if (asCommand)
                ChatLib.chat("&cReloading ChatTriggers...")

            thread {
                ModuleManager.setup()
                Client.getMinecraft().options.load()

                // Need to set isLoaded to true before running modules, otherwise custom triggers
                // activated at the top level will not work
                isLoaded = true

                ModuleManager.entryPass()

                if (asCommand)
                    ChatLib.chat("&aDone reloading!")

                TriggerType.GAME_LOAD.triggerAll()
                if (World.isLoaded())
                    TriggerType.WORLD_LOAD.triggerAll()
            }
        }
    }
}
