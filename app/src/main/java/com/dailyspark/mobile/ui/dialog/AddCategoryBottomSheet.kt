package com.dailyspark.mobile.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.dailyspark.mobile.R
import com.dailyspark.mobile.databinding.BottomSheetAddCategoryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddCategoryBottomSheet(
    private val currentCount: Int,
    private val maxCount: Int = 5,
    private val onCreateClick: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {

            dialog.window?.apply {
                navigationBarColor =
                    requireContext().getColor(R.color.card)
            }

            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let {

                it.setBackgroundColor(Color.TRANSPARENT)

                BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isDraggable = true
                }
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            BottomSheetAddCategoryBinding.inflate(
                inflater,
                container,
                false
            )

        setFeatureText(binding.tvFeature1, "Custom name")
        setFeatureText(binding.tvFeature2, "Saved quotes")
        setFeatureText(binding.tvFeature3, "Easy access")

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreate.isEnabled = false

        binding.tvCount.text =
            "$currentCount / $maxCount categories used"

        binding.etCategoryName.doAfterTextChanged { editable ->

            val text = editable?.toString()?.trim().orEmpty()
            val isEnabled = text.isNotEmpty()

            binding.btnCreate.isEnabled = isEnabled

            binding.closeBtn.visibility =
                if (isEnabled) View.VISIBLE else View.GONE

            if (isEnabled) {

                binding.btnCreate.setBackgroundResource(R.drawable.bg_btn_primary)

                binding.tvCreate.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.selected)
                )

                binding.ivCreate.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.selected)
                )

            } else {

                binding.btnCreate.setBackgroundResource(R.drawable.bg_btn_secondary)

                binding.tvCreate.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_muted)
                )

                binding.ivCreate.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.text_muted)
                )
            }

            binding.etCategoryName.error = null
        }

        binding.closeBtn.setOnClickListener {
            binding.etCategoryName.text?.clear()
        }

        binding.btnCreate.setOnClickListener {

            val categoryName =
                binding.etCategoryName.text.toString().trim()

            if (categoryName.isEmpty()) {
                binding.etCategoryName.error =
                    "Enter category name"
                return@setOnClickListener
            }

            onCreateClick(categoryName)

            dismiss()
        }

        binding.tvCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setFeatureText(
        textView: TextView,
        text: String
    ) {
        val fullText = "• $text"

        val spannable = SpannableString(fullText)

        spannable.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.tag_success
                )
            ),
            0,
            1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}