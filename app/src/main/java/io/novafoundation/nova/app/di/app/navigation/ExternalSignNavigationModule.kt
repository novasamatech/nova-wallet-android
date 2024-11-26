package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.externalSign.ExternalSignCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.externalSign.ExternalSignNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter

@Module
class ExternalSignNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: MainNavigationHolder): ExternalSignRouter = ExternalSignNavigator(navigationHolder)

    @ApplicationScope
    @Provides
    fun provideSignExtrinsicCommunicator(
        navigationHolder: MainNavigationHolder,
        automaticInteractionGate: AutomaticInteractionGate,
    ): ExternalSignCommunicator {
        return ExternalSignCommunicatorImpl(navigationHolder, automaticInteractionGate)
    }
}
