package com.main.covid.utils

import android.util.Log
import com.main.covid.App
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.network.DataUploader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.backend.ResponseCallback
import org.dpppt.android.sdk.backend.models.ExposeeAuthMethodAuthorization
import org.koin.java.KoinJavaComponent
import java.util.*


/**
 * Created by Rubén Izquierdo, Global Incubator
 */
object DP3TUploadData {

    private val sharedPrefsRepository by KoinJavaComponent.inject(SharedPrefsRepository::class.java)

    fun uploadDP3TIAmInfected() {

        val token = sharedPrefsRepository.getApiToken()
        DP3T.sendIAmInfected(
            App.AppContext,
            Date(System.currentTimeMillis()),
            ExposeeAuthMethodAuthorization("Bearer $token"),
            object : ResponseCallback<Void?> {
                override fun onSuccess(response: Void?) {
                    Log.i("DP3TUpload", "DP3T data uploaded correctly")
                    val dp3tStarted = DP3T.isStarted(App.AppContext)
                    if (dp3tStarted)
                        DP3T.stop(App.AppContext)
                    DP3T.clearData(App.AppContext) {
                        sharedPrefsRepository.putLastUploadTimestamp(0L)
                        App.initDP3T()
                        if (dp3tStarted)
                            DP3T.start(App.AppContext)
                        Log.i("DP3TUpload", "DP3T reinitialized")

                    }
                }

                override fun onError(throwable: Throwable) {
                    Log.i("DP3TUpload", "DP3T data uploaded incorrectly")
                }
            })
    }

    fun downloadDP3TData() {
        GlobalScope.launch {
            DP3T.sync(App.AppContext)
        }
    }

}