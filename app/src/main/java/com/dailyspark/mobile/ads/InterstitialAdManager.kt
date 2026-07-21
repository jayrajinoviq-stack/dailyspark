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
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var webTimeoutRunnable: Runnable? = null
    private const val AD_LOAD_TIMEOUT_MS = 8000L
    private const val WEB_AD_TIMEOUT_MS = 8000L
    private var interactiveAttempt = 0
    private const val MAX_INTERACTIVE_ATTEMPTS = 2
    private var retryDelayMs = 5000L
    private const val MAX_RETRY_DELAY_MS = 60000L
    private var scheduledRetryRunnable: Runnable? = null

    private var pendingAction: (() -> Unit)? = null
    private var pendingActivityRef: WeakReference<Activity>? = null
    private var loadingDialog: Dialog? = null

    fun onUserAction(activity: Activity, onComplete: () -> Unit) {
        Log.d("AdManager", "onUserAction tapped, state=$currentState, clickCount=$clickCount")
        if (currentState == ManagerState.SHOWING || currentState == ManagerState.WAITING_FOR_LOAD || AdState.isAnyAdShowing) {
            return
        }
        clickCount++
        if (clickCount >= AdsResponse.CLICK_THRESHOLD) {
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

        var isCallbackInvoked = false

        fun invokeOnCompleteOnce() {
            if (!isCallbackInvoked) {
                isCallbackInvoked = true
                currentState = ManagerState.IDLE
                AdState.isAnyAdShowing = false
                cancelWebTimeout()
                onComplete()
            }
        }

        try {
            currentState = ManagerState.SHOWING
            AdState.isAnyAdShowing = true

            val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)

            val rootLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
                alpha = 0f
            }

            fun fadeOutThen(action: () -> Unit) {
                rootLayout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { action() }
                    .start()
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
                visibility = View.INVISIBLE
                alpha = 0f
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
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD

                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#80000000"))
                }
                background = shape

                setOnClickListener {
                    fadeOutThen {
                        try {
                            webView.stopLoading()
                            webView.destroy()
                        } catch (e: Exception) {
                        }
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                        invokeOnCompleteOnce()
                    }
                }
            }

            webTimeoutRunnable = Runnable {
                fadeOutThen {
                    try {
                        webView.stopLoading()
                        webView.destroy()
                    } catch (e: Exception) {
                        // Ignored
                    }
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    invokeOnCompleteOnce()
                }
            }
            mainHandler.postDelayed(webTimeoutRunnable!!, WEB_AD_TIMEOUT_MS)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    cancelWebTimeout()
                    progressBar.animate().alpha(0f).setDuration(150).withEndAction {
                        progressBar.visibility = View.GONE
                    }.start()
                    webView.visibility = View.VISIBLE
                    webView.animate().alpha(1f).setDuration(250).start()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        fadeOutThen {
                            try {
                                webView.stopLoading()
                            } catch (e: Exception) {
                            }
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            invokeOnCompleteOnce()
                        }
                    }
                }
            }

            rootLayout.addView(webView)
            rootLayout.addView(progressBar)
            rootLayout.addView(closeButton)

            dialog.setContentView(rootLayout)
            dialog.setOnDismissListener {
                invokeOnCompleteOnce()
            }

            dialog.show()
            rootLayout.animate().alpha(1f).setDuration(250).start()
            webView.loadUrl(AdsResponse.ADS_URL)

        } catch (e: Exception) {
            invokeOnCompleteOnce()
        }
    }

    private fun startAdLoadWithProgress(activity: Activity, onComplete: () -> Unit) {
        showLoading(activity)

        pendingAction = onComplete
        pendingActivityRef = WeakReference(activity)
        interactiveAttempt = 1

        val previousState = currentState
        currentState = ManagerState.WAITING_FOR_LOAD

        armLoadTimeout()

        if (previousState != ManagerState.LOADING && interstitialAd == null) {
            loadInterstitialInternal(activity.applicationContext)
        }
    }

    private fun armLoadTimeout() {
        cancelTimeout()
        val runnable = Runnable { onInteractiveLoadFailed() }
        timeoutRunnable = runnable
        mainHandler.postDelayed(runnable, AD_LOAD_TIMEOUT_MS)
    }


    private fun onInteractiveLoadFailed() {
        Log.d("AdManager", "onInteractiveLoadFailed, attempt=$interactiveAttempt, state=$currentState")
        cancelTimeout()
        interstitialAd = null

        val activity = pendingActivityRef?.get()
        val activityUsable = activity != null && !activity.isFinishing && !activity.isDestroyed

        if (interactiveAttempt < MAX_INTERACTIVE_ATTEMPTS && activityUsable) {
            // Try loading a fresh ad one more time before giving up.
            interactiveAttempt++
            armLoadTimeout()
            loadInterstitialInternal(activity!!.applicationContext)
            return
        }

        // Out of retries (or the screen is gone) — stop waiting and move on.
        dismissLoading()
        val action = pendingAction
        clearPending()
        currentState = ManagerState.IDLE

        if (activityUsable && action != null && AdsResponse.isShowAdsURL) {
            showUrlAd(activity!!, action)
        } else {
            action?.invoke()
            activity?.let { scheduleRetry(it.applicationContext) }
        }
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
                    interactiveAttempt = 0

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
                    Log.d(
                        "AdManager",
                        "onAdFailedToLoad code=${error.code} domain=${error.domain} message=${error.message}"
                    )
                    interstitialAd = null

                    if (currentState == ManagerState.WAITING_FOR_LOAD) {
                        onInteractiveLoadFailed()
                    } else {
                        currentState = ManagerState.IDLE
                        scheduleRetry(appContext)
                    }
                }
            }
        )
    }

    private fun scheduleRetry(context: Context) {
        Log.d("AdManager", "scheduleRetry, state=$currentState")
        scheduledRetryRunnable?.let { mainHandler.removeCallbacks(it) }

        val runnable = Runnable { loadInterstitial(context) }
        scheduledRetryRunnable = runnable
        mainHandler.postDelayed(runnable, retryDelayMs)
        retryDelayMs = min(retryDelayMs * 2, MAX_RETRY_DELAY_MS)
    }


    private fun showLoading(activity: Activity) {
        Log.d("AdManager", "showLoading called, state=$currentState")
        if (activity.isFinishing || activity.isDestroyed) return
        dismissLoading()

        try {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)

            val density = activity.resources.displayMetrics.density
            fun dp(v: Int) = (v * density).toInt()

            val container = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val card = android.widget.LinearLayout(activity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    dp(160),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                setPadding(dp(24), dp(28), dp(24), dp(28))

                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(16).toFloat()
                    setColor(Color.parseColor("#121212"))
                }
            }

            val progressBar = ProgressBar(activity).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    dp(40), dp(40)
                )
            }

            val label = TextView(activity).apply {
                text = "Ad Loading..."
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
            }

            card.addView(progressBar)
            card.addView(label)
            container.addView(card)

            dialog.setContentView(container)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            dialog.show()
            loadingDialog = dialog
        } catch (e: Exception) {

        }
    }

    private fun dismissLoading() {
        Log.d("AdManager", "dismissLoading called, loadingDialog=$loadingDialog")
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

    private fun cancelWebTimeout() {
        webTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        webTimeoutRunnable = null
    }

    private fun clearPending() {
        pendingAction = null
        pendingActivityRef = null
    }
}