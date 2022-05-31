package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.ParachainStakingUnbondFragment

@Subcomponent(
    modules = [
        ParachainStakingUnbondModule::class
    ]
)
@ScreenScope
interface ParachainStakingUnbondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ParachainStakingUnbondComponent
    }

    fun inject(fragment: ParachainStakingUnbondFragment)
}
