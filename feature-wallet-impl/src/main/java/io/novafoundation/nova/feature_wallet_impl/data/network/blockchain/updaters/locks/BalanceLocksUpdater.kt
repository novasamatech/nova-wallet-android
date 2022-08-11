package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.locks

import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.*


class BalanceLocksUpdaterFactoryImpl(
    private val scope: AccountUpdateScope,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
) : BalanceLocksUpdaterFactory {

    override fun create(chainId: ChainId, chainAssetId: ChainAssetId): Updater {
        return BalanceLocksUpdater(
            scope,
            assetSourceRegistry,
            chainRegistry,
            chainId,
            chainAssetId
        )
    }
}

class BalanceLocksUpdater(
    override val scope: AccountUpdateScope,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
    private val chainId: ChainId,
    private val chainAssetId: ChainAssetId,
) : Updater {
    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
        val metaAccount = scope.getAccount()
        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()
        val assetSource = assetSourceRegistry.sourceFor(chainAsset)
        return assetSource.balance.startSyncingBalanceLocks(chain, chainAsset, accountId, storageSubscriptionBuilder)
            .noSideAffects()
    }
}
