package com.dailyspark.mobile.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.dailyspark.mobile.R
import com.dailyspark.mobile.adapter.FontAdapter
import com.dailyspark.mobile.databinding.LayoutFontBottomSheetBinding
import com.dailyspark.mobile.model.FontOption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FontPickerSheet(
    private val onFontSelected: (FontOption) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutFontBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutFontBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fontAdapter = FontAdapter { selectedFont ->
            onFontSelected(selectedFont)
            dismiss()
        }

        binding.rvFonts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = fontAdapter
        }

        fontAdapter.submitList(getAppFonts())
    }

    private fun getAppFonts() = listOf(
        FontOption(1, "Cormorant", R.font.cormorant_garamond),
        FontOption(2, "Dancing Script", R.font.dancing_script),
        FontOption(3, "DM Serif", R.font.dm_serif_display),
        FontOption(4, "Great Vibes", R.font.great_vibes),
        FontOption(5, "Kaushan", R.font.kaushan_script),
        FontOption(6, "Manrope", R.font.manrope),
        FontOption(7, "Merriweather", R.font.merriweather),
        FontOption(8, "Montserrat", R.font.montserrat),
        FontOption(9, "Nunito", R.font.nunito),
        FontOption(10, "Pacifico", R.font.pacifico),
        FontOption(11, "Playfair", R.font.playfair_display),
        FontOption(12, "Poppins", R.font.poppins),
        FontOption(13, "Raleway", R.font.raleway),
        FontOption(14, "Satisfy", R.font.satisfy)
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FontPickerSheet"
    }
}