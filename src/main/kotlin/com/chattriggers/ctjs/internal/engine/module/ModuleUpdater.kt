package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.engine.LogType
import com.chattriggers.ctjs.engine.printToConsole
import com.chattriggers.ctjs.internal.utils.Initializer

/** Compatibility for local module dependencies; this build never downloads code. */
object ModuleUpdater : Initializer {
    override fun init() {}
    fun updateModule(module: Module) {}
    fun importModule(moduleName: String, requiredBy: String? = null): List<Module> {
        val installed = ModuleManager.cachedModules.find { it.name.equals(moduleName, ignoreCase = true) }
        if (installed != null) {
            if (requiredBy != null) {
                installed.metadata.isRequired = true
                installed.requiredBy.add(requiredBy)
            }
        } else {
            "V5 Offline: install $moduleName manually in config/ChatTriggers/modules, then /ct load."
                .printToConsole(LogType.WARN)
        }
        return emptyList()
    }
}
