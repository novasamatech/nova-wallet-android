package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.data.model.CursorOrFull
import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.asCursorOrNull
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
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

abstract class SubstrateAssetHistory(
    private val subqueryApi: SubQueryOperationsApi,
    private val cursorStorage: TransferCursorStorage,
    coinPriceRepository: CoinPriceRepository
) : BaseAssetHistory(coinPriceRepository) {

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
        val earliestOperationTimestamp = getEarliestOperationTimestamp(subqueryResponse.historyElements)
        val latestOperationTimestamp = getLatestOperationTimestamp(subqueryResponse.historyElements)
        val coinPriceRange = getCoinPriceRange(chainAsset, currency, earliestOperationTimestamp, latestOperationTimestamp)

        val operations = subqueryResponse.historyElements.nodes.map { node ->
            val coinRate = coinPriceRepository.findNearestCoinRate(coinPriceRange, node.timestamp)
            mapNodeToOperation(node, coinRate, chainAsset)
        }

        val pageInfo = subqueryResponse.historyElements.pageInfo
        val newPageOffset = PageOffset.CursorOrFull(pageInfo.endCursor)

        return DataPage(newPageOffset, operations)
    }

    private fun getEarliestOperationTimestamp(operations: SubqueryHistoryElementResponse.Query.HistoryElements): Long? {
        return operations.nodes.minOfOrNull { it.timestamp }
    }

    private fun getLatestOperationTimestamp(operations: SubqueryHistoryElementResponse.Query.HistoryElements): Long? {
        return operations.nodes.maxOfOrNull { it.timestamp }
    }

    private fun Chain.substrateTransfersApi(): Chain.ExternalApi.Transfers.Substrate? {
        return externalApi()
    }
}
