package com.main.covid.network

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.main.covid.db.SharedPrefsRepository
import org.koin.java.KoinJavaComponent.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



/**
 * Created by Rubén Izquierdo, Global Incubator
 */
object DataUploader {

    private val apiService: ApiService by inject(ApiService::class.java)
    private val sharedPrefsRepository by inject(SharedPrefsRepository::class.java)

    fun uploadDeviceToken(token: String) {

        sharedPrefsRepository.putDeviceToken(token)
        sharedPrefsRepository.putNewApiToken(true)
        val dataToSend = HashMap<String, String>()
        dataToSend["devicetoken"] = token
        dataToSend["devicetype"] = "android"
        val message = Gson().toJson(dataToSend)
        apiService.sendDeviceToken(message)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        Log.d("DataUploader", "Upload of devicetoken was successful")
                        sharedPrefsRepository.putNewApiToken(false)

                    }
                    else
                        Log.d("DataUploader", "Upload of devicetoken not successful")

                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.d("DataUploader", "Upload of devicetoken not successful")
                }
            })

    }

    fun uploadIfNewDeviceTokenTrue(token: String) {
        val new = sharedPrefsRepository.getNewApiToken()
        new?.let {
            if (it)
                uploadDeviceToken(token)
        }
    }

    private fun uploadDeviceTokenNoUser(token: String) {

        val dataToSend = HashMap<String, String>()
        dataToSend["devicetoken"] = token
        dataToSend["devicetype"] = "android"
        val message = Gson().toJson(dataToSend)
        apiService.sendDeviceTokenNoUser(message)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        Log.d("DataUploader", "Upload of devicetoken was successful")
                        sharedPrefsRepository.putNoUserToken(false)

                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.d("DataUploader", "Upload of devicetoken not successful")
                }
            })

    }

    fun uploadIfNewDeviceTokenTrueNotUser(token: String) {
        val new = sharedPrefsRepository.getNoUserToken()
        new.let { new ->
            if (new)
                uploadDeviceTokenNoUser(token)
        }
    }

    fun forceUploadDeviceToken(deviceCodes: List<String>) {

        val currentToken = sharedPrefsRepository.getDeviceToken() ?: ""
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currentToken != "" && !deviceCodes.stream().anyMatch {
                    currentToken.contains(it)
                }
            } else {
                currentToken != "" && !deviceCodes.any {
                    currentToken.contains(it)
                }
            }
        ) {
            val dataToSend = HashMap<String, String>()
            dataToSend["devicetoken"] = currentToken
            dataToSend["devicetype"] = "android"
            val message = Gson().toJson(dataToSend)
            apiService.sendDeviceToken(message)
                .enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        if (response.isSuccessful) {
                            Log.d("DataUploader", "Upload of devicetoken was successful")

                        } else
                            Log.d("DataUploader", "Upload of devicetoken not successful")

                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Log.d("DataUploader", "Upload of devicetoken not successful")
                    }
                })
        }
    }
}
