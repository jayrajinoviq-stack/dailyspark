package com.example.dailyspark.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.FragmentProfileBinding
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.repository.StreakRepository
import com.example.dailyspark.service.ApiService
import com.example.dailyspark.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        displayStats()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())

        val apiService = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val quoteRepo = QuoteRepository(apiService, database.quoteDao(), requireContext())
        val streakRepo = StreakRepository(requireContext())

        val factory = ProfileViewModel.Factory(quoteRepo, streakRepo)
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    private fun displayStats() {
        binding.totalStreak.text = viewModel.getStreakCount().toString()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalSavedCount.collect { count ->
                    binding.totalSaved.text = count.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}