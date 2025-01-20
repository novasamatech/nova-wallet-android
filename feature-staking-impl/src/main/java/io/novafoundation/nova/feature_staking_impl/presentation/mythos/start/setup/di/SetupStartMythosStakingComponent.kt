package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup.SetupStartMythosStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingPayload

@Subcomponent(
    modules = [
        SetupStartMythosStakingModule::class
    ]
)
@ScreenScope
interface SetupStartMythosStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): SetupStartMythosStakingComponent
    }

    fun inject(fragment: SetupStartMythosStakingFragment)
}
