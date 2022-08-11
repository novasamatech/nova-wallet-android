package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.bindAccountInfoOrDefault
import io.novafoundation.nova.feature_wallet_api.data.cache.updateAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLocks
import io.novafoundation.nova.feature_wallet_api.domain.model.bindBalanceLocks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.updaters.insert
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class NativeAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val substrateRemoteSource: SubstrateRemoteSource,
    private val remoteStorageDataSource: StorageDataSource,
    private val storageCache: StorageCache
) : AssetBalance {

    override suspend fun queryBalanceLocks(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): Flow<BalanceLocks?> {
        return remoteStorageDataSource.query(chain.id) {
            runtime.metadata.balances().storage("Locks").observe(accountId, binding = ::bindBalanceLocks)
        }
    }

    override suspend fun startSyncingBalanceLocks(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<*> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val key = try {
            runtime.metadata.balances().storage("Locks").storageKey(runtime, accountId)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to construct account storage key: ${e.message} in ${chain.name}")

            return emptyFlow<Nothing>()
        }

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                storageCache.insert(change, chain.id)
            }
    }

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        val runtime = chainRegistry.getRuntime(chain.id)

        return runtime.metadata.balances().numberConstant("ExistentialDeposit", runtime)
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val accountInfo = substrateRemoteSource.getAccountInfo(chain.id, accountId)

        return accountInfo.data.free + accountInfo.data.reserved
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash?> {
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

                change.block.takeIf { assetChanged }
            }
    }
}
