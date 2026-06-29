package com.dailyspark.mobile.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.lang.ref.WeakReference
import java.util.Date

object AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivityRef: WeakReference<Activity>? = null
    private var isInitialized = false

    fun init(application: Application) {
        if (isInitialized) return
        isInitialized = true
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        loadAd(application)
    }

    private fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdsResponse.APP_OPEN_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                }
            })
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && (Date().time - loadTime) < 3600000 * 4
    }


    fun showAdOnSplash(activity: Activity, onComplete: () -> Unit) {
        if (!AdState.canShowAd()) {
            onComplete()
            return
        }

        if (isAdAvailable()) {
            showAdInternal(activity, onComplete)
        } else {
            AdState.isAdLoadingToShow = true
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (AdState.isAdLoadingToShow) {
                    AdState.isAdLoadingToShow = false
                    onComplete()
                }
            }
            handler.postDelayed(timeoutRunnable, 5000)

            AppOpenAd.load(
                activity,
                AdsResponse.APP_OPEN_ID,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        handler.removeCallbacks(timeoutRunnable)
                        AdState.isAdLoadingToShow = false
                        appOpenAd = ad
                        showAdInternal(activity, onComplete)
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        handler.removeCallbacks(timeoutRunnable)
                        AdState.isAdLoadingToShow = false
                        onComplete()
                    }
                })
        }
    }

    private fun showAdInternal(activity: Activity, onComplete: () -> Unit) {
        val ad = appOpenAd ?: run {
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                AdState.isAnyAdShowing = false
                onComplete()
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                appOpenAd = null
                AdState.isAnyAdShowing = false
                onComplete()
            }

            override fun onAdShowedFullScreenContent() {
                AdState.isAnyAdShowing = true
            }
        }
        ad.show(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        val activity = currentActivityRef?.get() ?: return
        if (activity.javaClass.simpleName.contains("Splash", ignoreCase = true)) return

        if (AdState.canShowAd() && isAdAvailable()) {
            showAdInternal(activity) {}
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) currentActivityRef = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}