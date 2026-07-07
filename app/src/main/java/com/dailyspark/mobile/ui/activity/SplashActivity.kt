package com.dailyspark.mobile.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dailyspark.mobile.R
import com.dailyspark.mobile.ads.AdsResponse
import com.dailyspark.mobile.ads.InterstitialAdManager
import com.dailyspark.mobile.ads.AppOpenAdManager
import com.dailyspark.mobile.data.RetrofitClient
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.ActivitySplashBinding
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val database = AppDatabase.getDatabase(this)
        val api = RetrofitClient.apiService
        val repo = QuoteRepository(api, database.quoteDao(), this)

        lifecycleScope.launch {

            val syncJob = launch(Dispatchers.IO) {
                repo.syncDataIfNeeded()
            }

            // Fetch remote config first so we know isShowAdsURL before deciding what to show
            fetchAdConfigSuspended(this@SplashActivity)

            // Only pre-load the real interstitial if we're not in URL-ad mode
            if (!AdsResponse.isShowAdsURL) {
                InterstitialAdManager.loadInterstitial(applicationContext)
            }

            delay(800)

            val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
            val isFinished = prefs.getBoolean("finished", false)

            val goNext: () -> Unit = {
                if (isFinished) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                }
                finish()
            }

            when {
                AdsResponse.isShowAdsURL -> {
                    InterstitialAdManager.showInterstitialDirect(this@SplashActivity) {
                        goNext()
                    }
                }
                isFinished -> {
                    AppOpenAdManager.showAdOnSplash(this@SplashActivity) {
                        goNext()
                    }
                }
                else -> goNext()
            }
        }
    }

    private suspend fun fetchAdConfigSuspended(context: Context): Unit =
        suspendCancellableCoroutine { continuation ->
            AdsResponse.fetchAdConfig(context) {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
}