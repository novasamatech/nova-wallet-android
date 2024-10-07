package io.novafoundation.nova.feature_account_impl.data.fee.types.hydra

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.updaters.BlockNumberUpdater
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn

class HydraDxQuoteSharedComputation(
    private val computationalCache: ComputationalCache,
    private val quotingFactory: HydraDxQuoting.Factory,
    private val pathQuoterFactory: PathQuoter.Factory,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val blockNumberUpdater: BlockNumberUpdater
) {

    suspend fun getQuoter(
        chain: Chain,
        accountId: AccountId,
        scope: CoroutineScope
    ): PathQuoter<QuotableEdge> {
        val key = "HydraDxQuoter:${chain.id}:${accountId.toHexString()}"

        return computationalCache.useCache(key, scope) {
            val assetConversion = getSwapQuoting(chain, accountId, scope)
            val edges = assetConversion.availableSwapDirections()
            val graph = Graph.create(edges)

            pathQuoterFactory.create(graph, scope)
        }
    }


    suspend fun getSwapQuoting(
        chain: Chain,
        accountId: AccountId,
        scope: CoroutineScope
    ): SwapQuoting {
        val key = "HydraDxAssetConversion:${chain.id}:${accountId.toHexString()}"

        return computationalCache.useCache(key, scope) {
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)
            val hydraDxQuoting = quotingFactory.create(chain)

            // Required at least for stable swap
            blockNumberUpdater.listenForUpdates(subscriptionBuilder, chain)
                .launchIn(this)

            hydraDxQuoting.sync()
            hydraDxQuoting.runSubscriptions(accountId, subscriptionBuilder)
                .launchIn(this)

            subscriptionBuilder.subscribe(this)

            hydraDxQuoting
        }
    }
}
