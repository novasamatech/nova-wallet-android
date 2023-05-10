package io.novafoundation.nova.feature_account_impl.presentation.biometric

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.BiometricResponse
import io.novafoundation.nova.feature_account_impl.R

fun mapBiometricErrors(resourceManager: ResourceManager, biometricResponse: BiometricResponse): String? {
    return when (biometricResponse) {
        is BiometricResponse.Fail -> resourceManager.getString(R.string.pincode_biometric_error)
        is BiometricResponse.Error -> if (!biometricResponse.cancelledByUser) biometricResponse.message else null
        else -> null
    }
}
