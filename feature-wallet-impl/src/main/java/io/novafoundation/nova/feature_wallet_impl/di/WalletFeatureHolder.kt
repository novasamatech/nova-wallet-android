package io.novafoundation.nova.feature_wallet_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_swap_core_api.di.SwapCoreApi
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class WalletFeatureHolder @Inject constructor(
    private val walletRouter: WalletRouter,
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerWalletFeatureComponent_WalletFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .swapCoreApi(getFeature(SwapCoreApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .xcmFeatureApi(getFeature(XcmFeatureApi::class.java))
            .build()
        return DaggerWalletFeatureComponent.factory()
            .create(walletRouter, dependencies)
    }
}
