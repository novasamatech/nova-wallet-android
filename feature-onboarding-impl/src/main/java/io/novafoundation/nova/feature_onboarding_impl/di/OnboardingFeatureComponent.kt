package io.novafoundation.nova.feature_onboarding_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_ledger_core.di.LedgerCoreApi
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.di.WelcomeComponent
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi

@Component(
    dependencies = [
        OnboardingFeatureDependencies::class
    ],
    modules = [
        OnboardingFeatureModule::class
    ]
)
@FeatureScope
interface OnboardingFeatureComponent : OnboardingFeatureApi {

    fun welcomeComponentFactory(): WelcomeComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance onboardingRouter: OnboardingRouter,
            deps: OnboardingFeatureDependencies
        ): OnboardingFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class,
            VersionsFeatureApi::class,
            LedgerCoreApi::class
        ]
    )
    interface OnboardingFeatureDependenciesComponent : OnboardingFeatureDependencies
}
