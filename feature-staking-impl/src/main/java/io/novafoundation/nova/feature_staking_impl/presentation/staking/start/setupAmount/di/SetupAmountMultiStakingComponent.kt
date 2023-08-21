package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.SetupAmountMultiStakingPayload

@Subcomponent(
    modules = [
        SetupAmountMultiStakingModule::class
    ]
)
@ScreenScope
interface SetupAmountMultiStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance argument: SetupAmountMultiStakingPayload
        ): SetupAmountMultiStakingComponent
    }

    fun inject(fragment: SetupAmountMultiStakingFragment)
}
