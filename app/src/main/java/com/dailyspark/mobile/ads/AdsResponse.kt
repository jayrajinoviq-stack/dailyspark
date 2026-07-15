package com.dailyspark.mobile.ads

import android.util.Log
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.gson.Gson

object AdsResponse {
    private const val TAG = "AdsResponse"
    const val KEY_IS_SHOW_ADS = "isShowAdsURL"
    const val KEY_ADS_URL = "AdsURL"
    const val KEY_CLICK_THRESHOLD = "clickCount"
    var isShowAdsURL: Boolean = false
    var ADS_URL: String = ""
    var CLICK_THRESHOLD: Long = 10
    var INTERSTITIAL_ID: String = "ca-app-pub-3940256099942544/1033173712"
    var NATIVE_ID: String = ""
    var APP_OPEN_ID: String = "ca-app-pub-3940256099942544/9257395921"
    var AdsLargeImgUrl: String = ""
    var AdsImgClickURl: String = ""
    var AdsSmallImgUrl: String = ""

    fun initialize() {
        if (BuildConfig.DEBUG) {
            loadDebugAds()
        } else {
            loadReleaseAds()
        }
    }
    fun loadDebugAds() {
        isShowAdsURL = true
        INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"


        ADS_URL = Firebase.remoteConfig.getString(KEY_ADS_URL)
        CLICK_THRESHOLD = Firebase.remoteConfig.getLong(KEY_CLICK_THRESHOLD)
//        AdsLargeImgUrl = ""
//        AdsSmallImgUrl =
//            ""
        AdsImgClickURl = Firebase.remoteConfig.getString(KEY_ADS_URL)

        try {
            val json = Firebase.remoteConfig.getString("web_native_ads")

            val jsonObject = Gson().fromJson(json, com.google.gson.JsonObject::class.java)

            AdsLargeImgUrl = jsonObject.get("large_web_native_img")?.asString ?: ""
            AdsSmallImgUrl = jsonObject.get("small_web_native_img")?.asString ?: ""

            Log.d(TAG, "Large Image: $AdsLargeImgUrl")
            Log.d(TAG, "Small Image: $AdsSmallImgUrl")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse web_native_ads", e)
            AdsLargeImgUrl = ""
            AdsSmallImgUrl = ""
        }

        AdsImgClickURl = Firebase.remoteConfig.getString(KEY_ADS_URL)

    }

    fun loadReleaseAds() {
//        INTERSTITIAL_ID = "ca-app-pub-7210024420146479/7665412188"
//        NATIVE_ID = "ca-app-pub-7210024420146479/8879291040"
//        APP_OPEN_ID = "ca-app-pub-7210024420146479/5215378204"


        var INTERSTITIAL_ID: String = "ca-app-pub-7210024420146479/2879605894"
        var NATIVE_ID: String = "ca-app-pub-7210024420146479/1566524228"
        var APP_OPEN_ID: String = "ca-app-pub-7210024420146479/5565744917"

        ADS_URL = Firebase.remoteConfig.getString(KEY_ADS_URL)
        CLICK_THRESHOLD = Firebase.remoteConfig.getLong(KEY_CLICK_THRESHOLD)
        isShowAdsURL = Firebase.remoteConfig.getBoolean(KEY_IS_SHOW_ADS)
        try {
            val json = Firebase.remoteConfig.getString("web_native_ads")
            val jsonObject = Gson().fromJson(json, com.google.gson.JsonObject::class.java)
            AdsLargeImgUrl = jsonObject.get("large_web_native_img")?.asString ?: ""
            AdsSmallImgUrl = jsonObject.get("small_web_native_img")?.asString ?: ""
            Log.d(TAG, "Large Image: $AdsLargeImgUrl")
            Log.d(TAG, "Small Image: $AdsSmallImgUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse web_native_ads", e)
            AdsLargeImgUrl = ""
            AdsSmallImgUrl = ""
        }
        AdsImgClickURl = Firebase.remoteConfig.getString(KEY_ADS_URL)
    }

    fun fetchAdConfig(onComplete: (() -> Unit)? = null) {
        RemoteConfigHelper.init(onComplete)
    }





}