package io.novafoundation.nova.common.sequrity.biometry

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import io.novafoundation.nova.common.sequrity.BiometricService

interface BiometricServiceFactory {

    fun create(
        biometricManager: BiometricManager,
        biometricPromptFactory: BiometricPromptFactory,
        promptInfo: BiometricPrompt.PromptInfo
    ): BiometricService
}
