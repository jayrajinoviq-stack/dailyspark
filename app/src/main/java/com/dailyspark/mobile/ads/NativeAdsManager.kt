package com.dailyspark.mobile.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.dailyspark.mobile.NetworkMonitor
import com.dailyspark.mobile.databinding.LayoutNativeAdBinding
import com.dailyspark.mobile.databinding.LayoutNativeShimmerBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NativeAdsManager {

    private const val LOAD_TIMEOUT_MS = 8000L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val adCache = ConcurrentHashMap<String, NativeAd>()
    private val loadingIds = ConcurrentHashMap.newKeySet<String>()
    fun showNativeAd(
        context: Context,
        container: FrameLayout,
        lifecycleOwner: LifecycleOwner,
        adUnitKey: String,
        onFinished: (() -> Unit)? = null
    ) {
        fun isAlive(): Boolean =
            lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED

        if (!isAlive()) return

        val inflater = LayoutInflater.from(context)

        val cachedAd = adCache[adUnitKey]
        if (cachedAd != null) {
            container.removeAllViews()
            val adBinding = LayoutNativeAdBinding.inflate(inflater, container, false)
            bindNativeAd(cachedAd, adBinding)
            container.addView(adBinding.root)
            container.visibility = View.VISIBLE
            onFinished?.invoke()
            return
        }

        if (loadingIds.contains(adUnitKey)) {
            showShimmerOnly(inflater, container)
            return
        }

        val completed = AtomicBoolean(false)

        fun finish() {
            if (completed.compareAndSet(false, true)) {
                loadingIds.remove(adUnitKey)
                mainHandler.post { onFinished?.invoke() }
            }
        }

        container.removeAllViews()

        val shimmerBinding = LayoutNativeShimmerBinding.inflate(inflater, container, false)
        container.addView(shimmerBinding.root)
        container.visibility = View.VISIBLE
        shimmerBinding.shimmerLayout.startShimmer()

        if (!NetworkMonitor(context).isConnected()) {
            collapse(shimmerBinding, container)
            finish()
            return
        }

        loadingIds.add(adUnitKey)

        val timeoutRunnable = Runnable {
            if (!completed.get()) {
                collapse(shimmerBinding, container)
                finish()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, LOAD_TIMEOUT_MS)

        val adLoader = AdLoader.Builder(context, AdsResponse.NATIVE_ID)
            .forNativeAd { nativeAd ->
                mainHandler.removeCallbacks(timeoutRunnable)

                if (completed.get() || !isAlive()) {
                    nativeAd.destroy()
                    loadingIds.remove(adUnitKey)
                    return@forNativeAd
                }

                adCache[adUnitKey] = nativeAd

                shimmerBinding.shimmerLayout.stopShimmer()
                container.removeAllViews()

                val adBinding = LayoutNativeAdBinding.inflate(inflater, container, false)
                bindNativeAd(nativeAd, adBinding)

                container.addView(adBinding.root)
                container.visibility = View.VISIBLE

                finish()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    collapse(shimmerBinding, container)
                    finish()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun showShimmerOnly(inflater: LayoutInflater, container: FrameLayout) {
        container.removeAllViews()
        val shimmerBinding = LayoutNativeShimmerBinding.inflate(inflater, container, false)
        container.addView(shimmerBinding.root)
        container.visibility = View.VISIBLE
        shimmerBinding.shimmerLayout.startShimmer()
    }

    private fun collapse(shimmerBinding: LayoutNativeShimmerBinding, container: FrameLayout) {
        shimmerBinding.shimmerLayout.stopShimmer()
        container.removeAllViews()
        container.visibility = View.GONE
    }

    private fun bindNativeAd(nativeAd: NativeAd, binding: LayoutNativeAdBinding) {
        binding.adHeadline.text = nativeAd.headline
        binding.nativeAdView.headlineView = binding.adHeadline

        binding.adAdvertiser.text = nativeAd.advertiser
        binding.nativeAdView.advertiserView = binding.adAdvertiser
        binding.adAdvertiser.visibility = if (nativeAd.advertiser == null) View.GONE else View.VISIBLE

        binding.adBody.text = nativeAd.body
        binding.nativeAdView.bodyView = binding.adBody
        binding.adBody.visibility = if (nativeAd.body == null) View.INVISIBLE else View.VISIBLE

        binding.adCallToAction.text = nativeAd.callToAction
        binding.nativeAdView.callToActionView = binding.adCallToAction
        binding.adCallToAction.visibility =
            if (nativeAd.callToAction == null) View.INVISIBLE else View.VISIBLE

        nativeAd.icon?.let {
            binding.adIcon.setImageDrawable(it.drawable)
            binding.adIcon.visibility = View.VISIBLE
        } ?: run {
            binding.adIcon.visibility = View.GONE
        }
        binding.nativeAdView.iconView = binding.adIcon
        binding.nativeAdView.mediaView = binding.adMedia
        binding.nativeAdView.setNativeAd(nativeAd)
    }

    fun invalidate(adUnitKey: String) {
        adCache.remove(adUnitKey)?.destroy()
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        adCache.values.forEach { it.destroy() }
        adCache.clear()
        loadingIds.clear()
    }
}