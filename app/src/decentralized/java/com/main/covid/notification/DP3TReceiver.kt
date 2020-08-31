package com.main.covid.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.main.covid.App
import com.main.covid.BuildConfig
import com.main.covid.R
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.utils.PermissionCompanion
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.InfectionStatus
import org.koin.java.KoinJavaComponent.inject


/**
 * Created by Rubén Izquierdo, Global Incubator
 */
class DP3TReceiver : BroadcastReceiver() {

    private val sharedPrefsRepository by inject(SharedPrefsRepository::class.java)

    override fun onReceive(context: Context?, intent: Intent?) {
        val status = DP3T.getStatus(context)

        when (DP3T.getStatus(context).infectionStatus) {
            InfectionStatus.EXPOSED ->
                if (PermissionCompanion.isTimeToCreateNotification()) {
                    sharedPrefsRepository.putLastUploadTimestamp(System.currentTimeMillis())
                    val notificationManager: OriginalNotificationManager?
                            = OriginalNotificationManager.getInstance(App.AppContext);

                    val notification = OriginalNotificationManager.createUrlNotification(
                        context,
                        sharedPrefsRepository.getExposedNotificationTitle(),
                        sharedPrefsRepository.getExposedNotificationBody(),
                        context?.getString(R.string.urlExposed) + BuildConfig.VERSION_CODE
                    )

                    notificationManager!!.notifyExec(
                        notification,
                        0,
                        App.AppContext
                    )
                }
        }
    }
}