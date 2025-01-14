package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.vote.VoteNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

@Module
class VoteNavigationModule {

    @Provides
    @ApplicationScope
    fun provideVoteRouter(navigator: Navigator): VoteRouter = VoteNavigator(navigator)
}
