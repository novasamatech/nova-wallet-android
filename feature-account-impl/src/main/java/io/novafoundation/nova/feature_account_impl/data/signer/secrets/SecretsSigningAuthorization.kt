package io.novafoundation.nova.feature_account_impl.data.signer.secrets

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationResult
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import java.util.concurrent.atomic.AtomicInteger

class SecretsSigningAuthorization(private val verificationService: TwoFactorVerificationService) {

    private val remaining = AtomicInteger(0)

    suspend fun preAuthorize(count: Int) {
        require(count in 1..2)
        clear()
        verify()
        remaining.set(count)
    }

    suspend fun authorizeSignature() {
        if (remaining.getAndUpdate { it.coerceAtLeast(1) - 1 } == 0) verify()
    }

    fun clear() {
        remaining.set(0)
    }

    private suspend fun verify() {
        if (verificationService.requestConfirmationIfEnabled() != TwoFactorVerificationResult.CONFIRMED) {
            throw SigningCancelledException()
        }
    }
}
