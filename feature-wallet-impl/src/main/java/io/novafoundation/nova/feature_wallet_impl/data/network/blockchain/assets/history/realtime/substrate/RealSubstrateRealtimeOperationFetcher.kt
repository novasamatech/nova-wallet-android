package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Extractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Factory
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.status


internal class SubstrateRealtimeOperationFetcherFactory(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val eventsRepository: EventsRepository
): Factory {

    override fun create(sources: List<Factory.Source>): SubstrateRealtimeOperationFetcher {
        val extractors = sources.map { it.extractor() }

        return RealSubstrateRealtimeOperationFetcher(eventsRepository, extractors)
    }

    private fun Factory.Source.extractor(): Extractor {
        return when(this) {
            is Factory.Source.FromExtractor -> extractor
            is Factory.Source.Known -> id.extractor()
        }
    }

    private fun Factory.Source.Known.Id.extractor(): Extractor {
        return when(this) {
            Factory.Source.Known.Id.ASSET_CONVERSION_SWAP -> assetConversionSwap()
        }
    }

    private fun assetConversionSwap(): Extractor {
        return AssetConversionSwapExtractor(multiLocationConverterFactory)
    }
}

private class RealSubstrateRealtimeOperationFetcher(
    private val repository: EventsRepository,
    private val extractors: List<Extractor>
): SubstrateRealtimeOperationFetcher {

    override suspend fun extractRealtimeHistoryUpdates(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String
    ): List<RealtimeHistoryUpdate> {
        val extrinsics = repository.getExtrinsicsWithEvents(chain.id, blockHash)

        return extrinsics.flatMap { extrinsic ->
            extractors.mapNotNull {
                val type = runCatching { it.extractRealtimeHistoryUpdates(extrinsic, chain, chainAsset) }.getOrNull() ?: return@mapNotNull null

                RealtimeHistoryUpdate(
                    txHash = extrinsic.extrinsicHash,
                    status = extrinsic.operationStatus(),
                    type = type
                )
            }
        }
    }

    private fun ExtrinsicWithEvents.operationStatus(): Operation.Status {
        return when(status()) {
            ExtrinsicStatus.SUCCESS -> Operation.Status.COMPLETED
            ExtrinsicStatus.FAILURE -> Operation.Status.FAILED
            null -> Operation.Status.PENDING
        }
    }
}
