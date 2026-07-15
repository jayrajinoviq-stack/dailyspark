package com.dailyspark.mobile.ads

import android.util.Log
import com.dailyspark.mobile.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

object RemoteConfigHelper {
    private const val TAG = "RemoteConfig"
    private val remoteConfig = Firebase.remoteConfig

    fun init(onComplete: (() -> Unit)? = null) {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                0
            } else {
                3600
            }
        }

        remoteConfig.setConfigSettingsAsync(settings)

        val defaultValues = mapOf(
            AdsResponse.KEY_IS_SHOW_ADS to AdsResponse.isShowAdsURL,
            AdsResponse.KEY_ADS_URL to AdsResponse.ADS_URL,
            AdsResponse.KEY_CLICK_THRESHOLD to AdsResponse.CLICK_THRESHOLD
        )
        remoteConfig.setDefaultsAsync(defaultValues)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Remote Config Loaded Successfully")
                } else {
                    Log.e(
                        TAG,
                        "Remote Config Fetch Failed. Using default or cached values.",
                        task.exception
                    )
                }

                AdsResponse.initialize()

                Log.d(TAG, "Ads URL : ${AdsResponse.ADS_URL}")
                Log.d(TAG, "Show URL : ${AdsResponse.isShowAdsURL}")
                Log.d(TAG, "Click Count : ${AdsResponse.CLICK_THRESHOLD}")
                Log.d(TAG, "web_native_ads = '${remoteConfig.getString("web_native_ads")}'")
                Log.d(
                    TAG,
                    "large_web_native_img = '${remoteConfig.getString("large_web_native_img")}'"
                )
                Log.d(
                    TAG,
                    "small_web_native_img = '${remoteConfig.getString("small_web_native_img")}'"
                )

                onComplete?.invoke()
            }
    }
}