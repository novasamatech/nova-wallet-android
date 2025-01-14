package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrmlAccountBalanceOrEmpty
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.model.AssetLocal.EDCountingModeLocal
import io.novafoundation.nova.core_db.model.AssetLocal.TransferableModeLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdate
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.bindBalanceLocks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.updateLocks
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class OrmlAssetBalance(
    private val assetCache: AssetCache,
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val lockDao: LockDao
) : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        val runtime = chainRegistry.getRuntime(chain.id)
        val storage = runtime.metadata.tokens().storage("Locks")

        val currencyId = chainAsset.ormlCurrencyId(runtime)
        val key = storage.storageKey(runtime, accountId, currencyId)

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                val balanceLocks = bindBalanceLocks(storage.decodeValue(change.value, runtime)).orEmpty()
                lockDao.updateLocks(balanceLocks, metaAccount.id, chain.id, chainAsset.id)
            }
    }

    override fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        return chainAsset.requireOrml().existentialDeposit
    }

    override suspend fun queryAccountBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): AccountBalance {
        return remoteStorageSource.query(
            chainId = chain.id,
            keyBuilder = { it.ormlBalanceKey(accountId, chainAsset) },
            binding = { scale, runtime -> bindOrmlAccountBalanceOrEmpty(scale, runtime) }
        )
    }

    override suspend fun subscribeTransferableAccountBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        sharedSubscriptionBuilder: SharedRequestsBuilder?
    ): Flow<TransferableBalanceUpdate> {
        return remoteStorageSource.subscribe(chain.id, sharedSubscriptionBuilder) {
            metadata.tokens().storage("Accounts").observeWithRaw(
                accountId,
                chainAsset.ormlCurrencyId(runtime),
                binding = ::bindOrmlAccountBalanceOrEmpty
            ).map {
                TransferableBalanceUpdate(
                    newBalance = TransferableMode.REGULAR.calculateTransferable(it.value),
                    updatedAt = it.at
                )
            }
        }
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val accountBalance = queryAccountBalance(chain, chainAsset, accountId)

        return accountBalance.free + accountBalance.reserved
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        val runtime = chainRegistry.getRuntime(chain.id)

        return subscriptionBuilder.subscribe(runtime.ormlBalanceKey(accountId, chainAsset))
            .map {
                val ormlAccountData = bindOrmlAccountBalanceOrEmpty(it.value, runtime)

                val assetChanged = updateAssetBalance(metaAccount.id, chainAsset, ormlAccountData)

                if (assetChanged) {
                    BalanceSyncUpdate.CauseFetchable(it.block)
                } else {
                    BalanceSyncUpdate.NoCause
                }
            }
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        ormlAccountData: AccountBalance
    ) = assetCache.updateAsset(metaId, chainAsset) { local ->
        with(ormlAccountData) {
            local.copy(
                frozenInPlanks = frozen,
                freeInPlanks = free,
                reservedInPlanks = reserved,
                transferableMode = TransferableModeLocal.REGULAR,
                edCountingMode = EDCountingModeLocal.TOTAL,
            )
        }
    }

    private fun RuntimeSnapshot.ormlBalanceKey(accountId: AccountId, chainAsset: Chain.Asset): String {
        return metadata.tokens().storage("Accounts").storageKey(this, accountId, chainAsset.ormlCurrencyId(this))
    }

    private fun bindOrmlAccountBalanceOrEmpty(scale: String?, runtime: RuntimeSnapshot): AccountBalance {
        return scale?.let { bindOrmlAccountData(it, runtime) } ?: AccountBalance.empty()
    }
}
