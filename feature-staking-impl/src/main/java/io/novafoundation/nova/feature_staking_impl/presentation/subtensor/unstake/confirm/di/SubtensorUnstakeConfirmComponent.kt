package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm.SubtensorUnstakeConfirmFragment
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm.SubtensorUnstakeConfirmPayload

@Subcomponent(
    modules = [
        SubtensorUnstakeConfirmModule::class,
    ],
)
@ScreenScope
interface SubtensorUnstakeConfirmComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SubtensorUnstakeConfirmPayload,
        ): SubtensorUnstakeConfirmComponent
    }

    fun inject(fragment: SubtensorUnstakeConfirmFragment)
}
