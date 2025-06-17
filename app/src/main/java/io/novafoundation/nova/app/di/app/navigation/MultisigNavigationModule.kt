package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.multisig.MultisigOperationsNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter

@Module
class MultisigNavigationModule {

    @ApplicationScope
    @Provides
    fun provideOperationsRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        commonDelegate: Navigator
    ): MultisigOperationsRouter = MultisigOperationsNavigator(navigationHoldersRegistry, commonDelegate)
}
