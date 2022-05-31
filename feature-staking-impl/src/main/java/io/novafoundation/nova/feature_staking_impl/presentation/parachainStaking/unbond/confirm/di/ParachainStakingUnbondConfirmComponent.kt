package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.ParachainStakingUnbondConfirmFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload

@Subcomponent(
    modules = [
        ParachainStakingUnbondConfirmModule::class
    ]
)
@ScreenScope
interface ParachainStakingUnbondConfirmComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ParachainStakingUnbondConfirmPayload,
        ): ParachainStakingUnbondConfirmComponent
    }

    fun inject(fragment: ParachainStakingUnbondConfirmFragment)
}
