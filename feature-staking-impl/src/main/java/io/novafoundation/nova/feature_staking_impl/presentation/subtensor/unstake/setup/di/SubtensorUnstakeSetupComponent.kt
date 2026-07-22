package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup.SubtensorUnstakeSetupFragment
import java.math.BigInteger

@Subcomponent(
    modules = [
        SubtensorUnstakeSetupModule::class,
    ],
)
@ScreenScope
interface SubtensorUnstakeSetupComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance netuid: Int,
            @BindsInstance hotkeyAddress: String,
            @BindsInstance positionPlanks: BigInteger,
        ): SubtensorUnstakeSetupComponent
    }

    fun inject(fragment: SubtensorUnstakeSetupFragment)
}
