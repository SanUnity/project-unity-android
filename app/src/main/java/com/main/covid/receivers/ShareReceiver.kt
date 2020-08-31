package com.main.covid.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_CHOSEN_COMPONENT
import android.util.Log
import com.main.covid.App
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.views.MainActivity
import org.koin.java.KoinJavaComponent


/**
 * Created by Rubén Izquierdo, Global Incubator
 */
class ShareReceiver : BroadcastReceiver() {

    private val sharedPrefsRepository by KoinJavaComponent.inject(SharedPrefsRepository::class.java)

    override fun onReceive(context: Context?, intent: Intent?) {
        val clickedComponent: ComponentName? =
            intent?.getParcelableExtra(EXTRA_CHOSEN_COMPONENT)

        if(clickedComponent != null)
            sharedPrefsRepository.putSharedResult(clickedComponent.toString())


    }
}