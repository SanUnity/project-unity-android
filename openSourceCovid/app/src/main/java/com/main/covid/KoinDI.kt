package com.main.covid

import com.main.covid.db.SharedPrefsRepository
import com.main.covid.network.ApiService
import com.main.covid.network.HeaderInterceptor
import com.main.covid.utils.Constants
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit


/**
 * Created by Rubén Izquierdo, Global Incubator
 */

val retrofitModule = module {

    single {
        CertificatePinner.Builder()
            .add(
                Constants.DOMAIN_BACK,
                "sha256/XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX="
            )
            .add(
                Constants.DOMAIN_APP,
                "sha256/XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX="
            )
            .build();
    }

    single {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor())
            .certificatePinner(get())
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS).build()
    }

    //API Service
    single {
        Retrofit.Builder()
            .baseUrl(Constants.END_POINT_URL)
            .client(get())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build().create(ApiService::class.java)
    }
}


val repositoryModule = module {
    single { SharedPrefsRepository(get()) }
}

val allModules = listOf(repositoryModule, retrofitModule)