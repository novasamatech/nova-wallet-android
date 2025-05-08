package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.pay.PayNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter

@Module
class PayNavigationModule {

    @Provides
    @ApplicationScope
    fun provideVoteRouter(navigator: Navigator): PayRouter = PayNavigator(navigator)
}
