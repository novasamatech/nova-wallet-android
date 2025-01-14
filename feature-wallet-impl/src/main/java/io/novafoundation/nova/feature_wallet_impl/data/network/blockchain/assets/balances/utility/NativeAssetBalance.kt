package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility

import android.util.Log
import io.novafoundation.nova.common.data.network.ext.transferableBalance
import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.dao.HoldsDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.model.BalanceHoldLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.bindAccountInfoOrDefault
import io.novafoundation.nova.feature_wallet_api.data.cache.updateAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.bindBalanceLocks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.updateLocks
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novafoundation.nova.runtime.storage.typed.account
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class NativeAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val substrateRemoteSource: SubstrateRemoteSource,
    private val remoteStorage: StorageDataSource,
    private val lockDao: LockDao,
    private val holdsDao: HoldsDao,
) : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        return remoteStorage.subscribe(chain.id, subscriptionBuilder) {
            combine(
                metadata.balances.locks.observe(accountId),
                metadata.balances.freezes.observe(accountId)
            ) { locks, freezes ->
                val all = locks.orEmpty() + freezes.orEmpty()

                lockDao.updateLocks(all, metaAccount.id, chain.id, chainAsset.id)
            }
        }
    }

    override suspend fun startSyncingBalanceHolds(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        val runtime = chainRegistry.getRuntime(chain.id)
        val storage = runtime.metadata.balances().storageOrNull("Holds") ?: return emptyFlow<Nothing>()
        val key = storage.storageKey(runtime, accountId)

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                val holds = bindBalanceHolds(storage.decodeValue(change.value, runtime)).orEmpty()
                holdsDao.updateHolds(holds, metaAccount.id, chain.id, chainAsset.id)
            }
    }

    override fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        val runtime = chainRegistry.getRuntime(chain.id)

        return runtime.metadata.balances().numberConstant("ExistentialDeposit", runtime)
    }

    override suspend fun queryAccountBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): AccountBalance {
        return substrateRemoteSource.getAccountInfo(chain.id, accountId).data
    }

    override suspend fun subscribeTransferableAccountBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        sharedSubscriptionBuilder: SharedRequestsBuilder?
    ): Flow<TransferableBalanceUpdate> {
        return remoteStorage.subscribe(chain.id, sharedSubscriptionBuilder) {
            metadata.system.account.observeWithRaw(accountId).map {
                val accountInfo = it.value ?: AccountInfo.empty()

                TransferableBalanceUpdate(
                    newBalance = accountInfo.transferableBalance(),
                    updatedAt = it.at
                )
            }
        }
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val accountData = queryAccountBalance(chain, chainAsset, accountId)

        return accountData.free + accountData.reserved
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val key = try {
            runtime.metadata.system().storage("Account").storageKey(runtime, accountId)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to construct account storage key: ${e.message} in ${chain.name}")

            return emptyFlow()
        }

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                val accountInfo = bindAccountInfoOrDefault(change.value, runtime)
                val assetChanged = assetCache.updateAsset(metaAccount.id, chain.utilityAsset, accountInfo)

                if (assetChanged) {
                    BalanceSyncUpdate.CauseFetchable(change.block)
                } else {
                    BalanceSyncUpdate.NoCause
                }
            }
    }

    private fun bindBalanceHolds(dynamicInstance: Any?): List<BlockchainHold>? {
        if (dynamicInstance == null) return null

        return bindList(dynamicInstance) {
            BlockchainHold(
                id = bindHoldId(it.castToStruct()["id"]),
                amount = bindNumber(it.castToStruct()["amount"])
            )
        }
    }

    private fun bindHoldId(id: Any?): BalanceHold.HoldId {
        val module = id.castToDictEnum()
        val reason = module.value.castToDictEnum()

        return BalanceHold.HoldId(module.name, reason.name)
    }

    private suspend fun HoldsDao.updateHolds(holds: List<BlockchainHold>, metaId: Long, chainId: ChainId, chainAssetId: ChainAssetId) {
        val balanceLocksLocal = holds.map {
            BalanceHoldLocal(
                metaId = metaId,
                chainId = chainId,
                assetId = chainAssetId,
                id = BalanceHoldLocal.HoldIdLocal(module = it.id.module, reason = it.id.reason),
                amount = it.amount
            )
        }
        updateHolds(balanceLocksLocal, metaId, chainId, chainAssetId)
    }

    private class BlockchainHold(val id: BalanceHold.HoldId, val amount: Balance)
}
