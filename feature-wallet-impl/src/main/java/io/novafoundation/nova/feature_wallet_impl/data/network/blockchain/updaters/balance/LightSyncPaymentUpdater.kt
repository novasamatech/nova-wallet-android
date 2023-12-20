package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.hash.Hasher.blake2b128Concat
import jp.co.soramitsu.fearless_utils.hash.Hasher.xxHash128
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

/**
 * Runtime-independent updater that watches on-chain account presence and switches to full sync mode if account is present
 */
class LightSyncPaymentUpdater(
    override val scope: AccountUpdateScope,
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val chain: Chain
) : Updater<MetaAccount> {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder, scopeValue: MetaAccount): Flow<Updater.SideEffect> {
        val accountId = scopeValue.accountIdIn(chain) ?: return emptyFlow()
        val storageKey = systemAccountStorageKey(accountId)

        return storageSubscriptionBuilder.subscribe(storageKey).onEach { storageChange ->
            if (storageChange.value != null) {
                switchToFullSync()

                Log.d("ConnectionState", "Detected balance during light sync for ${chain.name}, switching to full sync mode")
            } else {
                insertEmptyBalances(scopeValue)
            }
        }.noSideAffects()
    }

    private suspend fun insertEmptyBalances(metaAccount: MetaAccount) {
        assetCache.updateAssetsByChain(metaAccount, chain) { chainAsset ->
            AssetLocal.createEmpty(chainAsset.id, chainAsset.chainId, metaAccount.id)
        }
    }

    private suspend fun switchToFullSync() {
        chainRegistry.enableFullSync(chain.id)
    }

    private fun systemAccountStorageKey(accountId: AccountId): String {
        val keyBytes = "System".xxHash128() + "Account".xxHash128() + accountId.blake2b128Concat()

        return keyBytes.toHexString(withPrefix = true)
    }

    private fun String.xxHash128() = toByteArray().xxHash128()
}
