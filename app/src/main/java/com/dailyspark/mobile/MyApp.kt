package com.dailyspark.mobile

import android.app.Application
import android.util.Log
import com.dailyspark.mobile.ads.AdsResponse
import com.dailyspark.mobile.ads.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) { initializationStatus ->
            Log.d("AdManager", "MobileAds initialized: $initializationStatus")
        }
        AdsResponse.fetchAdConfig {
            AdsResponse.initialize()
        }

        FirebaseApp.initializeApp(this)
        AppOpenAdManager.init(this)
    }
}