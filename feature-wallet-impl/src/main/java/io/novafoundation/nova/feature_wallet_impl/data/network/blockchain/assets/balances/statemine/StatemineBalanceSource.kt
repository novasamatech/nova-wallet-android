package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.assets
import io.novafoundation.nova.common.utils.oneOf
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.filterOwn
import io.novafoundation.nova.runtime.ext.findAssetByStatemineId
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.status
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class StatemineBalanceSource(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val eventsRepository: EventsRepository,
    private val remoteStorage: StorageDataSource,
) : BalanceSource {

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        val statemineType = chainAsset.requireStatemine()

        val assetDetails = remoteStorage.query(
            chainId = chain.id,
            keyBuilder = { it.metadata.assets().storage("Asset").storageKey(it, statemineType.id) },
            binding = { scale, runtime -> bindAssetDetails(scale!!, runtime) }
        )

        return assetDetails.minimumBalance
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val statemineType = chainAsset.requireStatemine()

        val assetAccount = remoteStorage.query(
            chainId = chain.id,
            keyBuilder = { it.metadata.assets().storage("Account").storageKey(it, statemineType.id, accountId) },
            binding = { scale, runtime -> bindAssetAccountOrEmpty(scale, runtime) }
        )

        return assetAccount.balance
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash> {
        val statemineType = chainAsset.requireStatemine()

        val runtime = chainRegistry.getRuntime(chain.id)

        val assetDetailsKey = runtime.metadata.assets().storage("Asset").storageKey(runtime, statemineType.id)
        val assetAccountKey = runtime.metadata.assets().storage("Account").storageKey(runtime, statemineType.id, accountId)

        val isFrozenFlow = subscriptionBuilder.subscribe(assetDetailsKey)
            .map { bindAssetDetails(it.value!!, runtime).isFrozen }

        return combine(
            subscriptionBuilder.subscribe(assetAccountKey),
            isFrozenFlow
        ) { balanceStorageChange, isAssetFrozen ->
            val assetAccount = bindAssetAccountOrEmpty(balanceStorageChange.value, runtime)

            updateAssetBalance(metaAccount.id, chainAsset, isAssetFrozen, assetAccount)

            balanceStorageChange.block
        }
    }

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsic>> = runCatching {
        val runtime = chainRegistry.getRuntime(chain.id)
        val extrinsicsWithEvents = eventsRepository.getExtrinsicsWithEvents(chain.id, blockHash)

        extrinsicsWithEvents.filter { it.extrinsic.call.isTransfer(runtime) }
            .mapNotNull { extrinsicWithEvents ->
                val extrinsic = extrinsicWithEvents.extrinsic
                val chainAsset = chain.findAssetByStatemineId(bindNumber(extrinsic.call.arguments["id"]))

                chainAsset?.let {
                    TransferExtrinsic(
                        senderId = bindAccountIdentifier(extrinsic.signature!!.accountIdentifier),
                        recipientId = bindAccountIdentifier(extrinsic.call.arguments["target"]),
                        amountInPlanks = bindNumber(extrinsic.call.arguments["amount"]),
                        hash = extrinsicWithEvents.extrinsicHash,
                        chainAsset = chainAsset,
                        status = extrinsicWithEvents.status()
                    )
                }
            }.filterOwn(accountId)
    }

    private fun bindAssetAccountOrEmpty(scale: String?, runtime: RuntimeSnapshot): AssetAccount {
        return scale?.let { bindAssetAccount(it, runtime) } ?: AssetAccount.empty()
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        isAssetFrozen: Boolean,
        assetAccount: AssetAccount
    ) = assetCache.updateAsset(metaId, chainAsset) {
        val frozenBalance = if (isAssetFrozen || assetAccount.isFrozen) {
            assetAccount.balance
        } else {
            BigInteger.ZERO
        }

        it.copy(
            frozenInPlanks = frozenBalance,
            freeInPlanks = assetAccount.balance
        )
    }

    private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot): Boolean {
        val assets = runtime.metadata.assets()

        return oneOf(
            assets.call("transfer"),
            assets.call("transfer_keep_alive"),
        )
    }
}
