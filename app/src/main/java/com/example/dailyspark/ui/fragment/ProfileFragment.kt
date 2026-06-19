package com.example.dailyspark.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.dailyspark.BuildConfig
import com.example.dailyspark.R
import com.example.dailyspark.databinding.FragmentExploreBinding
import com.example.dailyspark.databinding.FragmentHomeBinding
import com.example.dailyspark.databinding.FragmentProfileBinding


class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)


        binding.appVersion.text = "Daily Spark v${BuildConfig.VERSION_NAME}"
    }
}