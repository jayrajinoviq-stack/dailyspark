package com.dailyspark.mobile.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.dailyspark.mobile.R
import com.dailyspark.mobile.adapter.OnboardingAdapter
import com.dailyspark.mobile.ads.AdsResponse
import com.dailyspark.mobile.ads.AppOpenAdManager
import com.dailyspark.mobile.ads.InterstitialAdManager
import com.dailyspark.mobile.databinding.ActivityOnboardingBinding
import com.dailyspark.mobile.model.OnboardingItem

class OnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private val onboardingAdapter = OnboardingAdapter()
    private lateinit var items: List<OnboardingItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.btnContinue) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as RelativeLayout.LayoutParams
            params.bottomMargin = systemBars.bottom + 60
            v.layoutParams = params
            insets
        }

        items = listOf(
            OnboardingItem(
                1,
                "Daily Motivation",
                "Start every day with powerful quotes that inspire action.",
                R.drawable.onboarding_1
            ),
            OnboardingItem(
                2,
                "Save Favorites",
                "Keep your favorite quotes and revisit them whenever you need.",
                R.drawable.onboarding_2
            ),
            OnboardingItem(
                3,
                "Build Consistency",
                "Maintain your daily streak and watch motivation become a habit.",
                R.drawable.onboarding_3
            )
        )

        setupViewPager()

        binding.tvSkip.setOnClickListener { finishOnboarding() }
        binding.btnContinue.setOnClickListener {
            if (binding.viewPager.currentItem < items.size - 1) {
                binding.viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = onboardingAdapter
        onboardingAdapter.submitList(items)
        binding.viewPager.offscreenPageLimit = items.size
        binding.dotsIndicator.attachTo(binding.viewPager)


        (binding.viewPager.getChildAt(0) as? RecyclerView)?.let {
            it.setItemViewCacheSize(items.size)
        }


        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val isLastPage = position == items.size - 1

                binding.btnContinue.text = if (isLastPage) "Get Started" else "Continue"

                if (isLastPage) {
                    binding.tvSkip.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction { binding.tvSkip.visibility = View.GONE }
                        .start()
                } else {
                    binding.tvSkip.visibility = View.VISIBLE
                    binding.tvSkip.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }

                animateTextChange(items[position])
            }
        })
    }

    private fun animateTextChange(item: OnboardingItem) {
        binding.tvTitle.clearAnimation()
        binding.tvDescription.clearAnimation()

        binding.tvTitle.alpha = 0f
        binding.tvTitle.translationY = 40f

        binding.tvDescription.alpha = 0f
        binding.tvDescription.translationY = 40f

        binding.tvTitle.text = item.title
        binding.tvDescription.text = item.description

        binding.tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.tvDescription.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(100)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun finishOnboarding() {
        val proceed: () -> Unit = {
            getSharedPreferences("onboarding", MODE_PRIVATE).edit().putBoolean("finished", true)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        if (AdsResponse.isShowAdsURL) {
            InterstitialAdManager.showInterstitialDirect(this) {
                proceed()
            }
        } else {
            AppOpenAdManager.showAdOnSplash(this) {
                proceed()
            }
        }
    }
}