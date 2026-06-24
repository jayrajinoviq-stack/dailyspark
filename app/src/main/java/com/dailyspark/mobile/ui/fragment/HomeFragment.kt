package com.dailyspark.mobile.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dailyspark.mobile.R
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.FragmentHomeBinding
import com.dailyspark.mobile.model.QuoteEntity
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.repository.StreakRepository
import com.dailyspark.mobile.service.ApiService
import com.dailyspark.mobile.ui.activity.QuotesViewActivity
import com.dailyspark.mobile.utils.ShareHelper
import com.dailyspark.mobile.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
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
                val quoteRepo =
                    QuoteRepository(apiServiceInstance, database.quoteDao(), requireContext())
                val streakRepo = StreakRepository(requireContext())
                return HomeViewModel(streakRepo, quoteRepo) as T
            }
        }
    }

    private fun getCategoryViews() = mapOf(
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupCategoryListeners()
        setupQuoteActions()
        observeData()
    }

    private fun setupCategoryListeners() {
        getCategoryViews().forEach { (name, layout) ->
            layout.setOnClickListener {
                viewModel.setCategory(name)
            }
        }
    }

    private fun setupQuoteActions() {
        binding.btnNext.setOnClickListener {
            val rotate = RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            rotate.duration = 500
            binding.ivNext.startAnimation(rotate)

            viewModel.showNextQuote()
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvQuote.text.toString()
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("quote", text))

            binding.tvCopy.text = "Copied!"
            binding.ivCopy.setImageResource(R.drawable.check)

            viewLifecycleOwner.lifecycleScope.launch {
                delay(1500)
                binding.tvCopy.text = "Copy"
                binding.ivCopy.setImageResource(R.drawable.copy)
            }
        }

        binding.btnShare.setOnClickListener {
            val quoteText = binding.tvQuote.text.toString()
            val authorText = binding.tvAuthor.text.toString()

            val shareText = """
        $quoteText
        
        $authorText
    """.trimIndent()

            ShareHelper.shareQuote(requireContext(), shareText)
        }
        binding.btnLike.setOnClickListener {
            val quote = viewModel.uiState.value.currentQuote ?: return@setOnClickListener
            val newStatus = !quote.isFavourite
            updateLikeButtonUI(newStatus)
            binding.ivLike.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
                binding.ivLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()

            viewModel.toggleFavourite(quote.id)
        }
    }

    private fun updateLikeButtonUI(isLiked: Boolean) {
        if (isLiked) {
            binding.ivLike.setImageResource(R.drawable.heart_selected)
            binding.ivLike.imageTintList = null
            binding.tvLike.text = "Liked"
        } else {
            binding.ivLike.setImageResource(R.drawable.heart)
            binding.ivLike.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.text_muted)
            )
            binding.tvLike.text = "Like"
        }
    }


    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateCategorySelectionUI(state.selectedCategory)
                    state.currentQuote?.let { updateQuoteCard(it, state.categoryQuotes) }
                    updateStreakUI(state.streakState)
                }
            }
        }
    }

    private fun updateCategorySelectionUI(selectedCategory: String) {
        val accentColor =
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent)
        val mutedTextColor = Color.parseColor("#8E8E93")

        getCategoryViews().forEach { (name, layout) ->
            val isSelected = name == selectedCategory

            val categoryThemeColor = getCategoryColor(name)
            val pastelBg = getPastelColor(categoryThemeColor)

            val selectionFrame = layout.getChildAt(0) as? FrameLayout
            selectionFrame?.let { frame ->
                val shape = GradientDrawable()
                shape.shape = GradientDrawable.RECTANGLE

                shape.cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    16f,
                    resources.displayMetrics
                )

                shape.setColor(pastelBg)

                if (isSelected) {
                    val strokeWidth = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        0.5f,
                        resources.displayMetrics
                    ).toInt()
                    shape.setStroke(strokeWidth, accentColor)
                } else {
                    val strokeWidth = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        0.5f,
                        resources.displayMetrics
                    ).toInt()
                    shape.setStroke(strokeWidth, pastelBg)
                }
                frame.background = shape
            }

            val textView = layout.getChildAt(1) as? TextView
            textView?.let { tv ->
                if (isSelected) {
                    tv.setTextColor(accentColor)
                    tv.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                } else {
                    tv.setTextColor(mutedTextColor)
                    tv.typeface = Typeface.DEFAULT
                }
            }
        }
    }


    private fun updateQuoteCard(quote: QuoteEntity, categoryQuotes: List<QuoteEntity>) {

        binding.tvQuote.text = '"' + quote.quote + '"'

        binding.tvAuthor.text = "— ${quote.author ?: "Unknown"}"
        updateLikeButtonUI(quote.isFavourite)

        binding.cardQuote.setOnClickListener {
            val intent = Intent(requireContext(), QuotesViewActivity::class.java).apply {
                putExtra("SELECTED_QUOTE_ID", quote.id)
                putExtra("QUOTE_IDS", categoryQuotes.map { it.id }.toIntArray())
                putExtra("RANDOM_NEXT", true)
            }
            startActivity(intent)
        }

        binding.tvQuote.text = '"' + quote.quote + '"'
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

    private fun getPastelColor(color: Int): Int {
        return Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "All", "Funny", "Happiness" -> Color.parseColor("#F5C842")
            "Motivational" -> Color.parseColor("#E87A56")
            "Love" -> Color.parseColor("#E84545")
            "Wisdom" -> Color.parseColor("#5AAAE8")
            "Friendship", "Life" -> Color.parseColor("#3DCBA0")
            "Success" -> Color.parseColor("#A49EED")
            else -> Color.parseColor("#F5C842")
        }
    }


    private fun updateStreakUI(state: HomeViewModel.StreakUiState) {
        binding.tvGreetingSmall.text = state.greeting
        binding.tvStreakCount.text = state.count.toString()

        val dayViews = listOf(
            binding.dayMon,
            binding.dayTue,
            binding.dayWed,
            binding.dayThu,
            binding.dayFri,
            binding.daySat,
            binding.daySun
        )
        state.dayStatuses.forEachIndexed { index, status ->
            val textView = dayViews[index]
            when (status) {
                is HomeViewModel.DayStatus.Completed -> {
                    textView.setBackgroundResource(R.drawable.bg_streak)
                    textView.setTextColor(resources.getColor(R.color.accent))
                }

                is HomeViewModel.DayStatus.Active -> {
                    textView.setBackgroundResource(R.drawable.bg_day_active)
                    textView.setTextColor(Color.WHITE)
                }

                else -> {
                    textView.setBackgroundResource(R.drawable.bg_day_normal)
                    textView.setTextColor(resources.getColor(R.color.text_primary))
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