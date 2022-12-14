package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.AssetBalanceScope
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class AssetBalanceScopeFactory(
    private val walletRepository: WalletRepository
) {

    fun create(asset: Chain.Asset, metaAccount: MetaAccount): AssetBalanceScope {
        return RealAssetBalanceScope(asset, metaAccount, walletRepository)
    }
}

class RealAssetBalanceScope(
    private val asset: Chain.Asset,
    private val metaAccount: MetaAccount,
    private val walletRepository: WalletRepository
) : AssetBalanceScope {

    override fun invalidationFlow(): Flow<Asset> {
        return walletRepository.assetFlow(metaAccount.id, asset)
            .distinctUntilChanged { old, new -> old.totalInPlanks == new.totalInPlanks }
    }
}
