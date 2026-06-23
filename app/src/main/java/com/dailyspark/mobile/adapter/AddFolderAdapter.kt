package com.dailyspark.mobile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dailyspark.mobile.databinding.ItemAddFolderBinding

class AddFolderAdapter(
    private val onAddFolderClick: () -> Unit
) : RecyclerView.Adapter<AddFolderAdapter.AddFolderViewHolder>() {

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddFolderViewHolder {
        val binding = ItemAddFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AddFolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddFolderViewHolder, position: Int) {
        holder.bind(onAddFolderClick)
    }

    class AddFolderViewHolder(
        private val binding: ItemAddFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(onClick: () -> Unit) {
            binding.root.setOnClickListener { onClick() }
        }
    }
}