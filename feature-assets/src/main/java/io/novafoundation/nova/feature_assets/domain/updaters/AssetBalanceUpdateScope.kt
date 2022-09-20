package io.novafoundation.nova.feature_assets.domain.updaters

import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

class AssetBalanceUpdateScope(
    private val asset: Chain.Asset,
    private val metaAccount: MetaAccount,
    private val walletRepository: WalletRepository
) : UpdateScope {

    private var currentAsset: Asset? = null

    override fun invalidationFlow(): Flow<Asset> {
        return walletRepository.assetFlow(metaAccount.id, asset)
            .filter { currentAsset?.totalInPlanks != it.totalInPlanks }

    }
}
