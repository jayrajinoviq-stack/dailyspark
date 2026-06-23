package com.dailyspark.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.FragmentProfileBinding
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.repository.StatsRepository
import com.dailyspark.mobile.repository.StreakRepository
import com.dailyspark.mobile.service.ApiService
import com.dailyspark.mobile.viewmodel.ProfileViewModel
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
        val statsRepo = StatsRepository.getInstance(requireContext())

        val factory = ProfileViewModel.Factory(quoteRepo, streakRepo, statsRepo)
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    private fun displayStats() {
        binding.totalStreak.text = viewModel.getStreakCount().toString()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.totalSavedCount.collect { count ->
                        binding.totalSaved.text = count.toString()
                    }
                }

                launch {
                    viewModel.totalSharedCount.collect { count ->
                        binding.totalShared.text = count.toString()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}