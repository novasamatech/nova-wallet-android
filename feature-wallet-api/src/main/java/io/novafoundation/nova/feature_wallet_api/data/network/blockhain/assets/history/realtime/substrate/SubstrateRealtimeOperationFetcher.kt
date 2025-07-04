package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Extractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Factory
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface SubstrateRealtimeOperationFetcher {

    suspend fun extractRealtimeHistoryUpdates(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
    ): List<RealtimeHistoryUpdate>

    interface Extractor {

        suspend fun extractRealtimeHistoryUpdates(
            extrinsicVisit: ExtrinsicVisit,
            chain: Chain,
            chainAsset: Chain.Asset
        ): RealtimeHistoryUpdate.Type?
    }

    interface Factory {

        sealed class Source {

            class FromExtractor(val extractor: Extractor) : Source()

            class Known(val id: Id) : Source() {

                enum class Id {
                    ASSET_CONVERSION_SWAP, HYDRA_DX_SWAP
                }
            }
        }

        fun create(sources: List<Source>): SubstrateRealtimeOperationFetcher
    }
}

fun Extractor.asSource(): Factory.Source {
    return Factory.Source.FromExtractor(this)
}

fun Factory.Source.Known.Id.asSource(): Factory.Source {
    return Factory.Source.Known(this)
}
