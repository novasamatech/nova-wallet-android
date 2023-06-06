package io.novafoundation.nova.app.root.di

import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class RootFeatureHolder @Inject constructor(
    private val navigationHolder: NavigationHolder,
    private val navigator: Navigator,
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val rootFeatureDependencies = DaggerRootComponent_RootFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .stakingFeatureApi(getFeature(StakingFeatureApi::class.java))
            .assetsFeatureApi(getFeature(AssetsFeatureApi::class.java))
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .crowdloanFeatureApi(getFeature(CrowdloanFeatureApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .versionsFeatureApi(getFeature(VersionsFeatureApi::class.java))
            .walletConnectFeatureApi(getFeature(WalletConnectFeatureApi::class.java))
            .build()

        return DaggerRootComponent.factory()
            .create(navigationHolder, navigator, rootFeatureDependencies)
    }
}
