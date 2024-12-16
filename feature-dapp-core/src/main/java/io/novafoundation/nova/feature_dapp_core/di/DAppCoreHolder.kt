package io.novafoundation.nova.feature_dapp_core.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import javax.inject.Inject

@ApplicationScope
class DAppCoreHolder @Inject constructor(
    featureContainer: FeatureContainer,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dappCoreDependencies = DaggerDappCoreComponent_DAppCoreDependenciesComponent.builder()
            .commonApi(commonApi())
            .build()

        return DaggerDappCoreComponent.factory()
            .create(dappCoreDependencies)
    }
}
