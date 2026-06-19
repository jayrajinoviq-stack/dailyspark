package com.example.dailyspark.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyspark.databinding.ItemFolderQuoteBinding
import com.example.dailyspark.model.FolderQuoteEntity

class FolderQuoteAdapter(
    private val onShareClick: (FolderQuoteEntity) -> Unit,
    private val onEditClick: (FolderQuoteEntity) -> Unit,
    private val onDeleteClick: (FolderQuoteEntity) -> Unit
) : ListAdapter<FolderQuoteEntity, FolderQuoteAdapter.QuoteViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): QuoteViewHolder {
        val binding = ItemFolderQuoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuoteViewHolder(private val binding: ItemFolderQuoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FolderQuoteEntity) {
            binding.tvCategory.text = item.category.uppercase()
            binding.tvQuote.text = "\u201C${item.quote}\u201D"
            binding.tvAuthor.text = "\u2014 ${item.author}"

            binding.ivShare.setOnClickListener { onShareClick(item) }
            binding.ivEdit.setOnClickListener { onEditClick(item) }
            binding.ivDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FolderQuoteEntity>() {
            override fun areItemsTheSame(oldItem: FolderQuoteEntity, newItem: FolderQuoteEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: FolderQuoteEntity, newItem: FolderQuoteEntity) =
                oldItem == newItem
        }
    }
}