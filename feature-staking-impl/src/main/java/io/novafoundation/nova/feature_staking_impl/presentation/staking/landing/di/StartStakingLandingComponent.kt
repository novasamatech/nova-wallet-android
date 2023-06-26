package io.novafoundation.nova.feature_staking_impl.presentation.staking.landing.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.landing.StartStakingLandingFragment

@Subcomponent(
    modules = [
        StartStakingLandingModule::class
    ]
)
@ScreenScope
interface StartStakingLandingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): StartStakingLandingComponent
    }

    fun inject(fragment: StartStakingLandingFragment)
}
