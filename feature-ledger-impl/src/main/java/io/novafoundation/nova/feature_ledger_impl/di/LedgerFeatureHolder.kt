package io.novafoundation.nova.feature_ledger_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class LedgerFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: LedgerRouter,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerLedgerFeatureComponent_LedgerFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .build()

        return DaggerLedgerFeatureComponent.factory()
            .create(accountFeatureDependencies, router)
    }
}
