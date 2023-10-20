package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.locks

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.runtime.ext.enabledAssets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge

class BalanceLocksUpdaterFactoryImpl(
    private val scope: AccountUpdateScope,
    private val assetSourceRegistry: AssetSourceRegistry,
) : BalanceLocksUpdaterFactory {

    override fun create(chain: Chain): Updater<MetaAccount> {
        return BalanceLocksUpdater(
            scope,
            assetSourceRegistry,
            chain
        )
    }
}

class BalanceLocksUpdater(
    override val scope: AccountUpdateScope,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chain: Chain
) : Updater<MetaAccount> {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount,
    ): Flow<Updater.SideEffect> {
        return chain.enabledAssets().map { chainAsset ->
            val metaAccount = scopeValue
            val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()
            val assetSource = assetSourceRegistry.sourceFor(chainAsset)
            assetSource.balance.startSyncingBalanceLocks(metaAccount, chain, chainAsset, accountId, storageSubscriptionBuilder)
        }
            .merge()
            .noSideAffects()
    }
}
