package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.StakingDashboardFragment

@Subcomponent(
    modules = [
        StakingDashboardModule::class
    ]
)
@ScreenScope
interface StakingDashboardComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): StakingDashboardComponent
    }

    fun inject(fragment: StakingDashboardFragment)
}
