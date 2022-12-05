package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationLocalToOperation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationToOperationLocalDb
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class RealTransactionHistoryRepository(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val operationDao: OperationDao,
) : TransactionHistoryRepository {

    override suspend fun syncOperationsFirstPage(
        pageSize: Int,
        filters: Set<TransactionFilter>,
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset
    ) = withContext(Dispatchers.Default) {
        val historySource = historySourceFor(chainAsset)
        val accountAddress = chain.addressOf(accountId)

        val dataPage = historySource.getOperations(pageSize, PageOffset.Loadable.FistPage, filters, accountId, chain, chainAsset)
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
        chainAsset: Chain.Asset
    ): DataPage<Operation> = withContext(Dispatchers.Default) {
        val historySource = historySourceFor(chainAsset)

        historySource.getOperations(
            pageSize = pageSize,
            pageOffset = pageOffset,
            filters = filters,
            accountId = accountId,
            chain = chain,
            chainAsset = chainAsset
        )
    }

    override fun operationsFirstPageFlow(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset
    ): Flow<DataPage<Operation>> {
        val accountAddress = chain.addressOf(accountId)
        val historySource = historySourceFor(chainAsset)

        return operationDao.observe(accountAddress, chain.id, chainAsset.id)
            .mapList {
                mapOperationLocalToOperation(it, chainAsset)
            }
            .mapLatest { operations ->
                val pageOffset = historySource.getSyncedPageOffset(accountId, chain, chainAsset)

                DataPage(pageOffset, operations)
            }
    }

    private fun historySourceFor(chainAsset: Chain.Asset): AssetHistory = assetSourceRegistry.sourceFor(chainAsset).history
}
