package com.dailyspark.mobile.util

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateHelper(private val activity: Activity) {

    companion object {
        private const val TAG = "InAppUpdateHelper"
        private const val DAYS_FOR_FLEXIBLE_UPDATE = 0
    }

    val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private lateinit var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "Flexible update downloaded")
                showRestartSnackbar()
            }
            InstallStatus.FAILED -> {
                Log.e(TAG, "Update FAILED: ${state.installErrorCode()}")
            }
            else -> Log.d(TAG, "installStatus = ${state.installStatus()}")
        }
    }


    fun registerLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateResultLauncher = launcher
    }

    fun register() = appUpdateManager.registerListener(listener)
    fun unregister() = appUpdateManager.unregisterListener(listener)

    fun checkForUpdate(type: Int = AppUpdateType.FLEXIBLE) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            Log.d(TAG, "updateAvailability=${info.updateAvailability()} " +
                    "isImmediateAllowed=${info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)} " +
                    "isFlexibleAllowed=${info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)} " +
                    "availableVersionCode=${info.availableVersionCode()}")

            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (info.isUpdateTypeAllowed(type)) {
                        startUpdate(info, type)
                    } else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        startUpdate(info, AppUpdateType.FLEXIBLE)
                    } else if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        startUpdate(info, AppUpdateType.IMMEDIATE)
                    }
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // resume a stalled immediate update
                    startUpdate(info, AppUpdateType.IMMEDIATE)
                }
                else -> Log.d(TAG, "No update available")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to fetch update info: ${it.message}")
        }
    }

    private fun startUpdate(info: com.google.android.play.core.appupdate.AppUpdateInfo, type: Int) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                type,
                activity,
                1001
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "startUpdateFlowForResult failed: ${e.message}")
        }
    }

    fun checkResumeState() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar()
            }
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    private fun showRestartSnackbar() {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            "An update has been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("RESTART") {
            appUpdateManager.completeUpdate()
        }.show()
    }
}