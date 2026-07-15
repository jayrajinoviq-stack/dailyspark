package com.dailyspark.mobile.ui.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dailyspark.mobile.R
import com.dailyspark.mobile.adapter.FolderQuoteAdapter
import com.dailyspark.mobile.ads.InterstitialAdManager
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.ActivityCategoriesItemBinding
import com.dailyspark.mobile.databinding.PopupMenuBinding
import com.dailyspark.mobile.model.FolderQuoteEntity
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.service.ApiService
import com.dailyspark.mobile.ui.dialog.AddQuoteBottomSheet
import com.dailyspark.mobile.viewmodel.FolderItemEvent
import com.dailyspark.mobile.viewmodel.FolderItemViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CategoriesItemActivity : BaseActivity() {

    companion object {
        const val EXTRA_FOLDER_ID = "extra_folder_id"
        const val EXTRA_FOLDER_NAME = "extra_folder_name"
        private const val MAX_ITEMS = FolderItemViewModel.MAX_ITEMS_PER_FOLDER
    }
    private lateinit var binding: ActivityCategoriesItemBinding
    private lateinit var viewModel: FolderItemViewModel
    private val adapter by lazy {
        FolderQuoteAdapter(
            onItemClick = { quote ->
                val intent = Intent(this, QuotesViewActivity::class.java).apply {
                    putExtra(QuotesViewActivity.EXTRA_SINGLE_ITEM, true)
                    putExtra(QuotesViewActivity.EXTRA_QUOTE_TEXT, quote.quote)
                    putExtra(QuotesViewActivity.EXTRA_QUOTE_AUTHOR, quote.author)
                    putExtra(QuotesViewActivity.EXTRA_QUOTE_CATEGORY, quote.category)
                }
                InterstitialAdManager.onUserAction(this@CategoriesItemActivity) {
                    startActivity(intent)
                }
            },
            onShareClick = { shareQuote(it) },
            onEditClick = {
                InterstitialAdManager.onUserAction(this@CategoriesItemActivity) {
                    showAddEditDialog(existing = it)
                }
            },
            onDeleteClick = {
                InterstitialAdManager.onUserAction(this@CategoriesItemActivity) {
                    confirmDeleteQuote(it)
                }
            }
        )
    }

    private var folderId: Int = -1
    private var folderName: String = "Item"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCategoriesItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        folderId = intent.getIntExtra(EXTRA_FOLDER_ID, -1)
        folderName = intent.getStringExtra(EXTRA_FOLDER_NAME) ?: "Item"

        binding.appCompatTextView.text = folderName

        if (folderId == -1) {
            finish()
            return
        }

        setupViewModel()
        setupRecyclerView()
        observeQuotes()
        observeEvents()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(applicationContext)
        val apiService = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val repository = QuoteRepository(apiService, database.quoteDao(), applicationContext)
        viewModel = ViewModelProvider(
            this,
            FolderItemViewModel.Factory(repository, folderId)
        )[FolderItemViewModel::class.java]
    }

    private fun setupRecyclerView() {
        binding.rvMyCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoriesItemActivity)
            adapter = this@CategoriesItemActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.back.setOnClickListener { finish() }

        binding.menu.setOnClickListener {
            showCategoryMenu()
        }

        binding.addItems.setOnClickListener {
            val currentCount = viewModel.quotes.value?.size ?: 0
            if (currentCount >= MAX_ITEMS) {
                Snackbar.make(
                    binding.root,
                    "Limit reached: max $MAX_ITEMS quotes per folder",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                InterstitialAdManager.onUserAction(this@CategoriesItemActivity) {
                    showAddEditDialog(existing = null)
                }
            }
        }
    }

    private fun showCategoryMenu() {

        val popupBinding = PopupMenuBinding.inflate(layoutInflater)

        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 16f
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        popupBinding.deleteCategory.setOnClickListener {
            InterstitialAdManager.onUserAction(this@CategoriesItemActivity) {
                popupWindow.dismiss()
                viewModel.deleteFolder()
            }
        }

        popupBinding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popupWidth = popupBinding.root.measuredWidth

        popupWindow.showAsDropDown(
            binding.menu,
            -popupWidth + binding.menu.width,
            8
        )
    }

    private fun observeQuotes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quotes.collect { list ->
                    adapter.submitList(list)

                    val count = list.size
                    binding.quotesProgress.max = MAX_ITEMS
                    binding.quotesProgress.setProgressCompat(count, true)
                    binding.totalQuotes.text = "$count/$MAX_ITEMS quotes"

                    if (count == 0) {
                        binding.rvMyCategories.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvMyCategories.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                    }

                    binding.addItems.isEnabled = count < MAX_ITEMS
                    binding.addItems.alpha = if (count < MAX_ITEMS) 1f else 0.5f
                }
            }
        }
    }

    private fun observeEvents() {
        viewModel.events.observe(this) { event ->
            when (event) {
                is FolderItemEvent.Error ->
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()

                is FolderItemEvent.Added ->
                    Snackbar.make(binding.root, "Quote added", Snackbar.LENGTH_SHORT).show()

                is FolderItemEvent.Updated ->
                    Snackbar.make(binding.root, "Quote updated", Snackbar.LENGTH_SHORT).show()

                is FolderItemEvent.Deleted ->
                    Snackbar.make(binding.root, "Quote removed", Snackbar.LENGTH_SHORT).show()

                is FolderItemEvent.FolderDeleted -> finish()
            }
        }
    }

    private fun showAddEditDialog(existing: FolderQuoteEntity?) {
        val currentCount = viewModel.quotes.value?.size ?: 0

        AddQuoteBottomSheet(
            folderName = folderName,
            currentCount = currentCount,
            maxCount = MAX_ITEMS,
            existing = existing
        ) { quoteText, author, category ->
            if (existing == null) {
                viewModel.addQuote(quoteText, author, category)
            } else {
                viewModel.updateQuote(existing, quoteText, author, category)
            }
        }.show(supportFragmentManager, "AddQuoteBottomSheet")
    }

    private fun confirmDeleteQuote(quote: FolderQuoteEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Quote")
            .setMessage("Remove this quote from the folder?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteQuote(quote) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareQuote(quote: FolderQuoteEntity) {
        val shareText = "\"${quote.quote}\" — ${quote.author}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share quote via"))
    }
}
