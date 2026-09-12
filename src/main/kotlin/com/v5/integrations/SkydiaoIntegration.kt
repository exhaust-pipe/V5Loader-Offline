package com.v5.integrations

import com.chattriggers.ctjs.api.client.Client
import com.chattriggers.ctjs.api.triggers.TriggerType
import com.chattriggers.ctjs.internal.engine.JSLoader
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

object SkydiaoIntegration {
    private val logger = LoggerFactory.getLogger("V5/Skydiao")
    private val reportedFailure = AtomicBoolean()
    private data class PacketFields(val type: Field, val message: Field)

    // Resolve public fields lazily so Skydiao remains an optional dependency.
    private val packetFields = object : ClassValue<PacketFields?>() {
        override fun computeValue(type: Class<*>): PacketFields? {
            return try {
                val fields = PacketFields(type.getField("packetType"), type.getField("message"))
                if (fields.type.type != String::class.java || fields.message.type != String::class.java) {
                    throw NoSuchFieldException("Expected String packetType and message fields")
                }
                fields
            } catch (e: ReflectiveOperationException) {
                reportFailure(e)
                null
            }
        }
    }

    @JvmStatic
    fun onPacket(packet: Any) {
        if (!JSLoader.hasTriggers(TriggerType.SKYDIAO_SYSTEM_MESSAGE)) return
        val fields = packetFields.get(packet.javaClass) ?: return
        val message = try {
            if (fields.type.get(packet) != "system") return
            fields.message.get(packet) as? String ?: return
        } catch (e: IllegalAccessException) {
            reportFailure(e)
            return
        }

        // Skydiao invokes its handler on the IRC reader thread, outside Minecraft's tick loop.
        Client.getMinecraft().execute {
            if (JSLoader.hasTriggers(TriggerType.SKYDIAO_SYSTEM_MESSAGE)) {
                TriggerType.SKYDIAO_SYSTEM_MESSAGE.triggerAll(message)
            }
        }
    }

    private fun reportFailure(error: ReflectiveOperationException) {
        if (reportedFailure.compareAndSet(false, true)) {
            logger.warn("Unable to read Skydiao IRC packets; this Skydiao version is not compatible with the integration", error)
        }
    }
}
