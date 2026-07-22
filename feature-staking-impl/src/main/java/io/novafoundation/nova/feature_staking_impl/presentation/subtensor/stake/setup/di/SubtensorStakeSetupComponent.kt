package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup.SubtensorStakeSetupFragment

@Subcomponent(
    modules = [
        SubtensorStakeSetupModule::class,
    ],
)
@ScreenScope
interface SubtensorStakeSetupComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance netuid: Int,
            @BindsInstance subnetName: String?,
        ): SubtensorStakeSetupComponent
    }

    fun inject(fragment: SubtensorStakeSetupFragment)
}
