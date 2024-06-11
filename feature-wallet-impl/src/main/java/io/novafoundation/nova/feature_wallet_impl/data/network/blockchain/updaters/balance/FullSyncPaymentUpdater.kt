package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.OperationLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetWithAmountToLocal
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapOperationStatusToOperationLocalStatus
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.enabledAssets
import io.novafoundation.nova.runtime.ext.localId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

internal class FullSyncPaymentUpdater(
    private val operationDao: OperationDao,
    private val assetSourceRegistry: AssetSourceRegistry,
    override val scope: AccountUpdateScope,
    private val chain: Chain,
) : Updater<MetaAccount> {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount,
    ): Flow<Updater.SideEffect> {
        val accountId = scopeValue.requireAccountIdIn(chain)

        return chain.enabledAssets().mapNotNull { chainAsset ->
            syncAsset(chainAsset, scopeValue, accountId, storageSubscriptionBuilder)
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

        return assetUpdateFlow.onEach { balanceUpdate ->
            assetSource.history.syncOperationsForBalanceChange(chainAsset, balanceUpdate, accountId)
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
    ) {
        when (balanceSyncUpdate) {
            is BalanceSyncUpdate.CauseFetchable -> runCatching { fetchOperationsForBalanceChange(chain, chainAsset, balanceSyncUpdate.blockHash, accountId) }
                .onSuccess { blockOperations ->
                    val localOperations = blockOperations
                        .filter { it.type.relates(accountId) }
                        .map { operation -> createOperationLocal(chainAsset, operation, accountId) }

                    operationDao.insertAll(localOperations)
                }.onFailure {
                    Log.e(LOG_TAG, "Failed to retrieve transactions from block (${chain.name}.${chainAsset.symbol})", it)
                }

            is BalanceSyncUpdate.CauseFetched -> {
                val local = createOperationLocal(chainAsset, balanceSyncUpdate.cause, accountId)
                operationDao.insert(local)
            }

            BalanceSyncUpdate.NoCause -> {}
        }
    }

    private suspend fun createOperationLocal(
        chainAsset: Chain.Asset,
        historyUpdate: RealtimeHistoryUpdate,
        accountId: ByteArray,
    ): OperationLocal {
        return when (val type = historyUpdate.type) {
            is RealtimeHistoryUpdate.Type.Swap -> createSwapOperation(chainAsset, historyUpdate, type, accountId)
            is RealtimeHistoryUpdate.Type.Transfer -> createTransferOperation(chainAsset, historyUpdate, type, accountId)
        }
    }

    private fun createSwapOperation(
        chainAsset: Chain.Asset,
        historyUpdate: RealtimeHistoryUpdate,
        swap: RealtimeHistoryUpdate.Type.Swap,
        accountId: ByteArray,
    ): OperationLocal {
        return OperationLocal.manualSwap(
            hash = historyUpdate.txHash,
            originAddress = chain.addressOf(accountId),
            assetId = chainAsset.localId,
            fee = mapAssetWithAmountToLocal(swap.amountFee),
            amountIn = mapAssetWithAmountToLocal(swap.amountIn),
            amountOut = mapAssetWithAmountToLocal(swap.amountOut),
            status = mapOperationStatusToOperationLocalStatus(historyUpdate.status),
            source = OperationBaseLocal.Source.BLOCKCHAIN
        )
    }

    private suspend fun createTransferOperation(
        chainAsset: Chain.Asset,
        historyUpdate: RealtimeHistoryUpdate,
        transfer: RealtimeHistoryUpdate.Type.Transfer,
        accountId: ByteArray,
    ): OperationLocal {
        val localStatus = mapOperationStatusToOperationLocalStatus(historyUpdate.status)
        val address = chain.addressOf(accountId)

        val localCopy = operationDao.getTransferType(historyUpdate.txHash, address, chain.id, chainAsset.id)

        return OperationLocal.manualTransfer(
            hash = historyUpdate.txHash,
            chainId = chain.id,
            address = address,
            chainAssetId = chainAsset.id,
            amount = transfer.amountInPlanks,
            senderAddress = chain.addressOf(transfer.senderId),
            receiverAddress = chain.addressOf(transfer.recipientId),
            fee = localCopy?.fee,
            status = localStatus,
            source = OperationBaseLocal.Source.BLOCKCHAIN,
        )
    }
}
