package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.confirm.SubtensorStakeConfirmFragment
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.confirm.SubtensorStakeConfirmPayload

@Subcomponent(
    modules = [
        SubtensorStakeConfirmModule::class,
    ],
)
@ScreenScope
interface SubtensorStakeConfirmComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SubtensorStakeConfirmPayload,
        ): SubtensorStakeConfirmComponent
    }

    fun inject(fragment: SubtensorStakeConfirmFragment)
}
