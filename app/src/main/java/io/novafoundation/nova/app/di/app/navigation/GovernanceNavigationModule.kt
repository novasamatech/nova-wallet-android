package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.governance.GovernanceNavigator
import io.novafoundation.nova.app.root.navigation.navigators.governance.SelectTracksCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.governance.TinderGovVoteCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator

@Module
class GovernanceNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        commonNavigator: Navigator,
        contextManager: ContextManager,
        dAppRouter: DAppRouter
    ): GovernanceRouter = GovernanceNavigator(navigationHoldersRegistry, commonNavigator, contextManager, dAppRouter)

    @Provides
    @ApplicationScope
    fun provideSelectTracksCommunicator(
        router: GovernanceRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): SelectTracksCommunicator = SelectTracksCommunicatorImpl(router, navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun provideTinderGovVoteCommunicator(
        router: GovernanceRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): TinderGovVoteCommunicator = TinderGovVoteCommunicatorImpl(router, navigationHoldersRegistry)
}
