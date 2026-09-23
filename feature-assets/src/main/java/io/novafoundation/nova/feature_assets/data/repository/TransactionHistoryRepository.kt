package io.novafoundation.nova.feature_assets.data.repository

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.common.utils.applyFilters
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.OperationJoin
import io.novafoundation.nova.core_db.model.operation.SwapOperationJoin
import io.novafoundation.nova.core_db.model.operation.SwapTypeJoin
import io.novafoundation.nova.core_db.model.operation.SwapTypeLocal
import io.novafoundation.nova.core_db.model.operation.TransferOperationJoin
import io.novafoundation.nova.feature_account_api.domain.account.system.SystemAccountMatcher
import io.novafoundation.nova.feature_assets.data.mappers.mapOperationLocalToOperation
import io.novafoundation.nova.feature_assets.data.mappers.mapOperationToOperationLocalDb
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_api.data.mythos.MythosMainPotMatcherFactory
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.poolRewardAccountMatcher
import io.novafoundation.nova.feature_swap_api.data.history.SwapTransferFilterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.getAllCoinPriceHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.findNearestCoinRate
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.math.MathContext
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
    private val poolAccountDerivation: PoolAccountDerivation,
    private val mythosMainPotMatcherFactory: MythosMainPotMatcherFactory,
    private val swapTransferFilterFactory: SwapTransferFilterFactory,
    private val coinPriceRepository: CoinPriceRepository,
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

        val dataPageResult = runCatching {
            // Persist gross swap amounts and their transfers together. Commission is deducted only
            // when presenting history, otherwise a later database emission can deduct it twice.
            historySource.getOperations(
                pageSize,
                PageOffset.Loadable.FirstPage,
                filters,
                accountId,
                chain,
                chainAsset,
                currency
            )
        }
        historySource.additionalFirstPageSync(chain, chainAsset, accountId, dataPageResult)

        val dataPage = dataPageResult.getOrThrow()

        val reconciled = dataPage.replaceKnownCommissionOperations(accountId, chain, chainAsset)
        val localOperations = reconciled.map { mapOperationToOperationLocalDb(it, OperationBaseLocal.Source.REMOTE) }

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
        // Operations in the database were already accepted by the history source. Applying the
        // remote safety filters again would hide the outer Utility.batchAll of a commissioned swap.
        val localFilters = listOfNotNull(swapTransferFilterFactory.create(chain))
        val operationsFlow = operationDao.observe(accountAddress, chain.id, chainAsset.id)
        val swapOperationsFlow = operationDao.observeSwapOperations(accountAddress, chain.id)
        val transferOperationsFlow = operationDao.observeTransferOperations(accountAddress, chain.id)

        return combine(operationsFlow, swapOperationsFlow, transferOperationsFlow) { operations, swapOperations, transferOperations ->
            val commissionAmounts = transferOperations.commissionAmounts(chain)
            operations to swapOperations.associateByHash(commissionAmounts)
        }
            .transform { (operations, swapOperationsByHash) ->
                emit(mapOperations(operations, chainAsset, chain, emptyList(), swapOperationsByHash).applyFilters(localFilters))

                runCatching { coinPriceRepository.getAllCoinPriceHistory(chainAsset.priceId!!, currency) }
                    .onSuccess {
                        emit(mapOperations(operations, chainAsset, chain, it, swapOperationsByHash).applyFilters(localFilters))
                    }
            }
            .mapLatest { operations ->
                val pageOffset = historySource.getSyncedPageOffset(accountId, chain, chainAsset)

                DataPage(pageOffset, operations)
            }
    }

    private fun mapOperations(
        operations: List<OperationJoin>,
        chainAsset: Chain.Asset,
        chain: Chain,
        coinPrices: List<HistoricalCoinRate>,
        swapOperationsByHash: Map<String, SwapOperationJoin> = emptyMap(),
    ): List<Operation> {
        return operations.mapNotNull { operation ->
            val operationTimestamp = operation.base.time.milliseconds.inWholeSeconds
            val coinPrice = coinPrices.findNearestCoinRate(operationTimestamp)
            val displayOperation = operation.replaceTechnicalSwapBatch(swapOperationsByHash)
            mapOperationLocalToOperation(displayOperation, chainAsset, chain, coinPrice)
        }
    }

    private fun historySourceFor(chainAsset: Chain.Asset): AssetHistory = assetSourceRegistry.sourceFor(chainAsset).history

    private fun List<SwapOperationJoin>.associateByHash(
        commissionAmounts: Map<CommissionTransferKey, BigInteger>,
    ): Map<String, SwapOperationJoin> {
        return mapNotNull { swap ->
            swap.base.hash?.let { hash -> hash to swap.withNetAmountOut(commissionAmounts) }
        }.toMap()
    }

    private fun SwapOperationJoin.withNetAmountOut(
        commissionAmounts: Map<CommissionTransferKey, BigInteger>,
    ): SwapOperationJoin {
        val hash = base.hash ?: return this
        if (base.status != OperationBaseLocal.Status.COMPLETED) return this
        val commission = commissionAmounts[CommissionTransferKey(hash, swap.assetOut.assetId)] ?: return this
        val netAmount = (swap.assetOut.amount - commission).max(BigInteger.ZERO)

        return SwapOperationJoin(
            base = base,
            swap = SwapTypeJoin(
                fee = swap.fee,
                assetIn = swap.assetIn,
                assetOut = SwapTypeLocal.AssetWithAmount(swap.assetOut.assetId, netAmount),
            )
        )
    }

    private fun List<TransferOperationJoin>.commissionAmounts(chain: Chain): Map<CommissionTransferKey, BigInteger> {
        val beneficiaries = swapTransferFilterFactory.commissionBeneficiaryAddresses(chain)
        if (beneficiaries.isEmpty()) return emptyMap()

        return mapNotNull { operation ->
            val hash = operation.base.hash ?: return@mapNotNull null
            val transfer = operation.transfer
            if (operation.base.status != OperationBaseLocal.Status.COMPLETED) return@mapNotNull null
            if (transfer.sender != operation.base.address || transfer.receiver !in beneficiaries) return@mapNotNull null

            CommissionTransferKey(hash, operation.base.assetId) to transfer.amount
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.fold(BigInteger.ZERO, BigInteger::add) }
    }

    private fun OperationJoin.replaceTechnicalSwapBatch(swapOperationsByHash: Map<String, SwapOperationJoin>): OperationJoin {
        if (swap == null && !isUtilityBatchAll()) return this

        val knownSwap = base.hash?.let(swapOperationsByHash::get) ?: return this
        if (base.assetId != knownSwap.swap.assetIn.assetId && base.assetId != knownSwap.swap.assetOut.assetId) return this

        return SwapOperationJoin(base, knownSwap.swap).asOperationJoin()
    }

    private fun OperationJoin.isUtilityBatchAll(): Boolean {
        val extrinsic = extrinsic ?: return false

        return extrinsic.module.equals("utility", ignoreCase = true) &&
            (extrinsic.call.equals("batchAll", ignoreCase = true) || extrinsic.call.equals("batch_all", ignoreCase = true))
    }

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

        val pageFilters = createTransactionFilters(chain, chainAsset)
        val reconciled = nonFiltered.replaceKnownCommissionOperations(accountId, chain, chainAsset)
        val filtered = reconciled.withNetSwapAmounts(chain).applyFilters(pageFilters)

        return DataPage(nonFiltered.nextOffset, items = filtered)
    }

    private suspend fun List<Operation>.replaceKnownCommissionOperations(
        accountId: AccountId,
        chain: Chain,
        chainAsset: Chain.Asset,
    ): List<Operation> {
        val knownSwaps = operationDao.observeSwapOperations(chain.addressOf(accountId), chain.id).first()
            .associateByHash(emptyMap())
        val commissionBeneficiaries = swapTransferFilterFactory.commissionBeneficiaryAddresses(chain)

        return map { operation ->
            val swap = operation.extrinsicHash?.let(knownSwaps::get) ?: return@map operation
            val assetId = AssetAndChainId(chainAsset.chainId, chainAsset.id)
            if (assetId != swap.swap.assetIn.assetId && assetId != swap.swap.assetOut.assetId) return@map operation
            if (!operation.isTechnicalCommissionOperation(commissionBeneficiaries)) return@map operation

            val mapped = mapOperationLocalToOperation(swap.asOperationJoin(), chainAsset, chain, null) ?: return@map operation
            operation.copy(type = mapped.type)
        }.distinctBy { operation ->
            if (operation.type is Operation.Type.Swap) operation.extrinsicHash ?: operation.id else operation.id
        }
    }

    private fun Operation.isTechnicalCommissionOperation(commissionBeneficiaries: Set<String>): Boolean {
        return when (val operationType = type) {
            is Operation.Type.Extrinsic -> {
                val call = operationType.content as? Operation.Type.Extrinsic.Content.SubstrateCall ?: return false
                call.module.equals("utility", ignoreCase = true) &&
                    (call.call.equals("batchAll", ignoreCase = true) || call.call.equals("batch_all", ignoreCase = true))
            }

            is Operation.Type.Transfer -> {
                operationType.sender == address && operationType.receiver in commissionBeneficiaries
            }

            else -> false
        }
    }

    private fun List<Operation>.withNetSwapAmounts(chain: Chain): List<Operation> {
        val beneficiaries = swapTransferFilterFactory.commissionBeneficiaryAddresses(chain)
        if (beneficiaries.isEmpty()) return this

        val commissions = mapNotNull { operation ->
            val hash = operation.extrinsicHash ?: return@mapNotNull null
            val transfer = operation.type as? Operation.Type.Transfer ?: return@mapNotNull null
            if (operation.status != Operation.Status.COMPLETED) return@mapNotNull null
            if (transfer.sender != operation.address || transfer.receiver !in beneficiaries) return@mapNotNull null

            CommissionTransferKey(hash, AssetAndChainId(operation.chainAsset.chainId, operation.chainAsset.id)) to transfer.amount
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.fold(BigInteger.ZERO, BigInteger::add) }

        return map { operation ->
            val hash = operation.extrinsicHash ?: return@map operation
            val swap = operation.type as? Operation.Type.Swap ?: return@map operation
            if (operation.status != Operation.Status.COMPLETED) return@map operation
            val output = swap.amountOut
            val outputId = AssetAndChainId(output.chainAsset.chainId, output.chainAsset.id)
            val commission = commissions[CommissionTransferKey(hash, outputId)] ?: return@map operation
            val netAmount = (output.amount - commission).max(BigInteger.ZERO)
            val netFiatAmount = if (operation.chainAsset.fullId == output.chainAsset.fullId && output.amount > BigInteger.ZERO) {
                swap.fiatAmount?.multiply(netAmount.toBigDecimal())?.divide(output.amount.toBigDecimal(), MathContext.DECIMAL128)
            } else {
                swap.fiatAmount
            }

            operation.copy(type = swap.copy(amountOut = output.copy(amount = netAmount), fiatAmount = netFiatAmount))
        }
    }

    private data class CommissionTransferKey(
        val hash: String,
        val assetId: AssetAndChainId,
    )

    private suspend fun AssetHistory.createTransactionFilters(chain: Chain, chainAsset: Chain.Asset): List<Filter<Operation>> {
        val systemAccountFilterCreator = { matcher: SystemAccountMatcher? ->
            matcher?.let { IgnoreTransfersFromSystemAccount(it, chain) }
        }

        return listOfNotNull(
            IgnoreUnsafeOperations(this),
            systemAccountFilterCreator(poolAccountDerivation.poolRewardAccountMatcher(chain.id)),
            systemAccountFilterCreator(mythosMainPotMatcherFactory.create(chainAsset)),
            swapTransferFilterFactory.create(chain),
        )
    }

    private class IgnoreTransfersFromSystemAccount(
        private val systemAccountMatcher: SystemAccountMatcher,
        private val chain: Chain
    ) : Filter<Operation> {

        override fun shouldInclude(model: Operation): Boolean {
            val operationType = model.type as? Operation.Type.Transfer ?: return true
            val accountId = chain.accountIdOrNull(operationType.sender) ?: return true

            return !systemAccountMatcher.isSystemAccount(accountId)
        }
    }

    private class IgnoreUnsafeOperations(private val assetsHistory: AssetHistory) : Filter<Operation> {

        override fun shouldInclude(model: Operation): Boolean {
            return assetsHistory.isOperationSafe(model)
        }
    }
}
