package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.StartParachainStakingFragment

@Subcomponent(
    modules = [
        StartParachainStakingModule::class
    ]
)
@ScreenScope
interface StartParachainStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): StartParachainStakingComponent
    }

    fun inject(fragment: StartParachainStakingFragment)
}
