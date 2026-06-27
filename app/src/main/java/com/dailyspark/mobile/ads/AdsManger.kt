package com.dailyspark.mobile.ads

import android.app.Activity
import android.app.ProgressDialog

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdsManager {

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var interstitialAd: InterstitialAd? = null

    private const val APP_OPEN_AD_ID =
        "ca-app-pub-3940256099942544/9257395921"
    private const val INTERSTITIAL_AD_ID =
        "ca-app-pub-3940256099942544/1033173712"
    private const val INTERSTITIAL_CLICK_THRESHOLD = 10
    private var clickCount = 0

    fun setClickCount(count: Int) {
        clickCount = count
    }

    fun getClickCount(): Int = clickCount

    private fun safeComplete(
        onComplete: () -> Unit,
        completed: java.util.concurrent.atomic.AtomicBoolean
    ) {
        if (completed.compareAndSet(false, true)) {
            onComplete()
        }
    }

    fun loadAppOpen(
        context: Context,
        onLoaded: (() -> Unit)? = null
    ) {
        AppOpenAd.load(
            context,
            APP_OPEN_AD_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    onLoaded?.invoke()
                }
            }
        )
    }

    fun showAppOpen(
        activity: Activity,
        onComplete: () -> Unit
    ) {

        if (appOpenAd != null) {
            showLoadedAd(activity, onComplete)
            return
        }

        if (isLoading) {
            onComplete()
            return
        }

        isLoading = true

        AppOpenAd.load(
            activity,
            APP_OPEN_AD_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    appOpenAd = ad

                    showLoadedAd(activity, onComplete)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    appOpenAd = null

                    onComplete()
                }
            }
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        onComplete: () -> Unit
    ) {

        val ad = appOpenAd

        if (ad == null) {
            onComplete()
            return
        }

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(
                    error: AdError
                ) {
                    appOpenAd = null
                    onComplete()
                }
            }

        ad.show(activity)
    }
    fun showInterstitialDirect(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        showInterstitial(activity, onComplete)
    }

    fun loadInterstitial(
        context: Context,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (interstitialAd != null || isLoading) {
            return
        }

        isLoading = true

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    onFailed?.invoke(error)
                }
            }
        )
    }
    fun onUserAction(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        clickCount++

        if (clickCount < INTERSTITIAL_CLICK_THRESHOLD) {
            onComplete()
            return
        }

        clickCount = 0
        showInterstitial(activity, onComplete)
    }

    fun showInterstitial(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)

        if (interstitialAd != null) {
            showLoadedAd(activity, completed, onComplete)
            return
        }

        val loadingDialog = createLoadingDialog(activity)
        safeShowDialog(loadingDialog)

        InterstitialAd.load(
            activity,
            INTERSTITIAL_AD_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    safeDismissDialog(loadingDialog)

                    if (activity.isFinishing || activity.isDestroyed) {
                        safeComplete(onComplete, completed)
                        return
                    }

                    showLoadedAd(activity, completed, onComplete)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    safeDismissDialog(loadingDialog)
                    safeComplete(onComplete, completed)
                }
            }
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        completed: java.util.concurrent.atomic.AtomicBoolean,
        onComplete: () -> Unit
    ) {
        val ad = interstitialAd

        if (ad == null) {
            safeComplete(onComplete, completed)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                safeComplete(onComplete, completed)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                safeComplete(onComplete, completed)
            }

            override fun onAdShowedFullScreenContent() {
                interstitialAd = null
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            interstitialAd = null
            safeComplete(onComplete, completed)
        }
    }


    private fun createLoadingDialog(activity: Activity): ProgressDialog {
        return ProgressDialog(activity).apply {
            setMessage("Loading ad...")
            setCancelable(false)
        }
    }

    private fun safeShowDialog(dialog: ProgressDialog) {
        try {
            if (!dialog.isShowing) dialog.show()
        } catch (_: Exception) {
            // ignore - activity state issues etc
        }
    }

    private fun safeDismissDialog(dialog: ProgressDialog) {
        try {
            if (dialog.isShowing) dialog.dismiss()
        } catch (_: Exception) {
            // ignore - activity might be finishing/destroyed
        }
    }

    fun isAdLoaded(): Boolean = interstitialAd != null



}