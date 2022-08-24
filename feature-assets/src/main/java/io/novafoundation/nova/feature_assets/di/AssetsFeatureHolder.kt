package io.novafoundation.nova.feature_assets.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectAddressCommunicator
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class AssetsFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val selectAddressCommunicator: SelectAddressCommunicator,
    private val router: AssetsRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerAssetsFeatureComponent_AssetsFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .nftFeatureApi(getFeature(NftFeatureApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .build()
        return DaggerAssetsFeatureComponent.factory()
            .create(router, selectAddressCommunicator, dependencies)
    }
}
