package io.novafoundation.nova.feature_swap_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_buy_api.di.BuyFeatureApi
import io.novafoundation.nova.feature_swap_core_api.di.SwapCoreApi
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class SwapFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: SwapRouter,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerSwapFeatureComponent_SwapFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .swapCoreApi(getFeature(SwapCoreApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .buyFeatureApi(getFeature(BuyFeatureApi::class.java))
            .xcmFeatureApi(getFeature(XcmFeatureApi::class.java))
            .build()

        return DaggerSwapFeatureComponent.factory()
            .create(accountFeatureDependencies, router)
    }
}
