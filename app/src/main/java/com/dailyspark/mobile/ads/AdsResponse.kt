package com.dailyspark.mobile.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dailyspark.mobile.AdNetworkHelper
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object AdsResponse {

    private const val TAG = "AdsResponse"
    var isShowAdsURL: Boolean = false
    var ADS_URL: String = ""
    var INTERSTITIAL_CLICK_THRESHOLD: Int = 10

//    var INTERSTITIAL_ID: String = "ca-app-pub-3940256099942544/1033173712"
//    var NATIVE_ID: String = "ca-app-pub-3940256099942544/2247696110"
//    var APP_OPEN_ID: String = "ca-app-pub-3940256099942544/9257395921"

    //     Live Ads IDs
    var INTERSTITIAL_ID: String = "ca-app-pub-7210024420146479/2879605894"
    var NATIVE_ID: String = "ca-app-pub-7210024420146479/1566524228"
    var APP_OPEN_ID: String = "ca-app-pub-7210024420146479/5565744917"


    @Volatile
    var isConfigFetched: Boolean = false
    private var isFetching: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private const val FETCH_TIMEOUT_MS = 4000L
    private var firestoreListener: ListenerRegistration? = null
    fun fetchAdConfig(context: Context, onComplete: () -> Unit) {
        if (isConfigFetched) {
            onComplete()
            return
        }

        if (!AdNetworkHelper.isInternetAvailable(context)) {
            Log.d(TAG, "No internet available. Using default values.")
            onComplete()
            return
        }

        if (isFetching) {
            onComplete()
            return
        }

        isFetching = true
        var isInitialCallbackTriggered = false

        val timeoutRunnable = Runnable {
            if (!isInitialCallbackTriggered) {
                isInitialCallbackTriggered = true
                isFetching = false
                Log.d(TAG, "Initial Firestore fetch timed out. Proceeding with defaults.")
                onComplete()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, FETCH_TIMEOUT_MS)

        try {
            val db = FirebaseFirestore.getInstance()
            firestoreListener?.remove()
            firestoreListener = db.collection("config").document("appSettings")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e(TAG, "Firestore live listener failed", error)
                        if (!isInitialCallbackTriggered) {
                            isInitialCallbackTriggered = true
                            isFetching = false
                            mainHandler.removeCallbacks(timeoutRunnable)
                            onComplete()
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        parseFirestoreDocument(snapshot)
                    } else {
                        Log.d(TAG, "Document config/appSettings does not exist.")
                    }

                    if (!isInitialCallbackTriggered) {
                        isInitialCallbackTriggered = true
                        isFetching = false
                        mainHandler.removeCallbacks(timeoutRunnable)
                        onComplete()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing live Firestore listener", e)
            mainHandler.removeCallbacks(timeoutRunnable)
            if (!isInitialCallbackTriggered) {
                isInitialCallbackTriggered = true
                isFetching = false
                onComplete()
            }
        }
    }

    private fun parseFirestoreDocument(document: DocumentSnapshot) {
        try {
            isShowAdsURL = getBooleanValue(document, "isShowAdsURL") ?: isShowAdsURL

            ADS_URL = document.getString("AdsURL")
                ?: document.getString("adsUrl")
                        ?: document.getString("ads_url")
                        ?: ADS_URL

            INTERSTITIAL_CLICK_THRESHOLD = getIntValue(document, "clickCount")
                ?: INTERSTITIAL_CLICK_THRESHOLD

            isConfigFetched = true
            Log.d(
                TAG,
                "Live Change Detected!\n isShowAdsURL: $isShowAdsURL, \n ADS_URL: $ADS_URL, " +
                        "\nINTERSTITIAL_CLICK_THRESHOLD: $INTERSTITIAL_CLICK_THRESHOLD"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing live document updates", e)
        }
    }

    private fun getBooleanValue(document: DocumentSnapshot, key: String): Boolean? {
        if (!document.contains(key)) return null
        val value = document.get(key)
        if (value is Boolean) {
            return value
        }
        if (value is String) {
            return value.trim().lowercase() == "true"
        }
        return null
    }

    private fun getIntValue(document: DocumentSnapshot, key: String): Int? {
        if (!document.contains(key)) return null
        return when (val value = document.get(key)) {
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Int -> value
            is String -> value.trim().toIntOrNull()
            else -> null
        }
    }

}