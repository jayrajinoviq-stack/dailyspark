package com.dailyspark.mobile.ui.activity

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dailyspark.mobile.R
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.ActivityQuotesViewBinding
import com.dailyspark.mobile.databinding.WallpaperLayoutBinding
import com.dailyspark.mobile.model.QuoteEntity
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.service.ApiService
import com.dailyspark.mobile.ui.dialog.FontPickerSheet
import com.dailyspark.mobile.utils.ShareHelper
import com.dailyspark.mobile.viewmodel.QuoteViewModel
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
    private var isSingleItemMode: Boolean = false

    companion object {
        const val EXTRA_SINGLE_ITEM = "extra_single_item"
        const val EXTRA_QUOTE_TEXT = "extra_quote_text"
        const val EXTRA_QUOTE_AUTHOR = "extra_quote_author"
        const val EXTRA_QUOTE_CATEGORY = "extra_quote_category"
    }

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

        isSingleItemMode = intent.getBooleanExtra(EXTRA_SINGLE_ITEM, false)

        if (isSingleItemMode) {
            setupSingleItemView()
            setupClickListeners()
            return
        }

        setupViewModel()
        handleIntentData()
        observeQuotes()
        setupClickListeners()
    }

    private fun setupSingleItemView() {
        val text = intent.getStringExtra(EXTRA_QUOTE_TEXT) ?: ""
        val author = intent.getStringExtra(EXTRA_QUOTE_AUTHOR) ?: ""
        val category = intent.getStringExtra(EXTRA_QUOTE_CATEGORY) ?: ""

        binding.quote.text = text
        binding.author.text = "──  $author  ──"
        binding.category.text = category.uppercase()

        binding.nextQuote.visibility = View.GONE

        binding.saveLayout.visibility = View.GONE
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

        binding.fonts.setOnClickListener {
            FontPickerSheet { selectedFont ->
                applyFontToUI(selectedFont.fontResId)
            }.show(supportFragmentManager, FontPickerSheet.TAG)
        }


        binding.copyLayout.setOnClickListener {
            val text = "${binding.quote.text}\n${binding.author.text}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("quote", text))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.shareLayout.setOnClickListener {
            val quoteText = binding.quote.text.toString()
            val authorText = binding.author.text.toString()

            val shareText = """
        $quoteText
        
        $authorText
    """.trimIndent()

            ShareHelper.shareQuote(this@QuotesViewActivity, shareText)
        }

        binding.saveLayout.setOnClickListener {
            if (!isSingleItemMode && currentQuoteId != -1) {
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

        val selectedTypeface = androidx.core.content.res.ResourcesCompat.getFont(this, currentTypefaceResId)

        wallpaperBinding.wpQuote.typeface = selectedTypeface
        wallpaperBinding.wpAuthor.typeface = selectedTypeface

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
    private var currentTypefaceResId: Int = R.font.montserrat
    private fun applyFontToUI(fontResId: Int) {
        val typeface = androidx.core.content.res.ResourcesCompat.getFont(this, fontResId)
        binding.quote.typeface = typeface
        binding.author.typeface = typeface
        this.currentTypefaceResId = fontResId
    }

}