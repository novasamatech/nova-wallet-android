package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.ExtrinsicStatusEvent
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.bindAccountInfoOrDefault
import io.novafoundation.nova.feature_wallet_api.data.cache.updateAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationStatusToOperationLocalStatus
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.bindings.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source.BalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source.BalanceSourceProvider
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import java.lang.Exception


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
                val local = blockTransfers.map {
                    val localStatus = when (it.statusEvent) {
                        ExtrinsicStatusEvent.SUCCESS -> Operation.Status.COMPLETED
                        ExtrinsicStatusEvent.FAILURE -> Operation.Status.FAILED
                        null -> Operation.Status.PENDING
                    }

                    createTransferOperationLocal(it.extrinsic, localStatus, accountId)
                }

                operationDao.insertAll(local)
            }.onFailure {
                Log.e(LOG_TAG, "Failed to retrieve transactions from block (${chain.name}.${chainAsset.symbol}): ${it.message}")
            }
    }

    private suspend fun createTransferOperationLocal(
        extrinsic: TransferExtrinsic,
        status: Operation.Status,
        accountId: ByteArray,
    ): OperationLocal {
        val localCopy = operationDao.getOperation(extrinsic.hash)

        val fee = localCopy?.fee

        val senderAddress = chain.addressOf(extrinsic.senderId)
        val recipientAddress = chain.addressOf(extrinsic.recipientId)

        return OperationLocal.manualTransfer(
            hash = extrinsic.hash,
            chainId = chain.id,
            address = chain.addressOf(accountId),
            chainAssetId = chainAsset.id,
            amount = extrinsic.amountInPlanks,
            senderAddress = senderAddress,
            receiverAddress = recipientAddress,
            fee = fee,
            status = mapOperationStatusToOperationLocalStatus(status),
            source = OperationLocal.Source.BLOCKCHAIN,
        )
    }
}
