package com.dailyspark.mobile.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dailyspark.mobile.R
import com.dailyspark.mobile.ads.AdsManager
import com.dailyspark.mobile.ads.AppOpenAdManager
import com.dailyspark.mobile.data.RetrofitClient
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.ActivitySplashBinding
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            launch(Dispatchers.IO) {
                repo.syncDataIfNeeded()
            }

            delay(800)

            val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
            val isFinished = prefs.getBoolean("finished", false)

            if (isFinished) {
                AppOpenAdManager.showAdOnSplash(this@SplashActivity) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            } else {
                startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                finish()
            }
        }
    }
}