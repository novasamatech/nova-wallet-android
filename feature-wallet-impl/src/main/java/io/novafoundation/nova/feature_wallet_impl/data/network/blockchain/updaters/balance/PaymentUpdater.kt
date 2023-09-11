package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.enabledAssets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow

class RealPaymentUpdaterFactory(
    private val operationDao: OperationDao,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val scope: AccountUpdateScope,
    private val currencyRepository: CurrencyRepository
) : PaymentUpdaterFactory {

    override fun create(chain: Chain): Updater<MetaAccount> {
        return PaymentUpdater(
            operationDao = operationDao,
            assetSourceRegistry = assetSourceRegistry,
            scope = scope,
            chain = chain,
            currencyRepository = currencyRepository
        )
    }
}

private class PaymentUpdater(
    private val operationDao: OperationDao,
    private val assetSourceRegistry: AssetSourceRegistry,
    override val scope: AccountUpdateScope,
    private val chain: Chain,
    private val currencyRepository: CurrencyRepository
) : Updater<MetaAccount> {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount,
    ): Flow<Updater.SideEffect> {
        val metaAccount = scopeValue

        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()

        return chain.enabledAssets().mapNotNull { chainAsset ->
            syncAsset(chainAsset, metaAccount, accountId, storageSubscriptionBuilder)
        }
            .mergeIfMultiple()
            .noSideAffects()
    }

    private suspend fun syncAsset(
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        storageSubscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate>? {
        val assetSource = assetSourceRegistry.sourceFor(chainAsset)

        val assetUpdateFlow = runCatching {
            assetSource.balance.startSyncingBalance(chain, chainAsset, metaAccount, accountId, storageSubscriptionBuilder)
        }
            .onFailure { logSyncError(chain, chainAsset, error = it) }
            .getOrNull()
            ?: return null

        val currencyFlow = currencyRepository.observeSelectCurrency()

        return combine(assetUpdateFlow, currencyFlow) { balanceUpdate, currency ->
            assetSource.history.syncOperationsForBalanceChange(chainAsset, balanceUpdate, accountId, currency)
            balanceUpdate
        }
            .catch { logSyncError(chain, chainAsset, error = it) }
    }

    private fun logSyncError(chain: Chain, chainAsset: Chain.Asset, error: Throwable) {
        Log.e(LOG_TAG, "Failed to sync balance for ${chainAsset.symbol} in ${chain.name}", error)
    }

    private suspend fun AssetHistory.syncOperationsForBalanceChange(
        chainAsset: Chain.Asset,
        balanceSyncUpdate: BalanceSyncUpdate,
        accountId: AccountId,
        currency: Currency
    ) {
        when (balanceSyncUpdate) {
            is BalanceSyncUpdate.CauseFetchable -> fetchOperationsForBalanceChange(chain, chainAsset, balanceSyncUpdate.blockHash, accountId, currency)
                .onSuccess { blockTransfers ->
                    val localOperations = blockTransfers.map { transfer -> createTransferOperationLocal(chainAsset, transfer, accountId) }

                    operationDao.insertAll(localOperations)
                }.onFailure {
                    Log.e(LOG_TAG, "Failed to retrieve transactions from block (${chain.name}.${chainAsset.symbol})", it)
                }

            is BalanceSyncUpdate.CauseFetched -> {
                val local = createTransferOperationLocal(chainAsset, balanceSyncUpdate.cause, accountId)
                operationDao.insert(local)
            }

            BalanceSyncUpdate.NoCause -> {}
        }
    }

    private suspend fun createTransferOperationLocal(
        chainAsset: Chain.Asset,
        extrinsic: TransferExtrinsic,
        accountId: ByteArray,
    ): OperationLocal {
        val localStatus = when (extrinsic.status) {
            ExtrinsicStatus.SUCCESS -> OperationLocal.Status.COMPLETED
            ExtrinsicStatus.FAILURE -> OperationLocal.Status.FAILED
            ExtrinsicStatus.UNKNOWN -> OperationLocal.Status.PENDING
        }

        val localCopy = operationDao.getOperation(extrinsic.hash)

        return OperationLocal.manualTransfer(
            hash = extrinsic.hash,
            chainId = chain.id,
            address = chain.addressOf(accountId),
            chainAssetId = chainAsset.id,
            amount = extrinsic.amountInPlanks,
            senderAddress = chain.addressOf(extrinsic.senderId),
            receiverAddress = chain.addressOf(extrinsic.recipientId),
            fee = localCopy?.fee,
            status = localStatus,
            source = OperationLocal.Source.BLOCKCHAIN,
        )
    }
}
