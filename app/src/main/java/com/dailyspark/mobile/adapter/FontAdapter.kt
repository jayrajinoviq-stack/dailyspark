package com.dailyspark.mobile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dailyspark.mobile.R
import com.dailyspark.mobile.databinding.ItemFontBinding
import com.dailyspark.mobile.databinding.ItemFontHeaderBinding
import com.dailyspark.mobile.model.FontListItem
import com.dailyspark.mobile.model.FontOption

class FontAdapter(
    private val isUserPremium: Boolean,
    private val onFontClick: (FontOption) -> Unit,
    private val onLockedFontClick: (FontOption) -> Unit = {}
) : ListAdapter<FontListItem, RecyclerView.ViewHolder>(FontListDiffCallback()) {

    private var selectedId: Int = -1

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FONT = 1
    }

    fun setSelected(fontId: Int) {
        val prev = selectedId
        selectedId = fontId
        currentList.forEachIndexed { index, item ->
            if (item is FontListItem.Item && (item.font.id == prev || item.font.id == fontId)) {
                notifyItemChanged(index)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is FontListItem.Header -> TYPE_HEADER
            is FontListItem.Item -> TYPE_FONT
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemFontHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemFontBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            FontViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FontListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is FontListItem.Item -> (holder as FontViewHolder).bind(item.font)
        }
    }

     class HeaderViewHolder(private val binding: ItemFontHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: FontListItem.Header) {
            binding.tvHeader.text = header.title
            binding.proFont.visibility =
                if (header.isPremium) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    inner class FontViewHolder(private val binding: ItemFontBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FontOption) {
            val ctx = itemView.context
            val isSelected = item.id == selectedId
            val isLocked = item.isPremium && !isUserPremium

            binding.fontName.text = item.name

            val typeface = ResourcesCompat.getFont(ctx, item.fontResId)
            binding.fontPreview.typeface = typeface
            binding.fontPreview.text = "Aa"

            binding.ivPremium.visibility = if (item.isPremium) android.view.View.VISIBLE else android.view.View.GONE

            if (isSelected) {
                binding.cardFont.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.accent))
                binding.cardFont.strokeWidth = 0
                binding.cardFont.cardElevation = ctx.resources.getDimension(R.dimen.font_card_elevation_selected)
                binding.fontPreview.setTextColor(ContextCompat.getColor(ctx, R.color.selected))
                binding.fontName.setTextColor(ContextCompat.getColor(ctx, R.color.selected))
            } else {
                binding.cardFont.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_secondary))
                binding.cardFont.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.font_card_stroke)
                binding.cardFont.strokeColor = ContextCompat.getColor(ctx, R.color.border)
                binding.cardFont.cardElevation = 0f
                binding.fontPreview.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                binding.fontName.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }

            binding.root.setOnClickListener {
                if (isLocked) onLockedFontClick(item) else onFontClick(item)
            }
        }
    }

    private class FontListDiffCallback : DiffUtil.ItemCallback<FontListItem>() {
        override fun areItemsTheSame(oldItem: FontListItem, newItem: FontListItem): Boolean {
            return when {
                oldItem is FontListItem.Header && newItem is FontListItem.Header ->
                    oldItem.title == newItem.title
                oldItem is FontListItem.Item && newItem is FontListItem.Item ->
                    oldItem.font.id == newItem.font.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: FontListItem, newItem: FontListItem): Boolean =
            oldItem == newItem
    }
}