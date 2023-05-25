package io.novafoundation.nova.common.sequrity

import android.os.Parcelable
import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Parcelize
enum class TwoFactorVerificationResult : Parcelable {
    CONFIRMED, CANCELED
}

interface TwoFactorVerificationService {

    fun isEnabled(): Boolean

    fun isEnabledFlow(): Flow<Boolean>

    suspend fun toggle()

    suspend fun requestConfirmationIfEnabled(): TwoFactorVerificationResult

    suspend fun requestConfirmation(useBiometry: Boolean): TwoFactorVerificationResult
}

interface TwoFactorVerificationExecutor {

    fun cancel()

    fun confirm()

    suspend fun runConfirmation(useBiometry: Boolean): TwoFactorVerificationResult
}

class RealTwoFactorVerificationService(
    private val preferences: Preferences,
    private val twoFactorVerificationExecutor: TwoFactorVerificationExecutor
) : TwoFactorVerificationService {

    companion object {
        private const val PREF_TWO_FACTOR_CONFIRMATION_STATE = "two_factor_confirmation_statE"
    }

    private val state = MutableStateFlow(isEnabled())

    override fun isEnabled(): Boolean {
        return preferences.getBoolean(PREF_TWO_FACTOR_CONFIRMATION_STATE, false)
    }

    override fun isEnabledFlow(): Flow<Boolean> = state

    override suspend fun toggle() {
        val isEnabled = isEnabled()

        if (isEnabled) {
            val confirmationResult = requestConfirmationIfEnabled()
            if (confirmationResult != TwoFactorVerificationResult.CONFIRMED) {
                return
            }
        }

        setEnable(!isEnabled)
    }

    override suspend fun requestConfirmationIfEnabled(): TwoFactorVerificationResult {
        if (isEnabled()) {
            return requestConfirmation(true)
        }

        return TwoFactorVerificationResult.CONFIRMED
    }

    override suspend fun requestConfirmation(useBiometry: Boolean): TwoFactorVerificationResult {
        return twoFactorVerificationExecutor.runConfirmation(useBiometry)
    }

    private fun setEnable(enable: Boolean) {
        preferences.putBoolean(PREF_TWO_FACTOR_CONFIRMATION_STATE, enable)
        state.value = enable
    }
}
