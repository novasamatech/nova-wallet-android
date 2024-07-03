package io.novafoundation.nova.feature_swap_impl.data.assetExchange

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface AssetExchange {

    interface Factory {

        suspend fun create(
            chain: Chain,
            parentQuoter: ParentQuoter,
            coroutineScope: CoroutineScope
        ): AssetExchange?
    }

    interface ParentQuoter {

        suspend fun quote(quoteArgs: ParentQuoterArgs): Balance
    }

    /**
     * Implementations should expect `asset` to be non-utility asset,
     * e.g. they don't need to additionally check whether asset is utility or not
     * They can also expect this method is called only when asset is present in [AssetExchange.availableDirectSwapConnections]
     */
    suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean

    suspend fun availableDirectSwapConnections(): List<SwapGraphEdge>

    suspend fun slippageConfig(): SlippageConfig

    fun runSubscriptions(chain: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger>
}

data class ParentQuoterArgs(
    val chainAssetIn: Chain.Asset,
    val chainAssetOut: Chain.Asset,
    val amount: Balance,
    val swapDirection: SwapDirection,
)


