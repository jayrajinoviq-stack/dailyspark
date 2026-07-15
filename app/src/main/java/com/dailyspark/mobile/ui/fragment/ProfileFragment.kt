package com.dailyspark.mobile.ui.fragment

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dailyspark.mobile.BuildConfig
import com.dailyspark.mobile.R
import com.dailyspark.mobile.data.database.AppDatabase
import com.dailyspark.mobile.databinding.DialogTimePickerBinding
import com.dailyspark.mobile.databinding.FragmentProfileBinding
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.repository.StatsRepository
import com.dailyspark.mobile.repository.StreakRepository
import com.dailyspark.mobile.service.ApiService
import com.dailyspark.mobile.ui.dialog.RateUsDialog
import com.dailyspark.mobile.utils.ReminderHelper
import com.dailyspark.mobile.utils.ThemeManager
import com.dailyspark.mobile.viewmodel.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            handleReminderToggle(true)
        } else {
            binding.switchReminder.isChecked = false
            handleReminderToggle(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showSettingsDialog()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        displayStats()
        setupThemeSwitch()
        setupReminderUI()
        setupClick()
    }

    private fun setupClick() {
        binding.rateUs.setOnClickListener {
            RateUsDialog().show(parentFragmentManager, RateUsDialog.TAG)
        }
        binding.appVersion.text = "Daily Spark v${BuildConfig.VERSION_NAME}"
        binding.shareApp.setOnClickListener {
            shareApp()
        }
        binding.privacyPolicy.setOnClickListener {

            openUrl("https://yashbhalala.github.io/dailyspark-legal/privacy-policy.html")
        }
        binding.termsConditions.setOnClickListener {
            openUrl("https://yashbhalala.github.io/dailyspark-legal/terms-of-use.html")
        }
    }


    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "No browser found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun shareApp() {
        val appLink = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

        val shareMessage = """
        ✨ Daily Spark
        
        Stay motivated every day with inspiring quotes, daily streaks, and positive thoughts.
        
        Download Daily Spark:
        $appLink
    """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Share Daily Spark"
            )
        )
    }


    private fun setupReminderUI() {
        val isEnabled = ReminderHelper.isEnabled(requireContext())
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val finalState = isEnabled && hasPermission
        binding.switchReminder.isChecked = finalState
        updateReminderTimeUI(finalState)

        binding.switchReminder.setOnClickListener {
            val checked = binding.switchReminder.isChecked
            if (checked) {
                checkAndRequestNotificationPermission()
            } else {
                handleReminderToggle(false)
            }
        }

        binding.layoutReminderTime.setOnClickListener {
            if (binding.switchReminder.isChecked) {
                showTimePickerDialog()
            }
        }
    }


    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    handleReminderToggle(true)
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showRationaleDialog()
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            handleReminderToggle(true)
        }
    }

    private fun handleReminderToggle(enabled: Boolean) {

        ReminderHelper.setReminderEnabled(
            requireContext(),
            enabled
        )

        updateReminderTimeUI(enabled)
    }

    private fun updateReminderTimeUI(enabled: Boolean) {
        binding.layoutReminderTime.alpha = if (enabled) 1.0f else 0.4f

        val h = ReminderHelper.getHour(requireContext())
        val m = ReminderHelper.getMin(requireContext())
        val amPm = if (h < 12) "AM" else "PM"
        val hour12 = if (h % 12 == 0) 12 else h % 12

        binding.tvReminderTimeValue.text = String.format("%d:%02d %s", hour12, m, amPm)
    }

    private fun showTimePickerDialog() {

        val dialogBinding = DialogTimePickerBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())

        dialog.setOnShowListener {

            dialog.window?.let { window ->

                window.navigationBarColor =
                    ContextCompat.getColor(requireContext(), R.color.card)

                val isDark =
                    (resources.configuration.uiMode
                            and Configuration.UI_MODE_NIGHT_MASK) ==
                            Configuration.UI_MODE_NIGHT_YES

                WindowCompat.getInsetsController(
                    window,
                    window.decorView
                )?.isAppearanceLightNavigationBars = !isDark
            }

            dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )?.let { sheet ->

                sheet.setBackgroundColor(Color.TRANSPARENT)

                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isDraggable = true
                }
            }
        }

        val savedHour24 = ReminderHelper.getHour(requireContext())
        val savedMin = ReminderHelper.getMin(requireContext())

        val hour12 = if (savedHour24 % 12 == 0) 12 else savedHour24 % 12
        val amPmIndex = if (savedHour24 < 12) 0 else 1

        dialogBinding.npHour.apply {
            minValue = 1
            maxValue = 12
            value = hour12
        }

        dialogBinding.npMin.apply {
            minValue = 0
            maxValue = 59
            value = savedMin
            setFormatter { i -> String.format("%02d", i) }
        }

        val amPmValues = arrayOf("AM", "PM")

        dialogBinding.npAmPm.apply {
            minValue = 0
            maxValue = amPmValues.size - 1
            displayedValues = amPmValues
            value = amPmIndex
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        dialogBinding.btnDone.setOnClickListener {

            val selectedHour12 = dialogBinding.npHour.value
            val selectedMin = dialogBinding.npMin.value
            val isPm = dialogBinding.npAmPm.value == 1

            val hour24 = when {
                isPm && selectedHour12 < 12 -> selectedHour12 + 12
                !isPm && selectedHour12 == 12 -> 0
                else -> selectedHour12
            }

            ReminderHelper.saveTime(
                requireContext(),
                hour24,
                selectedMin
            )

            updateReminderTimeUI(true)
            dialog.dismiss()
        }

        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun showRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Daily Reminders")
            .setMessage("We need notification permission to send you your daily quotes and help maintain your streak.")
            .setPositiveButton("Allow") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Not Now") { _, _ ->
                binding.switchReminder.isChecked = false
            }
            .show()
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permission Required")
            .setMessage("Notification permission is permanently denied. Please enable it in Settings to receive reminders.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchReminder.isChecked = false
            }
            .show()
    }

    private fun setupThemeSwitch() {
        binding.switchDark.isChecked = ThemeManager.isDarkMode(requireContext())
        binding.switchDark.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(requireContext(), isChecked)
            requireActivity().recreate()
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val apiService = Retrofit.Builder()
            .baseUrl("https://hifdlykomariwzphnfop.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val quoteRepo = QuoteRepository(apiService, database.quoteDao(), requireContext())
        val streakRepo = StreakRepository(requireContext())
        val statsRepo = StatsRepository.getInstance(requireContext())

        val factory = ProfileViewModel.Factory(quoteRepo, streakRepo, statsRepo)
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    private fun displayStats() {
        binding.totalStreak.text = viewModel.getStreakCount().toString()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.totalSavedCount.collect { count ->
                        binding.totalSaved.text = count.toString()
                    }
                }
                launch {
                    viewModel.totalSharedCount.collect { count ->
                        binding.totalShared.text = count.toString()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}