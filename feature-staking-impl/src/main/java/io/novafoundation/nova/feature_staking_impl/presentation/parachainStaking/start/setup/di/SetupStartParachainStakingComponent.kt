package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingPayload

@Subcomponent(
    modules = [
        SetupStartParachainStakingModule::class
    ]
)
@ScreenScope
interface SetupStartParachainStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: StartParachainStakingPayload,
        ): SetupStartParachainStakingComponent
    }

    fun inject(fragment: StartParachainStakingFragment)
}
