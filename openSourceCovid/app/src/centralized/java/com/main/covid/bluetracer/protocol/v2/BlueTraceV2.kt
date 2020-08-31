package com.main.covid.bluetracer.protocol.v2

import com.main.covid.App
import com.main.covid.bluetracer.logging.CentralLog
import com.main.covid.bluetracer.streetpass.CentralDevice
import com.main.covid.bluetracer.streetpass.ConnectionRecord
import com.main.covid.bluetracer.streetpass.PeripheralDevice
import io.bluetrace.opentrace.protocol.BlueTraceProtocol
import io.bluetrace.opentrace.protocol.CentralInterface
import io.bluetrace.opentrace.protocol.PeripheralInterface
import io.bluetrace.opentrace.protocol.v2.V2ReadRequestPayload
import io.bluetrace.opentrace.protocol.v2.V2WriteRequestPayload

class BlueTraceV2 : BlueTraceProtocol(
    versionInt = 2,
    peripheral = V2Peripheral(),
    central = V2Central()
)

class V2Peripheral : PeripheralInterface {

    private val TAG = "V2Peripheral"

    override fun prepareReadRequestData(protocolVersion: Int): ByteArray {
        return V2ReadRequestPayload(
            v = protocolVersion,
            id = App.thisDeviceMsg(),
            o = App.ORG,
            peripheral = App.asPeripheralDevice()
        ).getPayload()
    }

    override fun processWriteRequestDataReceived(
        dataReceived: ByteArray,
        centralAddress: String
    ): ConnectionRecord? {
        try {
            val dataWritten =
                V2WriteRequestPayload.fromPayload(
                    dataReceived
                )

            return ConnectionRecord(
                version = dataWritten.v,
                msg = dataWritten.id,
                org = dataWritten.o,
                peripheral = App.asPeripheralDevice(),
                central = CentralDevice(dataWritten.mc, centralAddress),
                rssi = dataWritten.rs,
                txPower = null
            )
        } catch (e: Throwable) {
            CentralLog.e(TAG, "Failed to deserialize write payload ${e.message}")
        }
        return null
    }
}

class V2Central : CentralInterface {

    private val TAG = "V2Central"

    override fun prepareWriteRequestData(
        protocolVersion: Int,
        rssi: Int,
        txPower: Int?
    ): ByteArray {
        return V2WriteRequestPayload(
            v = protocolVersion,
            id = App.thisDeviceMsg(),
            o = App.ORG,
            central = App.asCentralDevice(),
            rs = rssi
        ).getPayload()
    }

    override fun processReadRequestDataReceived(
        dataRead: ByteArray,
        peripheralAddress: String,
        rssi: Int,
        txPower: Int?
    ): ConnectionRecord? {
        try {
            val readData =
                V2ReadRequestPayload.fromPayload(
                    dataRead
                )
            val peripheral =
                PeripheralDevice(readData.mp, peripheralAddress)

            val connectionRecord = ConnectionRecord(
                version = readData.v,
                msg = readData.id,
                org = readData.o,
                peripheral = peripheral,
                central = App.asCentralDevice(),
                rssi = rssi,
                txPower = txPower
            )
            return connectionRecord
        } catch (e: Throwable) {
            CentralLog.e(TAG, "Failed to deserialize read payload ${e.message}")
        }

        return null
    }
}
