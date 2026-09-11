package com.chattriggers.ctjs.internal.console

import com.chattriggers.ctjs.api.Config
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.internal.utils.Initializer
import org.slf4j.LoggerFactory
import java.awt.Color

/** Local logging avoids an unauthenticated socket that could evaluate JavaScript. */
object ConsoleHostProcess : Initializer {
    private val logger = LoggerFactory.getLogger("V5-Offline-Scripts")
    override fun init() {}
    fun clear() {}
    fun println(obj: Any, logType: LogType, end: String, customColor: Color?) {
        when (logType) {
            LogType.ERROR -> logger.error("{}", obj.toString())
            LogType.WARN -> logger.warn("{}", obj.toString())
            else -> logger.info("{}", obj.toString())
        }
    }
    fun printStackTrace(error: Throwable) { logger.error("Script error", error) }
    fun show() { ChatLib.chat("&7V5 Offline: script output is in logs/latest.log.") }
    fun close() {}
    fun onConsoleSettingsChanged(settings: Config.ConsoleSettings) {}
}
