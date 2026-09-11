package com.lucifer.hamrahyar.ui.theme.utils

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.lucifer.hamrahyar.ui.theme.data.local.LocalSixDigitPasswordManager

class OrderSecurityManager(private val context: Context) {
    private val pinManager = LocalSixDigitPasswordManager(context)

    fun startVerification(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onPinRequired: (isSetup: Boolean) -> Unit,
        onCancel: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (DeviceSecurityAuthenticator.canAuthenticate(context)) {
            DeviceSecurityAuthenticator.authenticate(
                activity = activity,
                onSuccess = onSuccess,
                onError = onError,
                onCancel = onCancel
            )
        } else {
            // Check App Pin
            if (pinManager.isPinSet()) {
                onPinRequired(false) // Verification
            } else {
                onPinRequired(true) // Setup
            }
        }
    }
}
