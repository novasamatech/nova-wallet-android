package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class HasChainAccountTest {

    @Mock
    lateinit var chain: Chain

    @Mock
    lateinit var metaAccount: MetaAccount

    @Test
    fun `should return true for substrate chain`() {
        chainHasId("1")
        metaAccountHasChainAccounts(emptySet())
        assertTrue(metaAccount.hasAccountIn(chain))

        metaAccountHasChainAccounts(setOf("1", "2"))
        assertTrue(metaAccount.hasAccountIn(chain))
    }

    @Test
    fun `should return true for ethereum chain with ethereum keypair`() {
        chainHasId("1")
        chainIsEthereumBased(true)
        metaAccountHasEthereumPublicKey(byteArrayOf())
        metaAccountHasChainAccounts(emptySet())

        assertTrue(metaAccount.hasAccountIn(chain))
    }

    @Test
    fun `should return false for ethereum chain with no ethereum keypair`() {
        chainHasId("1")
        chainIsEthereumBased(true)
        metaAccountHasEthereumPublicKey(null)
        metaAccountHasChainAccounts(emptySet())

        assertFalse(metaAccount.hasAccountIn(chain))
    }

    @Test
    fun `should return true for ethereum chain with no ethereum keypair but with chain account`() {
        chainHasId("1")
        chainIsEthereumBased(true)
        metaAccountHasEthereumPublicKey(null)
        metaAccountHasChainAccounts(setOf("1"))

        assertTrue(metaAccount.hasAccountIn(chain))
    }

    private fun chainHasId(id: String) = `when`(chain.id).thenReturn(id)
    private fun chainIsEthereumBased(ethereumBased: Boolean) = `when`(chain.isEthereumBased).thenReturn(ethereumBased)

    private fun metaAccountHasChainAccounts(chainIds: Set<String>) = `when`(metaAccount.chainAccounts).thenReturn(
        chainIds.associateWith { Mockito.mock(MetaAccount.ChainAccount::class.java) }
    )

    private fun metaAccountHasEthereumPublicKey(publicKey: ByteArray?) = `when`(metaAccount.ethereumPublicKey).thenReturn(publicKey)
}
