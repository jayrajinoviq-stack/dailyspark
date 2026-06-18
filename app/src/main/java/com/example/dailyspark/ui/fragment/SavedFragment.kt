package com.example.dailyspark.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyspark.adapter.QuoteAdapter
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.FragmentSavedBinding
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.service.ApiService
import com.example.dailyspark.viewmodel.QuoteUiState
import com.example.dailyspark.viewmodel.QuoteViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: QuoteViewModel
    private val adapter by lazy {
        QuoteAdapter(onFavouriteClick = { quote ->
            viewModel.toggleFavourite(quote.id)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        setupRecyclerView()
        observeFavourites()
    }

    private fun initViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val apiService = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val repository = QuoteRepository(apiService, database.quoteDao(), requireContext())
        viewModel = ViewModelProvider(
            this,
            QuoteViewModel.Factory(repository)
        )[QuoteViewModel::class.java]
    }

    private fun setupRecyclerView() {
        binding.rvFavourites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SavedFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeFavourites() {
        viewModel.favouriteUiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuoteUiState.Loading -> {
                    binding.rvFavourites.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.totalFavItems.text = "0 Saved Quotes"
                }

                is QuoteUiState.Success -> {
                    binding.rvFavourites.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                    adapter.submitList(state.quotes)
                    binding.totalFavItems.text = "${state.quotes.size} Saved Quotes"
                }

                is QuoteUiState.Empty -> {
                    binding.rvFavourites.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text =
                        "No favourites yet.\nTap \u2665 on any quote to save it here."
                    adapter.submitList(emptyList())
                    binding.totalFavItems.text = "0 Saved Quotes"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}