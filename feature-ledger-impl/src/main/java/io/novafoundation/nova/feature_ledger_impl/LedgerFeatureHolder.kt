package io.novafoundation.nova.feature_ledger_impl

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class LedgerFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerLedgerFeatureComponent_LedgerFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .build()

        return DaggerLedgerFeatureComponent.factory()
            .create(accountFeatureDependencies)
    }
}
