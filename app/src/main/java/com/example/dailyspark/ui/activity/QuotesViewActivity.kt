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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.dailyspark.R
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.ActivityQuotesViewBinding
import com.example.dailyspark.model.QuoteEntity
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.service.ApiService
import com.example.dailyspark.viewmodel.QuoteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class QuotesViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuotesViewBinding
    private lateinit var viewModel: QuoteViewModel
    private var quotesList: List<QuoteEntity> = emptyList()
    private var currentIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQuotesViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewModel()
        handleIntentData()
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
        viewModel = ViewModelProvider(this, QuoteViewModel.Factory(repository))[QuoteViewModel::class.java]
    }

    private fun handleIntentData() {
        val currentQuote = intent.getSerializableExtra("QUOTE_DATA") as? QuoteEntity
        val list = intent.getSerializableExtra("QUOTE_LIST") as? ArrayList<QuoteEntity>

        if (list != null && currentQuote != null) {
            quotesList = list
            currentIndex = quotesList.indexOfFirst { it.id == currentQuote.id }

            binding.nextQuote.visibility = View.VISIBLE
            displayQuote(quotesList[currentIndex])
        } else if (currentQuote != null) {
            displayQuote(currentQuote)
            binding.nextQuote.visibility = View.GONE
        }
    }

    private fun displayQuote(item: QuoteEntity) {
        binding.quote.text = item.quote
        binding.author.text = "--- ${item.author} ---"
        binding.category.text = item.category.uppercase()

        // Update Heart Icon
        updateFavoriteIcon(item.isFavourite)
    }

    private fun setupClickListeners() {


        binding.copyLayout.setOnClickListener {
            val text = "${binding.quote.text} ${binding.author.text}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("quote", text))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.shareLayout.setOnClickListener {
            val text = "${binding.quote.text} \n ${binding.author.text}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        }


        binding.saveLayout.setOnClickListener {
            val currentQuote = quotesList[currentIndex]
            viewModel.toggleFavourite(currentQuote.id)
            currentQuote.isFavourite = !currentQuote.isFavourite
            updateFavoriteIcon(currentQuote.isFavourite)
        }

        binding.nextQuote.setOnClickListener {
            if (currentIndex < quotesList.size - 1) {
                currentIndex++
                displayQuote(quotesList[currentIndex])
            } else {
                Toast.makeText(this, "End of list", Toast.LENGTH_SHORT).show()
            }
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
//                setAsWallpaper(which)
            }
            .show()
    }

//    private fun setAsWallpaper(choice: Int) {
//        val bitmap = viewToBitmap(binding.quoteContainer)
//        val wallpaperManager = WallpaperManager.getInstance(this)
//
//        try {
//            val flag = when (choice) {
//                0 -> WallpaperManager.FLAG_SYSTEM
//                1 -> WallpaperManager.FLAG_LOCK
//                else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
//            }
//            wallpaperManager.setBitmap(bitmap, null, true, flag)
//            Toast.makeText(this, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            Toast.makeText(this, "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun viewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}