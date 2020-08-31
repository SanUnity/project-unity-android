package com.main.covid.views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.main.covid.R
import com.main.covid.network.DataUploader
import com.main.covid.network.DataUploader.uploadIfNewDeviceTokenTrue
import com.scottyab.rootbeer.RootBeer

class SplashScreenActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 2000 // 1 sec
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        //                if (!RootHelper.isDeviceRooted()) {
//                    if (!RootBeer(this).isRooted)
        continueInitilization()
//                } else {
//                    Toast.makeText(this, "Tampering detected", Toast.LENGTH_SHORT).show();
//                }
    }

    private fun continueInitilization() {
        Handler().postDelayed({
            // This method will be executed once the timer is over
            // Start your app main activity
            val urlNotification = intent.getStringExtra("urlNotification")
            val intent = Intent(this, MainActivity::class.java)
            if (urlNotification != null)
                intent.putExtra("urlNotification", urlNotification)
            startActivity(Intent(this, MainActivity::class.java))

            // close this activity
            finish()
        }, SPLASH_TIME_OUT)
    }
}
