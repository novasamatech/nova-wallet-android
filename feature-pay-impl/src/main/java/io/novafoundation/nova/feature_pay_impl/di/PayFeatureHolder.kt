package io.novafoundation.nova.feature_pay_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class PayFeatureHolder @Inject constructor(
    private val payRouter: PayRouter,
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val onboardingFeatureDependencies = DaggerPayFeatureComponent_PayFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .walletConnectFeatureApi(getFeature(WalletConnectFeatureApi::class.java))
            .build()

        return DaggerPayFeatureComponent.factory()
            .create(payRouter, onboardingFeatureDependencies)
    }
}
