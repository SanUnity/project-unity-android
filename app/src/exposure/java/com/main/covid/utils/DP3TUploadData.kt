package com.main.covid.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.main.covid.App
import com.main.covid.R
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.network.ApiService
import com.main.covid.views.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dpppt.android.sdk.DP3T
import org.dpppt.android.sdk.backend.ResponseCallback
import org.dpppt.android.sdk.models.ExposeeAuthMethodAuthorization
import org.koin.java.KoinJavaComponent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * Created by Rubén Izquierdo, Global Incubator
 */
object DP3TUploadData {

    private val sharedPrefsRepository by KoinJavaComponent.inject(SharedPrefsRepository::class.java)

    private val service: ApiService = KoinJavaComponent.get(ApiService::class.java)

    fun uploadDP3TIAmInfected(activity: Activity) {

        val token = sharedPrefsRepository.getApiToken()
        DP3T.sendIAmInfected(
            activity,
            Date(System.currentTimeMillis()),
            ExposeeAuthMethodAuthorization("Bearer $token"),
            object : ResponseCallback<Void?> {
                override fun onSuccess(response: Void?) {
                    Log.i("DP3TUpload", "DP3T data uploaded correctly")
                    val dp3tStarted = DP3T.isTracingEnabled(App.AppContext)
                    if (dp3tStarted)
                        DP3T.stop(App.AppContext)
                    DP3T.clearData(App.AppContext)
                    sharedPrefsRepository.putLastUploadTimestamp(0L)
                    App.initDP3T(App.AppContext)
                    if (dp3tStarted)
                        try {
                            activity as MainActivity
                            activity.startBTService()
                        } catch (e: Exception) {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.noMainActStartBt),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    Log.i("DP3TUpload", "DP3T reinitialized")
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

    data class ExposedConfig(
        val minimumRiskScore: Int,
        val attenuationLevelValues: ArrayList<Int>,
        val daysSinceLastExposureLevelValues: ArrayList<Int>,
        val durationLevelValues: ArrayList<Int>,
        val transmissionRiskLevelValues: ArrayList<Int>,
        val lowerThreshold: Int,
        val higherThreshold: Int,
        val factorLow: Float,
        val factorHigh: Float,
        val triggerThreshold: Int,
        val alert: AlertConfig
    )

    data class AlertConfig(
        val title: String,
        val body: String
    )

    fun getExposureNotificationsConfig(context: Context) {
        return service.getExposedConfiguration().enqueue(object : Callback<String> {

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {

                    val gson = GsonBuilder().create()
                    val myType = object : com.google.gson.reflect.TypeToken<ExposedConfig>() {}.type
                    try {
                        val serverObject: ExposedConfig = gson.fromJson(response.body(), myType)

                        DP3T.setMatchingParameters(
                            context,
                            serverObject.minimumRiskScore,
                            serverObject.attenuationLevelValues,
                            serverObject.daysSinceLastExposureLevelValues,
                            serverObject.durationLevelValues,
                            serverObject.transmissionRiskLevelValues,
                            serverObject.lowerThreshold,
                            serverObject.higherThreshold,
                            serverObject.factorLow,
                            serverObject.factorHigh,
                            serverObject.triggerThreshold
                        )

                        sharedPrefsRepository.setExposedNotificationTitle(serverObject.alert.title)
                        sharedPrefsRepository.setExposedNotificationBody(serverObject.alert.body)

                        Log.w(
                            "EXPOSURE_CONFIG",
                            "Configuration loaded correctly"
                        )
                    }
                    catch (ex: JsonSyntaxException){
                        Log.w(
                            "EXPOSURE_CONFIG",
                            "Error getting exposure notifications config"
                        )
                    }
                } else {
                    Log.w(
                        "EXPOSURE_CONFIG",
                        "Error getting exposure notifications config"
                    )
                }

            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.w(
                    "EXPOSURE_CONFIG",
                    " Error getting exposure notifications config"
                )
            }

        })

    }

}