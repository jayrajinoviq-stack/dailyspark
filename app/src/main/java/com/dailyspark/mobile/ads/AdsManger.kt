package com.dailyspark.mobile.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdsManager {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var clickCount = 0
    private const val INTERSTITIAL_CLICK_THRESHOLD = 10

    fun showInterstitialDirect(activity: Activity, onComplete: () -> Unit) {
        if (!AdState.canShowAd()) {
            onComplete()
            return
        }

        if (interstitialAd != null) {
            showActualAd(activity, onComplete)
        } else {
            loadInterstitial(activity)
            onComplete()
        }
    }

    fun onUserAction(activity: Activity, onComplete: () -> Unit) {
        clickCount++

        if (clickCount >= INTERSTITIAL_CLICK_THRESHOLD) {
            if (AdState.canShowAd() && interstitialAd != null) {
                clickCount = 0
                showActualAd(activity, onComplete)
                return
            }
        }

        onComplete()
    }

    private fun showActualAd(activity: Activity, onComplete: () -> Unit) {
        val ad = interstitialAd ?: return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdState.isAnyAdShowing = true
                interstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                AdState.isAnyAdShowing = false
                onComplete()
                loadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdState.isAnyAdShowing = false
                onComplete()
                loadInterstitial(activity)
            }
        }

        AdState.isAnyAdShowing = true
        ad.show(activity)
    }

    fun loadInterstitial(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdsResponse.INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                }
            })
    }
}