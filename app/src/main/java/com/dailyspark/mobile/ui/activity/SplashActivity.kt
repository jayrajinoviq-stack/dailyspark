package com.dailyspark.mobile.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.dailyspark.mobile.R
import com.dailyspark.mobile.data.database.AppConstants
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.ActivitySplashBinding
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.service.ApiService
import com.dailyspark.mobile.utils.ThemeManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val database = Room.databaseBuilder(
            applicationContext, AppDatabase::class.java,
            AppConstants.DATABASE_NAME
        ).build()

        val api = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiService::class.java)

        val repo = QuoteRepository(api, database.quoteDao(), this)

        lifecycleScope.launch {
            val delayJob = async { delay(1500) }
            val syncJob = async { repo.syncDataIfNeeded() }

            awaitAll(delayJob, syncJob)

            val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
            val isFinished = prefs.getBoolean("finished", false)

            val destination =
                if (isFinished) MainActivity::class.java else OnboardingActivity::class.java

            startActivity(Intent(this@SplashActivity, destination))
            finish()
        }
    }

}