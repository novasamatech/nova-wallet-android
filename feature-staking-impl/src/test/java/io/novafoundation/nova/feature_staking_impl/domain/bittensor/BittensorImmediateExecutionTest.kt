package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions

class BittensorImmediateExecutionTest {

    private val wallet = mock(MetaAccount::class.java)
    private val signer = mock(NovaSigner::class.java)
    private val signerProvider = mock(SignerProvider::class.java)
    private val capability = BittensorImmediateExecution(signerProvider)

    @Test
    fun `immediate multisig is supported using its explicit root signer`() = runBlocking {
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(signerProvider.rootSignerFor(wallet)).thenReturn(signer)
        `when`(signer.callExecutionType()).thenReturn(CallExecutionType.IMMEDIATE)

        assertTrue(capability.isSupported(wallet))

        verify(signerProvider).rootSignerFor(wallet)
        verify(signer).callExecutionType()
        verifyNoMoreInteractions(signerProvider, signer)
    }

    @Test
    fun `delayed multisig is not supported`() = runBlocking {
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(signerProvider.rootSignerFor(wallet)).thenReturn(signer)
        `when`(signer.callExecutionType()).thenReturn(CallExecutionType.DELAYED)

        assertEquals(BittensorWalletSupport.DELAYED_EXECUTION, capability.support(wallet))
        assertTrue(runCatching { capability.requireSupported(wallet) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `proxied wallet is refused by type even behind an immediate proxy`() = runBlocking {
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.PROXIED)
        `when`(signerProvider.rootSignerFor(wallet)).thenReturn(signer)
        `when`(signer.callExecutionType()).thenReturn(CallExecutionType.IMMEDIATE)

        assertEquals(BittensorWalletSupport.PROXIED_NOT_SUPPORTED, capability.support(wallet))
        assertTrue(runCatching { capability.requireSupported(wallet) }.exceptionOrNull() is IllegalArgumentException)
        verifyNoInteractions(signerProvider)
    }

    @Test
    fun `generic and legacy ledger wallets are refused by type`() = runBlocking {
        for (type in listOf(LightMetaAccount.Type.LEDGER, LightMetaAccount.Type.LEDGER_LEGACY)) {
            `when`(wallet.type).thenReturn(type)

            assertEquals(BittensorWalletSupport.LEDGER_NOT_SUPPORTED, capability.support(wallet))
        }
        verifyNoInteractions(signerProvider)
    }

    @Test
    fun `secrets, vault, parity signer and watch-only wallets pass without a signer lookup`() = runBlocking {
        val types = listOf(
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.POLKADOT_VAULT,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.WATCH_ONLY
        )
        for (type in types) {
            `when`(wallet.type).thenReturn(type)

            assertEquals(BittensorWalletSupport.SUPPORTED, capability.support(wallet))
        }
        verifyNoInteractions(signerProvider)
    }

    @Test
    fun `execution gate rechecks capability instead of retaining preview result`() = runBlocking {
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(signerProvider.rootSignerFor(wallet)).thenReturn(signer)
        `when`(signer.callExecutionType()).thenReturn(CallExecutionType.IMMEDIATE, CallExecutionType.DELAYED)

        assertTrue(capability.isSupported(wallet))
        assertTrue(runCatching { capability.requireSupported(wallet) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `immediate execution gate does not request signing or bypass signer permissions`() = runBlocking {
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(signerProvider.rootSignerFor(wallet)).thenReturn(signer)
        `when`(signer.callExecutionType()).thenReturn(CallExecutionType.IMMEDIATE)

        capability.requireSupported(wallet)

        verify(signerProvider).rootSignerFor(wallet)
        verify(signer).callExecutionType()
        verifyNoMoreInteractions(signerProvider, signer)
    }

    @Test
    fun `unavailable signer fails closed`() = runBlocking {
        val failure = IllegalStateException("Signatory is unavailable")
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(signerProvider.rootSignerFor(wallet)).thenThrow(failure)

        assertSame(failure, runCatching { capability.requireSupported(wallet) }.exceptionOrNull())
    }

    @Test
    fun `cancellation is not converted to a capability verdict`() = runBlocking {
        val cancellation = CancellationException("Cancelled")
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(signerProvider.rootSignerFor(wallet)).thenReturn(signer)
        `when`(signer.callExecutionType()).thenThrow(cancellation)

        assertSame(cancellation, runCatching { capability.isSupported(wallet) }.exceptionOrNull())
    }
}
