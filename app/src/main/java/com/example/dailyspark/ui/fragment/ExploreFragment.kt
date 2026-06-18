package com.example.dailyspark.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyspark.R
import com.example.dailyspark.adapter.QuoteAdapter
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.FragmentExploreBinding
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.service.ApiService
import com.example.dailyspark.viewmodel.QuoteUiState
import com.example.dailyspark.viewmodel.QuoteViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: QuoteViewModel

    private val quoteAdapter by lazy {
        QuoteAdapter(onFavouriteClick = { quote ->
            viewModel.toggleFavourite(quote.id)
        })
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupRecyclerView()
        setupSearch()
        setupCategoryListeners()
        observeUiState()
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
        val factory = QuoteViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[QuoteViewModel::class.java]
    }

    private fun setupRecyclerView() {
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quoteAdapter
            setHasFixedSize(true)
        }
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
        val currentCategory = try {
            viewModel.javaClass.getDeclaredField("_category").apply {
                isAccessible = true
            }.let { (it.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<String>).value }
        } catch (e: Exception) {
            "All"
        }
        updateCategorySelectionUI(currentCategory)
    }

    private fun updateCategorySelectionUI(selectedCategory: String) {
        categoryViews.forEach { (name, view) ->
            val background = if (name == selectedCategory) {
                R.drawable.bg_category_selected
            } else {
                R.drawable.bg_category_normal
            }
            view.setBackgroundResource(background)
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuoteUiState.Loading -> {
                    binding.rvList.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE
                }
                is QuoteUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvList.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                    quoteAdapter.submitList(state.quotes) {
                        binding.rvList.scrollToPosition(0)
                    }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}