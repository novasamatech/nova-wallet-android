package io.novafoundation.nova.feature_dapp_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi

@Component(
    dependencies = [
        DAppFeatureDependencies::class
    ],
    modules = [
        DappFeatureModule::class
    ]
)
@FeatureScope
interface DAppFeatureComponent : OnboardingFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: DAppRouter,
            deps: DAppFeatureDependencies
        ): DAppFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class
        ]
    )
    interface DAppFeatureDependenciesComponent : DAppFeatureDependencies
}
