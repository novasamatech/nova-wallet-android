package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.governance.GovernanceNavigator
import io.novafoundation.nova.app.root.navigation.navigators.governance.SelectTracksCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.governance.TinderGovVoteCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator

@Module
class GovernanceNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHolder: SplitScreenNavigationHolder,
        commonNavigator: Navigator,
    ): GovernanceRouter = GovernanceNavigator(navigationHolder, commonNavigator)

    @Provides
    @ApplicationScope
    fun provideSelectTracksCommunicator(
        router: GovernanceRouter,
        navigationHolder: SplitScreenNavigationHolder
    ): SelectTracksCommunicator = SelectTracksCommunicatorImpl(router, navigationHolder)

    @Provides
    @ApplicationScope
    fun provideTinderGovVoteCommunicator(
        router: GovernanceRouter,
        navigationHolder: SplitScreenNavigationHolder
    ): TinderGovVoteCommunicator = TinderGovVoteCommunicatorImpl(router, navigationHolder)
}
