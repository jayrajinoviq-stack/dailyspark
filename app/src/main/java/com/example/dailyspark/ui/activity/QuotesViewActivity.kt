package com.example.dailyspark.ui.activity

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dailyspark.R
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.ActivityQuotesViewBinding
import com.example.dailyspark.databinding.WallpaperLayoutBinding
import com.example.dailyspark.model.QuoteEntity
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.service.ApiService
import com.example.dailyspark.viewmodel.QuoteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class QuotesViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuotesViewBinding
    private lateinit var viewModel: QuoteViewModel
    private var quotesList: List<QuoteEntity> = emptyList()
    private var currentQuoteId: Int = -1
    private var randomNext: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQuotesViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewModel()
        handleIntentData()
        observeQuotes()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val apiService = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val repository = QuoteRepository(apiService, database.quoteDao(), this)
        viewModel =
            ViewModelProvider(this, QuoteViewModel.Factory(repository))[QuoteViewModel::class.java]
    }

    private fun handleIntentData() {
        currentQuoteId = intent.getIntExtra("SELECTED_QUOTE_ID", -1)
        val ids = intent.getIntArrayExtra("QUOTE_IDS")?.toList() ?: emptyList()
        randomNext = intent.getBooleanExtra("RANDOM_NEXT", false)
        viewModel.setQuoteIds(ids)
    }

    private fun observeQuotes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quoteListState.collect { list ->
                    if (list.isEmpty()) return@collect

                    val ids = intent.getIntArrayExtra("QUOTE_IDS")?.toList() ?: emptyList()
                    quotesList = ids.mapNotNull { id -> list.find { it.id == id } }

                    val current =
                        quotesList.find { it.id == currentQuoteId } ?: quotesList.firstOrNull()
                    if (current != null) {
                        currentQuoteId = current.id
                        displayQuote(current)
                    }

                    binding.nextQuote.visibility =
                        if (randomNext && quotesList.size > 1) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun displayQuote(item: QuoteEntity) {
        binding.quote.text = item.quote
        binding.author.text = "──  ${item.author}  ──"
        binding.category.text = item.category.uppercase()
        updateFavoriteIcon(item.isFavourite)
    }

    private fun setupClickListeners() {
        binding.copyLayout.setOnClickListener {
            val text = "${binding.quote.text}\n${binding.author.text}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("quote", text))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.shareLayout.setOnClickListener {
            val text = "${binding.quote.text}\n${binding.author.text}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        }

        binding.saveLayout.setOnClickListener {
            if (currentQuoteId != -1) {
                viewModel.toggleFavourite(currentQuoteId)
            }
        }

        binding.nextQuote.setOnClickListener {
            if (quotesList.isEmpty()) return@setOnClickListener
            val next = if (quotesList.size > 1) {
                quotesList.filter { it.id != currentQuoteId }.random()
            } else {
                quotesList.first()
            }
            currentQuoteId = next.id
            displayQuote(next)
        }

        binding.menu.setOnClickListener {
            showWallpaperDialog()
        }
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        val icon = if (isFav) R.drawable.heart_selected else R.drawable.heart
        binding.saveIcon.setImageResource(icon)
    }

    private fun showWallpaperDialog() {
        val options = arrayOf("Set as Home Screen", "Set as Lock Screen", "Both")
        MaterialAlertDialogBuilder(this)
            .setTitle("Set Quote as Wallpaper")
            .setItems(options) { _, which ->
                setAsWallpaper(which)
            }
            .show()
    }



    private fun createWallpaperBitmap(): Bitmap {

        val wallpaperBinding =
            WallpaperLayoutBinding.inflate(layoutInflater)

        wallpaperBinding.wpCategory.text = binding.category.text
        wallpaperBinding.wpQuote.text = binding.quote.text
        wallpaperBinding.wpAuthor.text = binding.author.text

        wallpaperBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        wallpaperBinding.root.layout(
            0,
            0,
            wallpaperBinding.root.measuredWidth,
            wallpaperBinding.root.measuredHeight
        )

        val bitmap = Bitmap.createBitmap(
            wallpaperBinding.root.measuredWidth,
            wallpaperBinding.root.measuredHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        wallpaperBinding.root.draw(canvas)

        return bitmap
    }

    private fun setAsWallpaper(which: Int) {
        lifecycleScope.launch {

            try {

                val bitmap = createWallpaperBitmap()

                val manager = WallpaperManager.getInstance(this@QuotesViewActivity)

                when (which) {

                    0 -> manager.setBitmap(
                        bitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                    )

                    1 -> manager.setBitmap(
                        bitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_LOCK
                    )

                    2 -> {
                        manager.setBitmap(
                            bitmap,
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM
                        )

                        manager.setBitmap(
                            bitmap,
                            null,
                            true,
                            WallpaperManager.FLAG_LOCK
                        )
                    }
                }

                Toast.makeText(
                    this@QuotesViewActivity,
                    "Wallpaper Applied",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@QuotesViewActivity,
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}