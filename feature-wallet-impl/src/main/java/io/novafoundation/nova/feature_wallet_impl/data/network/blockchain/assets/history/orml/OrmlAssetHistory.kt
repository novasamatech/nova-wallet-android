package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.orml

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.currenciesOrNull
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.asSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.SubstrateAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.findAssetByOrmlCurrencyId
import io.novafoundation.nova.runtime.ext.isSwapSupported
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.callOrNull

class OrmlAssetHistory(
    private val chainRegistry: ChainRegistry,
    realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory,
    walletOperationsApi: SubQueryOperationsApi,
    cursorStorage: TransferCursorStorage,
    coinPriceRepository: CoinPriceRepository
) : SubstrateAssetHistory(walletOperationsApi, cursorStorage, realtimeOperationFetcherFactory, coinPriceRepository) {

    override fun realtimeFetcherSources(): List<SubstrateRealtimeOperationFetcher.Factory.Source> {
        return listOf(TransferExtractor().asSource())
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
            extrinsic: ExtrinsicWithEvents,
            chain: Chain,
            chainAsset: Chain.Asset
        ): RealtimeHistoryUpdate.Type? {
            val runtime = chainRegistry.getRuntime(chain.id)

            val call = extrinsic.extrinsic.call
            if (!call.isTransfer(runtime)) return null

            val inferredAsset = chain.findAssetByOrmlCurrencyId(runtime, call.arguments["currency_id"]) ?: return null
            val amount = bindNumber(call.arguments["amount"])

            return RealtimeHistoryUpdate.Type.Transfer(
                senderId = bindAccountIdentifier(extrinsic.extrinsic.signature!!.accountIdentifier),
                recipientId = bindAccountIdentifier(call.arguments["dest"]),
                amountInPlanks = amount,
                chainAsset = inferredAsset,
            )
        }

        private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot): Boolean {
            val transferCall = runtime.metadata.currenciesOrNull()?.callOrNull("transfer")
                ?: runtime.metadata.tokens().call("transfer")

            return instanceOf(transferCall)
        }
    }
}
