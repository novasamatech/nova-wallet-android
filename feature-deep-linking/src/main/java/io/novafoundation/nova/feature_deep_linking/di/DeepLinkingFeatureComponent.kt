package io.novafoundation.nova.feature_deep_linking.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_api.presentation.DAppRouter
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
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
            @BindsInstance assetsRouter: AssetsRouter,
            @BindsInstance governanceRouter: GovernanceRouter,
            @BindsInstance accountRouter: AccountRouter,
            @BindsInstance dAppRouter: DAppRouter,
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
            AssetsFeatureApi::class
        ]
    )
    interface DeepLinkingFeatureDependenciesComponent : DeepLinkingFeatureDependencies
}
