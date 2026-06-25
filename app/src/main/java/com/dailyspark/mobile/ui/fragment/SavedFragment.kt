package com.dailyspark.mobile.ui.fragment

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dailyspark.mobile.R
import com.dailyspark.mobile.adapter.QuoteAdapter
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.FragmentSavedBinding
import com.dailyspark.mobile.model.QuoteEntity
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.service.ApiService
import com.dailyspark.mobile.ui.activity.QuotesViewActivity
import com.dailyspark.mobile.utils.ShareHelper
import com.dailyspark.mobile.viewmodel.QuoteUiState
import com.dailyspark.mobile.viewmodel.QuoteViewModel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: QuoteViewModel
    private val adapter by lazy {
        QuoteAdapter(
            isSavedMode = true,
            onFavouriteClick = { quote -> viewModel.toggleFavourite(quote.id) },
            onShareClick = { quote -> shareQuote(quote) },
            onItemClick = { quote, currentList ->
                val intent = Intent(requireContext(), QuotesViewActivity::class.java).apply {
                    putExtra("SELECTED_QUOTE_ID", quote.id)
                    putExtra("QUOTE_IDS", currentList.map { it.id }.toIntArray())
                }
                startActivity(intent)
            }
        )
    }


    private fun shareQuote(quote: QuoteEntity) {
        val quoteText = quote.quote
        val authorText = quote.author

        val shareText = """
        $quoteText
        
        $authorText
    """.trimIndent()

        ShareHelper.shareQuote(requireContext(), shareText)
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
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            private val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.delete)?.apply {
                DrawableCompat.setTint(this, Color.WHITE)
            }
            private val background = ColorDrawable(Color.parseColor("#E53935"))

            private val iconSizePx = (24 * resources.displayMetrics.density).toInt()

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val quote = adapter.currentList[position]

                viewModel.toggleFavourite(quote.id)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                deleteIcon?.let {
                    val iconTop = itemView.top + (itemHeight - iconSizePx) / 2
                    val iconBottom = iconTop + iconSizePx

                    val margin = (itemHeight - iconSizePx) / 2
                    val iconRight = itemView.right - margin
                    val iconLeft = itemView.right - margin - iconSizePx

                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    if (dX < -margin) {
                        it.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvFavourites)
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
        }
    }

    private fun observeFavourites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favouriteUiState.collect { state ->
                    when (state) {
                        is QuoteUiState.Loading -> {
                            binding.totalFavItems.text = "0 Saved Quotes"
                        }
                        is QuoteUiState.Success -> {
                            binding.rvFavourites.visibility = View.VISIBLE
                            binding.layoutEmpty.visibility = View.GONE
                            adapter.submitList(state.quotes)
                            binding.totalFavItems.text = "${state.quotes.size} Saved Quotes"
                        }
                        is QuoteUiState.Empty -> {
                            binding.rvFavourites.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                            adapter.submitList(emptyList())
                            binding.totalFavItems.text = "0 Saved Quotes"
                        }
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