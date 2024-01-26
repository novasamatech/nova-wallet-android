package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Extractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Factory
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.api.walkToList
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository

internal class SubstrateRealtimeOperationFetcherFactory(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val eventsRepository: EventsRepository,
    private val extrinsicWalk: ExtrinsicWalk,
) : Factory {

    override fun create(sources: List<Factory.Source>): SubstrateRealtimeOperationFetcher {
        val extractors = sources.map { it.extractor() }

        return RealSubstrateRealtimeOperationFetcher(eventsRepository, extractors, extrinsicWalk)
    }

    private fun Factory.Source.extractor(): Extractor {
        return when (this) {
            is Factory.Source.FromExtractor -> extractor
            is Factory.Source.Known -> id.extractor()
        }
    }

    private fun Factory.Source.Known.Id.extractor(): Extractor {
        return when (this) {
            Factory.Source.Known.Id.ASSET_CONVERSION_SWAP -> assetConversionSwap()
            Factory.Source.Known.Id.HYDRA_DX_SWAP -> hydraDxOmniPoolSwap()
        }
    }

    private fun assetConversionSwap(): Extractor {
        return AssetConversionSwapExtractor(multiLocationConverterFactory)
    }

    private fun hydraDxOmniPoolSwap(): Extractor {
        return HydraDxOmniPoolSwapExtractor(hydraDxAssetIdConverter)
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
        val extrinsics = repository.getExtrinsicsWithEvents(chain.id, blockHash)

        return extrinsics.flatMap { extrinsic ->
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
