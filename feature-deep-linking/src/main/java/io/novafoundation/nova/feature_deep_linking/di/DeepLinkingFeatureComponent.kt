package io.novafoundation.nova.feature_deep_linking.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        DeepLinkingFeatureDependencies::class
    ],
    modules = [
        DeepLinkingFeatureModule::class
    ]
)
@FeatureScope
interface DeepLinkingFeatureComponent : DeepLinkingFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance deepLinkingRouter: DeepLinkingRouter,
            deps: DeepLinkingFeatureDependencies
        ): DeepLinkingFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class,
            GovernanceFeatureApi::class
        ]
    )
    interface DeepLinkingFeatureDependenciesComponent : DeepLinkingFeatureDependencies
}
