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
import com.dailyspark.mobile.model.FontOption

class FontAdapter(
    private val isUserPremium: Boolean,
    private val onFontClick: (FontOption) -> Unit
) : ListAdapter<FontOption, FontAdapter.FontViewHolder>(FontDiffCallback()) {

    private var selectedId: Int = -1

    fun setSelected(fontId: Int) {
        val prev = selectedId
        selectedId = fontId
        currentList.forEachIndexed { index, item ->
            if (item.id == prev || item.id == fontId) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(getItem(position))
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

            binding.cardFont.alpha = if (isLocked) 0.55f else 1f

            binding.root.setOnClickListener {
                onFontClick(item)
            }
        }
    }

    private class FontDiffCallback : DiffUtil.ItemCallback<FontOption>() {
        override fun areItemsTheSame(oldItem: FontOption, newItem: FontOption): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FontOption, newItem: FontOption): Boolean =
            oldItem == newItem
    }
}