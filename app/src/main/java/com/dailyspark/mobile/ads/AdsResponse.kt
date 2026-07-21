package com.dailyspark.mobile.ads

import android.util.Log
import com.dailyspark.mobile.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.gson.Gson

//object AdsResponse {
//    private const val TAG = "AdsResponse"
//    const val KEY_IS_SHOW_ADS = "isShowAdsURL"
//    const val KEY_ADS_URL = "AdsURL"
//    const val KEY_CLICK_THRESHOLD = "clickCount"
//    var isShowAdsURL: Boolean = false
//    var ADS_URL: String = ""
//    var CLICK_THRESHOLD: Long = 10
//    var INTERSTITIAL_ID: String = ""
//    var NATIVE_ID: String = ""
//    var APP_OPEN_ID: String = ""
//    var AdsLargeImgUrl: String = ""
//    var AdsImgClickURl: String = ""
//    var AdsSmallImgUrl: String = ""
//
//    fun initialize() {
//        if (BuildConfig.DEBUG) {
//            loadDebugAds()
//        } else {
//            loadReleaseAds()
//        }
//    }
//
//    fun loadDebugAds() {
//        isShowAdsURL = true
//        INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
//        NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
//        APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
//        ADS_URL = "https://www.testing.com/"
//        CLICK_THRESHOLD = Firebase.remoteConfig.getLong(KEY_CLICK_THRESHOLD)
//        AdsImgClickURl = Firebase.remoteConfig.getString(KEY_ADS_URL)
//        parseWebNativeAds()
//        AdsImgClickURl = Firebase.remoteConfig.getString(KEY_ADS_URL)
//    }
//
//    fun loadReleaseAds() {
//
//
//        ADS_URL = Firebase.remoteConfig.getString(KEY_ADS_URL)
//        CLICK_THRESHOLD = Firebase.remoteConfig.getLong(KEY_CLICK_THRESHOLD)
//        isShowAdsURL = Firebase.remoteConfig.getBoolean(KEY_IS_SHOW_ADS)
//
//        parseWebNativeAds()
//
//        AdsImgClickURl = Firebase.remoteConfig.getString(KEY_ADS_URL)
//    }
//
//    private fun parseWebNativeAds() {
////        AdsLargeImgUrl = ""
////        AdsSmallImgUrl = ""
//        val json = Firebase.remoteConfig.getString("web_native_ads")
//        if (json.isBlank()) {
//            Log.w(TAG, "web_native_ads is empty — Remote Config likely not fetched/activated yet")
//            return
//        }
//        try {
//            val jsonObject = Gson().fromJson(json, com.google.gson.JsonObject::class.java)
//            if (jsonObject == null) {
//                Log.w(TAG, "web_native_ads parsed to null JsonObject, raw value: $json")
//                return
//            }
//            AdsLargeImgUrl = jsonObject.get("large_web_native_img")?.asString ?: ""
//            AdsSmallImgUrl = jsonObject.get("small_web_native_img")?.asString ?: ""
//            Log.d(TAG, "Large Image: $AdsLargeImgUrl")
//            Log.d(TAG, "Small Image: $AdsSmallImgUrl")
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to parse web_native_ads", e)
//        }
//    }
//
//    fun fetchAdConfig(onComplete: (() -> Unit)? = null) {
//        RemoteConfigHelper.init(onComplete)
//    }
//
//
//}


object AdsResponse {
    private const val TAG = "AdsResponse"
    const val KEY_IS_SHOW_ADS = "isShowAdsURL"
    const val KEY_ADS_URL = "AdsURL"
    const val KEY_CLICK_THRESHOLD = "clickCount"
    var isShowAdsURL: Boolean = false
    var ADS_URL: String = ""
    var CLICK_THRESHOLD: Long = 10
    var INTERSTITIAL_ID: String = ""
    var NATIVE_ID: String = ""
    var APP_OPEN_ID: String = ""
    var AdsLargeImgUrl: String = ""
    var AdsImgClickURl: String = ""
    var AdsSmallImgUrl: String = ""
    var REWARDED_ID: String = ""

    fun initialize() {
        if (BuildConfig.DEBUG) {
            loadDebugAds()
        } else {
            loadReleaseAds()
        }
    }

    fun loadDebugAds() {
        isShowAdsURL = false
        INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
        REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

        ADS_URL = Firebase.remoteConfig.getString(KEY_ADS_URL)
        CLICK_THRESHOLD = 20
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
        INTERSTITIAL_ID = "ca-app-pub-7210024420146479/2879605894"
        NATIVE_ID = "ca-app-pub-7210024420146479/1566524228"
        APP_OPEN_ID = "ca-app-pub-7210024420146479/5565744917"

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