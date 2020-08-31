package com.main.covid.db

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.main.covid.R



/**
 * Created by Rubén Izquierdo, Global Incubator
 */
class SharedPrefsRepository(val c: Context) {


    companion object {
        const val DEVICE_BUID = "DEVICE_BUID"
        const val API_TOKEN = "API_TOKEN"
        const val NEW_DT = "NEW_DT"
        const val DEVICE_TOKEN = "DEVICE_TOKEN"
        const val BT_SERVICE_ENABLED = "BT_SERVICE_ON"
        const val GPS_SERVICE_ENABLED = "GPS_SERVICE_ON"
        const val LAST_GPS_UPLOAD_TS = "LAST_GPS_UPLOAD_TS"
        const val SHARE_SUCCESS = "shared_success"

        const val NEW_DT_NU = "NEW_DT_NU"
        const val APP_PAUSED = "preference.app_paused"

        const val EXPOSED_NOTIFICATION_TITLE = "exposed_title"
        const val EXPOSED_NOTIFICATION_BODY = "exposed_body"


        const val TAG = "SharedPrefsRepository"
    }

    private val prefs: SharedPreferences = c.getSharedPreferences("prefs", MODE_PRIVATE)

    private val encryptedShared: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        encryptedShared = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKeyAlias,
            c,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    }

    //Device BUID it doesnt get used right now
    fun putDeviceBuid(buid: String) {
        prefs.edit().putString(DEVICE_BUID, buid).apply()
    }

    fun removeDeviceBuid() {
        prefs.edit().remove(DEVICE_BUID).apply()
    }

    fun getDeviceBuid(): String? {
        return prefs.getString(DEVICE_BUID, null)
    }

    //This is the device token used for firebase messages
    fun putDeviceToken(buid: String) {
        encryptedShared.edit().putString(DEVICE_TOKEN, buid).apply()
    }

    fun removeDeviceToken() {
        encryptedShared.edit().remove(DEVICE_TOKEN).apply()
    }

    fun getDeviceToken(): String? {
        return encryptedShared.getString(DEVICE_TOKEN, null)
    }

    //This indicates if the devicetoken has been uploaded to server
    fun putNewApiToken(buid: Boolean) {
        prefs.edit().putBoolean(NEW_DT, buid).apply()
    }

    fun getNewApiToken(): Boolean? {
        return prefs.getBoolean(NEW_DT, true)
    }

    //This indicates if the devicetoken has been uploaded without user to the server
    fun putNoUserToken(buid: Boolean) {
        prefs.edit().putBoolean(NEW_DT_NU, buid).apply()
    }

    fun getNoUserToken(): Boolean {
        return prefs.getBoolean(NEW_DT_NU, true)
    }
    //This token is used to upload data to our service
    fun putApiToken(token: String) {
        encryptedShared.edit().putString(API_TOKEN, token).apply()
    }

    fun removeApiToken() {
        encryptedShared.edit().remove(API_TOKEN).apply()
    }

    fun getApiToken(): String? {
        return encryptedShared.getString(API_TOKEN, null)
    }

    //Indicates if BT service has been activated
    fun putBTOn(isOn: Boolean) {
        prefs.edit().putBoolean(BT_SERVICE_ENABLED, isOn).apply()
    }

    fun getBTOn(): Boolean {
        return prefs.getBoolean(BT_SERVICE_ENABLED, false)
    }

    //Indicates if GPS service has been activated
    fun putGPSOn(isOn: Boolean) {
        prefs.edit().putBoolean(GPS_SERVICE_ENABLED, isOn).apply()
    }

    fun getGPSOn(): Boolean {
        return prefs.getBoolean(GPS_SERVICE_ENABLED, false)
    }


    fun putLastUploadTimestamp(timestamp: Long) {
        prefs.edit().putLong(LAST_GPS_UPLOAD_TS, timestamp).apply()
    }

    fun getLastUploadTimestamp(): Long {
        return prefs.getLong(LAST_GPS_UPLOAD_TS, 0)
    }

    fun setGPSServicePaused(isPaused: Boolean) =
        prefs.edit().putBoolean(APP_PAUSED, isPaused).apply()

    fun getGPSServicePaused() = prefs.getBoolean(APP_PAUSED, false)

    fun setExposedNotificationTitle(notificationTitle: String){
        prefs.edit().putString(EXPOSED_NOTIFICATION_TITLE, notificationTitle).apply()
    }

    fun setExposedNotificationBody(notificationBody: String){
        prefs.edit().putString(EXPOSED_NOTIFICATION_TITLE, notificationBody).apply()
    }

    fun getExposedNotificationTitle() =
        prefs.getString(EXPOSED_NOTIFICATION_TITLE, c.getString(R.string.titleExposed))

    fun getExposedNotificationBody() =
        prefs.getString(EXPOSED_NOTIFICATION_TITLE, c.getString(R.string.bodyExposed))

    fun putSharedResult(toString: String?) {
        prefs.edit().putString(SHARE_SUCCESS, toString).apply()
    }

    fun getSharedResult() : String?{
        return prefs.getString(SHARE_SUCCESS, null)
    }

    fun clear() {
        prefs.edit().clear().apply()
        encryptedShared.edit().clear().apply()
    }
}