package com.dailyspark.mobile.ui.dialog

import android.app.Activity
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
import com.dailyspark.mobile.ads.InterstitialAdManager
import com.dailyspark.mobile.databinding.LayoutFontBottomSheetBinding
import com.dailyspark.mobile.model.FontListItem
import com.dailyspark.mobile.model.FontOption
import com.dailyspark.mobile.model.PremiumBottomSheetData
import com.dailyspark.mobile.utils.FontUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FontPickerSheet(
    private val activity: Activity,
    private val isUserPremium: Boolean = false,
    private val currentFontId: Int = -1,
    private val onFontSelected: (FontOption) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutFontBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var pendingSelection: FontOption? = null
    private lateinit var fontAdapter: FontAdapter

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

        fontAdapter = FontAdapter(
            isUserPremium = isUserPremium,
            onFontClick = { selectedFont ->
                pendingSelection = selectedFont
                fontAdapter.setSelected(selectedFont.id)
            },
            onLockedFontClick = { lockedFont ->
                PremiumBottomSheet(

                    data = PremiumBottomSheetData(

                        icon = R.drawable.play,

                        title = "Premium Font",

                        description = "Watch a short ad to unlock this font instantly or upgrade to Pro for unlimited premium fonts.",

                        features = listOf(
                            "Free forever",
                            "~30 seconds",
                            "Skippable"
                        ),

                        primaryButton = "Watch Ad & Unlock",

                        secondaryButton = "Upgrade to Pro — Remove All Ads"
                    ),

                    onPrimaryClick = {
                        InterstitialAdManager.showInterstitialDirect(activity) {
                            pendingSelection = lockedFont
                            fontAdapter.setSelected(lockedFont.id)
                        }
                    },

                    onSecondaryClick = {

                    }

                ).show(parentFragmentManager, "premium")
            }
        )

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (fontAdapter.currentList.getOrNull(position)) {
                    is FontListItem.Header -> 2
                    else -> 1
                }
            }
        }

        binding.rvFonts.apply {
            layoutManager = gridLayoutManager
            adapter = fontAdapter
        }

        fontAdapter.submitList(FontUtils.getGroupedFontListItems())
        if (currentFontId != -1) fontAdapter.setSelected(currentFontId)

        binding.btnDone.setOnClickListener {
            pendingSelection?.let { onFontSelected(it) }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FontPickerSheet"
    }
}