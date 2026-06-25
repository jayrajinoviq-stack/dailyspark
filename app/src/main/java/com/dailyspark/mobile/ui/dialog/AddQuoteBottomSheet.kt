package com.dailyspark.mobile.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.dailyspark.mobile.R
import com.dailyspark.mobile.databinding.BottomSheetAddQuoteBinding
import com.dailyspark.mobile.model.FolderQuoteEntity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddQuoteBottomSheet(
    private val folderName: String,
    private val currentCount: Int,
    private val maxCount: Int = 10,
    private val existing: FolderQuoteEntity? = null,
    private val onSaveClick: (quote: String, author: String, category: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddQuoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {

            dialog.window?.apply {
                navigationBarColor = requireContext().getColor(R.color.card)
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

        _binding = BottomSheetAddQuoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvFolderName.text = folderName
        binding.tvCount.text = "$currentCount/$maxCount quotes in this category"

        existing?.let {
            binding.etQuote.setText(it.quote)
            binding.etAuthor.setText(it.author)
            binding.etCategory.setText(it.category)
            binding.tvAdd.text = "Save Quote"
        }

        binding.etQuote.doAfterTextChanged {
            val text = it.toString()

            if (binding.etQuote.lineCount > 5) {
                binding.etQuote.setText(text.dropLast(1))
                binding.etQuote.setSelection(binding.etQuote.text?.length ?: 0)
            }
        }

        binding.etAuthor.doAfterTextChanged {
            val clean = it.toString().replace("\n", "")
            if (clean != it.toString()) {
                binding.etAuthor.setText(clean)
                binding.etAuthor.setSelection(clean.length)
            }
        }

        binding.etCategory.doAfterTextChanged {
            val clean = it.toString().replace("\n", "")
            if (clean != it.toString()) {
                binding.etCategory.setText(clean)
                binding.etCategory.setSelection(clean.length)
            }
        }

        binding.btnAdd.isEnabled = !binding.etQuote.text.isNullOrBlank()
        updateButtonState(!binding.etQuote.text.isNullOrBlank())

        binding.etQuote.doAfterTextChanged { editable ->
            val isEnabled = !editable?.toString()?.trim().isNullOrEmpty()
            binding.btnAdd.isEnabled = isEnabled
            updateButtonState(isEnabled)
        }

        binding.closeBtn.setOnClickListener {
            binding.etCategory.text?.clear()
        }

        binding.etCategory.doAfterTextChanged { editable ->
            binding.closeBtn.visibility =
                if (editable?.toString()?.trim().isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        binding.btnAdd.setOnClickListener {

            val quote = binding.etQuote.text.toString().trim()

            if (quote.isEmpty()) {
                binding.etQuote.error = "Enter a quote"
                return@setOnClickListener
            }

            val author = binding.etAuthor.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()

            onSaveClick(quote, author, category)

            dismiss()
        }

    }

    private fun updateButtonState(isEnabled: Boolean) {

        if (isEnabled) {

            binding.btnAdd.setBackgroundResource(R.drawable.bg_btn_primary)

            binding.tvAdd.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.selected)
            )

        } else {

            binding.btnAdd.setBackgroundResource(R.drawable.bg_btn_secondary)

            binding.tvAdd.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_muted)
            )

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}