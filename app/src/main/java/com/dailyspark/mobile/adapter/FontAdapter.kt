package com.dailyspark.mobile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dailyspark.mobile.databinding.ItemFontBinding
import com.dailyspark.mobile.model.FontOption

class FontAdapter(private val onFontClick: (FontOption) -> Unit) :
    ListAdapter<FontOption, FontAdapter.FontViewHolder>(FontDiffCallback()) {

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
            binding.fontPreview.text = item.name

            val typeface = ResourcesCompat.getFont(itemView.context, item.fontResId)
            binding.fontPreview.typeface = typeface

            binding.root.setOnClickListener { onFontClick(item) }
        }
    }

    private class FontDiffCallback : DiffUtil.ItemCallback<FontOption>() {
        override fun areItemsTheSame(oldItem: FontOption, newItem: FontOption): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FontOption, newItem: FontOption): Boolean =
            oldItem == newItem
    }
}