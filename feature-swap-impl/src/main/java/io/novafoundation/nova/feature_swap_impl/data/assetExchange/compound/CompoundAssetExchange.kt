package io.novafoundation.nova.feature_swap_impl.data.assetExchange.compound

import io.novafoundation.nova.common.utils.forEachAsync
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class CompoundAssetExchange(
    private val delegates: List<AssetExchange>
): AssetExchange {

    override suspend fun sync() {
       delegates.forEachAsync { it.sync() }
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
       return delegates.flatMap { it.availableDirectSwapConnections() }
    }

    override fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return delegates.map { it.runSubscriptions(metaAccount) }
            .mergeIfMultiple()
    }

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
       return delegates.all { it.canPayFeeInNonUtilityToken(chainAsset) }
    }
}
