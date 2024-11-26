package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.parachain.ParachainStakingNavigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.parachain.SelectCollatorInterScreenCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.staking.parachain.SelectCollatorSettingsInterScreenCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator

@Module
class ParachainStakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideParachainStakingRouter(navigationHolder: MainNavigationHolder, navigator: Navigator): ParachainStakingRouter {
        return ParachainStakingNavigator(navigationHolder, navigator)
    }

    @Provides
    @ApplicationScope
    fun provideSelectCollatorCommunicator(navigationHolder: MainNavigationHolder): SelectCollatorInterScreenCommunicator {
        return SelectCollatorInterScreenCommunicatorImpl(navigationHolder)
    }

    @Provides
    @ApplicationScope
    fun provideSelectCollatorSettingsCommunicator(navigationHolder: MainNavigationHolder): SelectCollatorSettingsInterScreenCommunicator {
        return SelectCollatorSettingsInterScreenCommunicatorImpl(navigationHolder)
    }
}
