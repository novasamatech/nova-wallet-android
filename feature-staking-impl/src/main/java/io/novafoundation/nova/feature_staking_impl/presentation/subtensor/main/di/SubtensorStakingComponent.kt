package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.main.SubtensorStakingFragment

@Subcomponent(
    modules = [
        SubtensorStakingModule::class,
    ],
)
@ScreenScope
interface SubtensorStakingComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): SubtensorStakingComponent
    }

    fun inject(fragment: SubtensorStakingFragment)
}
