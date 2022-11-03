package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.app.root.navigation.governance.GovernanceNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter

@Module
class GovernanceNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHolder: NavigationHolder,
        commonNavigator: Navigator,
    ): GovernanceRouter = GovernanceNavigator(navigationHolder, commonNavigator)
}
