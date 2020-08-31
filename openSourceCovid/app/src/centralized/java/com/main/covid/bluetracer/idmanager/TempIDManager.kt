package com.main.covid.bluetracer.idmanager

import android.content.Context
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.main.covid.bluetracer.Preference
import com.main.covid.bluetracer.logging.CentralLog
import com.main.covid.network.ApiService
import com.main.covid.service.bluetooth.BluetoothMonitoringService.Companion.bmValidityCheck
import org.koin.java.KoinJavaComponent.get
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*
import kotlin.collections.HashMap


object TempIDManager {

    private const val TAG = "TempIDManager"

    private val service : ApiService = get(ApiService::class.java)

    fun storeTemporaryIDs(context: Context, packet: String) {
        CentralLog.d(TAG, "[TempID] Storing temporary IDs into internal storage...")
        val file = File(context.filesDir, "tempIDs")
        file.writeText(packet)
    }

    fun retrieveTemporaryID(context: Context): TemporaryID? {
        val file = File(context.filesDir, "tempIDs")
        if (file.exists()) {
            val readback = file.readText()
            CentralLog.d(TAG, "[TempID] fetched broadcastmessage from file:  $readback")
            val tempIDArrayList =
                convertToTemporaryIDs(
                    readback
                )
            val tempIDQueue =
                convertToQueue(
                    tempIDArrayList
                )
            return getValidOrLastTemporaryID(
                context,
                tempIDQueue
            )
        }
        return null
    }

    private fun getValidOrLastTemporaryID(
        context: Context,
        tempIDQueue: Queue<TemporaryID>
    ): TemporaryID {
        CentralLog.d(TAG, "[TempID] Retrieving Temporary ID")
        val currentTime = System.currentTimeMillis()

        var pop = 0
        while (tempIDQueue.size > 1) {
            val tempID = tempIDQueue.peek()
            tempID.print()

            if (tempID.isValidForCurrentTime()) {
                CentralLog.d(TAG, "[TempID] Breaking out of the loop")
                break
            }

            tempIDQueue.poll()
            pop++
        }

        val foundTempID = tempIDQueue.peek()
        val foundTempIDStartTime = foundTempID.startTime * 1000
        val foundTempIDExpiryTime = foundTempID.expiryTime * 1000

        CentralLog.d(TAG, "[TempID Total number of items in queue: ${tempIDQueue.size}")
        CentralLog.d(TAG, "[TempID Number of items popped from queue: $pop")
        CentralLog.d(TAG, "[TempID] Current time: ${currentTime}")
        CentralLog.d(TAG, "[TempID] Start time: ${foundTempIDStartTime}")
        CentralLog.d(TAG, "[TempID] Expiry time: ${foundTempIDExpiryTime}")
        CentralLog.d(TAG, "[TempID] Updating expiry time")
        Preference.putExpiryTimeInMillis(
            context,
            foundTempIDExpiryTime
        )
        return foundTempID
    }

    private fun convertToTemporaryIDs(tempIDString: String): Array<TemporaryID> {
        val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

        val tempIDResult = gson.fromJson(tempIDString, Array<TemporaryID>::class.java)
        CentralLog.d(
            TAG,
            "[TempID] After GSON conversion: ${tempIDResult[0].tempID} ${tempIDResult[0].startTime}"
        )

        return tempIDResult
    }

    private fun convertToQueue(tempIDArray: Array<TemporaryID>): Queue<TemporaryID> {
        CentralLog.d(TAG, "[TempID] Before Sort: ${tempIDArray[0]}")

        //Sort based on start time
        tempIDArray.sortBy {
            return@sortBy it.startTime
        }
        CentralLog.d(TAG, "[TempID] After Sort: ${tempIDArray[0]}")

        //Preserving order of array which was sorted
        val tempIDQueue: Queue<TemporaryID> = LinkedList<TemporaryID>()
        for (tempID in tempIDArray) {
            tempIDQueue.offer(tempID)
        }

        CentralLog.d(TAG, "[TempID] Retrieving from Queue: ${tempIDQueue.peek()}")
        return tempIDQueue
    }


    //use this function to run mockApi
    fun getTemporaryIDMocks(context: Context): String {
        val json = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"refreshTime\": 5000,\n" +
                "  \"tempIDs\": [\n" +
                "    {\n" +
                "      \"tempID\": \"string\",\n" +
                "      \"startTime\": 0,\n" +
                "      \"expiryTime\": 0\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        val mapType = object : TypeToken<HashMap<String, Any>>() {}.type
        val result: HashMap<String, Any> = Gson().fromJson(json, mapType)

        val tempIDs = result["tempIDs"]

        val status = result["status"].toString()
        if (status.toLowerCase().contentEquals("success")) {
            CentralLog.w(TAG, "Retrieved Temporary IDs from Server")
            val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
            val jsonByteArray = gson.toJson(tempIDs).toByteArray(Charsets.UTF_8)
            storeTemporaryIDs(
                context,
                jsonByteArray.toString(Charsets.UTF_8)
            )

            val refreshTime = result["refreshTime"].toString()
            val refresh = refreshTime.toLongOrNull() ?: 0
            Preference.putNextFetchTimeInMillis(
                context,
                refresh * 1000
            )
            Preference.putLastFetchTimeInMillis(
                context,
                System.currentTimeMillis() * 1000
            )
        }
        return "Ruben"
    }


    interface ApiCallback{
        fun onSuccess(context: Context)
        fun onFailure(context: Context)
    }

    fun getTemporaryIDs(context: Context, returnCallback: ApiCallback){
        return service.getTempIDs().enqueue(object : Callback<String>{

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response.isSuccessful) {
                    val mapType = object : TypeToken<HashMap<String, Any>>() {}.type
                    val result: HashMap<String, Any> = Gson().fromJson(response.body(), mapType)
                    val tempIDs = result["tempIDs"]

                    val status = result["status"].toString()
                    if (status.toLowerCase().contentEquals("success")) {
                        CentralLog.w(TAG, "Retrieved Temporary IDs from Server")
                        val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
                        val jsonByteArray = gson.toJson(tempIDs).toByteArray(Charsets.UTF_8)
                        storeTemporaryIDs(
                            context,
                            jsonByteArray.toString(Charsets.UTF_8)
                        )

                        val refreshTime = result["refreshTime"].toString()
                        val refresh = refreshTime.toLongOrNull() ?: 0
                        Preference.putNextFetchTimeInMillis(
                            context,
                            refresh * 1000
                        )
                        Preference.putLastFetchTimeInMillis(
                            context,
                            System.currentTimeMillis() * 1000
                        )
                        returnCallback.onSuccess(context)
                    }
                }
                else{
                    returnCallback.onFailure(context)
                }

            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                CentralLog.d(TAG, "[TempID] Error getting Temporary IDs")
                returnCallback.onFailure(context)
            }

        })

    }

    fun needToUpdate(context: Context): Boolean {
        val nextFetchTime =
            Preference.getNextFetchTimeInMillis(context)
        val currentTime = System.currentTimeMillis()

        val update = currentTime >= nextFetchTime
        CentralLog.i(
            TAG,
            "Need to update and fetch TemporaryIDs? $nextFetchTime vs $currentTime: $update"
        )
        return update
    }

    fun needToRollNewTempID(context: Context): Boolean {
        val expiryTime =
            Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime >= expiryTime
        CentralLog.d(
            TAG,
            "[TempID] Need to get new TempID? $expiryTime vs $currentTime: $update"
        )
        return update
    }

    //Can Cleanup, this function always return true
    fun bmValid(context: Context): Boolean {
        val expiryTime =
            Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime < expiryTime

        if (bmValidityCheck) {
            CentralLog.w(TAG, "Temp ID is valid")
            return update
        }

        return true
    }
}
