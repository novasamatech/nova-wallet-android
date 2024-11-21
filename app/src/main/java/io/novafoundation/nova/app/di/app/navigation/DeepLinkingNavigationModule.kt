package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.deepLinking.DeepLinkingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter

@Module
class DeepLinkingNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        accountRouter: AccountRouter,
        assetsRouter: AssetsRouter,
        dAppRouter: DAppRouter,
        governanceRouter: GovernanceRouter
    ): DeepLinkingRouter = DeepLinkingNavigator(
        accountRouter = accountRouter,
        assetsRouter = assetsRouter,
        dAppRouter = dAppRouter,
        governanceRouter = governanceRouter
    )
}
