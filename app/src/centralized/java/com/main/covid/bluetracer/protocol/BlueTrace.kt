package io.bluetrace.opentrace.protocol

import com.main.covid.BuildConfig
import com.main.covid.bluetracer.protocol.v2.BlueTraceV2
import java.util.*

//we're loosely using the characteristic IDs to indicate the protocol version?
//will this code become too bloated in time?
object BlueTrace {

    private val characteristicToProtocolVersionMap = mapOf<UUID, Int>(
        UUID.fromString(BuildConfig.V2_CHARACTERISTIC_ID) to 2
    )

    private val implementations = mapOf<Int, BlueTraceProtocol>(
        2 to BlueTraceV2()
    )

    fun supportsCharUUID(charUUID: UUID?): Boolean {
        if (charUUID == null) {
            return false
        }

        characteristicToProtocolVersionMap[charUUID]?.let { version ->
            return implementations[version] != null
        }
        return false
    }

    //gets the protocol implementation via charUUID map.
    //fallbacks to V2
    fun getImplementation(charUUID: UUID): BlueTraceProtocol {
        val protocolVersion = characteristicToProtocolVersionMap[charUUID] ?: 1
        return getImplementation(protocolVersion)
    }

    //gets the protocol implementation.
    //fallbacks to V2
    private fun getImplementation(protocolVersion: Int): BlueTraceProtocol {
        val impl = implementations[protocolVersion]
        return impl ?: BlueTraceV2()
    }
}
