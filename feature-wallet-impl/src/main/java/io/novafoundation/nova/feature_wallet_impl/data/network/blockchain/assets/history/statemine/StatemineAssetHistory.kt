package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
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
import io.novafoundation.nova.runtime.ext.isSwapSupported
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.hasSameId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.module

class StatemineAssetHistory(
    private val chainRegistry: ChainRegistry,
    realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory,
    walletOperationsApi: SubQueryOperationsApi,
    cursorStorage: TransferCursorStorage,
    coinPriceRepository: CoinPriceRepository
) : SubstrateAssetHistory(walletOperationsApi, cursorStorage, realtimeOperationFetcherFactory, coinPriceRepository) {

    override fun realtimeFetcherSources(): List<Source> {
        return listOf(
            TransferExtractor().asSource(),
            Source.Known.Id.ASSET_CONVERSION_SWAP.asSource()
        )
    }

    override fun availableOperationFilters(chain: Chain, asset: Chain.Asset): Set<TransactionFilter> {
        return setOfNotNull(
            TransactionFilter.TRANSFER,
            TransactionFilter.EXTRINSIC.takeIf { asset.isUtilityAsset },
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
            if (!call.isTransfer(runtime, chainAsset)) return null

            val amount = bindNumber(call.arguments["amount"])

            return RealtimeHistoryUpdate.Type.Transfer(
                senderId = extrinsicVisit.origin,
                recipientId = bindAccountIdentifier(call.arguments["target"]),
                amountInPlanks = amount,
                chainAsset = chainAsset,
            )
        }

        private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot, chainAsset: Chain.Asset): Boolean {
            val statemineType = chainAsset.requireStatemine()
            val moduleName = statemineType.palletNameOrDefault()
            val module = runtime.metadata.module(moduleName)

            val matchingCall = oneOf(
                module.call("transfer"),
                module.call("transfer_keep_alive"),
            )

            return matchingCall && statemineType.hasSameId(runtime, dynamicInstanceId = arguments["id"])
        }
    }
}
