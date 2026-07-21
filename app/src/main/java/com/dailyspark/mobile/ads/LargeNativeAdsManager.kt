package com.dailyspark.mobile.ads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.dailyspark.mobile.AdNetworkHelper
import com.dailyspark.mobile.databinding.LayoutNativeAdBinding
import com.dailyspark.mobile.databinding.LayoutNativeShimmerBinding
import com.dailyspark.mobile.databinding.LayoutWebNativeImageAdBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

object LargeNativeAdsManager {

    private const val LOAD_TIMEOUT_MS = 5000L

    private const val MAX_ATTEMPTS = 2
    private val mainHandler = Handler(Looper.getMainLooper())
    private val adCache = ConcurrentHashMap<String, NativeAd>()
    private val adOwners = ConcurrentHashMap<String, WeakReference<LifecycleOwner>>()
    private val pendingOwners = ConcurrentHashMap<String, WeakReference<LifecycleOwner>>()

    private data class Waiter(
        val container: FrameLayout,
        val inflater: LayoutInflater,
        val lifecycleOwner: LifecycleOwner,
        val onFinished: (() -> Unit)?
    )

    private val waiters = ConcurrentHashMap<String, CopyOnWriteArrayList<Waiter>>()

    private fun isAlive(owner: LifecycleOwner): Boolean =
        owner.lifecycle.currentState != Lifecycle.State.DESTROYED

    fun showNativeAd(
        context: Context,
        container: FrameLayout,
        lifecycleOwner: LifecycleOwner,
        adUnitKey: String,
        onFinished: (() -> Unit)? = null
    ) {
        if (!isAlive(lifecycleOwner)) return
        val inflater = LayoutInflater.from(context)

        if (AdsResponse.isShowAdsURL) {
            if (AdsResponse.AdsLargeImgUrl.isNotBlank()) {
                showImageAd(context, container, inflater, onFinished)
            } else {
                container.removeAllViews()
                container.visibility = View.GONE
                onFinished?.invoke()
            }
            return
        }

        val cachedAd = adCache[adUnitKey]
        val cachedOwner = adOwners[adUnitKey]?.get()
        if (cachedAd != null) {
            if (cachedOwner === lifecycleOwner) {
                container.removeAllViews()
                val adBinding = LayoutNativeAdBinding.inflate(inflater, container, false)
                bindNativeAd(cachedAd, adBinding)
                container.addView(adBinding.root)
                container.visibility = View.VISIBLE
                onFinished?.invoke()
                return
            } else {
                adCache.remove(adUnitKey)?.destroy()
                adOwners.remove(adUnitKey)
            }
        }

        container.removeAllViews()
        val shimmerBinding = LayoutNativeShimmerBinding.inflate(inflater, container, false)
        container.addView(shimmerBinding.root)
        container.visibility = View.VISIBLE
        shimmerBinding.shimmerLayout.startShimmer()

        if (!AdNetworkHelper.isInternetAvailable(context)) {
            container.removeAllViews()
            container.visibility = View.GONE
            onFinished?.invoke()
            return
        }
        val existing = waiters[adUnitKey]
        if (existing != null) {
            existing.add(Waiter(container, inflater, lifecycleOwner, onFinished))
            return
        }

        val list = CopyOnWriteArrayList<Waiter>()
        list.add(Waiter(container, inflater, lifecycleOwner, onFinished))
        waiters[adUnitKey] = list
        pendingOwners[adUnitKey] = WeakReference(lifecycleOwner)

        loadAttempt(context, adUnitKey, attempt = 1)
    }

    private fun loadAttempt(context: Context, adUnitKey: String, attempt: Int) {
        val completed = AtomicBoolean(false)

        val timeoutRunnable = Runnable {
            if (completed.compareAndSet(false, true)) {
                onAttemptFinished(context, adUnitKey, attempt, nativeAd = null)
            }
        }
        mainHandler.postDelayed(timeoutRunnable, LOAD_TIMEOUT_MS)

        val adLoader = AdLoader.Builder(context, AdsResponse.NATIVE_ID)
            .forNativeAd { nativeAd ->
                mainHandler.removeCallbacks(timeoutRunnable)
                if (completed.compareAndSet(false, true)) {
                    onAttemptFinished(context, adUnitKey, attempt, nativeAd)
                } else {
                    nativeAd.destroy()
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    if (completed.compareAndSet(false, true)) {
                        onAttemptFinished(context, adUnitKey, attempt, nativeAd = null)
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun onAttemptFinished(
        context: Context,
        adUnitKey: String,
        attempt: Int,
        nativeAd: NativeAd?
    ) {
        if (nativeAd != null) {
            adCache[adUnitKey] = nativeAd
            pendingOwners.remove(adUnitKey)?.let { adOwners[adUnitKey] = it }
            resolveWaiters(adUnitKey) { waiter ->
                waiter.container.removeAllViews()
                val adBinding = LayoutNativeAdBinding.inflate(waiter.inflater, waiter.container, false)
                bindNativeAd(nativeAd, adBinding)
                waiter.container.addView(adBinding.root)
                waiter.container.visibility = View.VISIBLE
            }
            return
        }

        if (attempt < MAX_ATTEMPTS) {
            mainHandler.post { loadAttempt(context, adUnitKey, attempt + 1) }
            return
        }

        pendingOwners.remove(adUnitKey)
        resolveWaiters(adUnitKey) { waiter ->
            waiter.container.removeAllViews()
            waiter.container.visibility = View.GONE
        }
    }

    private fun resolveWaiters(adUnitKey: String, apply: (Waiter) -> Unit) {
        val list = waiters.remove(adUnitKey) ?: return
        for (waiter in list) {
            if (isAlive(waiter.lifecycleOwner)) {
                apply(waiter)
            }
            waiter.onFinished?.invoke()
        }
    }

    private fun showImageAd(
        context: Context,
        container: FrameLayout,
        inflater: LayoutInflater,
        onFinished: (() -> Unit)?
    ) {
        container.removeAllViews()

        val imageBinding = LayoutWebNativeImageAdBinding.inflate(inflater, container, false)

        Glide.with(context)
            .load(AdsResponse.AdsLargeImgUrl)
            .into(imageBinding.adImage)

        imageBinding.adImage.setOnClickListener {
            openUrl(context, AdsResponse.AdsImgClickURl)
        }

        container.addView(imageBinding.root)
        container.visibility = View.VISIBLE
        onFinished?.invoke()
    }

    private fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bindNativeAd(nativeAd: NativeAd, binding: LayoutNativeAdBinding) {
        binding.adHeadline.text = nativeAd.headline
        binding.nativeAdView.headlineView = binding.adHeadline

        binding.adAdvertiser.text = nativeAd.body
        binding.nativeAdView.bodyView = binding.adAdvertiser
        binding.adAdvertiser.visibility =
            if (nativeAd.body == null) View.INVISIBLE else View.VISIBLE

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
        adOwners.remove(adUnitKey)
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        adCache.values.forEach { it.destroy() }
        adCache.clear()
        adOwners.clear()
        pendingOwners.clear()
        waiters.clear()
    }
}