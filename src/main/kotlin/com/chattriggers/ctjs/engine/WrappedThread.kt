package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.internal.engine.JSContextFactory
import org.mozilla.javascript.Context
import java.util.concurrent.ForkJoinPool

@Suppress("unused")
class WrappedThread(private val task: Runnable) {
    private val generation = CTJS.scriptGeneration

    fun start() {
        ForkJoinPool.commonPool().execute {
            if (!CTJS.isScriptGenerationCurrent(generation)) return@execute

            activeGeneration.set(generation)
            try {
                JSContextFactory.enterContext()
                try {
                    if (CTJS.isScriptGenerationCurrent(generation)) task.run()
                } finally {
                    Context.exit()
                }
            } catch (e: InterruptedException) {
                // Script reload invalidates sleeping work from the previous generation.
                if (CTJS.isScriptGenerationCurrent(generation)) e.printStackTraceToConsole()
            } catch (e: Throwable) {
                if (CTJS.isScriptGenerationCurrent(generation)) e.printStackTraceToConsole()
            } finally {
                activeGeneration.remove()
            }
        }
    }

    // Provide the following methods as no-ops to avoid breaking
    // changes, as this class use to extend Thread
    fun run() {}
    fun stop() {}
    fun interrupt() {}
    fun isInterrupted() = false
    fun destroy() {}
    fun isAlive() = true
    fun suspend() {}
    fun resume() {}
    fun setDaemon(on: Boolean) {}
    fun getId() = 0L

    companion object {
        private val activeGeneration = ThreadLocal<Long?>()

        @JvmStatic
        @JvmOverloads
        fun sleep(millis: Long, nanos: Int = 0) {
            Thread.sleep(millis, nanos)
            val generation = activeGeneration.get()
            if (generation != null && !CTJS.isScriptGenerationCurrent(generation)) {
                throw InterruptedException("Script generation expired")
            }
        }

        @JvmStatic
        fun currentThread(): Thread = Thread.currentThread()
    }
}
