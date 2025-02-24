package com.main.covid.bluetracer.idmanager

import com.main.covid.bluetracer.logging.CentralLog

class TemporaryID(
    val startTime: Long,
    val tempID: String,
    val expiryTime: Long
) {

    fun isValidForCurrentTime(): Boolean {
        val currentTime = System.currentTimeMillis()
        return ((currentTime > (startTime * 1000)) && (currentTime < (expiryTime * 1000)))
    }

    fun print() {
        val tempIDStartTime = startTime * 1000
        val tempIDExpiryTime = expiryTime * 1000
        CentralLog.d(
            TAG,
            "[TempID] Start time: ${tempIDStartTime}"
        )
        CentralLog.d(
            TAG,
            "[TempID] Expiry time: ${tempIDExpiryTime}"
        )
    }

    companion object {
        private const val TAG = "TempID"
    }
}
