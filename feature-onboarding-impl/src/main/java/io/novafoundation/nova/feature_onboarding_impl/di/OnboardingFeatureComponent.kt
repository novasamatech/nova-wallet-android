package io.novafoundation.nova.feature_onboarding_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.importChooser.di.ImportWalletOptionsComponent
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

    fun importWalletOptionsComponentFactory(): ImportWalletOptionsComponent.Factory

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
            VersionsFeatureApi::class
        ]
    )
    interface OnboardingFeatureDependenciesComponent : OnboardingFeatureDependencies
}
