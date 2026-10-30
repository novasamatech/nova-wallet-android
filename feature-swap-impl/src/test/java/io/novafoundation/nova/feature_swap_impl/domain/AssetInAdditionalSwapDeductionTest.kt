package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AssetInAdditionalSwapDeductionTest {

    private val registry = mock(AssetSourceRegistry::class.java)
    private val chains = mock(ChainRegistry::class.java)
    private val source = mock(AssetSource::class.java)
    private val balance = mock(AssetBalance::class.java)
    private val transfers = mock(AssetTransfers::class.java)
    private val input = mock(Chain.Asset::class.java)
    private val output = mock(Chain.Asset::class.java)
    private val useCase = RealAssetInAdditionalSwapDeductionUseCase(registry, chains)

    @Test
    fun `keep alive reserves runtime ED even when source can be reaped and output is sufficient`() = runBlocking {
        `when`(registry.sourceFor(input)).thenReturn(source)
        `when`(registry.sourceFor(output)).thenReturn(source)
        `when`(source.balance).thenReturn(balance)
        `when`(source.transfers).thenReturn(transfers)
        `when`(input.chainId).thenReturn("asset-hub")
        `when`(output.chainId).thenReturn("asset-hub")
        `when`(transfers.totalCanDropBelowMinimumBalance(input)).thenReturn(true)
        `when`(balance.isSelfSufficient(output)).thenReturn(true)
        val ed = BigInteger("100000000")
        `when`(balance.existentialDeposit(input)).thenReturn(ed)

        assertEquals(ed, useCase.invoke(input, output, keepAlive = true))
        // Other exchanges retain their existing reaping policy.
        assertEquals(BigInteger.ZERO, useCase.invoke(input, output))
    }
}
