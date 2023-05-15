package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.data.model.CursorOrFull
import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.asCursorOrNull
import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceDataSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapNodeToOperation
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryHistoryRequest
import io.novafoundation.nova.feature_wallet_impl.data.network.model.response.SubqueryHistoryElementResponse
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

abstract class SubstrateAssetHistory(
    private val subqueryApi: SubQueryOperationsApi,
    private val cursorStorage: TransferCursorStorage,
    protected val coinPriceDataSource: CoinPriceDataSource
) : AssetHistory {

    override suspend fun additionalFirstPageSync(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        page: DataPage<Operation>
    ) {
        val newCursor = page.nextOffset.asCursorOrNull()?.value
        cursorStorage.saveCursor(chain.id, chainAsset.id, accountId, newCursor)
    }

    override suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency,
    ): DataPage<Operation> {
        val substrateTransfersApi = chain.substrateTransfersApi()

        return if (substrateTransfersApi != null) {
            getOperationsInternal(
                pageSize = pageSize,
                pageOffset = pageOffset,
                filters = filters,
                accountId = accountId,
                apiUrl = substrateTransfersApi.url,
                chainAsset = chainAsset,
                chain = chain,
                currency = currency
            )
        } else {
            DataPage.empty()
        }
    }

    override suspend fun getSyncedPageOffset(accountId: AccountId, chain: Chain, chainAsset: Chain.Asset): PageOffset {
        val substrateTransfersApi = chain.substrateTransfersApi()

        return if (substrateTransfersApi != null) {
            val cursor = cursorStorage.awaitCursor(chain.id, chainAsset.id, accountId)

            PageOffset.CursorOrFull(cursor)
        } else {
            PageOffset.FullData
        }
    }

    private suspend fun getOperationsInternal(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency,
        apiUrl: String
    ): DataPage<Operation> {
        val cursor = when (pageOffset) {
            is PageOffset.Loadable.Cursor -> pageOffset.value
            PageOffset.Loadable.FirstPage -> null
            else -> error("SubQuery requires cursor pagination")
        }

        val request = SubqueryHistoryRequest(
            accountAddress = chain.addressOf(accountId),
            pageSize = pageSize,
            cursor = cursor,
            filters = filters,
            assetType = chainAsset.type
        )

        val subqueryResponse = subqueryApi.getOperationsHistory(apiUrl, request).data.query
        val latestOperationTimestamp = getLatestOperationTimestamp(subqueryResponse.historyElements)
        val earliestOperationTimestamp = getEarliestOperationTimestamp(subqueryResponse.historyElements)
        val coinPriceRange = getCoinPriceRange(chainAsset, currency, latestOperationTimestamp, earliestOperationTimestamp)

        var startThreshold = 0
        val operations = subqueryResponse.historyElements.nodes.map { node ->
            startThreshold = coinPriceRange.binarySearchFloor(fromIndex = startThreshold) {
                val timestamp = it.millis.milliseconds.inWholeSeconds
                timestamp.compareTo(node.timestamp)
            }
            val coinRate = coinPriceRange[startThreshold]
            mapNodeToOperation(node, coinRate, chainAsset)
        }

        val pageInfo = subqueryResponse.historyElements.pageInfo
        val newPageOffset = PageOffset.CursorOrFull(pageInfo.endCursor)

        return DataPage(newPageOffset, operations)
    }

    private suspend fun getCoinPriceRange(
        chainAsset: Chain.Asset,
        currency: Currency,
        from: Long,
        to: Long
    ): List<HistoricalCoinRate> {
        val coinPriceRange = chainAsset.priceId?.let { coinPriceDataSource.getCoinPriceRange(it, currency, from, to) }
        return coinPriceRange ?: emptyList()
    }

    private fun getEarliestOperationTimestamp(operations: SubqueryHistoryElementResponse.Query.HistoryElements): Long {
        val timestamp = operations.nodes.minOfOrNull { it.timestamp } ?: 0L
        return timestamp.seconds
            .minus(1.hours)
            .inWholeMilliseconds
    }

    private fun getLatestOperationTimestamp(operations: SubqueryHistoryElementResponse.Query.HistoryElements): Long {
        val timestamp = operations.nodes.maxOfOrNull { it.timestamp } ?: 0L
        return timestamp.seconds
            .plus(1.hours)
            .inWholeMilliseconds
    }

    private fun Chain.substrateTransfersApi(): Chain.ExternalApi.Transfers.Substrate? {
        return externalApi()
    }
}
