import android.app.Activity

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

object AdsManager {

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private const val APP_OPEN_AD_ID =
        "ca-app-pub-3940256099942544/9257395921"

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






}