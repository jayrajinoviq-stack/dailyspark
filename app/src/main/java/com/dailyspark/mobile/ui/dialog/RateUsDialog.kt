package com.dailyspark.mobile.ui.dialog

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.dailyspark.mobile.R
import com.dailyspark.mobile.databinding.DialogRateUsBinding

class RateUsDialog : DialogFragment() {

    companion object {
        const val TAG = "RateUsDialog"
    }

    private var _binding: DialogRateUsBinding? = null
    private val binding get() = _binding!!

    private var selectedRating = 0

    private val stars by lazy {
        listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRateUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.88f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        animateDialog()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStars()

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.alpha = .5f

        binding.btnSubmit.setOnClickListener {

            if (selectedRating == 0)
                return@setOnClickListener

            openPlayStore()

            dismiss()
        }

        binding.btnLater.setOnClickListener {
            dismiss()
        }
    }

    private fun setupStars() {

        stars.forEachIndexed { index, imageView ->

            imageView.setOnClickListener {

                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                selectedRating = index + 1

                updateStars()

                animateStar(imageView)

                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.alpha = 1f
            }
        }
    }

    private fun updateStars() {

        val selectedColor = ContextCompat.getColor(requireContext(), R.color.app_accent)
        val normalColor = ContextCompat.getColor(requireContext(), R.color.text_muted)

        stars.forEachIndexed { index, image ->

            val isSelected = index < selectedRating

            image.setImageResource(
                if (isSelected)
                    R.drawable.star_selected
                else
                    R.drawable.star_outline
            )

            image.imageTintList = ColorStateList.valueOf(
                if (isSelected) selectedColor else normalColor
            )
        }

        binding.tvRatingMessage.text = when (selectedRating) {

            1 -> "We're sorry to hear that."

            2 -> "We'll keep improving."

            3 -> "Thanks for your feedback."

            4 -> "Glad you like DailySpark!"

            5 -> "Awesome! Thank you ❤️"

            else -> "Tap a star to rate."
        }
    }

    private fun animateStar(view: View) {

        view.animate()
            .scaleX(1.30f)
            .scaleY(1.30f)
            .setDuration(120)
            .withEndAction {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(OvershootInterpolator())
                    .start()

            }.start()
    }

    private fun animateDialog() {

        binding.root.apply {

            alpha = 0f
            scaleX = .90f
            scaleY = .90f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    private fun openPlayStore() {

        val packageName = requireContext().packageName

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )

        } catch (e: ActivityNotFoundException) {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    override fun dismiss() {

        binding.root.animate()
            .alpha(0f)
            .scaleX(.9f)
            .scaleY(.9f)
            .setDuration(150)
            .withEndAction {

                super.dismiss()
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}