//package com.dailyspark.mobile.ads
//
//import android.app.Activity
//import android.app.Dialog
//import android.content.Context
//import android.graphics.Color
//import android.graphics.drawable.ColorDrawable
//import android.os.Handler
//import android.os.Looper
//import android.view.Window
//import android.widget.ProgressBar
//import com.dailyspark.mobile.AdNetworkHelper
//import com.google.android.gms.ads.AdError
//import com.google.android.gms.ads.AdRequest
//import com.google.android.gms.ads.FullScreenContentCallback
//import com.google.android.gms.ads.LoadAdError
//import com.google.android.gms.ads.interstitial.InterstitialAd
//import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
//import java.lang.ref.WeakReference
//import kotlin.math.min
//
//object InterstitialAdManager {
//
//    private enum class ManagerState {
//        IDLE, LOADING, READY, WAITING_FOR_LOAD, SHOWING
//    }
//    private var currentState = ManagerState.IDLE
//    private var interstitialAd: InterstitialAd? = null
//    private var clickCount = 0
//    private const val INTERSTITIAL_CLICK_THRESHOLD = 10
//    private val mainHandler = Handler(Looper.getMainLooper())
//    private var timeoutRunnable: Runnable? = null
//    private const val AD_LOAD_TIMEOUT_MS = 4000L
//    private var retryDelayMs = 5000L
//    private const val MAX_RETRY_DELAY_MS = 60000L
//    private var pendingAction: (() -> Unit)? = null
//    private var pendingActivityRef: WeakReference<Activity>? = null
//    private var loadingDialog: Dialog? = null
//    fun onUserAction(activity: Activity, onComplete: () -> Unit) {
//        if (currentState == ManagerState.SHOWING || currentState == ManagerState.WAITING_FOR_LOAD || AdState.isAnyAdShowing) {
//            return
//        }
//
//        clickCount++
//
//        if (clickCount >= INTERSTITIAL_CLICK_THRESHOLD) {
//            if (AdState.canShowAd()) {
//                clickCount = 0
//                showOrLoadAndShow(activity, onComplete)
//                return
//            }
//        }
//
//        onComplete()
//    }
//
//    fun showInterstitialDirect(activity: Activity, onComplete: () -> Unit) {
//        if (currentState == ManagerState.SHOWING || currentState == ManagerState.WAITING_FOR_LOAD || AdState.isAnyAdShowing) {
//            return
//        }
//
//        if (!AdState.canShowAd()) {
//            onComplete()
//            return
//        }
//
//        showOrLoadAndShow(activity, onComplete)
//    }
//
//    private fun showOrLoadAndShow(activity: Activity, onComplete: () -> Unit) {
//        if (currentState == ManagerState.READY && interstitialAd != null) {
//            showActualAd(activity, onComplete)
//        } else {
//            if (!AdNetworkHelper.isInternetAvailable(activity)) {
//                onComplete()
//                return
//            }
//            startAdLoadWithProgress(activity, onComplete)
//        }
//    }
//
//    private fun startAdLoadWithProgress(activity: Activity, onComplete: () -> Unit) {
//        showLoading(activity)
//
//        pendingAction = onComplete
//        pendingActivityRef = WeakReference(activity)
//
//        val previousState = currentState
//        currentState = ManagerState.WAITING_FOR_LOAD
//
//        timeoutRunnable = Runnable {
//            handleLoadTimeoutOrFailure()
//        }
//        timeoutRunnable?.let { mainHandler.postDelayed(it, AD_LOAD_TIMEOUT_MS) }
//
//        if (previousState != ManagerState.LOADING && interstitialAd == null) {
//            loadInterstitialInternal(activity.applicationContext)
//        }
//    }
//
//    private fun handleLoadTimeoutOrFailure() {
//        cancelTimeout()
//        dismissLoading()
//
//        val action = pendingAction
//        val activity = pendingActivityRef?.get()
//
//        clearPending()
//        currentState = ManagerState.IDLE
//
//        action?.invoke()
//
//        activity?.let { scheduleRetry(it.applicationContext) }
//    }
//
//    private fun showActualAd(activity: Activity, onComplete: () -> Unit) {
//        val ad = interstitialAd ?: run {
//            currentState = ManagerState.IDLE
//            onComplete()
//            return
//        }
//
//        val activityWeakReference = WeakReference(activity)
//
//        currentState = ManagerState.SHOWING
//        AdState.isAnyAdShowing = true
//
//        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
//            override fun onAdShowedFullScreenContent() {
//                interstitialAd = null
//            }
//
//            override fun onAdDismissedFullScreenContent() {
//                handleAdClosure()
//            }
//
//            override fun onAdFailedToShowFullScreenContent(error: AdError) {
//                handleAdClosure()
//            }
//
//            private fun handleAdClosure() {
//                currentState = ManagerState.IDLE
//                AdState.isAnyAdShowing = false
//                ad.fullScreenContentCallback = null
//
//                val currentActivity = activityWeakReference.get()
//                if (currentActivity != null && !currentActivity.isFinishing && !currentActivity.isDestroyed) {
//                    onComplete()
//                    loadInterstitial(currentActivity.applicationContext)
//                } else {
//                    onComplete()
//                }
//            }
//        }
//
//        val currentActivity = activityWeakReference.get()
//        if (currentActivity != null && !currentActivity.isFinishing && !currentActivity.isDestroyed) {
//            ad.show(currentActivity)
//        } else {
//            currentState = ManagerState.IDLE
//            AdState.isAnyAdShowing = false
//            onComplete()
//        }
//    }
//
//    fun loadInterstitial(context: Context) {
//        if (currentState != ManagerState.IDLE || interstitialAd != null) {
//            return
//        }
//        if (!AdNetworkHelper.isInternetAvailable(context)) {
//            scheduleRetry(context)
//            return
//        }
//        currentState = ManagerState.LOADING
//        loadInterstitialInternal(context.applicationContext)
//    }
//
//    private fun loadInterstitialInternal(appContext: Context) {
//        val adRequest = AdRequest.Builder().build()
//
//        InterstitialAd.load(
//            appContext,
//            AdsResponse.INTERSTITIAL_ID,
//            adRequest,
//            object : InterstitialAdLoadCallback() {
//                override fun onAdLoaded(ad: InterstitialAd) {
//                    interstitialAd = ad
//                    retryDelayMs = 5000L // Reset delay
//
//                    if (currentState == ManagerState.WAITING_FOR_LOAD) {
//                        cancelTimeout()
//                        dismissLoading()
//
//                        val activity = pendingActivityRef?.get()
//                        val action = pendingAction
//
//                        clearPending()
//
//                        if (activity != null && !activity.isFinishing && !activity.isDestroyed && action != null) {
//                            showActualAd(activity, action)
//                        } else {
//                            currentState = ManagerState.READY
//                            action?.invoke()
//                        }
//                    } else {
//                        currentState = ManagerState.READY
//                    }
//                }
//
//                override fun onAdFailedToLoad(error: LoadAdError) {
//                    interstitialAd = null
//
//                    if (currentState == ManagerState.WAITING_FOR_LOAD) {
//                        handleLoadTimeoutOrFailure()
//                    } else {
//                        currentState = ManagerState.IDLE
//                        scheduleRetry(appContext)
//                    }
//                }
//            }
//        )
//    }
//
//    private fun scheduleRetry(context: Context) {
//        mainHandler.removeCallbacksAndMessages(null)
//        mainHandler.postDelayed({
//            loadInterstitial(context)
//        }, retryDelayMs)
//        retryDelayMs = min(retryDelayMs * 2, MAX_RETRY_DELAY_MS)
//    }
//
//    private fun showLoading(activity: Activity) {
//        if (activity.isFinishing || activity.isDestroyed) return
//        dismissLoading()
//
//        try {
//            val dialog = Dialog(activity)
//            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//            dialog.setCancelable(false)
//
//            val progressBar = ProgressBar(activity)
//            dialog.setContentView(progressBar)
//
//            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//            dialog.show()
//            loadingDialog = dialog
//        } catch (e: Exception) {
//            // Catches case where parent reference is no longer valid
//        }
//    }
//
//    private fun dismissLoading() {
//        try {
//            loadingDialog?.let {
//                if (it.isShowing) {
//                    it.dismiss()
//                }
//            }
//        } catch (e: Exception) {
//        } finally {
//            loadingDialog = null
//        }
//    }
//
//    private fun cancelTimeout() {
//        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
//        timeoutRunnable = null
//    }
//
//    private fun clearPending() {
//        pendingAction = null
//        pendingActivityRef = null
//    }
//}


package com.dailyspark.mobile.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.dailyspark.mobile.AdNetworkHelper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference
import kotlin.math.min

object InterstitialAdManager {

    private enum class ManagerState {
        IDLE, LOADING, READY, WAITING_FOR_LOAD, SHOWING
    }

    private var currentState = ManagerState.IDLE
    private var interstitialAd: InterstitialAd? = null
    private var clickCount = 0
    private const val INTERSTITIAL_CLICK_THRESHOLD = 10
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private const val AD_LOAD_TIMEOUT_MS = 4000L
    private var retryDelayMs = 5000L
    private const val MAX_RETRY_DELAY_MS = 60000L
    private var pendingAction: (() -> Unit)? = null
    private var pendingActivityRef: WeakReference<Activity>? = null
    private var loadingDialog: Dialog? = null

    fun onUserAction(activity: Activity, onComplete: () -> Unit) {
        if (currentState == ManagerState.SHOWING || currentState == ManagerState.WAITING_FOR_LOAD || AdState.isAnyAdShowing) {
            return
        }

        clickCount++

        if (clickCount >= INTERSTITIAL_CLICK_THRESHOLD) {
            if (AdsResponse.isShowAdsURL) {
                clickCount = 0
                showUrlAd(activity, onComplete)
                return
            } else if (AdState.canShowAd()) {
                clickCount = 0
                showOrLoadAndShow(activity, onComplete)
                return
            }
        }

        onComplete()
    }

    fun showInterstitialDirect(activity: Activity, onComplete: () -> Unit) {
        if (currentState == ManagerState.SHOWING || currentState == ManagerState.WAITING_FOR_LOAD || AdState.isAnyAdShowing) {
            return
        }

        if (AdsResponse.isShowAdsURL) {
            showUrlAd(activity, onComplete)
            return
        }

        if (!AdState.canShowAd()) {
            onComplete()
            return
        }

        showOrLoadAndShow(activity, onComplete)
    }

    private fun showOrLoadAndShow(activity: Activity, onComplete: () -> Unit) {
        if (currentState == ManagerState.READY && interstitialAd != null) {
            showActualAd(activity, onComplete)
        } else {
            if (!AdNetworkHelper.isInternetAvailable(activity)) {
                onComplete()
                return
            }
            startAdLoadWithProgress(activity, onComplete)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showUrlAd(activity: Activity, onComplete: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        if (!AdNetworkHelper.isInternetAvailable(activity)) {
            onComplete()
            return
        }

        try {
            val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)

            val rootLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
            }

            val webView = WebView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            }

            val progressBar = ProgressBar(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }

            val sizeInDp = 36
            val sizeInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sizeInDp.toFloat(),
                activity.resources.displayMetrics
            ).toInt()

            val closeButton = TextView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        16f,
                        activity.resources.displayMetrics
                    ).toInt()
                    rightMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        16f,
                        activity.resources.displayMetrics
                    ).toInt()
                }
                text = "✕"
                textSize = 18f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD

                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#80000000"))
                }
                background = shape

                setOnClickListener {
                    try {
                        webView.stopLoading()
                        webView.destroy()
                    } catch (e: Exception) {
                    }
                    dialog.dismiss()
                    onComplete()
                }
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = android.view.View.GONE
                }
            }

            webView.webChromeClient = object : WebChromeClient() {}

            rootLayout.addView(webView)
            rootLayout.addView(progressBar)
            rootLayout.addView(closeButton)

            dialog.setContentView(rootLayout)
            dialog.setOnDismissListener {
                AdState.isAnyAdShowing = false
            }

            AdState.isAnyAdShowing = true
            dialog.show()
            webView.loadUrl(AdsResponse.ADS_URL)

        } catch (e: Exception) {
            onComplete()
        }
    }

    private fun startAdLoadWithProgress(activity: Activity, onComplete: () -> Unit) {
        showLoading(activity)

        pendingAction = onComplete
        pendingActivityRef = WeakReference(activity)

        val previousState = currentState
        currentState = ManagerState.WAITING_FOR_LOAD

        timeoutRunnable = Runnable {
            handleLoadTimeoutOrFailure()
        }
        timeoutRunnable?.let { mainHandler.postDelayed(it, AD_LOAD_TIMEOUT_MS) }

        if (previousState != ManagerState.LOADING && interstitialAd == null) {
            loadInterstitialInternal(activity.applicationContext)
        }
    }

    private fun handleLoadTimeoutOrFailure() {
        cancelTimeout()
        dismissLoading()

        val action = pendingAction
        val activity = pendingActivityRef?.get()

        clearPending()
        currentState = ManagerState.IDLE

        action?.invoke()

        activity?.let { scheduleRetry(it.applicationContext) }
    }

    private fun showActualAd(activity: Activity, onComplete: () -> Unit) {
        val ad = interstitialAd ?: run {
            currentState = ManagerState.IDLE
            onComplete()
            return
        }

        val activityWeakReference = WeakReference(activity)

        currentState = ManagerState.SHOWING
        AdState.isAnyAdShowing = true

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                interstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                handleAdClosure()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                handleAdClosure()
            }

            private fun handleAdClosure() {
                currentState = ManagerState.IDLE
                AdState.isAnyAdShowing = false
                ad.fullScreenContentCallback = null

                val currentActivity = activityWeakReference.get()
                if (currentActivity != null && !currentActivity.isFinishing && !currentActivity.isDestroyed) {
                    onComplete()
                    loadInterstitial(currentActivity.applicationContext)
                } else {
                    onComplete()
                }
            }
        }

        val currentActivity = activityWeakReference.get()
        if (currentActivity != null && !currentActivity.isFinishing && !currentActivity.isDestroyed) {
            ad.show(currentActivity)
        } else {
            currentState = ManagerState.IDLE
            AdState.isAnyAdShowing = false
            onComplete()
        }
    }

    fun loadInterstitial(context: Context) {
        if (AdsResponse.isShowAdsURL) return
        if (currentState != ManagerState.IDLE || interstitialAd != null) {
            return
        }
        if (!AdNetworkHelper.isInternetAvailable(context)) {
            scheduleRetry(context)
            return
        }
        currentState = ManagerState.LOADING
        loadInterstitialInternal(context.applicationContext)
    }

    private fun loadInterstitialInternal(appContext: Context) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            AdsResponse.INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    retryDelayMs = 5000L // Reset delay

                    if (currentState == ManagerState.WAITING_FOR_LOAD) {
                        cancelTimeout()
                        dismissLoading()

                        val activity = pendingActivityRef?.get()
                        val action = pendingAction

                        clearPending()

                        if (activity != null && !activity.isFinishing && !activity.isDestroyed && action != null) {
                            showActualAd(activity, action)
                        } else {
                            currentState = ManagerState.READY
                            action?.invoke()
                        }
                    } else {
                        currentState = ManagerState.READY
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null

                    if (currentState == ManagerState.WAITING_FOR_LOAD) {
                        handleLoadTimeoutOrFailure()
                    } else {
                        currentState = ManagerState.IDLE
                        scheduleRetry(appContext)
                    }
                }
            }
        )
    }

    private fun scheduleRetry(context: Context) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            loadInterstitial(context)
        }, retryDelayMs)
        retryDelayMs = min(retryDelayMs * 2, MAX_RETRY_DELAY_MS)
    }

    private fun showLoading(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        dismissLoading()

        try {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)

            val progressBar = ProgressBar(activity)
            dialog.setContentView(progressBar)

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
            loadingDialog = dialog
        } catch (e: Exception) {
            // Catches case where parent reference is no longer valid
        }
    }

    private fun dismissLoading() {
        try {
            loadingDialog?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
        } catch (e: Exception) {
        } finally {
            loadingDialog = null
        }
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun clearPending() {
        pendingAction = null
        pendingActivityRef = null
    }
}