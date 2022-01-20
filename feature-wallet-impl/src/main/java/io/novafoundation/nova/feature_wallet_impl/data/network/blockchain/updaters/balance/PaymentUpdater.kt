package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

class PaymentUpdaterFactory(
    private val operationDao: OperationDao,
    private val balaneSourceProvider: BalanceSourceProvider,
    private val scope: AccountUpdateScope,
) {

    fun create(chain: Chain, chainAsset: Chain.Asset): PaymentUpdater {
        return PaymentUpdater(
            operationDao = operationDao,
            balanceSource = balaneSourceProvider.provideFor(chainAsset),
            scope = scope,
            chain = chain,
            chainAsset = chainAsset
        )
    }
}

class PaymentUpdater(
    private val operationDao: OperationDao,
    private val balanceSource: BalanceSource,
    override val scope: AccountUpdateScope,
    private val chain: Chain,
    private val chainAsset: Chain.Asset
) : Updater {

    override val requiredModules: List<String> = listOf(Modules.SYSTEM)

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val metaAccount = scope.getAccount()

        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()

        return balanceSource.startSyncingBalance(chain, chainAsset, metaAccount, accountId, storageSubscriptionBuilder)
            .onEach { blockHash -> fetchTransfers(blockHash, accountId) }
            .noSideAffects()
    }

    private suspend fun fetchTransfers(blockHash: String, accountId: AccountId) {
        balanceSource.fetchOperationsForBalanceChange(chain, blockHash, accountId)
            .onSuccess { blockTransfers ->
                val localOperations = blockTransfers.map { createTransferOperationLocal(it, accountId) }

                operationDao.insertAll(localOperations)
            }.onFailure {
                Log.e(LOG_TAG, "Failed to retrieve transactions from block (${chain.name}.${chainAsset.symbol}): ${it.message}")
            }
    }

    private suspend fun createTransferOperationLocal(
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
