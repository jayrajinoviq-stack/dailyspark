package com.example.dailyspark.ui.activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.dailyspark.data.database.AppConstants
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.ActivitySplashBinding
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.service.ApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.jvm.java

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java,
            AppConstants.DATABASE_NAME).build()

        val api = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiService::class.java)

        val repo = QuoteRepository(api, database.quoteDao(), this)

        lifecycleScope.launch {
            val delayJob = async { delay(1500) }

            val syncJob = async { repo.syncDataIfNeeded() }

            awaitAll(delayJob, syncJob)

            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

}