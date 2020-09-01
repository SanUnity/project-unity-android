package com.main.covid.web

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.main.covid.App
import com.main.covid.BuildConfig
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.utils.Constants
import com.main.covid.views.MainActivity
import org.dpppt.android.sdk.DP3T
import org.json.JSONObject
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Created by Rubén Izquierdo, Global Incubator
 */
class WebAppInterface(
    private val mContext: Context,
    mWebView: WebView
) : KoinComponent {

    private val sharedPrefsRepository: SharedPrefsRepository by inject()

    @JavascriptInterface
    fun postData(input: String) {
        val res = input.split(",").map { it.trim() }

        if (!res.isNullOrEmpty() || res.isNotEmpty()) {
            //Get id
            val id = if (res[0].contains("id"))
                res[0].replace("id:", "")
            else
                null

            //Get token
            val token = if (res[1].contains("token"))
                res[1].replace("token:", "")
            else
                null

            id?.let {
                sharedPrefsRepository.putDeviceBuid(it)
            }

            token?.let {
                sharedPrefsRepository.putApiToken(it)
                try {
                    mContext as MainActivity
                    mContext.updateFirebaseToken()
                } catch (ex: Exception) {
                    Log.i("E", "Couldn't get main activity")
                }
            }
        }
    }

    @JavascriptInterface
    fun requestBT() {
        try {
            mContext as MainActivity
            mContext.requestEnableBt()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }

    @JavascriptInterface
    fun requestGPS() {
        try {
            mContext as MainActivity
            mContext.requestLocationPermission()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }

    @JavascriptInterface
    fun startBluetooth() {
        sharedPrefsRepository.putBTOn(true)
        try {
            mContext as MainActivity
            mContext.startBTService()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }

    }

    @JavascriptInterface
    fun stopBluetooth() {
        sharedPrefsRepository.putBTOn(false)
    }

    @JavascriptInterface
    fun getBTDataFromApp() {
        try {
            mContext as MainActivity
            mContext.setIAmInfectedDP3T()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }

    @JavascriptInterface
    fun syncExposedUser() {
        DP3T.sync(mContext)
    }

    @JavascriptInterface
    fun shareApp() {
        try {
            mContext as MainActivity
            mContext.shareApp()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }


    @JavascriptInterface
    fun stopExposedNotifications() {
        try {
            mContext as MainActivity
            val dp3tStarted = DP3T.isStarted(this.mContext)
            if (dp3tStarted)
                DP3T.stop(this.mContext)
            DP3T.clearData(this.mContext) {
                sharedPrefsRepository.putLastUploadTimestamp(0L)
                App.initDP3T()
                mContext.startBTService()
            }
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }

    @JavascriptInterface
    fun logout() {
        sharedPrefsRepository.putBTOn(false)
        if (sharedPrefsRepository.getBTOn() && BuildConfig.BT_TYPE == Constants.DECENTRALIZED ) {
            if (DP3T.isStarted(this.mContext))
                DP3T.stop(this.mContext)
            DP3T.clearData(mContext) {}
        }
        sharedPrefsRepository.clear()
        try {
            mContext as MainActivity
            mContext.sendEmptyToken()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }

    @JavascriptInterface
    fun getToken(){
        try {
            mContext as MainActivity
            mContext.sendToken()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }

    @JavascriptInterface
    fun getContacts() {
        try {
            mContext as MainActivity
            mContext.getContacts()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }

    @JavascriptInterface
    fun share(data: String) {
        val jsonObject = JSONObject(data)
        val message = jsonObject.getString("message")
        val image = if (jsonObject.has("image") && jsonObject.getString("image") != "null")
            jsonObject.getString("image")
        else null
        val url = if (jsonObject.has("url") && jsonObject.getString("url") != "null")
            jsonObject.getString("url")
        else null

        try {
            mContext as MainActivity
            mContext.share(message, image, url)
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }


    @JavascriptInterface
    fun getStatus() {
        try {
            mContext as MainActivity
            mContext.getStatus()
        } catch (ex: Exception) {
            Log.i("E", "Couldn't get main activity")
        }
    }
}