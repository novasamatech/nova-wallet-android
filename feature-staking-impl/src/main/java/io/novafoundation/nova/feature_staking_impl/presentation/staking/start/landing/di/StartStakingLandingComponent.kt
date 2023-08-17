package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.StartStakingLandingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload

@Subcomponent(
    modules = [
        StartStakingLandingModule::class
    ]
)
@ScreenScope
interface StartStakingLandingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance argument: StartStakingLandingPayload): StartStakingLandingComponent
    }

    fun inject(fragment: StartStakingLandingFragment)
}
