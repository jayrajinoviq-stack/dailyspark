package com.dailyspark.mobile.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.dailyspark.mobile.R
import com.dailyspark.mobile.adapter.FontAdapter
import com.dailyspark.mobile.databinding.LayoutFontBottomSheetBinding
import com.dailyspark.mobile.model.FontOption
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FontPickerSheet(
    private val isUserPremium: Boolean = false,
    private val currentFontId: Int = -1,
    private val onFontSelected: (FontOption) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutFontBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var pendingSelection: FontOption? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {

            dialog.window?.apply {
                navigationBarColor = requireContext().getColor(R.color.background)

                val isDark = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES

                androidx.core.view.WindowInsetsControllerCompat(this, decorView)
                    .isAppearanceLightNavigationBars = !isDark
            }

            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let {

                it.setBackgroundColor(Color.TRANSPARENT)

                val screenHeight = resources.displayMetrics.heightPixels
                val targetHeight = (screenHeight * 0.75).toInt()
                it.layoutParams.height = targetHeight
                it.requestLayout()

                BottomSheetBehavior.from(it).apply {
                    peekHeight = targetHeight
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
        _binding = LayoutFontBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fontAdapter = FontAdapter(isUserPremium) { selectedFont ->
            val locked = selectedFont.isPremium && !isUserPremium
            if (locked) {
                return@FontAdapter
            }
            pendingSelection = selectedFont
            (binding.rvFonts.adapter as? FontAdapter)?.setSelected(selectedFont.id)
        }

        binding.rvFonts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = fontAdapter
        }

        val fonts = getAppFonts()
        fontAdapter.submitList(fonts)
        if (currentFontId != -1) fontAdapter.setSelected(currentFontId)

        binding.btnDone.setOnClickListener {
            pendingSelection?.let { onFontSelected(it) }
            dismiss()
        }
    }

    private fun getAppFonts() = listOf(
        FontOption(7, "Manrope", R.font.manrope),
        FontOption(8, "Merriweather", R.font.merriweather),
        FontOption(9, "Montserrat", R.font.montserrat),
        FontOption(10, "Nunito", R.font.nunito),
        FontOption(11, "Pacifico", R.font.pacifico),
        FontOption(12, "Poppins", R.font.poppins),
        FontOption(13, "Raleway", R.font.raleway),
        FontOption(14, "Satisfy", R.font.satisfy),

        FontOption(1, "Great Vibes", R.font.great_vibes, isPremium = true),
        FontOption(2, "Dancing Script", R.font.dancing_script, isPremium = true),
        FontOption(3, "Playfair", R.font.playfair_display, isPremium = true),
        FontOption(4, "Cormorant", R.font.cormorant_garamond, isPremium = true),
        FontOption(5, "DM Serif", R.font.dm_serif_display, isPremium = true),
        FontOption(6, "Kaushan", R.font.kaushan_script, isPremium = true)
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FontPickerSheet"
    }
}