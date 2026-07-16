//package com.dailyspark.mobile.ads
//
//import android.app.Activity
//import android.app.Application
//import android.content.Context
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import androidx.lifecycle.DefaultLifecycleObserver
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.ProcessLifecycleOwner
//import com.dailyspark.mobile.AdNetworkHelper
//import com.google.android.gms.ads.AdError
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.FullScreenContentCallback
//import com.google.android.gms.ads.LoadAdError
//import com.google.android.gms.ads.appopen.AppOpenAd
//import java.lang.ref.WeakReference
//import java.util.Date
//import kotlin.math.min
//
//object AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
//
//    private var appOpenAd: AppOpenAd? = null
//    private var isLoadingAd = false
//    private var loadTime: Long = 0
//    private var currentActivityRef: WeakReference<Activity>? = null
//    private var isInitialized = false
//    private val handler = Handler(Looper.getMainLooper())
//    private var retryDelayMs = 5000L
//    private const val MAX_RETRY_DELAY_MS = 60000L
//    private var applicationContext: Context? = null
//
//    fun init(application: Application) {
//        if (isInitialized) return
//        isInitialized = true
//        applicationContext = application.applicationContext
//        application.registerActivityLifecycleCallbacks(this)
//        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//        loadAd(application)
//    }
//
////    private fun loadAd(context: Context) {
////        if (isLoadingAd || isAdAvailable()) return
////
////        if (!AdNetworkHelper.isInternetAvailable(context)) {
////            scheduleRetry(context)
////            return
////        }
////
////        isLoadingAd = true
////        val request = AdRequest.Builder().build()
////        AppOpenAd.load(
////            context,
////            AdsResponse.APP_OPEN_ID,
////            request,
////            object : AppOpenAd.AppOpenAdLoadCallback() {
////                override fun onAdLoaded(ad: AppOpenAd) {
////                    appOpenAd = ad
////                    isLoadingAd = false
////                    loadTime = Date().time
////                    retryDelayMs = 5000L
////                }
////
////                override fun onAdFailedToLoad(error: LoadAdError) {
////                    isLoadingAd = false
////                    scheduleRetry(context)
////                }
////            })
////    }
//
//    private fun loadAd(context: Context) {
//        if (isLoadingAd || isAdAvailable() || AdsResponse.isShowAdsURL) return
//
//        if (!AdNetworkHelper.isInternetAvailable(context)) {
//            scheduleRetry(context)
//            return
//        }
//
//        isLoadingAd = true
//        val request = AdRequest.Builder().build()
//        AppOpenAd.load(
//            context,
//            AdsResponse.APP_OPEN_ID,
//            request,
//            object : AppOpenAd.AppOpenAdLoadCallback() {
//                override fun onAdLoaded(ad: AppOpenAd) {
//                    appOpenAd = ad
//                    isLoadingAd = false
//                    loadTime = Date().time
//                    retryDelayMs = 5000L
//                }
//
//                override fun onAdFailedToLoad(error: LoadAdError) {
//                    isLoadingAd = false
//                    scheduleRetry(context)
//                }
//            })
//    }
//
//    private fun scheduleRetry(context: Context) {
//        handler.removeCallbacksAndMessages(null)
//        handler.postDelayed({
//            loadAd(context)
//        }, retryDelayMs)
//        retryDelayMs = min(retryDelayMs * 2, MAX_RETRY_DELAY_MS)
//    }
//
//    private fun isAdAvailable(): Boolean {
//        return appOpenAd != null && (Date().time - loadTime) < 3600000 * 4
//    }
//
////    fun showAdOnSplash(activity: Activity, onComplete: () -> Unit) {
////        if (!AdState.canShowAd()) {
////            onComplete()
////            return
////        }
////
////        if (isAdAvailable()) {
////            showAdInternal(activity, onComplete)
////        } else {
////            if (!AdNetworkHelper.isInternetAvailable(activity)) {
////                onComplete()
////                return
////            }
////
////            AdState.isAdLoadingToShow = true
////            val timeoutRunnable = Runnable {
////                if (AdState.isAdLoadingToShow) {
////                    AdState.isAdLoadingToShow = false
////                    onComplete()
////                }
////            }
////            handler.postDelayed(timeoutRunnable, 5000)
////
////            AppOpenAd.load(
////                activity,
////                AdsResponse.APP_OPEN_ID,
////                AdRequest.Builder().build(),
////                object : AppOpenAd.AppOpenAdLoadCallback() {
////                    override fun onAdLoaded(ad: AppOpenAd) {
////                        handler.removeCallbacks(timeoutRunnable)
////                        AdState.isAdLoadingToShow = false
////                        appOpenAd = ad
////                        showAdInternal(activity, onComplete)
////                    }
////
////                    override fun onAdFailedToLoad(p0: LoadAdError) {
////                        handler.removeCallbacks(timeoutRunnable)
////                        AdState.isAdLoadingToShow = false
////                        onComplete()
////                    }
////                })
////        }
////    }
//
//    fun showAdOnSplash(activity: Activity, onComplete: () -> Unit) {
//        if (AdsResponse.isShowAdsURL) {
//            onComplete()
//            return
//        }
//
//        if (!AdState.canShowAd()) {
//            onComplete()
//            return
//        }
//
//        if (isAdAvailable()) {
//            showAdInternal(activity, onComplete)
//        } else {
//            if (!AdNetworkHelper.isInternetAvailable(activity)) {
//                onComplete()
//                return
//            }
//
//            AdState.isAdLoadingToShow = true
//            val timeoutRunnable = Runnable {
//                if (AdState.isAdLoadingToShow) {
//                    AdState.isAdLoadingToShow = false
//                    onComplete()
//                }
//            }
//            handler.postDelayed(timeoutRunnable, 5000)
//
//            AppOpenAd.load(
//                activity,
//                AdsResponse.APP_OPEN_ID,
//                AdRequest.Builder().build(),
//                object : AppOpenAd.AppOpenAdLoadCallback() {
//                    override fun onAdLoaded(ad: AppOpenAd) {
//                        handler.removeCallbacks(timeoutRunnable)
//                        AdState.isAdLoadingToShow = false
//                        appOpenAd = ad
//                        showAdInternal(activity, onComplete)
//                    }
//
//                    override fun onAdFailedToLoad(p0: LoadAdError) {
//                        handler.removeCallbacks(timeoutRunnable)
//                        AdState.isAdLoadingToShow = false
//                        onComplete()
//                    }
//                })
//        }
//    }
//
//    private fun showAdInternal(activity: Activity, onComplete: () -> Unit) {
//        val ad = appOpenAd ?: run {
//            onComplete()
//            return
//        }
//
//        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
//            override fun onAdDismissedFullScreenContent() {
//                appOpenAd = null
//                AdState.isAnyAdShowing = false
//                onComplete()
//                loadAd(activity)
//            }
//
//            override fun onAdFailedToShowFullScreenContent(e: AdError) {
//                appOpenAd = null
//                AdState.isAnyAdShowing = false
//                onComplete()
//            }
//
//            override fun onAdShowedFullScreenContent() {
//                AdState.isAnyAdShowing = true
//            }
//        }
//        ad.show(activity)
//    }
//
//    override fun onStart(owner: LifecycleOwner) {
//        val activity = currentActivityRef?.get() ?: return
//        if (activity.javaClass.simpleName.contains("Splash", ignoreCase = true)) return
//        if (AdsResponse.isShowAdsURL) return
//
//        if (AdState.canShowAd() && isAdAvailable()) {
//            showAdInternal(activity) {}
//        } else {
//            applicationContext?.let { loadAd(it) }
//        }
//    }
//
//    override fun onActivityResumed(activity: Activity) {
//        currentActivityRef = WeakReference(activity)
//    }
//
//    override fun onActivityStarted(activity: Activity) {
//        currentActivityRef = WeakReference(activity)
//    }
//
//    override fun onActivityDestroyed(activity: Activity) {
//        if (currentActivityRef?.get() == activity) currentActivityRef = null
//    }
//
//    fun showAdOnMain(
//        activity: Activity,
//        onAdLoadStart: () -> Unit = {},
//        onComplete: () -> Unit
//    ) {
//        if (AdsResponse.isShowAdsURL) {
//            onComplete()
//            return
//        }
//        if (!AdState.canShowAd()) {
//            onComplete()
//            return
//        }
//
//        if (isAdAvailable()) {
//            showAdInternal(activity, onComplete)
//            return
//        }
//
//        if (!AdNetworkHelper.isInternetAvailable(activity)) {
//            onComplete()
//            return
//        }
//
//        onAdLoadStart()
//        AdState.isAdLoadingToShow = true
//        val timeoutRunnable = Runnable {
//            if (AdState.isAdLoadingToShow) {
//                AdState.isAdLoadingToShow = false
//                onComplete()
//            }
//        }
//        handler.postDelayed(timeoutRunnable, 5000)
//
//        AppOpenAd.load(
//            activity,
//            AdsResponse.APP_OPEN_ID,
//            AdRequest.Builder().build(),
//            object : AppOpenAd.AppOpenAdLoadCallback() {
//                override fun onAdLoaded(ad: AppOpenAd) {
//                    handler.removeCallbacks(timeoutRunnable)
//                    AdState.isAdLoadingToShow = false
//                    appOpenAd = ad
//                    showAdInternal(activity, onComplete)
//                }
//
//                override fun onAdFailedToLoad(p0: LoadAdError) {
//                    handler.removeCallbacks(timeoutRunnable)
//                    AdState.isAdLoadingToShow = false
//                    onComplete()
//                }
//            })
//    }
//
//
//    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
//    override fun onActivityPaused(activity: Activity) {}
//    override fun onActivityStopped(activity: Activity) {}
//    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
//}


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
import com.dailyspark.mobile.AdNetworkHelper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.lang.ref.WeakReference
import java.util.Date
import kotlin.math.min

object AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime: Long = 0
    private var currentActivityRef: WeakReference<Activity>? = null
    private var isInitialized = false
    private val handler = Handler(Looper.getMainLooper())
    private var retryDelayMs = 5000L
    private const val MAX_RETRY_DELAY_MS = 60000L
    private var applicationContext: Context? = null

    // --- Race-condition guard ---------------------------------------------------
    // Both onStart() (automatic, fires on every foreground) and showAdOnMain()
    // (explicit, called from MainActivity.onCreate) can attempt to show the SAME
    // cached ad at nearly the same moment on cold start. Without this flag, both
    // can pass their own local checks before either one gets to
    // AdState.isAnyAdShowing = true, and you get two competing show attempts
    // ("override"). tryClaim() makes the check-and-set atomic: whichever caller
    // reaches it first wins the attempt; the other backs off immediately and
    // just proceeds with its own flow (e.g. onComplete()), no ad shown twice.
    @Volatile
    private var isAttemptInProgress = false

    private fun tryClaim(): Boolean {
        if (isAttemptInProgress || AdState.isAnyAdShowing) return false
        isAttemptInProgress = true
        return true
    }

    private fun release() {
        isAttemptInProgress = false
    }
    // ------------------------------------------------------------------------------

    fun init(application: Application) {
        if (isInitialized) return
        isInitialized = true
        applicationContext = application.applicationContext
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        loadAd(application)
    }

    private fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable() || AdsResponse.isShowAdsURL) return

        if (!AdNetworkHelper.isInternetAvailable(context)) {
            scheduleRetry(context)
            return
        }

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
                    retryDelayMs = 5000L
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    scheduleRetry(context)
                }
            })
    }

    private fun scheduleRetry(context: Context) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            loadAd(context)
        }, retryDelayMs)
        retryDelayMs = min(retryDelayMs * 2, MAX_RETRY_DELAY_MS)
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && (Date().time - loadTime) < 3600000 * 4
    }

    /**
     * Called from SplashActivity when NOT in URL-ad mode. Kept for API
     * compatibility with your existing call sites. Guarded with tryClaim()
     * so it can never collide with onStart() or showAdOnMain().
     */
    fun showAdOnSplash(activity: Activity, onComplete: () -> Unit) {
        if (AdsResponse.isShowAdsURL) {
            onComplete()
            return
        }

        if (!AdState.canShowAd()) {
            onComplete()
            return
        }

        if (!tryClaim()) {
            // Someone else (onStart / showAdOnMain) already owns a show attempt.
            onComplete()
            return
        }

        val finish: () -> Unit = { release(); onComplete() }

        if (isAdAvailable()) {
            showAdInternal(activity, finish)
        } else {
            if (!AdNetworkHelper.isInternetAvailable(activity)) {
                finish()
                return
            }

            AdState.isAdLoadingToShow = true
            val timeoutRunnable = Runnable {
                if (AdState.isAdLoadingToShow) {
                    AdState.isAdLoadingToShow = false
                    finish()
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
                        showAdInternal(activity, finish)
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        handler.removeCallbacks(timeoutRunnable)
                        AdState.isAdLoadingToShow = false
                        finish()
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

    /**
     * Fires automatically whenever the app process comes to the foreground
     * (app-switcher return, screen unlock while app is backgrounded, etc).
     * This is the "background / on resume" trigger you want app-open ads
     * shown from.
     */
    override fun onStart(owner: LifecycleOwner) {
        val activity = currentActivityRef?.get() ?: return
        if (activity.javaClass.simpleName.contains("Splash", ignoreCase = true)) return
        if (AdsResponse.isShowAdsURL) return

        if (!tryClaim()) return // MainActivity's cold-start call already owns this attempt

        if (AdState.canShowAd() && isAdAvailable()) {
            showAdInternal(activity) { release() }
        } else {
            release()
            applicationContext?.let { loadAd(it) }
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

    /**
     * Called once from MainActivity.onCreate() on cold start (hasShownAppOpenAdOnEntry
     * guard lives in MainActivity). This is the "app first opened, show app-open ad"
     * path. Guarded with tryClaim() so it can never collide with onStart().
     */
    fun showAdOnMain(
        activity: Activity,
        onAdLoadStart: () -> Unit = {},
        onComplete: () -> Unit
    ) {
        if (AdsResponse.isShowAdsURL) {
            onComplete()
            return
        }
        if (!AdState.canShowAd()) {
            onComplete()
            return
        }

        if (!tryClaim()) {
            // onStart() already grabbed this attempt (e.g. process was already
            // foregrounded by the time onCreate ran) — don't show twice, just move on.
            onComplete()
            return
        }

        val finish: () -> Unit = { release(); onComplete() }

        if (isAdAvailable()) {
            showAdInternal(activity, finish)
            return
        }

        if (!AdNetworkHelper.isInternetAvailable(activity)) {
            finish()
            return
        }

        onAdLoadStart()
        AdState.isAdLoadingToShow = true
        val timeoutRunnable = Runnable {
            if (AdState.isAdLoadingToShow) {
                AdState.isAdLoadingToShow = false
                finish()
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
                    showAdInternal(activity, finish)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    handler.removeCallbacks(timeoutRunnable)
                    AdState.isAdLoadingToShow = false
                    finish()
                }
            })
    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}