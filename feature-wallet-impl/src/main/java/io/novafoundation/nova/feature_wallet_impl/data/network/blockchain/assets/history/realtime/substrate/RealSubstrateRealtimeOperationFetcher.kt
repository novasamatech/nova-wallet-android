package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Extractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Factory
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate.hydraDx.HydraDxOmniPoolSwapExtractor
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate.hydraDx.HydraDxRouterSwapExtractor
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.walkToList
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository

internal class SubstrateRealtimeOperationFetcherFactory(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val eventsRepository: EventsRepository,
    private val extrinsicWalk: ExtrinsicWalk,
) : Factory {

    override fun create(sources: List<Factory.Source>): SubstrateRealtimeOperationFetcher {
        val extractors = sources.flatMap { it.extractors() }

        return RealSubstrateRealtimeOperationFetcher(eventsRepository, extractors, extrinsicWalk)
    }

    private fun Factory.Source.extractors(): List<Extractor> {
        return when (this) {
            is Factory.Source.FromExtractor -> listOf(extractor)
            is Factory.Source.Known -> id.extractors()
        }
    }

    private fun Factory.Source.Known.Id.extractors(): List<Extractor> {
        return when (this) {
            Factory.Source.Known.Id.ASSET_CONVERSION_SWAP -> listOf(assetConversionSwap())
            Factory.Source.Known.Id.HYDRA_DX_SWAP -> listOf(hydraDxOmniPoolSwap(), hydraDxRouterSwap())
        }
    }

    private fun assetConversionSwap(): Extractor {
        return AssetConversionSwapExtractor(multiLocationConverterFactory)
    }

    private fun hydraDxOmniPoolSwap(): Extractor {
        return HydraDxOmniPoolSwapExtractor(hydraDxAssetIdConverter)
    }

    private fun hydraDxRouterSwap(): Extractor {
        return HydraDxRouterSwapExtractor(hydraDxAssetIdConverter)
    }
}

private class RealSubstrateRealtimeOperationFetcher(
    private val repository: EventsRepository,
    private val extractors: List<Extractor>,
    private val callWalk: ExtrinsicWalk,
) : SubstrateRealtimeOperationFetcher {

    override suspend fun extractRealtimeHistoryUpdates(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String
    ): List<RealtimeHistoryUpdate> {
        val extrinsicWithEvents = repository.getBlockEvents(chain.id, blockHash).applyExtrinsic

        return extrinsicWithEvents.flatMap { extrinsic ->
            val visits = runCatching { callWalk.walkToList(extrinsic, chain.id) }.getOrElse { emptyList() }

            visits.flatMap { extrinsicVisit ->
                extractors.mapNotNull {
                    val type = runCatching { it.extractRealtimeHistoryUpdates(extrinsicVisit, chain, chainAsset) }.getOrNull() ?: return@mapNotNull null

                    RealtimeHistoryUpdate(
                        txHash = extrinsic.extrinsicHash,
                        status = Operation.Status.fromSuccess(extrinsicVisit.success),
                        type = type
                    )
                }
            }
        }
    }
}
