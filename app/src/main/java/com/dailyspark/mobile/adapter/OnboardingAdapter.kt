package com.dailyspark.mobile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dailyspark.mobile.databinding.ItemOnboardingBinding
import com.dailyspark.mobile.model.OnboardingItem

class OnboardingAdapter : ListAdapter<OnboardingItem, OnboardingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(getItem(position).imageRes)
            .centerCrop()
            .into(holder.binding.ivBackground)
    }

    class ViewHolder(val binding: ItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root)

    class DiffCallback : DiffUtil.ItemCallback<OnboardingItem>() {
        override fun areItemsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem) = oldItem == newItem
    }
}