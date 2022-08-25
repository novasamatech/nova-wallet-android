package io.novafoundation.nova.feature_currency_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_currency_api.presentation.CurrencyRouter
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class CurrencyFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    val currencyRouter: CurrencyRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerCurrencyFeatureComponent_CurrencyFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .build()
        return DaggerCurrencyFeatureComponent.factory()
            .create(currencyRouter, dependencies)
    }
}
