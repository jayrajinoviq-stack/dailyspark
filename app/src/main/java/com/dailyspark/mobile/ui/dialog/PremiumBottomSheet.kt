package com.dailyspark.mobile.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dailyspark.mobile.databinding.BottomSheetPremiumBinding
import com.dailyspark.mobile.model.PremiumBottomSheetData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PremiumBottomSheet(
    private val data: PremiumBottomSheetData,
    private val onPrimaryClick: () -> Unit,
    private val onSecondaryClick: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetPremiumBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = BottomSheetPremiumBinding.inflate(inflater)

        binding.ivIcon.setImageResource(data.icon)

        binding.tvTitle.text = data.title

        binding.tvDescription.text = data.description

        binding.btnPrimaryText.text = data.primaryButton

        binding.ivSecondaryText.text = data.secondaryButton

        binding.btnPrimary.setOnClickListener {
            dismiss()
            onPrimaryClick()
        }

        binding.btnSecondary.setOnClickListener {
            dismiss()
            onSecondaryClick?.invoke()
        }

        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }


}