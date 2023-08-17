package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.StartStakingLandingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.model.StartStakingLandingPayload

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
