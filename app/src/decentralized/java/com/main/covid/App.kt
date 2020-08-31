package com.main.covid

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import com.main.covid.notification.DP3TReceiver
import com.main.covid.utils.Constants
import okhttp3.CertificatePinner
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.backend.models.ApplicationInfo
import org.dpppt.android.sdk.util.SignatureUtil
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

        initDP3T()
        val broadcastReceiver: BroadcastReceiver = DP3TReceiver()
        registerReceiver(broadcastReceiver, DP3T.getUpdateIntentFilter())
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@App)
            modules(allModules)
        }
    }

    companion object {
        lateinit var AppContext: Context

        fun initDP3T() {

            val publicKey = SignatureUtil.getPublicKeyFromBase64OrThrow(
                "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0F" +
                        "FbXRQb3NheERoRFRxMjltQ3pKblpMem85Wm4veQpnREp4SHRUcHFMc3RDMTZzYVFySEkzL1By" +
                        "KzQ4MUVEcDJ6eDREakJVSjBVdWFZWWFDWWhaOHZvSVFnPT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t"
            )

            DP3T.init(
                AppContext,
                ApplicationInfo(
                    BuildConfig.APPLICATION_ID,
                    Constants.END_POINT_URL,
                    Constants.END_POINT_URL
                ),
                publicKey
            )

            if (!BuildConfig.DEBUG) {
                val certificatePinner = CertificatePinner.Builder()
                    .add(
                        Constants.DOMAIN_BACK,
                        "sha256/XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX="
                    )
                    .build()
                DP3T.setCertificatePinner(certificatePinner)

            }

        }
    }
}