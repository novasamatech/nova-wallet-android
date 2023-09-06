package io.novafoundation.nova.feature_assets.data.repository

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.common.utils.applyFilters
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.feature_assets.data.mappers.mapOperationLocalToOperation
import io.novafoundation.nova.feature_assets.data.mappers.mapOperationToOperationLocalDb
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.poolRewardAccountFilter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.findNearestCoinRate
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

interface TransactionHistoryRepository {

    suspend fun syncOperationsFirstPage(
        pageSize: Int,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    )

    suspend fun getOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): DataPage<Operation>

    suspend fun operationsFirstPageFlow(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): Flow<DataPage<Operation>>
}

class RealTransactionHistoryRepository(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val operationDao: OperationDao,
    private val coinPriceRepository: CoinPriceRepository,
    private val poolAccountDerivation: PoolAccountDerivation,
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

        val dataPage = historySource.getFilteredOperations(pageSize, PageOffset.Loadable.FirstPage, filters, accountId, chain, chainAsset, currency)
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

        historySource.getFilteredOperations(
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
                emit(mapOperations(operations, chainAsset, coinPrices = emptyList()))
                val coinPrices = runCatching {
                    val fromTimestamp = operations.minOf { it.time }.milliseconds.inWholeSeconds
                    val toTimestamp = operations.maxOf { it.time }.milliseconds.inWholeSeconds
                    coinPriceRepository.getCoinPriceRange(chainAsset.priceId!!, currency, fromTimestamp, toTimestamp)
                }.getOrElse { emptyList() }
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
            val coinPrice = coinPrices.findNearestCoinRate(operationTimestamp)
            mapOperationLocalToOperation(operation, chainAsset, coinPrice)
        }
    }

    private fun historySourceFor(chainAsset: Chain.Asset): AssetHistory = assetSourceRegistry.sourceFor(chainAsset).history

    private suspend fun AssetHistory.getFilteredOperations(
        pageSize: Int,
        pageOffset: PageOffset.Loadable,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
        currency: Currency
    ): DataPage<Operation> {
        val nonFiltered = getOperations(pageSize, pageOffset, filters, accountId, chain, chainAsset, currency)

        val pageFilters = createTransactionFilters(chain)
        val filtered = nonFiltered.applyFilters(pageFilters)

        return DataPage(nonFiltered.nextOffset, items = filtered)
    }

    private suspend fun AssetHistory.createTransactionFilters(chain: Chain): List<Filter<Operation>> {
        val isPoolAccountFilter = poolAccountDerivation.poolRewardAccountFilter(chain.id)

        return listOf(
            IgnoreUnsafeOperations(this),
            IgnorePoolRewardTransfers(isPoolAccountFilter, chain)
        )
    }

    private class IgnorePoolRewardTransfers(
        private val isPoolRewardAccount: Filter<AccountId>,
        private val chain: Chain,
    ) : Filter<Operation> {

        override fun shouldInclude(model: Operation): Boolean {
            val operationType = model.type as? Operation.Type.Transfer ?: return true
            val accountId = chain.accountIdOrNull(operationType.sender) ?: return true

            return !isPoolRewardAccount.shouldInclude(accountId)
        }
    }

    private class IgnoreUnsafeOperations(private val assetsHistory: AssetHistory) : Filter<Operation> {

        override fun shouldInclude(model: Operation): Boolean {
            return assetsHistory.isOperationSafe(model)
        }
    }
}
