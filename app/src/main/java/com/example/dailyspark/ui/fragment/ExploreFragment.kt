package com.example.dailyspark.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyspark.R
import com.example.dailyspark.adapter.AddFolderAdapter
import com.example.dailyspark.adapter.FolderAdapter
import com.example.dailyspark.adapter.QuoteAdapter
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.FragmentExploreBinding
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.service.ApiService
import com.example.dailyspark.ui.activity.CategoriesItemActivity
import com.example.dailyspark.ui.activity.QuotesViewActivity
import com.example.dailyspark.viewmodel.QuoteUiState
import com.example.dailyspark.viewmodel.QuoteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExploreFragment : Fragment() {
    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: QuoteViewModel
    private val quoteAdapter by lazy {
        QuoteAdapter(
            onFavouriteClick = { quote -> viewModel.toggleFavourite(quote.id) },
            onItemClick = { quote, list ->
                val intent = Intent(requireContext(), QuotesViewActivity::class.java).apply {

                }
                startActivity(intent)
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
                startActivity(intent)
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
        observeFolders()
        restoreUiState()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val apiService = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val repository = QuoteRepository(apiService, database.quoteDao(), requireContext())
        viewModel =
            ViewModelProvider(this, QuoteViewModel.Factory(repository))[QuoteViewModel::class.java]
    }

    private fun setupQuoteRecyclerView() {
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quoteAdapter
            setHasFixedSize(true)
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
                    folderAdapter.submitList(folderList.reversed()) {
                        (binding.rvFolders.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(0, 0)
                    }
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QuoteUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvList.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }

                        is QuoteUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvList.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                            quoteAdapter.submitList(state.quotes)
                        }

                        is QuoteUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvList.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            quoteAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

    private fun showAddFolderDialog() {
        if (folderAdapter.itemCount >= 5) {
            Toast.makeText(requireContext(), "You can only have 5 folders", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val editText = EditText(requireContext()).apply {
            hint = "Folder name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(48, 24, 48, 24)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Folder")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) viewModel.addNewFolder(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
    }

    private fun setupCategoryListeners() {
        categoryViews.forEach { (name, view) ->
            view.setOnClickListener {
                viewModel.onCategoryChanged(name)
                updateCategorySelectionUI(name)
            }
        }
    }

    private fun restoreUiState() {
        updateCategorySelectionUI(viewModel.currentCategory.value)
    }

    private fun updateCategorySelectionUI(selectedCategory: String) {
        categoryViews.forEach { (name, view) ->
            view.setBackgroundResource(
                if (name == selectedCategory) R.drawable.bg_category_selected
                else R.drawable.bg_category_normal
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}