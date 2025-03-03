package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingPayload

@Subcomponent(
    modules = [
        ConfirmStartMythosStakingModule::class
    ]
)
@ScreenScope
interface ConfirmStartMythosStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmStartMythosStakingPayload,
        ): ConfirmStartMythosStakingComponent
    }

    fun inject(fragment: ConfirmStartMythosStakingFragment)
}
