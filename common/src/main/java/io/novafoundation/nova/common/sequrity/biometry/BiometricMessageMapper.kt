package io.novafoundation.nova.common.sequrity.biometry

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.resources.ResourceManager

fun mapBiometricErrors(resourceManager: ResourceManager, biometricResponse: BiometricResponse): String? {
    return when (biometricResponse) {
        is BiometricResponse.Fail -> resourceManager.getString(R.string.pincode_biometric_error)
        is BiometricResponse.Error -> if (!biometricResponse.cancelledByUser) biometricResponse.message else null
        else -> null
    }
}
