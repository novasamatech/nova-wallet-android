package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.utility

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.oneOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Factory.Source
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.asSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.SubstrateAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.hydraDxSupported
import io.novafoundation.nova.runtime.ext.isSwapSupported
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.metadata.callOrNull

class NativeAssetHistory(
    private val chainRegistry: ChainRegistry,
    realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory,
    walletOperationsApi: SubQueryOperationsApi,
    cursorStorage: TransferCursorStorage,
    coinPriceRepository: CoinPriceRepository
) : SubstrateAssetHistory(walletOperationsApi, cursorStorage, realtimeOperationFetcherFactory, coinPriceRepository) {

    override fun realtimeFetcherSources(chain: Chain): List<Source> {
        return buildList {
            add(TransferExtractor().asSource())

            if (chain.swap.assetConversionSupported()) {
                Source.Known.Id.ASSET_CONVERSION_SWAP.asSource()
            }

            if (chain.swap.hydraDxSupported()) {
                add(Source.Known.Id.HYDRA_DX_SWAP.asSource())
            }
        }
    }

    override fun availableOperationFilters(chain: Chain, asset: Chain.Asset): Set<TransactionFilter> {
        return setOfNotNull(
            TransactionFilter.TRANSFER,
            TransactionFilter.EXTRINSIC,
            TransactionFilter.REWARD,
            TransactionFilter.SWAP.takeIf { chain.isSwapSupported() }
        )
    }

    private inner class TransferExtractor : SubstrateRealtimeOperationFetcher.Extractor {

        override suspend fun extractRealtimeHistoryUpdates(
            extrinsicVisit: ExtrinsicVisit,
            chain: Chain,
            chainAsset: Chain.Asset
        ): RealtimeHistoryUpdate.Type? {
            val runtime = chainRegistry.getRuntime(chain.id)

            val call = extrinsicVisit.call
            if (!call.isTransfer(runtime)) return null

            val amount = bindNumber(call.arguments["value"])

            return RealtimeHistoryUpdate.Type.Transfer(
                senderId = extrinsicVisit.origin,
                recipientId = bindAccountIdentifier(call.arguments["dest"]),
                amountInPlanks = amount,
                chainAsset = chainAsset,
            )
        }

        private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot): Boolean {
            val balances = runtime.metadata.balances()

            return oneOf(
                balances.callOrNull("transfer"),
                balances.callOrNull("transfer_keep_alive"),
                balances.callOrNull("transfer_allow_death")
            )
        }
    }
}
