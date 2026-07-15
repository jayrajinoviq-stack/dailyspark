package com.dailyspark.mobile.ui.activity

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ProgressBar
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.dailyspark.mobile.R
import com.dailyspark.mobile.ads.AdsResponse
import com.dailyspark.mobile.ads.AppOpenAdManager
import com.dailyspark.mobile.databinding.ActivityMainBinding
import com.dailyspark.mobile.util.InAppUpdateHelper
import com.google.android.play.core.install.model.AppUpdateType

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var inAppUpdateHelper: InAppUpdateHelper
    private var adLoadingDialog: AlertDialog? = null

    companion object {
        private var hasShownAppOpenAdOnEntry = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        inAppUpdateHelper = InAppUpdateHelper(this)
        inAppUpdateHelper.register()
        inAppUpdateHelper.checkForUpdate(AppUpdateType.FLEXIBLE)
        setupBottomNavigation()

        maybeShowAppOpenAdOnEntry()
    }

    private fun maybeShowAppOpenAdOnEntry() {

        if (AdsResponse.isShowAdsURL) return
        if (hasShownAppOpenAdOnEntry) return
        hasShownAppOpenAdOnEntry = true

        AppOpenAdManager.showAdOnMain(
            activity = this,
            onAdLoadStart = { showAdLoadingDialog() },
            onComplete = { dismissAdLoadingDialog() }
        )
    }

    private fun showAdLoadingDialog() {
        if (isFinishing || isDestroyed) return

        adLoadingDialog = AlertDialog.Builder(this)
            .setView(ProgressBar(this).apply {
                isIndeterminate = true
            })
            .setCancelable(false)
            .create()

        adLoadingDialog?.show()

        val size = (120 * resources.displayMetrics.density).toInt()

        adLoadingDialog?.window?.setLayout(size, size)
    }

    private fun dismissAdLoadingDialog() {
        adLoadingDialog?.dismiss()
        adLoadingDialog = null
    }

    private fun setupBottomNavigation() {

        binding.navHome.setOnClickListener {
            navigateTo(R.id.homeFragment)
        }

        binding.navExplore.setOnClickListener {
            navigateTo(R.id.exploreFragment)
        }

        binding.navSaved.setOnClickListener {
            navigateTo(R.id.savedFragment)
        }

        binding.navProfile.setOnClickListener {
            navigateTo(R.id.profileFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateSelection(destination.id)
        }
    }

    private fun navigateTo(destination: Int) {

        if (navController.currentDestination?.id == destination)
            return

        navController.navigate(
            destination, null,
            NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    false,
                    true
                )
                .build()
        )
    }


    private fun updateSelection(id: Int) {

        resetAll()

        when (id) {
            R.id.homeFragment -> {
                binding.imgHome.setImageResource(R.drawable.home_selected_img)
                binding.imgHome.imageTintList =
                    ColorStateList.valueOf(getColor(R.color.accent))
                binding.txtHome.setTextColor(getColor(R.color.accent))
            }

            R.id.exploreFragment -> {
                binding.imgExplore.setImageResource(R.drawable.compass_selected)
                binding.imgExplore.imageTintList =
                    ColorStateList.valueOf(getColor(R.color.accent))
                binding.txtExplore.setTextColor(getColor(R.color.accent))
            }

            R.id.savedFragment -> {
                binding.imgSaved.setImageResource(R.drawable.heart_selected)
                binding.imgSaved.imageTintList =
                    ColorStateList.valueOf(getColor(R.color.accent))
                binding.txtSaved.setTextColor(getColor(R.color.accent))
            }

            R.id.profileFragment -> {
                binding.imgProfile.setImageResource(R.drawable.profile_selected)
                binding.imgProfile.imageTintList =
                    ColorStateList.valueOf(getColor(R.color.accent))
                binding.txtProfile.setTextColor(getColor(R.color.accent))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateHelper.checkResumeState()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAdLoadingDialog()
        inAppUpdateHelper.checkResumeState()
    }

    private fun resetAll() {

        val color = getColor(R.color.text_muted)

        binding.imgHome.apply {
            setImageResource(R.drawable.home_img)
            imageTintList = ColorStateList.valueOf(color)
        }

        binding.imgExplore.apply {
            setImageResource(R.drawable.compass)
            imageTintList = ColorStateList.valueOf(color)
        }

        binding.imgSaved.apply {
            setImageResource(R.drawable.heart)
            imageTintList = ColorStateList.valueOf(color)
        }

        binding.imgProfile.apply {
            setImageResource(R.drawable.profile)
            imageTintList = ColorStateList.valueOf(color)
        }

        binding.txtHome.setTextColor(color)
        binding.txtExplore.setTextColor(color)
        binding.txtSaved.setTextColor(color)
        binding.txtProfile.setTextColor(color)
    }

}