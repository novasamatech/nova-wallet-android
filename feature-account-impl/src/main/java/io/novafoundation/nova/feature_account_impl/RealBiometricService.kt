package io.novafoundation.nova.feature_account_impl

import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import io.novafoundation.nova.common.sequrity.BiometricResponse
import io.novafoundation.nova.common.sequrity.BiometricService
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.common.sequrity.biometry.BiometricPromptFactory
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import kotlinx.coroutines.flow.Flow

class RealBiometricServiceFactory(
    private val accountRepository: AccountRepository
) : BiometricServiceFactory {
    override fun create(
        biometricManager: BiometricManager,
        biometricPromptFactory: BiometricPromptFactory,
        promptInfo: BiometricPrompt.PromptInfo
    ): BiometricService {
        return RealBiometricService(
            accountRepository,
            biometricManager,
            biometricPromptFactory,
            promptInfo
        )
    }
}

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
        enableBiometry(!isEnabled())
    }

    override fun cancel() {
        biometricPrompt.cancelAuthentication()
    }

    override fun enableBiometry(enable: Boolean) {
        if (!isBiometricReady()) {
            biometryServiceResponseFlow.tryEmit(BiometricResponse.NotReady)
            return
        }

        if (enable) {
            accountRepository.setBiometricOn()
        } else {
            accountRepository.setBiometricOff()
        }
    }

    override fun refreshBiometryState() {
        if (!isBiometricReady() && isEnabled()) {
            accountRepository.setBiometricOff()
        }
    }

    override fun requestBiometric() {
        if (!isBiometricReady()) {
            biometryServiceResponseFlow.tryEmit(BiometricResponse.NotReady)
            return
        }

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
