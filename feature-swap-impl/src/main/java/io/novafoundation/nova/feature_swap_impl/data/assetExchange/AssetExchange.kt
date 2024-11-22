package io.novafoundation.nova.feature_swap_impl.data.assetExchange

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProvider
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface AssetExchange {

    interface SingleChainFactory {

        suspend fun create(chain: Chain, swapHost: SwapHost): AssetExchange
    }

    interface MultiChainFactory {

        suspend fun create(swapHost: SwapHost): AssetExchange
    }

    interface SwapHost {

        val scope: CoroutineScope

        suspend fun quote(quoteArgs: ParentQuoterArgs): Balance

        suspend fun extrinsicService(): ExtrinsicService
    }

    suspend fun sync()

    suspend fun availableDirectSwapConnections(): List<SwapGraphEdge>

    fun feePaymentOverrides(): List<FeePaymentProviderOverride>

    fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger>
}

data class FeePaymentProviderOverride(
    val provider: FeePaymentProvider,
    val chain: Chain
)

data class ParentQuoterArgs(
    val chainAssetIn: Chain.Asset,
    val chainAssetOut: Chain.Asset,
    val amount: Balance,
    val swapDirection: SwapDirection,
)
