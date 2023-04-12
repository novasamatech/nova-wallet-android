package io.novafoundation.nova.web3names.di

import io.novafoundation.nova.caip.di.CaipApi
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class Web3NamesHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dbDependencies = DaggerWeb3NamesFeatureComponent_Web3NamesDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .caipApi(getFeature(CaipApi::class.java))
            .build()

        return DaggerWeb3NamesFeatureComponent.builder()
            .web3NamesDependencies(dbDependencies)
            .build()
    }
}
