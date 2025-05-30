package io.novafoundation.nova.feature_deep_linking.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_migration.di.AccountMigrationFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_deep_link_building.di.DeepLinkBuildingFeatureApi
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
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
            GovernanceFeatureApi::class,
            DAppFeatureApi::class,
            WalletConnectFeatureApi::class,
            DeepLinkBuildingFeatureApi::class,
            AccountMigrationFeatureApi::class
        ]
    )
    interface DeepLinkingFeatureDependenciesComponent : DeepLinkingFeatureDependencies
}
