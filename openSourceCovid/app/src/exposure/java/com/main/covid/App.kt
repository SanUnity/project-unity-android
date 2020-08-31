package com.main.covid

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import com.main.covid.notification.DP3TReceiver
import com.main.covid.utils.Constants
import okhttp3.CertificatePinner
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.internal.backend.BackendBucketRepository
import org.dpppt.android.sdk.internal.logger.LogLevel
import org.dpppt.android.sdk.internal.logger.Logger
import org.dpppt.android.sdk.models.ApplicationInfo
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
        Logger.init(
            applicationContext,
            LogLevel.DEBUG
        )
        initDP3T(AppContext)
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

        private const val TAG = "CovidApp"
        const val ORG = BuildConfig.ORG

        lateinit var AppContext: Context

        fun initDP3T(context: Context?) {
            val signaturePublicKey =
                SignatureUtil.getPublicKeyFromBase64OrThrow(
                    "*** YOUR DP3T PUBLIC KEY HERE***"
                )
            DP3T.init(
                context,
                ApplicationInfo(
                    BuildConfig.APPLICATION_ID,
                    Constants.END_POINT_URL,
                    Constants.END_POINT_URL
                ),
                signaturePublicKey
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
            BackendBucketRepository.BATCH_LENGTH =
                1 * 60 * 1000L

            val sdkInt = Build.VERSION.SDK_INT


            val userAgent = BuildConfig.APPLICATION_ID + ";" +
                    BuildConfig.VERSION_NAME + ";" +
                    BuildConfig.VERSION_CODE + ";" +
                    "Android;" +
                    sdkInt
            DP3T.setUserAgent(userAgent)
        }
    }
}