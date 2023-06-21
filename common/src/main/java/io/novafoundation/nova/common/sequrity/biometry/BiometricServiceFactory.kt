package io.novafoundation.nova.common.sequrity.biometry

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt

interface BiometricServiceFactory {

    fun create(
        biometricManager: BiometricManager,
        biometricPromptFactory: BiometricPromptFactory,
        promptInfo: BiometricPrompt.PromptInfo
    ): BiometricService
}
