package io.novafoundation.nova.feature_swap_impl.presentation.common.route

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById

interface SwapRouteFormatter {

    suspend fun formatSwapRoute(quote: SwapQuote): SwapRouteModel?
}

class RealSwapRouteFormatter(
    private val chainRegistry: ChainRegistry
) : SwapRouteFormatter {

    override suspend fun formatSwapRoute(quote: SwapQuote): SwapRouteModel? {
        val routeChainIds = determinePathChains(quote.quotedPath.path) ?: return null

        val allKnownChains = chainRegistry.chainsById()
        val chainModels = routeChainIds.map { mapChainToUi(allKnownChains[it]!!) }

        return SwapRouteModel(chainModels)
    }

    private fun determinePathChains(path: Path<QuotedEdge<SwapGraphEdge>>): List<ChainId>? {
        // Do not display path of less then 2 elements
        if (path.size < 2) return null

        val firstEdge = path.first().edge
        val firstChain = firstEdge.to.chainId

        var currentChainId = firstChain
        val foundChains = mutableListOf(currentChainId)

        path.forEach {
            val nextChainId = it.edge.to.chainId
            if (nextChainId != currentChainId) {
                currentChainId = nextChainId
                foundChains.add(nextChainId)
            }
        }

        return foundChains
    }
}
