package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.data.model.CursorOrFull
import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.asCursorOrNull
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapNodeToOperation
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryHistoryRequest
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.Section
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.TransferHistoryApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.TransferHistoryApi.AssetType
import jp.co.soramitsu.fearless_utils.runtime.AccountId

abstract class SubstrateAssetHistory(
    private val subqueryApi: SubQueryOperationsApi,
    private val cursorStorage: TransferCursorStorage,
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
        chainAsset: Chain.Asset
    ): DataPage<Operation> {
        val substrateTransfersApi = chain.substrateTransfersApi()

        return when (substrateTransfersApi?.apiType) {
            Section.Type.SUBQUERY -> getOperationsSubQuery(
                pageSize = pageSize,
                pageOffset = pageOffset,
                filters = filters,
                accountId = accountId,
                apiUrl = substrateTransfersApi.url,
                chainAsset = chainAsset,
                chain = chain
            )
            else -> DataPage.empty()
        }
    }

    override suspend fun getSyncedPageOffset(accountId: AccountId, chain: Chain, chainAsset: Chain.Asset): PageOffset {
        val substrateTransfersApi = chain.substrateTransfersApi()

        return when (substrateTransfersApi?.apiType) {
            Section.Type.SUBQUERY -> {
                val cursor = cursorStorage.awaitCursor(chain.id, chainAsset.id, accountId)

                PageOffset.CursorOrFull(cursor)
            }

            else -> PageOffset.FullData
        }
    }

    private suspend fun getOperationsSubQuery(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
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

        val response = subqueryApi.getOperationsHistory(apiUrl, request).data.query

        val pageInfo = response.historyElements.pageInfo
        val operations = response.historyElements.nodes.map { mapNodeToOperation(it, chainAsset) }
        val newPageOffset = PageOffset.CursorOrFull(pageInfo.endCursor)

        return DataPage(newPageOffset, operations)
    }

    private fun Chain.substrateTransfersApi(): TransferHistoryApi? {
        return externalApi?.history?.firstOrNull {
            it.assetType == AssetType.SUBSTRATE
        }
    }
}
