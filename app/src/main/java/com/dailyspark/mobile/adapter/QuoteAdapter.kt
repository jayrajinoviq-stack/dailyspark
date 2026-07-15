package com.dailyspark.mobile.adapter

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dailyspark.mobile.R
import com.dailyspark.mobile.ads.InterstitialAdManager
import com.dailyspark.mobile.ads.LargeNativeAdsManager
import com.dailyspark.mobile.databinding.ItemQuoteAdsBinding
import com.dailyspark.mobile.databinding.ItemQuoteBinding
import com.dailyspark.mobile.model.QuoteEntity

sealed class QuoteListItem {
    data class Quote(val quote: QuoteEntity) : QuoteListItem()
    object Ad : QuoteListItem()
}

class QuoteAdapter(
    private val context: Activity,
    private val isSavedMode: Boolean,
    private val lifecycleOwner: LifecycleOwner,
    private val onFavouriteClick: (QuoteEntity) -> Unit,
    private val onShareClick: (QuoteEntity) -> Unit,
    private val onItemClick: (QuoteEntity, List<QuoteEntity>) -> Unit
) : ListAdapter<QuoteListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val PAYLOAD_FAVOURITE = "favourite_changed"
        private const val TYPE_QUOTE = 0
        private const val TYPE_AD = 1
        private const val AD_POSITION = 2 // 3rd row
        private const val MIN_ITEMS_FOR_AD = 2
    }

    /** Holds the last real list of quotes (without the ad), for callbacks needing pure quotes. */
    private var realQuotes: List<QuoteEntity> = emptyList()

    /** Call this instead of submitList() directly from the fragment. */
    fun submitQuotes(quotes: List<QuoteEntity>, commitCallback: Runnable? = null) {
        realQuotes = quotes
        val wrapped = mutableListOf<QuoteListItem>()
        quotes.forEachIndexed { index, q ->
            if (isSavedMode && index == AD_POSITION && quotes.size >= MIN_ITEMS_FOR_AD) {
                wrapped.add(QuoteListItem.Ad)
            }
            wrapped.add(QuoteListItem.Quote(q))
        }
        if (commitCallback != null) submitList(wrapped, commitCallback) else submitList(wrapped)
    }

    /** Safe helper to get the real QuoteEntity for a given adapter position. */
    fun quoteAt(position: Int): QuoteEntity? {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= itemCount) return null
        return (getItem(position) as? QuoteListItem.Quote)?.quote
    }

    fun isAdAt(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= itemCount) return false
        return getItem(position) is QuoteListItem.Ad
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is QuoteListItem.Ad -> TYPE_AD
            is QuoteListItem.Quote -> TYPE_QUOTE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AD -> {
                val binding = ItemQuoteAdsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                AdViewHolder(binding)
            }

            else -> {
                val binding = ItemQuoteBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                QuoteViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is QuoteListItem.Ad -> (holder as AdViewHolder).bind()
            is QuoteListItem.Quote -> {
                (holder as QuoteViewHolder).bind(item.quote)
                holder.itemView.setOnClickListener {
                    onItemClick(item.quote, realQuotes)
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position)
        if (holder is QuoteViewHolder && item is QuoteListItem.Quote &&
            payloads.contains(PAYLOAD_FAVOURITE)
        ) {
            holder.updateFavouriteUI(item.quote.isFavourite)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class AdViewHolder(private val binding: ItemQuoteAdsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            LargeNativeAdsManager.showNativeAd(
                context = binding.root.context,
                container = binding.nativeAdFrame,
                lifecycleOwner = lifecycleOwner,
                adUnitKey = "saved_list_ad_slot_${System.currentTimeMillis()}"
            )
        }
    }

    inner class QuoteViewHolder(private val binding: ItemQuoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuoteEntity) {
            binding.tvQuote.text = "“${item.quote}”"
            binding.tvAuthor.text = "— ${item.author}"

            setCategoryStyle(binding.tvCategory, binding.quotesColor, item.category)

            if (isSavedMode) {
                binding.saveFavourite.visibility = View.GONE
                binding.shareQuote.visibility = View.VISIBLE
                binding.quotesColor.visibility = View.VISIBLE
            } else {
                binding.saveFavourite.visibility = View.VISIBLE
                binding.shareQuote.visibility = View.GONE
                binding.quotesColor.visibility = View.GONE
                updateFavouriteUI(item.isFavourite)
            }

            binding.saveFavourite.setOnClickListener { onFavouriteClick(item) }
            binding.shareQuote.setOnClickListener { onShareClick(item) }

            binding.copyQuote.setOnClickListener {
                val ctx = binding.root.context
                val text = "“${item.quote}” — ${item.author}"
                InterstitialAdManager.onUserAction(context) {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("quote", text))
                    Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun updateFavouriteUI(isFavourite: Boolean) {
            if (isFavourite) {
                binding.saveFavourite.setImageResource(R.drawable.heart_selected)
                binding.saveFavourite.imageTintList = null
            } else {
                binding.saveFavourite.setImageResource(R.drawable.heart)
                val tintColor = Color.parseColor("#777770")
                binding.saveFavourite.imageTintList = ColorStateList.valueOf(tintColor)
            }
        }
    }

    private fun setCategoryStyle(textView: TextView, colorLine: View, category: String) {
        val colorHex = when (category.lowercase()) {
            "motivational", "success" -> "#A49EED"
            "love" -> "#E84545"
            "wisdom" -> "#5AAAE8"
            "friendship", "life" -> "#3DCBA0"
            "funny", "humor", "happiness", "all" -> "#F5C842"
            else -> "#F5C842"
        }
        val baseColor = Color.parseColor(colorHex)
        textView.text = category.uppercase()
        textView.setTextColor(baseColor)
        val alphaColor = ColorUtils.setAlphaComponent(baseColor, 40)
        textView.backgroundTintList = ColorStateList.valueOf(alphaColor)
        colorLine.backgroundTintList = ColorStateList.valueOf(baseColor)
    }

    class DiffCallback : DiffUtil.ItemCallback<QuoteListItem>() {
        override fun areItemsTheSame(old: QuoteListItem, new: QuoteListItem): Boolean {
            return when {
                old is QuoteListItem.Ad && new is QuoteListItem.Ad -> true
                old is QuoteListItem.Quote && new is QuoteListItem.Quote -> old.quote.id == new.quote.id
                else -> false
            }
        }

        override fun areContentsTheSame(old: QuoteListItem, new: QuoteListItem): Boolean =
            old == new

        override fun getChangePayload(old: QuoteListItem, new: QuoteListItem): Any? {
            if (old is QuoteListItem.Quote && new is QuoteListItem.Quote &&
                old.quote.isFavourite != new.quote.isFavourite
            ) {
                return PAYLOAD_FAVOURITE
            }
            return null
        }
    }
}