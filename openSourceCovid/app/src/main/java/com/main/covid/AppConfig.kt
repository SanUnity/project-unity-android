package com.main.covid

import android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_BALANCED
import android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED



/**
 * Created by Rubén Izquierdo, Global Incubator
 */
class AppConfig {


    companion object {
        //A day
        const val periodUploadGPS = 20*60 * 1000L
        const val scanMode = ScanSettings.SCAN_MODE_BALANCED
        const val advertiseTxPower = ADVERTISE_TX_POWER_HIGH
        const val advertiseMode = ADVERTISE_MODE_BALANCED
        const val waitingSeconds = 5L
        const val GetLocationRestartMinutes = 5L
    }

}