package io.novafoundation.nova.feature_dapp_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.di.DAppBrowserComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.di.DAppSignExtrinsicComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.main.di.MainDAppComponent
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

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

    fun mainComponentFactory(): MainDAppComponent.Factory

    fun browserComponentFactory(): DAppBrowserComponent.Factory

    fun signExtrinsicComponentFactory(): DAppSignExtrinsicComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: DAppRouter,
            @BindsInstance signExtrinsicCommunicator: DAppSignExtrinsicCommunicator,
            deps: DAppFeatureDependencies
        ): DAppFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class,
            WalletFeatureApi::class,
            RuntimeApi::class
        ]
    )
    interface DAppFeatureDependenciesComponent : DAppFeatureDependencies
}
