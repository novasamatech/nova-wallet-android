package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.app.root.navigation.staking.parachain.ParachainStakingNavigator
import io.novafoundation.nova.app.root.navigation.staking.parachain.SelectCollatorInterScreenCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.staking.parachain.SelectCollatorSettingsInterScreenCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator

@Module
class ParachainStakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideParachainStakingRouter(navigationHolder: NavigationHolder, navigator: Navigator): ParachainStakingRouter {
        return ParachainStakingNavigator(navigationHolder, navigator)
    }

    @Provides
    @ApplicationScope
    fun provideSelectCollatorCommunicator(navigationHolder: NavigationHolder): SelectCollatorInterScreenCommunicator {
        return SelectCollatorInterScreenCommunicatorImpl(navigationHolder)
    }

    @Provides
    @ApplicationScope
    fun provideSelectCollatorSettingsCommunicator(navigationHolder: NavigationHolder): SelectCollatorSettingsInterScreenCommunicator {
        return SelectCollatorSettingsInterScreenCommunicatorImpl(navigationHolder)
    }
}
