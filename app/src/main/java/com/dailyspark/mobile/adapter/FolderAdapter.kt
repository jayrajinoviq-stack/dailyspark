package com.dailyspark.mobile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dailyspark.mobile.databinding.ItemFolderCardBinding
import com.dailyspark.mobile.model.FolderWithCount

class FolderAdapter(
    private val onFolderClick: (FolderWithCount) -> Unit,
    private val onFolderLongClick: (FolderWithCount) -> Unit = {}
) : ListAdapter<FolderWithCount, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    companion object {
        private const val MAX_ITEMS_PER_FOLDER = 10
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position), onFolderClick, onFolderLongClick)
    }

    class FolderViewHolder(
        private val binding: ItemFolderCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: FolderWithCount,
            onClick: (FolderWithCount) -> Unit,
            onLongClick: (FolderWithCount) -> Unit
        ) {
            binding.tvFolderName.text = item.name
            binding.tvQuoteCount.text = "${item.itemCount}/$MAX_ITEMS_PER_FOLDER"

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }
}

private class FolderDiffCallback : DiffUtil.ItemCallback<FolderWithCount>() {
    override fun areItemsTheSame(oldItem: FolderWithCount, newItem: FolderWithCount): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: FolderWithCount, newItem: FolderWithCount): Boolean =
        oldItem == newItem
}