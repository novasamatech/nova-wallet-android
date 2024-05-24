package io.novafoundation.nova.feature_ledger_core

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class LedgerCoreHolder @Inject constructor(
    featureContainer: FeatureContainer,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerLedgerCoreComponent_LedgerCoreDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .build()

        return DaggerLedgerCoreComponent.factory()
            .create(accountFeatureDependencies)
    }
}
