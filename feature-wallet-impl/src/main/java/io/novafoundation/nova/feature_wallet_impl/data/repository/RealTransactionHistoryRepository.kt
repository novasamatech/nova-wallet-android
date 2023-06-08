package io.novafoundation.nova.feature_wallet_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationLocalToOperation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationToOperationLocalDb
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.hash.isPositive
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class RealTransactionHistoryRepository(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val operationDao: OperationDao,
    private val coinPriceRepository: CoinPriceRepository
) : TransactionHistoryRepository {

    override suspend fun syncOperationsFirstPage(
        pageSize: Int,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ) = withContext(Dispatchers.Default) {
        val historySource = historySourceFor(chainAsset)
        val accountAddress = chain.addressOf(accountId)

        val dataPage = historySource.getSafeOperations(pageSize, PageOffset.Loadable.FirstPage, filters, accountId, chain, chainAsset, currency)
        historySource.additionalFirstPageSync(chain, chainAsset, accountId, dataPage)

        val localOperations = dataPage.map { mapOperationToOperationLocalDb(it, chainAsset, OperationLocal.Source.REMOTE) }

        operationDao.insertFromRemote(accountAddress, chain.id, chainAsset.id, localOperations)
    }

    override suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): DataPage<Operation> = withContext(Dispatchers.Default) {
        val historySource = historySourceFor(chainAsset)

        historySource.getSafeOperations(
            pageSize = pageSize,
            pageOffset = pageOffset,
            filters = filters,
            accountId = accountId,
            chain = chain,
            chainAsset = chainAsset,
            currency = currency
        )
    }

    override suspend fun operationsFirstPageFlow(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): Flow<DataPage<Operation>> {
        val accountAddress = chain.addressOf(accountId)
        val historySource = historySourceFor(chainAsset)

        return operationDao.observe(accountAddress, chain.id, chainAsset.id)
            .transform { operations ->
                emit(mapOperations(operations, chainAsset, emptyList()))
                val coinPrices = try {
                    val fromTimestamp = operations.minOf { it.time }.milliseconds.inWholeSeconds
                    val toTimestamp = operations.maxOf { it.time }.milliseconds.inWholeSeconds
                    coinPriceRepository.getCoinPriceRange(chainAsset.priceId!!, currency, fromTimestamp, toTimestamp)
                } catch (e: Exception) {
                    emptyList()
                }
                emit(mapOperations(operations, chainAsset, coinPrices))
            }
            .mapLatest { operations ->
                val pageOffset = historySource.getSyncedPageOffset(accountId, chain, chainAsset)

                DataPage(pageOffset, operations)
            }
    }

    private fun mapOperations(operations: List<OperationLocal>, chainAsset: Chain.Asset, coinPrices: List<HistoricalCoinRate>): List<Operation> {
        return operations.map { operation ->
            val operationTimestamp = operation.time.milliseconds.inWholeSeconds
            val coinPrice = coinPriceRepository.findNearestCoinRate(coinPrices, operationTimestamp)
            mapOperationLocalToOperation(operation, chainAsset, coinPrice)
        }
    }

    private fun historySourceFor(chainAsset: Chain.Asset): AssetHistory = assetSourceRegistry.sourceFor(chainAsset).history

    private suspend fun AssetHistory.getSafeOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): DataPage<Operation> {
        val nonFiltered = getOperations(pageSize, pageOffset, filters, accountId, chain, chainAsset, currency)
        val filtered = nonFiltered.filter { it.isSafe() }

        return DataPage(nonFiltered.nextOffset, items = filtered)
    }

    private fun Operation.isSafe(): Boolean {
        val txType = type

        return if (txType is Operation.Type.Transfer) {
            txType.amount.isPositive()
        } else {
            true
        }
    }
}
