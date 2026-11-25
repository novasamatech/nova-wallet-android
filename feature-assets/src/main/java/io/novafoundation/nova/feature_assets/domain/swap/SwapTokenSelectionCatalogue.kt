package io.novafoundation.nova.feature_assets.domain.swap

import io.novafoundation.nova.common.utils.applyFilter
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_swap_api.domain.exchanger.catalogue.ExchangerASwapPair
import io.novafoundation.nova.feature_swap_api.domain.exchanger.catalogue.ExchangerATaoCatalogue
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.enabledChainsFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class SwapTokenSelectionCatalogue(
    private val chainRegistry: ChainRegistry,
    private val swapService: SwapService,
    private val exchangerATaoCatalogue: ExchangerATaoCatalogue,
    private val filterFactory: SwapTokenSelectionFilterFactory
) {

    internal fun observe(opposite: FullChainAssetId?, selectingPay: Boolean, metaAccountId: Long, scope: CoroutineScope): Flow<SwapSelectionTokens> {
        val onChainAssets = flowOfAll { swapService.assetsAvailableForSwap(scope) }
        val onChainCompatible = flowOfAll {
            if (opposite == null) flowOf(emptySet()) else swapService.availableAssetsForSelection(opposite, selectingPay, metaAccountId, scope)
        }

        return combine(selectableChains(), onChainAssets, onChainCompatible, exchangerPairs()) { chains, assets, compatible, pairs ->
            val configurations = chains.flatMap { it.assets }
            val enabledIds = configurations.mapTo(mutableSetOf()) { it.fullId }
            val candidateIds = assets.toMutableSet()
            val compatibleIds = compatible.toMutableSet()
            for (pair in pairs) {
                val pay = pair.assetIn.fullId
                val receive = pair.assetOut.fullId
                if (pay !in enabledIds || receive !in enabledIds) continue

                val candidate = if (selectingPay) pay else receive
                candidateIds += candidate
                val otherSide = if (selectingPay) receive else pay
                if (opposite == otherSide) compatibleIds += candidate
            }

            SwapSelectionTokens(chains, configurations.filter { it.fullId in candidateIds }, compatibleIds)
        }.distinctUntilChanged()
    }

    suspend fun canSwap(
        pay: FullChainAssetId,
        receive: FullChainAssetId,
        metaAccountId: Long,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Default) {
        val destinations = swapService.availableSwapDirectionsFor(chainRegistry.asset(pay), scope, metaAccountId).first()
        if (receive in destinations) return@withContext true
        if (!exchangerATaoCatalogue.supportsPair(pay, receive)) return@withContext false

        val chains = selectableChains().first()
        chains.any { it.id == pay.chainId } && chains.any { it.id == receive.chainId }
    }

    private fun selectableChains() = chainRegistry.enabledChainsFlow().map { it.applyFilter(filterFactory.createChainFilter(it)) }

    private fun exchangerPairs(): Flow<List<ExchangerASwapPair>> = flow { emit(exchangerATaoCatalogue.availablePairs()) }
        .catch { error ->
            if (error is CancellationException || error !is Exception) throw error
            // Hide optional provider routes when the service is unavailable.
            emit(emptyList())
        }
        .onStart { emit(emptyList()) }
}

internal data class SwapSelectionTokens(val chains: List<Chain>, val assets: List<Chain.Asset>, val compatible: Set<FullChainAssetId>)
