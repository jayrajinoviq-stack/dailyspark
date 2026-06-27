package com.dailyspark.mobile.ui.activity

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dailyspark.mobile.NetworkMonitor
import com.dailyspark.mobile.R
import com.dailyspark.mobile.ads.AdsManager
import com.dailyspark.mobile.data.RetrofitClient
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.ActivityQuotesViewBinding
import com.dailyspark.mobile.databinding.WallpaperLayoutBinding
import com.dailyspark.mobile.model.QuoteEntity
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.ui.dialog.FontPickerSheet
import com.dailyspark.mobile.utils.FontPreference
import com.dailyspark.mobile.utils.FontUtils
import com.dailyspark.mobile.utils.ShareHelper
import com.dailyspark.mobile.viewmodel.QuoteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuotesViewActivity : BaseActivity() {
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

        binding.cancelButton.setOnClickListener {
            onBackPressedAction()
        }

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

        binding.main.background = getGradientDrawable(category)

        binding.nextQuote.visibility = View.GONE
        binding.saveLayout.visibility = View.GONE

        binding.main.background = getGradientDrawable(category)
        applyCategoryStyle(binding.category, binding.quoteIcon, category)

    }


    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val apiService = RetrofitClient.apiService

        val repository = QuoteRepository(apiService, database.quoteDao(), this)

        val networkMonitor = NetworkMonitor(this)

        val factory = QuoteViewModel.Factory(repository, networkMonitor)

        viewModel = ViewModelProvider(this, factory)[QuoteViewModel::class.java]
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
        binding.main.background = getGradientDrawable(item.category)

        applyCategoryStyle(binding.category, binding.quoteIcon, item.category)

        updateFavoriteIcon(item.isFavourite)


        val fontId = FontPreference.getFont(this, item.id)

        val font = FontUtils.getAppFonts()
            .firstOrNull { it.id == fontId }
            ?: FontUtils.getAppFonts().first()

        applyFontToUI(font.fontResId)

    }

    private fun setupClickListeners() {

        binding.fonts.setOnClickListener {
            FontPickerSheet(
                this,
                currentFontId = FontPreference.getFont(this, currentQuoteId)
            ) { selectedFont ->

                applyFontToUI(selectedFont.fontResId)

                FontPreference.saveFont(
                    this,
                    currentQuoteId,
                    selectedFont.id
                )
            }.show(supportFragmentManager, FontPickerSheet.TAG)
        }

        binding.copyLayout.setOnClickListener {
            val text = "${binding.quote.text}\n${binding.author.text}"
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            AdsManager.onUserAction(this) {
                clipboard.setPrimaryClip(ClipData.newPlainText("quote", text))
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
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
                AdsManager.onUserAction(this) {
                    viewModel.toggleFavourite(currentQuoteId)
                }
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
        if (isFav) {
            binding.saveIcon.setImageResource(R.drawable.heart_selected)
            binding.saveIcon.clearColorFilter()
        } else {
            binding.saveIcon.setImageResource(R.drawable.heart)
            binding.saveIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.text_primary)
            )
        }
    }

    private fun showWallpaperDialog() {
        val options = arrayOf("Set as Home Screen", "Set as Lock Screen", "Both")

        MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
            .setTitle("Set Quote as Wallpaper")
            .setItems(options) { _, which ->
                AdsManager.showInterstitial(
                    this
                ) {
                    setAsWallpaper(which)
                }
            }
            .show()
    }


    private fun createWallpaperBitmap(): Bitmap {
        val wallpaperBinding = WallpaperLayoutBinding.inflate(layoutInflater)

        val selectedTypeface =
            androidx.core.content.res.ResourcesCompat.getFont(this, currentTypefaceResId)
        wallpaperBinding.wpQuote.typeface = selectedTypeface
        wallpaperBinding.wpAuthor.typeface = selectedTypeface

        val catText = binding.category.text.toString()
        wallpaperBinding.wpCategory.text = catText
        wallpaperBinding.wpQuote.text = binding.quote.text
        wallpaperBinding.wpAuthor.text = binding.author.text

        applyCategoryStyle(wallpaperBinding.wpCategory, wallpaperBinding.quoteIcon, catText)

        wallpaperBinding.root.background = getGradientDrawable(catText)

        wallpaperBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        wallpaperBinding.root.layout(0, 0, 1080, 1920)


        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        wallpaperBinding.root.draw(canvas)

        return bitmap
    }

    private fun applyCategoryStyle(
        categoryView: TextView,
        quoteView: TextView,
        category: String?
    ) {
        val colorHex = when (category?.lowercase()) {
            "success", "motivational" -> "#A49EED"
            "love", "happiness" -> "#E84545"
            "wisdom", "life" -> "#5AAAE8"
            "friendship" -> "#3DCBA0"
            else -> "#F5C842"
        }

        val baseColor = Color.parseColor(colorHex)

        categoryView.setTextColor(baseColor)
        quoteView.setTextColor(baseColor)

        val alphaColor = ColorUtils.setAlphaComponent(baseColor, 38)
        categoryView.backgroundTintList = ColorStateList.valueOf(alphaColor)
    }

    private fun setAsWallpaper(which: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = createWallpaperBitmap()

                saveImageToGallery(bitmap)

                val manager = WallpaperManager.getInstance(this@QuotesViewActivity)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    when (which) {
                        0 -> {
                            manager.setBitmap(
                                bitmap,
                                null,
                                true,
                                WallpaperManager.FLAG_SYSTEM
                            )
                        }

                        1 -> {
                            manager.setBitmap(
                                bitmap,
                                null,
                                true,
                                WallpaperManager.FLAG_LOCK
                            )
                        }

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
                } else {
                    manager.setBitmap(bitmap)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@QuotesViewActivity,
                        "Wallpaper Applied Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@QuotesViewActivity,
                        e.message ?: "Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun saveImageToGallery(bitmap: Bitmap) {
        val fileName = "DailySpark_${System.currentTimeMillis()}.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DailySpark")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return

        resolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    private fun getGradientDrawable(category: String?): GradientDrawable {
        val isDark =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES

        val colors = when (category?.lowercase()) {
            "success", "motivational" -> if (isDark) {
                intArrayOf(
                    Color.parseColor("#1E1A2E"),
                    Color.parseColor("#16101F"),
                    Color.parseColor("#0D0D0D")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#F0EDFC"),
                    Color.parseColor("#E5DFFA"),
                    Color.parseColor("#F5F2EC")
                )
            }

            "love", "happiness" -> if (isDark) {
                intArrayOf(
                    Color.parseColor("#2A1414"),
                    Color.parseColor("#1F0E0E"),
                    Color.parseColor("#0D0D0D")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#FCEAEA"),
                    Color.parseColor("#FAD9D9"),
                    Color.parseColor("#F5F2EC")
                )
            }

            "wisdom", "life" -> if (isDark) {
                intArrayOf(
                    Color.parseColor("#142028"),
                    Color.parseColor("#0F171D"),
                    Color.parseColor("#0D0D0D")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#E8F2FC"),
                    Color.parseColor("#D6E9FA"),
                    Color.parseColor("#F5F2EC")
                )
            }

            "friendship" -> if (isDark) {
                intArrayOf(
                    Color.parseColor("#142822"),
                    Color.parseColor("#0F1F19"),
                    Color.parseColor("#0D0D0D")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#E8FCF4"),
                    Color.parseColor("#D6F5E8"),
                    Color.parseColor("#F5F2EC")
                )
            }

            else -> if (isDark) {
                intArrayOf(
                    Color.parseColor("#1E1A0E"),
                    Color.parseColor("#2A2210"),
                    Color.parseColor("#1E1A0E"),
                    Color.parseColor("#0D0D0D")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#FFF8E8"),
                    Color.parseColor("#FEF0C8"),
                    Color.parseColor("#FBE8C4"),
                    Color.parseColor("#F5F2EC")
                )
            }
        }

        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
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