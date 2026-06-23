package com.dailyspark.mobile.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dailyspark.mobile.R
import com.dailyspark.mobile.databinding.ItemQuoteBinding
import com.dailyspark.mobile.model.QuoteEntity

class QuoteAdapter(
    private val isSavedMode: Boolean,
    private val onFavouriteClick: (QuoteEntity) -> Unit,
    private val onShareClick: (QuoteEntity) -> Unit,
    private val onItemClick: (QuoteEntity, List<QuoteEntity>) -> Unit
) : ListAdapter<QuoteEntity, QuoteAdapter.QuoteViewHolder>(DiffCallback()) {

    companion object {
        private const val PAYLOAD_FAVOURITE = "favourite_changed"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onItemClick(item, currentList)
        }
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_FAVOURITE)) {
            holder.updateFavouriteUI(getItem(position).isFavourite)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class QuoteViewHolder(private val binding: ItemQuoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuoteEntity) {
            binding.tvQuote.text = "“${item.quote}”"
            binding.tvAuthor.text = "— ${item.author}"
            binding.tvCategory.text = item.category

            if (isSavedMode) {
                binding.saveFavourite.visibility = View.GONE
                binding.shareQuote.visibility = View.VISIBLE
            } else {
                binding.saveFavourite.visibility = View.VISIBLE
                binding.shareQuote.visibility = View.GONE
                updateFavouriteUI(item.isFavourite)
            }

            binding.saveFavourite.setOnClickListener { onFavouriteClick(item) }
            binding.shareQuote.setOnClickListener { onShareClick(item) }

            binding.copyQuote.setOnClickListener {
                val ctx = binding.root.context
                val text = "“${item.quote}” — ${item.author}"
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("quote", text))
                Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        fun updateFavouriteUI(isFavourite: Boolean) {
            binding.saveFavourite.setImageResource(
                if (isFavourite) R.drawable.heart_selected else R.drawable.heart
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<QuoteEntity>() {
        override fun areItemsTheSame(old: QuoteEntity, new: QuoteEntity) = old.id == new.id
        override fun areContentsTheSame(old: QuoteEntity, new: QuoteEntity) = old == new
        override fun getChangePayload(old: QuoteEntity, new: QuoteEntity): Any? =
            if (old.isFavourite != new.isFavourite) PAYLOAD_FAVOURITE else null
    }
}