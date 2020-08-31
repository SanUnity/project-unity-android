package com.main.covid.network

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.main.covid.App
import com.main.covid.bluetracer.Preference
import com.main.covid.bluetracer.logging.CentralLog
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.db.status.persistence.StatusRecord
import com.main.covid.db.status.persistence.StatusRecordStorage
import com.main.covid.db.streetpass.StreetPassRecord
import com.main.covid.db.streetpass.StreetPassRecordStorage
import com.main.covid.service.bluetooth.BluetoothMonitoringService
import com.main.covid.service.bluetooth.BluetoothMonitoringService.Companion.TAG
import com.main.covid.utils.BluetracerUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * Created by Rubén Izquierdo, Global Incubator
 */
data class ExportData(val recordList: List<StreetPassRecord>, val statusList: List<StatusRecord>)

data class updatedDeviceReadyToSend(
    var v: Int,
    var msg: String,
    var org: String,
    val modelP: String,
    val modelC: String,
    val rssi: Int,
    var txPower: String
)

object BluetracerUploader {

    private val apiService: ApiService by KoinJavaComponent.inject(ApiService::class.java)
    private val sharedPrefsRepository by KoinJavaComponent.inject(SharedPrefsRepository::class.java)

    fun uploadBTData(): Disposable? {
        val observableStreetRecords = Observable.create<List<StreetPassRecord>> {
            val result = StreetPassRecordStorage(App.AppContext).getAllRecords()
            it.onNext(result)
        }
        val observableStatusRecords = Observable.create<List<StatusRecord>> {
            val result = StatusRecordStorage(App.AppContext).getAllRecords()
            it.onNext(result)
        }

        return Observable.zip(observableStreetRecords, observableStatusRecords,

            BiFunction<List<StreetPassRecord>, List<StatusRecord>, ExportData> { records, status ->
                ExportData(
                    records,
                    status
                )
            }

        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { exportedData ->
                Log.d(TAG, "records: ${exportedData.recordList}")
                Log.d(TAG, "status: ${exportedData.statusList}")


                writeBTJSonAndUpload(
                    exportedData.recordList,
                    exportedData.statusList
                ).enqueue(object : Callback<String> {

                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        if (response.isSuccessful) {
                            val context = App.AppContext
                            GlobalScope.launch {
                                val before = System.currentTimeMillis()
                                CentralLog.i(
                                    BluetoothMonitoringService.TAG,
                                    "Coroutine - Purging of data before epoch time $before"
                                )
                                StreetPassRecordStorage(App.AppContext).purgeOldRecords(before)
                                StatusRecordStorage(App.AppContext).purgeOldRecords(before)
                                Preference.putLastPurgeTime(
                                    context,
                                    System.currentTimeMillis()
                                )
                            }
                            CentralLog.d(TAG, "Upload was successful")
                        }
                        else{
                            CentralLog.d(TAG, "Upload was not successful")
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        CentralLog.d(TAG, "Upload was not successful")

                    }

                })
            }
    }


    private fun writeBTJSonAndUpload(
        deviceDataList: List<StreetPassRecord>,
        statusList: List<StatusRecord>
    ): Call<String> {

        val date = BluetracerUtils.getDateFromUnix(System.currentTimeMillis())
        val gson = Gson()

        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        val updatedDeviceList = deviceDataList.map {
            it.timestamp = it.timestamp / 1000
            return@map it
        }

        val deviceDataReady = ArrayList<updatedDeviceReadyToSend>()
        updatedDeviceList.forEach { x ->
            val tx = if (x.txPower == null)
                "null"
            else
                x.txPower.toString()
            deviceDataReady.add(
                updatedDeviceReadyToSend(
                    x.v,
                    x.msg,
                    x.org,
                    x.modelP,
                    x.modelC,
                    x.rssi,
                    tx
                )
            )
        }

        val map: MutableMap<String, Any> = HashMap()
        map["manufacturer"] = manufacturer
        map["model"] = model
        map["todayDate"] = date ?: ""
        map["records"] = deviceDataReady as Any

        val mapString = if (map.isNotEmpty()) {
            gson.toJson(map)
        } else {
            ""
        }
        return apiService.sendSavedBTData(mapString)
    }
}