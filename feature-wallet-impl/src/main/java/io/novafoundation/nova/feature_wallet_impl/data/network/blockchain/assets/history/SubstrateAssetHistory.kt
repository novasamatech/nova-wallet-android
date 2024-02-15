package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.data.model.CursorOrFull
import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.asCursorOrNull
import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.convertPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.findNearestCoinRate
import io.novafoundation.nova.feature_wallet_impl.data.network.model.AssetsBySubQueryId
import io.novafoundation.nova.feature_wallet_impl.data.network.model.assetsBySubQueryId
import io.novafoundation.nova.feature_wallet_impl.data.network.model.request.SubqueryHistoryRequest
import io.novafoundation.nova.feature_wallet_impl.data.network.model.response.SubqueryHistoryElementResponse
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlin.time.Duration.Companion.seconds

abstract class SubstrateAssetHistory(
    private val subqueryApi: SubQueryOperationsApi,
    private val cursorStorage: TransferCursorStorage,
    private val realtimeOperationFetcherFactory: SubstrateRealtimeOperationFetcher.Factory,
    coinPriceRepository: CoinPriceRepository
) : BaseAssetHistory(coinPriceRepository) {

    abstract fun realtimeFetcherSources(chain: Chain): List<SubstrateRealtimeOperationFetcher.Factory.Source>

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId
    ): List<RealtimeHistoryUpdate> {
        val sources = realtimeFetcherSources(chain)
        val realtimeOperationFetcher = realtimeOperationFetcherFactory.create(sources)

        return realtimeOperationFetcher.extractRealtimeHistoryUpdates(chain, chainAsset, blockHash)
    }

    override suspend fun additionalFirstPageSync(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        pageResult: Result<DataPage<Operation>>
    ) {
        pageResult
            .onSuccess { page ->
                val newCursor = page.nextOffset.asCursorOrNull()?.value
                cursorStorage.saveCursor(chain.id, chainAsset.id, accountId, newCursor)
            }
            .onFailure {
                // Empty cursor means we haven't yet synced any data for this asset
                // However we still want to store null cursor on failure to show items
                // that came not from the remote (e.g. local pending operations)
                if (!cursorStorage.hasCursor(chain.id, chainAsset.id, accountId)) {
                    cursorStorage.saveCursor(chain.id, chainAsset.id, accountId, cursor = null)
                }
            }
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

    override fun isOperationSafe(operation: Operation): Boolean {
        return true
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
            asset = chainAsset,
            chain = chain
        )

        val subqueryResponse = subqueryApi.getOperationsHistory(apiUrl, request).data.query
        val earliestOperationTimestamp = getEarliestOperationTimestamp(subqueryResponse.historyElements)
        val latestOperationTimestamp = getLatestOperationTimestamp(subqueryResponse.historyElements)
        val coinPriceRange = getCoinPriceRange(chainAsset, currency, earliestOperationTimestamp, latestOperationTimestamp)

        val assetsBySubQueryId = chain.assetsBySubQueryId()

        val operations = subqueryResponse.historyElements.nodes.mapNotNull { node ->
            val coinRate = coinPriceRange.findNearestCoinRate(node.timestamp)
            mapNodeToOperation(node, coinRate, chainAsset, assetsBySubQueryId)
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

    private fun mapNodeToOperation(
        node: SubqueryHistoryElementResponse.Query.HistoryElements.Node,
        coinRate: CoinRate?,
        chainAsset: Chain.Asset,
        chainAssetsBySubQueryId: AssetsBySubQueryId
    ): Operation? {
        val type: Operation.Type
        val status: Operation.Status

        when {
            node.reward != null -> with(node.reward) {
                val planks = amount?.toBigIntegerOrNull().orZero()

                type = Operation.Type.Reward(
                    amount = planks,
                    fiatAmount = coinRate?.convertPlanks(chainAsset, planks),
                    isReward = isReward,
                    kind = Operation.Type.Reward.RewardKind.Direct(
                        era = era,
                        validator = validator.nullIfEmpty(),
                    ),
                    eventId = eventId(node.blockNumber, node.reward.eventIdx)
                )
                status = Operation.Status.COMPLETED
            }

            node.poolReward != null -> with(node.poolReward) {
                type = Operation.Type.Reward(
                    amount = amount,
                    fiatAmount = coinRate?.convertPlanks(chainAsset, amount),
                    isReward = isReward,
                    kind = Operation.Type.Reward.RewardKind.Pool(
                        poolId = poolId
                    ),
                    eventId = eventId(node.blockNumber, node.poolReward.eventIdx)
                )
                status = Operation.Status.COMPLETED
            }

            node.extrinsic != null -> with(node.extrinsic) {
                type = Operation.Type.Extrinsic(
                    content = Operation.Type.Extrinsic.Content.SubstrateCall(module, call),
                    fee = fee,
                    fiatFee = coinRate?.convertPlanks(chainAsset, fee),
                )
                status = Operation.Status.fromSuccess(success)
            }

            node.transfer != null -> with(node.transfer) {
                type = Operation.Type.Transfer(
                    myAddress = node.address,
                    amount = amount,
                    fiatAmount = coinRate?.convertPlanks(chainAsset, amount),
                    receiver = to,
                    sender = from,
                    fee = fee,
                )
                status = Operation.Status.fromSuccess(success)
            }

            node.assetTransfer != null -> with(node.assetTransfer) {
                type = Operation.Type.Transfer(
                    myAddress = node.address,
                    amount = amount,
                    fiatAmount = coinRate?.convertPlanks(chainAsset, amount),
                    receiver = to,
                    sender = from,
                    fee = fee,
                )
                status = Operation.Status.fromSuccess(success)
            }

            node.swap != null -> with(node.swap) {
                val assetIn = chainAssetsBySubQueryId[assetIdIn] ?: return null
                val assetOut = chainAssetsBySubQueryId[assetIdOut] ?: return null
                val assetFee = chainAssetsBySubQueryId[assetIdFee] ?: return null

                val amount = if (assetIn.fullId == chainAsset.fullId) amountIn else amountOut

                type = Operation.Type.Swap(
                    fee = ChainAssetWithAmount(
                        chainAsset = assetFee,
                        amount = fee,
                    ),
                    amountIn = ChainAssetWithAmount(
                        chainAsset = assetIn,
                        amount = amountIn,
                    ),
                    amountOut = ChainAssetWithAmount(
                        chainAsset = assetOut,
                        amount = amountOut,
                    ),
                    fiatAmount = coinRate?.convertPlanks(chainAsset, amount)
                )
                status = Operation.Status.fromSuccess(success ?: true)
            }

            else -> return null
        }

        return Operation(
            id = node.id,
            address = node.address,
            type = type,
            time = node.timestamp.seconds.inWholeMilliseconds,
            chainAsset = chainAsset,
            extrinsicHash = node.extrinsicHash,
            status = status
        )
    }

    private fun eventId(blockNumber: Long, eventIdx: Int): String {
        return "$blockNumber-$eventIdx"
    }
}
