package io.novafoundation.nova.feature_staking_impl.presentation.period.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.period.StakingPeriodFragment

@Subcomponent(
    modules = [
        StakingPeriodModule::class
    ]
)
@ScreenScope
interface StakingPeriodComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): StakingPeriodComponent
    }

    fun inject(fragment: StakingPeriodFragment)
}
