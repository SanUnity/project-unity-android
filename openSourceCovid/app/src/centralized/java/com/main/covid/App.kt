package com.main.covid

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import com.main.covid.bluetracer.idmanager.TempIDManager
import com.main.covid.bluetracer.logging.CentralLog
import com.main.covid.bluetracer.streetpass.CentralDevice
import com.main.covid.bluetracer.streetpass.PeripheralDevice
import com.main.covid.service.bluetooth.BluetoothMonitoringService
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin



/**
 * Created by Rubén Izquierdo, Global Incubator
 */
class App : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()
        AppContext = applicationContext
        setupKoin()
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@App)
            modules(allModules)
        }
    }

    companion object {

        private const val TAG = "CovidApp"
        const val ORG = BuildConfig.ORG

        lateinit var AppContext: Context

        fun thisDeviceMsg(): String {
            BluetoothMonitoringService.broadcastMessage?.let {
                CentralLog.i(TAG, "Retrieved BM for storage: $it")

                if (!it.isValidForCurrentTime()) {

                    val fetch = TempIDManager.retrieveTemporaryID(AppContext)
                    fetch?.let {
                        CentralLog.i(TAG, "Grab New Temp ID")
                        BluetoothMonitoringService.broadcastMessage = it
                    }

                    if (fetch == null) {
                        CentralLog.e(TAG, "Failed to grab new Temp ID")
                    }

                }
            }
            return BluetoothMonitoringService.broadcastMessage?.tempID ?: "Missing TempID"
        }

        fun asPeripheralDevice(): PeripheralDevice {
            return PeripheralDevice(Build.MODEL, "SELF")
        }

        fun asCentralDevice(): CentralDevice {
            return CentralDevice(Build.MODEL, "SELF")
        }
    }
}