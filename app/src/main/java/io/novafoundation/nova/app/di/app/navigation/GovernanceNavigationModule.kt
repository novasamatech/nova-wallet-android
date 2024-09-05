package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.app.root.navigation.governance.GovernanceNavigator
import io.novafoundation.nova.app.root.navigation.governance.SelectTracksCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.governance.TinderGovVoteCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator

@Module
class GovernanceNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHolder: NavigationHolder,
        commonNavigator: Navigator,
    ): GovernanceRouter = GovernanceNavigator(navigationHolder, commonNavigator)

    @Provides
    @ApplicationScope
    fun provideSelectTracksCommunicator(
        router: GovernanceRouter,
        navigationHolder: NavigationHolder
    ): SelectTracksCommunicator = SelectTracksCommunicatorImpl(router, navigationHolder)

    @Provides
    @ApplicationScope
    fun provideTinderGovVoteCommunicator(
        router: GovernanceRouter,
        navigationHolder: NavigationHolder
    ): TinderGovVoteCommunicator = TinderGovVoteCommunicatorImpl(router, navigationHolder)
}
