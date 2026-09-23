package io.novafoundation.nova.feature_swap_impl.data.history

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.feature_swap_api.domain.model.HydrationSystemAccounts
import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class RealSwapTransferFilterFactoryTest {

    private val commission = NovaSwapCommission()
    private val hydrationAccounts = HydrationSystemAccounts()
    private val factory = RealSwapTransferFilterFactory(commission, hydrationAccounts)

    @Test
    fun `asset conversion commission transfer is hidden`() {
        val chain = chainWith(Chain.Swap.ASSET_CONVERSION, chainId = Chain.Geneses.POLKADOT_ASSET_HUB)
        val filter = requireNotNull(factory.create(chain))
        val feeAccountId = requireNotNull(commission.assetHubFeeAccountId(chain.id))

        assertFalse(filter.shouldInclude(transfer(chain, receiver = chain.addressOf(feeAccountId))))
    }

    @Test
    fun `regular asset conversion transfer stays visible`() {
        val chain = chainWith(Chain.Swap.ASSET_CONVERSION, chainId = Chain.Geneses.POLKADOT_ASSET_HUB)
        val filter = requireNotNull(factory.create(chain))

        assertTrue(filter.shouldInclude(transfer(chain, receiver = chain.addressOf(ByteArray(32) { 2 }))))
    }

    @Test
    fun `hydration router transfer is still hidden`() {
        val chain = chainWith(Chain.Swap.HYDRA_DX)
        val filter = requireNotNull(factory.create(chain))

        assertFalse(filter.shouldInclude(transfer(chain, receiver = chain.addressOf(hydrationAccounts.routerAccountId))))
    }

    @Test
    fun `chain without supported swap has no filter`() {
        assertNull(factory.create(chainWith()))
    }

    @Test
    fun `asset conversion chain without configured beneficiary has no filter`() {
        assertNull(factory.create(chainWith(Chain.Swap.ASSET_CONVERSION)))
    }

    private fun transfer(chain: Chain, receiver: String): Operation {
        val sender = chain.addressOf(ByteArray(32) { 1 })

        return Operation(
            id = "operation",
            address = sender,
            type = Operation.Type.Transfer(
                myAddress = sender,
                amount = BigInteger.ONE,
                fiatAmount = null,
                receiver = receiver,
                sender = sender,
                fee = null
            ),
            time = 0,
            chainAsset = chain.assets.first(),
            extrinsicHash = null,
            status = Operation.Status.COMPLETED
        )
    }

    private fun chainWith(vararg swaps: Chain.Swap, chainId: String = "chain"): Chain {
        val asset = Chain.Asset(
            icon = null,
            id = 0,
            priceId = null,
            chainId = chainId,
            symbol = TokenSymbol("UNIT"),
            precision = 12.asPrecision(),
            buyProviders = emptyMap(),
            sellProviders = emptyMap(),
            staking = emptyList(),
            type = Chain.Asset.Type.Native,
            source = Chain.Asset.Source.DEFAULT,
            name = "Unit"
        )

        return Chain(
            id = chainId,
            name = "Chain",
            assets = listOf(asset),
            nodes = Chain.Nodes(
                autoBalanceStrategy = Chain.Nodes.AutoBalanceStrategy.ROUND_ROBIN,
                wssNodeSelectionStrategy = Chain.Nodes.NodeSelectionStrategy.AutoBalance,
                nodes = emptyList()
            ),
            explorers = emptyList(),
            externalApis = emptyList(),
            icon = null,
            addressPrefix = 42,
            legacyAddressPrefix = null,
            types = null,
            isEthereumBased = false,
            isTestNet = false,
            source = Chain.Source.DEFAULT,
            hasSubstrateRuntime = true,
            pushSupport = false,
            hasCrowdloans = false,
            supportProxy = false,
            governance = emptyList(),
            swap = swaps.toList(),
            customFee = emptyList(),
            multisigSupport = false,
            connectionState = Chain.ConnectionState.FULL_SYNC,
            parentId = null,
            additional = null
        )
    }
}
