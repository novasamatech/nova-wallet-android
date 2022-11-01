package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.bindBalanceLocks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.mapBlockchainLockToLocal
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
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
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<*> {
        val runtime = chainRegistry.getRuntime(chain.id)
        val storage = runtime.metadata.tokens().storage("Locks")

        val currencyId = chainAsset.ormlCurrencyId(runtime)
        val key = storage.storageKey(runtime, accountId, currencyId)

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                val balanceLocks = bindBalanceLocks(storage.decodeValue(change.value, runtime))
                balanceLocks?.map { mapBlockchainLockToLocal(metaAccount.id, chain.id, chainAsset.id, it) }
                    ?.let { lockDao.updateLocks(it, metaAccount.id, chain.id, chainAsset.id) }
            }
    }

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        return chainAsset.requireOrml().existentialDeposit
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val ormlAccountData = remoteStorageSource.query(
            chainId = chain.id,
            keyBuilder = { it.ormlBalanceKey(accountId, chainAsset) },
            binding = { scale, runtime -> bindOrmlAccountDataOrEmpty(scale, runtime) }
        )

        return ormlAccountData.free + ormlAccountData.reserved
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash?> {
        val runtime = chainRegistry.getRuntime(chain.id)

        return subscriptionBuilder.subscribe(runtime.ormlBalanceKey(accountId, chainAsset))
            .map {
                val ormlAccountData = bindOrmlAccountDataOrEmpty(it.value, runtime)

                val assetChanged = updateAssetBalance(metaAccount.id, chainAsset, ormlAccountData)

                it.block.takeIf { assetChanged }
            }
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        ormlAccountData: OrmlAccountData
    ) = assetCache.updateAsset(metaId, chainAsset) {
        it.copy(
            frozenInPlanks = ormlAccountData.frozen,
            freeInPlanks = ormlAccountData.free,
            reservedInPlanks = ormlAccountData.reserved
        )
    }

    private fun RuntimeSnapshot.ormlBalanceKey(accountId: AccountId, chainAsset: Chain.Asset): String {
        return metadata.tokens().storage("Accounts").storageKey(this, accountId, chainAsset.ormlCurrencyId(this))
    }

    private fun bindOrmlAccountDataOrEmpty(scale: String?, runtime: RuntimeSnapshot): OrmlAccountData {
        return scale?.let { bindOrmlAccountData(it, runtime) } ?: OrmlAccountData.empty()
    }
}
