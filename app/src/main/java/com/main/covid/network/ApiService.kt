package com.main.covid.network

import android.util.Log
import com.main.covid.db.SharedPrefsRepository
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.koin.core.KoinComponent
import org.koin.core.get
import retrofit2.Call
import retrofit2.http.*
import java.io.IOException

/*
* @author  Iván Fernández Rico, Rubén Izquierdo Belinchón, Globalincubator
*/
interface ApiService {


    companion object Factory {
        const val GET_TEMPIDS = "users/bluetrace/tempIDs"
        const val POST_SAVED_BT_DATA = "users/bluetrace"
        const val POST_SAVED_LOCATION_DATA = "users/locations"
        const val POST_DEVICE_TOKEN = "users/devicetoken"
        const val POST_DEVICE_TOKEN_NO_USER = "devicetoken"
        const val GET_EXPOSED_CONFIGURATION = "exposed/config"

    }

    @GET(GET_TEMPIDS)
    fun getTempIDs(): Call<String>

    @GET(GET_EXPOSED_CONFIGURATION)
    fun getExposedConfiguration(): Call<String>

    @Headers("Content-type: application/json")
    @POST(POST_SAVED_BT_DATA)
    fun sendSavedBTData(@Body body: String): Call<String>

    @Headers("Content-type: application/json")
    @POST(POST_SAVED_LOCATION_DATA)
    fun sendSavedLocationData(@Body body: String): Call<String>

    @Headers("Content-type: application/json")
    @POST(POST_DEVICE_TOKEN)
    fun sendDeviceToken(@Body body: String): Call<String>

    @Headers("Content-type: application/json")
    @POST(POST_DEVICE_TOKEN_NO_USER)
    fun sendDeviceTokenNoUser(@Body body: String): Call<String>
}

class HeaderInterceptor : Interceptor, KoinComponent {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder: Request.Builder
        val serverSharedPreferencesManager: SharedPrefsRepository = get()

        val userToken = serverSharedPreferencesManager.getApiToken()

        requestBuilder = request.newBuilder()
            .addHeader("Authorization", "Bearer $userToken")

        Log.i(
            "REQUEST",
            String.format(
                "Sending request %s on %s %s",
                request.url(),
                chain.connection(),
                request.headers()
            )
        )

        val response = chain.proceed(requestBuilder.build())

        Log.i(
            "REQUEST",
            String.format("Received response for %s, headers: %s", request.url(), response.body())
        )

        val body = ResponseBody.create(response.body()?.contentType(), response.body()!!.string())
        return response.newBuilder().body(body).build()
    }
}

