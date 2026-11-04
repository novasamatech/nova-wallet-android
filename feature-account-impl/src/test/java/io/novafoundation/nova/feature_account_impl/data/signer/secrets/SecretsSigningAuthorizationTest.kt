package io.novafoundation.nova.feature_account_impl.data.signer.secrets

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationResult
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class SecretsSigningAuthorizationTest {

    private val verification = mock(TwoFactorVerificationService::class.java)

    private val authorization = SecretsSigningAuthorization(verification)

    @Test
    fun `two signatures use one approval and a third asks again`() = runTest {
        `when`(verification.requestConfirmationIfEnabled()).thenReturn(TwoFactorVerificationResult.CONFIRMED)
        authorization.preAuthorize(2)
        authorization.authorizeSignature()
        authorization.authorizeSignature()
        verify(verification, times(1)).requestConfirmationIfEnabled()
        authorization.authorizeSignature()
        verify(verification, times(2)).requestConfirmationIfEnabled()
    }

    @Test
    fun `cancelled preauthorization grants no signatures`() = runTest {
        `when`(verification.requestConfirmationIfEnabled()).thenReturn(TwoFactorVerificationResult.CANCELED)
        assertTrue(runCatching { authorization.preAuthorize(2) }.exceptionOrNull() is SigningCancelledException)
        assertTrue(runCatching { authorization.authorizeSignature() }.exceptionOrNull() is SigningCancelledException)
        verify(verification, times(2)).requestConfirmationIfEnabled()
    }

    @Test
    fun `clearing an interrupted sequence revokes unused approval`() = runTest {
        `when`(verification.requestConfirmationIfEnabled()).thenReturn(TwoFactorVerificationResult.CONFIRMED)
        authorization.preAuthorize(2)
        authorization.authorizeSignature()
        authorization.clear()
        authorization.authorizeSignature()
        verify(verification, times(2)).requestConfirmationIfEnabled()
    }
}
