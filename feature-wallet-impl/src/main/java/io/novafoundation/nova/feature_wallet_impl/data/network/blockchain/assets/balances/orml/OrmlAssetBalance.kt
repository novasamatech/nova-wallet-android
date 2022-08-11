package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLocks
import io.novafoundation.nova.feature_wallet_api.domain.model.bindBalanceLocks
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.updaters.insert
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class OrmlAssetBalance(
    private val assetCache: AssetCache,
    private val remoteStorageSource: StorageDataSource,
    private val localStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val storageCache: StorageCache,
) : AssetBalance {
    override suspend fun queryBalanceLocks(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): Flow<BalanceLocks?> {
        return localStorageSource.query(chain.id) {
            val currencyId = chainAsset.ormlCurrencyId(runtime)
            runtime.metadata.tokens()
                .storage("Locks")
                .observe(accountId, currencyId, binding = ::bindBalanceLocks)
        }
    }

    override suspend fun startSyncingBalanceLocks(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId, subscriptionBuilder: SubscriptionBuilder): Flow<*> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val key = try {
            val currencyId = chainAsset.ormlCurrencyId(runtime)
            runtime.metadata.tokens()
                .storage("Locks")
                .storageKey(runtime, accountId, currencyId)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to construct account storage key: ${e.message} in ${chain.name}")

            return emptyFlow<Nothing>()
        }

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                Log.e(LOG_TAG, "It's done")
                storageCache.insert(change, chain.id)
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
