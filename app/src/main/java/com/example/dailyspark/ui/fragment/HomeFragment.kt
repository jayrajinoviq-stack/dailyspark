package com.example.dailyspark.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dailyspark.R
import com.example.dailyspark.data.database.AppDatabase
import com.example.dailyspark.databinding.FragmentHomeBinding
import com.example.dailyspark.model.QuoteEntity
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.repository.StreakRepository
import com.example.dailyspark.service.ApiService
import com.example.dailyspark.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val retrofit = Retrofit.Builder()
        .baseUrl("https://your-project-id.supabase.co/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(requireContext())
                val apiServiceInstance = retrofit.create(ApiService::class.java)
                val quoteRepo = QuoteRepository(apiServiceInstance, database.quoteDao(), requireContext())
                val streakRepo = StreakRepository(requireContext())
                return HomeViewModel(streakRepo, quoteRepo) as T
            }
        }
    }

    private val categoryViews by lazy {
        mapOf(
            "All" to binding.catAll,
            "Motivational" to binding.catMotivational,
            "Love" to binding.catLove,
            "Wisdom" to binding.catWisdom,
            "Friendship" to binding.catFriendship,
            "Funny" to binding.catFunny,
            "Life" to binding.catLife,
            "Success" to binding.catSuccess,
            "Happiness" to binding.catHappiness
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupCategoryListeners()
        setupQuoteActions()
        observeData()
    }

    private fun setupCategoryListeners() {
        categoryViews.forEach { (name, layout) ->
            layout.setOnClickListener {
                viewModel.onCategoryChanged(name)
            }
        }
    }

    private fun setupQuoteActions() {
        binding.btnNext.setOnClickListener { viewModel.showRandomQuote() }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvQuote.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("quote", text))
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, binding.tvQuote.text.toString())
            }
            startActivity(Intent.createChooser(intent, "Share Quote"))
        }

        binding.btnLike.setOnClickListener {
            viewModel.currentQuote.value?.let { quote ->
                viewModel.toggleFavourite(quote.id)
                Toast.makeText(context, "Updated Favourites", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Categories UI
                launch {
                    viewModel.selectedCategory.collect { selected ->
                        updateCategorySelectionUI(selected)
                    }
                }
                launch {
                    viewModel.currentQuote.collect { quote ->
                        quote?.let { updateQuoteCard(it) }
                    }
                }
                launch {
                    viewModel.streakState.collect { state ->
                        updateStreakUI(state)
                    }
                }
            }
        }
    }

    private fun updateCategorySelectionUI(selectedCategory: String) {
        categoryViews.forEach { (name, layout) ->
            val isSelected = name == selectedCategory
            val frame = layout.getChildAt(0) as FrameLayout
            frame.setBackgroundResource(if (isSelected) R.drawable.bg_category_selected else R.drawable.bg_category_normal)
        }
    }

    private fun updateQuoteCard(quote: QuoteEntity) {
        binding.tvQuote.text = quote.quote
        binding.tvAuthor.text = "— ${quote.author ?: "Unknown"}"
        binding.tvCategory.text = quote.category.uppercase()

        val emoji = when (quote.category.lowercase()) {
            "all" -> "🔥"
            "motivational" -> "💪"
            "love" -> "❤️"
            "wisdom" -> "🧠"
            "friendship" -> "🤝"
            "funny" -> "😂"
            "life" -> "🌿"
            "success" -> "💼"
            "happiness" -> "😊"
            else -> "✨"
        }

        val emojiTextView = binding.layoutBadge.getChildAt(0) as TextView
        emojiTextView.text = emoji
    }

    private fun updateStreakUI(state: HomeViewModel.StreakUiState) {
        binding.tvGreetingSmall.text = state.greeting
        binding.tvStreakCount.text = state.count.toString()

        val dayViews = listOf(binding.dayMon, binding.dayTue, binding.dayWed, binding.dayThu, binding.dayFri, binding.daySat, binding.daySun)
        state.dayStatuses.forEachIndexed { index, status ->
            val textView = dayViews[index]
            when (status) {
                is HomeViewModel.DayStatus.Completed -> {
                    textView.setBackgroundResource(R.drawable.bg_streak)
                    textView.setTextColor(Color.WHITE)
                }
                is HomeViewModel.DayStatus.Active -> {
                    textView.setBackgroundResource(R.drawable.bg_day_active)
                    textView.setTextColor(Color.WHITE)
                }
                else -> {
                    textView.setBackgroundResource(R.drawable.bg_day_normal)
                    textView.setTextColor(Color.BLACK)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) viewModel.updateStreak()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}