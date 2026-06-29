package com.dailyspark.mobile

import android.app.Application
import com.dailyspark.mobile.ads.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this)
        AppOpenAdManager.init(this)
    }
}