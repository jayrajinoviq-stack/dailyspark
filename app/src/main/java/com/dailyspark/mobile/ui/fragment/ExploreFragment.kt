package com.dailyspark.mobile.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.dailyspark.mobile.NetworkMonitor
import com.dailyspark.mobile.R
import com.dailyspark.mobile.adapter.AddFolderAdapter
import com.dailyspark.mobile.adapter.FolderAdapter
import com.dailyspark.mobile.adapter.QuoteAdapter
import com.dailyspark.mobile.ads.AdsManager
import com.dailyspark.mobile.data.RetrofitClient
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.FragmentExploreBinding
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.ui.activity.CategoriesItemActivity
import com.dailyspark.mobile.ui.activity.QuotesViewActivity
import com.dailyspark.mobile.ui.dialog.AddCategoryBottomSheet
import com.dailyspark.mobile.viewmodel.QuoteUiState
import com.dailyspark.mobile.viewmodel.QuoteViewModel
import com.dailyspark.mobile.viewmodel.SyncStatus
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {
    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: QuoteViewModel
    private var shouldScrollToTop = false
    private val quoteAdapter by lazy {
        QuoteAdapter(
            requireActivity(),
            isSavedMode = false,
            lifecycleOwner = viewLifecycleOwner,
            onFavouriteClick = { quote ->
                AdsManager.onUserAction(requireActivity()) {
                    viewModel.toggleFavourite(quote.id)
                }
            },
            onShareClick = { quote -> },
            onItemClick = { quote, currentList ->
                val intent = Intent(requireContext(), QuotesViewActivity::class.java).apply {
                    putExtra("SELECTED_QUOTE_ID", quote.id)
                    putExtra("QUOTE_IDS", currentList.map { it.id }.toIntArray())
                }

                AdsManager.onUserAction(requireActivity()) {
                    startActivity(intent)
                }
            }
        )
    }

    private val addFolderAdapter by lazy {
        AddFolderAdapter(onAddFolderClick = { showAddFolderDialog() })
    }

    private val folderAdapter by lazy {
        FolderAdapter(
            onFolderClick = { folder ->
                val intent = Intent(requireContext(), CategoriesItemActivity::class.java).apply {
                    putExtra(CategoriesItemActivity.EXTRA_FOLDER_ID, folder.id)
                    putExtra(CategoriesItemActivity.EXTRA_FOLDER_NAME, folder.name)
                }
                AdsManager.onUserAction(requireActivity()) {
                    startActivity(intent)
                }
            }
        )
    }

    private val categoryViews by lazy {
        mapOf(
            "All" to binding.tvCatAll,
            "Motivational" to binding.tvCatMotivation,
            "Love" to binding.tvCatLove,
            "Wisdom" to binding.tvCatWisdom,
            "Friendship" to binding.tvCatFriendship,
            "Funny" to binding.tvCatFunny,
            "Life" to binding.tvCatLife,
            "Success" to binding.tvCatSuccess,
            "Happiness" to binding.tvCatHappiness
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupQuoteRecyclerView()
        setupFolderRecyclerView()
        setupSearch()
        setupCategoryListeners()
        observeUiState()
        setupRetryButton()
        observeFolders()
        restoreUiState()
        observeDataAndSync()
    }


    private fun setupRetryButton() {
        binding.btnRetry.setOnClickListener {
            viewModel.startSync(isRetry = true)
        }
    }

    private fun observeDataAndSync() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is QuoteUiState.Success -> {
                                binding.rvList.isVisible = true
                                binding.tvEmptyState.isVisible = false
                                quoteAdapter.submitQuotes(state.quotes)
                            }

                            is QuoteUiState.Empty -> {
                                if (viewModel.syncStatus.value !is SyncStatus.Loading) {
                                    binding.tvEmptyState.isVisible = true
                                    binding.rvList.isVisible = false
                                }
                            }

                            is QuoteUiState.Loading -> {
                                // Do nothing, let SyncStatus handle the spinner
                            }
                        }
                    }
                }


                launch {
                    viewModel.syncStatus.collect { sync ->

                        val isListEmpty = quoteAdapter.itemCount == 0
                        binding.progressBar.isVisible = (sync is SyncStatus.Loading && isListEmpty)

                        binding.layoutRetry.isVisible =
                            (sync is SyncStatus.Error || sync is SyncStatus.NoInternet) && isListEmpty
                    }
                }

                launch {
                    viewModel.toastEvent.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val apiService = RetrofitClient.apiService

        val repository = QuoteRepository(apiService, database.quoteDao(), requireContext())

        val networkMonitor = NetworkMonitor(requireContext())

        val factory = QuoteViewModel.Factory(repository, networkMonitor)

        viewModel = ViewModelProvider(this, factory)[QuoteViewModel::class.java]
    }

    private fun setupQuoteRecyclerView() {
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quoteAdapter
        }
        binding.addBtn.setOnClickListener {
            showAddFolderDialog()
        }
    }

    private fun setupFolderRecyclerView() {
        binding.rvFolders.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = ConcatAdapter(addFolderAdapter, folderAdapter)
            itemAnimator = null
        }

    }

    private fun observeFolders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folders.collect { folderList ->
                    folderAdapter.submitList(folderList)
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QuoteUiState.Success -> {
                            quoteAdapter.submitQuotes(state.quotes) {
                                if (shouldScrollToTop) {
                                    binding.rvList.scrollToPosition(0)
                                    shouldScrollToTop = false
                                }
                            }
                        }

                        is QuoteUiState.Empty -> {
                            quoteAdapter.submitList(emptyList())
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showAddFolderDialog() {

        val currentFolders = folderAdapter.itemCount
        val maxFolders = 10

        if (currentFolders >= maxFolders) {
            Toast.makeText(
                requireContext(),
                "Maximum $maxFolders categories allowed",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AddCategoryBottomSheet(
            currentCount = currentFolders,
            maxCount = maxFolders
        ) { folderName ->
            viewModel.addNewFolder(folderName)
        }.show(
            childFragmentManager,
            "AddCategoryBottomSheet"
        )
    }

    private fun setupSearch() {
        binding.closeBtn.isVisible = binding.etSearch.text?.isNotEmpty() == true

        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            binding.closeBtn.isVisible = query.isNotEmpty()

            shouldScrollToTop = true
            viewModel.onSearchQueryChanged(query)
        }

        binding.closeBtn.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.etSearch.requestFocus()
        }
    }

    private fun setupCategoryListeners() {
        categoryViews.forEach { (name, view) ->
            view.setOnClickListener {
                if (viewModel.currentCategory.value != name) {
                    shouldScrollToTop = true
                    viewModel.onCategoryChanged(name)
                    updateCategorySelectionUI(name)
                }
            }
        }
    }

    private fun restoreUiState() {
        updateCategorySelectionUI(viewModel.currentCategory.value)
    }

    private fun updateCategorySelectionUI(selectedCategory: String) {
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.selected)
        val normalTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        categoryViews.forEach { (name, view) ->
            if (name == selectedCategory) {
                view.setBackgroundResource(R.drawable.bg_categories_cap)
                view.setTextColor(selectedTextColor)
            } else {
                view.setBackgroundResource(R.drawable.bg_card)
                view.setTextColor(normalTextColor)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}