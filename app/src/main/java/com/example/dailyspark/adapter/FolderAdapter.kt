package com.example.dailyspark.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailyspark.databinding.ItemAddFolderBinding
import com.example.dailyspark.databinding.ItemFolderCardBinding
import com.example.dailyspark.model.FolderWithCount

class FolderAdapter(
    private val onAddFolderClick: () -> Unit,
    private val onFolderClick: FolderWithCount.() -> Unit
) : ListAdapter<FolderWithCount, RecyclerView.ViewHolder>(FolderDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_ADD = 0
        private const val VIEW_TYPE_FOLDER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ADD else VIEW_TYPE_FOLDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_ADD) {
            val binding = ItemAddFolderBinding.inflate(layoutInflater, parent, false)
            AddFolderViewHolder(binding)
        } else {
            val binding = ItemFolderCardBinding.inflate(layoutInflater, parent, false)
            FolderViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AddFolderViewHolder) {
            holder.bind(onAddFolderClick)
        } else if (holder is FolderViewHolder) {
            val folder = getItem(position - 1)
            holder.bind(folder, onFolderClick)
        }
    }

    override fun getItemCount(): Int = currentList.size + 1

    class AddFolderViewHolder(private val binding: ItemAddFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(onClick: () -> Unit) {
            binding.root.setOnClickListener { onClick() }
        }
    }

    class FolderViewHolder(private val binding: ItemFolderCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FolderWithCount, onClick: (FolderWithCount) -> Unit) {
            binding.tvFolderName.text = item.name
            binding.tvQuoteCount.text = item.itemCount.toString()
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

class FolderDiffCallback : DiffUtil.ItemCallback<FolderWithCount>() {
    override fun areItemsTheSame(oldItem: FolderWithCount, newItem: FolderWithCount): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FolderWithCount, newItem: FolderWithCount): Boolean {
        return oldItem == newItem
    }
}