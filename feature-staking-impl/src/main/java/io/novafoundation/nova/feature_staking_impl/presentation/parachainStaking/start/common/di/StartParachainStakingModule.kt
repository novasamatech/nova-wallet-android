package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints.ConfirmStartParachainStakingHintsMixinFactory

@Module
class StartParachainStakingModule {

    @Provides
    @ScreenScope
    fun provideConfirmStartParachainStakingHintsMixinFactory(
        resourceManager: ResourceManager
    ): ConfirmStartParachainStakingHintsMixinFactory {
        return ConfirmStartParachainStakingHintsMixinFactory(resourceManager)
    }
}
