package io.novafoundation.nova.feature_currency_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_currency_api.presentation.CurrencyRouter
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        CurrencyFeatureDependencies::class
    ],
    modules = [
        CurrencyFeatureModule::class
    ]
)
@FeatureScope
interface CurrencyFeatureComponent : CurrencyFeatureApi {

    fun selectCurrencyComponentFactory(): SelectCurrencyComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance walletRouter: CurrencyRouter,
            deps: CurrencyFeatureDependencies
        ): CurrencyFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class
        ]
    )
    interface CurrencyFeatureDependenciesComponent : CurrencyFeatureDependencies
}
