package io.novafoundation.nova.feature_account_impl.presentation.pincode.fingerprint

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import java.util.concurrent.Executor

class BiometricPromptFactory(private val fragment: Fragment, private val executor: Executor) {

    fun create(
        callback: BiometricPrompt.AuthenticationCallback
    ): BiometricPrompt {
        return BiometricPrompt(fragment, executor, callback)
    }
}
