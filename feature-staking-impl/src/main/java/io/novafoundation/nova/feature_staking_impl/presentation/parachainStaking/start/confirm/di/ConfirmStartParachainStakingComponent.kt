package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.ConfirmStartParachainStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload

@Subcomponent(
    modules = [
        ConfirmStartParachainStakingModule::class
    ]
)
@ScreenScope
interface ConfirmStartParachainStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmStartParachainStakingPayload,
        ): ConfirmStartParachainStakingComponent
    }

    fun inject(fragment: ConfirmStartParachainStakingFragment)
}
