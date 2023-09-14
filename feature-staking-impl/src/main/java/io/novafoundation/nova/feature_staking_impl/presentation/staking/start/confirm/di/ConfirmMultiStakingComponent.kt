package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingPayload

@Subcomponent(
    modules = [
        ConfirmMultiStakingModule::class
    ]
)
@ScreenScope
interface ConfirmMultiStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance argument: ConfirmMultiStakingPayload
        ): ConfirmMultiStakingComponent
    }

    fun inject(fragment: ConfirmMultiStakingFragment)
}
