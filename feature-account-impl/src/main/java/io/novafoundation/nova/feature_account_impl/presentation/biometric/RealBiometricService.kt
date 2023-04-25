package io.novafoundation.nova.feature_account_impl.presentation.biometric

import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import io.novafoundation.nova.common.sequrity.BiometricResponse
import io.novafoundation.nova.common.sequrity.BiometricService
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.presentation.pincode.fingerprint.BiometricPromptFactory
import kotlinx.coroutines.flow.Flow

class RealBiometricService(
    private val accountRepository: AccountRepository,
    private val biometricManager: BiometricManager,
    private val biometricPromptFactory: BiometricPromptFactory,
    private val promptInfo: BiometricPrompt.PromptInfo
) : BiometricPrompt.AuthenticationCallback(), BiometricService {

    override val biometryServiceResponseFlow = singleReplaySharedFlow<BiometricResponse>()

    private val biometricPrompt = biometricPromptFactory.create(this)

    override fun isEnabled(): Boolean {
        return accountRepository.isBiometricEnabled()
    }

    override fun isEnabledFlow(): Flow<Boolean> = accountRepository.isBiometricEnabledFlow()

    override suspend fun toggle() {
        enableBiometry(isEnabled())
    }

    override fun cancel() {
        biometricPrompt.cancelAuthentication()
    }

    override fun enableBiometry(enable: Boolean) {
        if (enable) {
            accountRepository.setBiometricOn()
        } else {
            accountRepository.setBiometricOff()
        }
    }

    override fun requestBiometric() {
        if (!isBiometricReady()) return

        biometricPrompt.authenticate(promptInfo)
    }

    override fun isBiometricReady(): Boolean {
        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        val cancelledByUser = errorCode == BiometricConstants.ERROR_CANCELED ||
            errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON ||
            errorCode == BiometricConstants.ERROR_USER_CANCELED

        biometryServiceResponseFlow.tryEmit(BiometricResponse.Error(cancelledByUser, errString.toString()))
    }

    override fun onAuthenticationFailed() {
        biometryServiceResponseFlow.tryEmit(BiometricResponse.Fail)
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        biometryServiceResponseFlow.tryEmit(BiometricResponse.Success)
    }
}
