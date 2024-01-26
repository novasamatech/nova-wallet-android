package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.equilibrium

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.eqBalances
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher.Factory.Source
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.asSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.SubstrateAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call

class EquilibriumAssetHistory(
    private val chainRegistry: ChainRegistry,
    walletOperationsApi: SubQueryOperationsApi,
    cursorStorage: TransferCursorStorage,
    coinPriceRepository: CoinPriceRepository,
    realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory
) : SubstrateAssetHistory(walletOperationsApi, cursorStorage, realtimeOperationFetcherFactory, coinPriceRepository) {

    override fun realtimeFetcherSources(chain: Chain): List<Source> {
        return listOf(TransferExtractor().asSource())
    }

    override fun availableOperationFilters(chain: Chain, asset: Chain.Asset): Set<TransactionFilter> {
        return setOfNotNull(
            TransactionFilter.TRANSFER,
            TransactionFilter.EXTRINSIC.takeIf { asset.isUtilityAsset }
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
                recipientId = bindAccountIdentifier(call.arguments["to"]),
                amountInPlanks = amount,
                chainAsset = chainAsset,
            )
        }

        private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot): Boolean {
            val balances = runtime.metadata.eqBalances()

            return instanceOf(balances.call("transfer"))
        }
    }
}
