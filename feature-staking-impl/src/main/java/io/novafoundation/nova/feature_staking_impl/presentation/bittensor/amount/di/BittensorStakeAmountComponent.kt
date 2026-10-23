package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorRootStakePresentation
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorStakeAmountFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorStakeAmountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectionState

@Subcomponent(
    modules = [
        BittensorStakeAmountModule::class
    ]
)
@ScreenScope
interface BittensorStakeAmountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: BittensorStakeAmountPayload?,
            @BindsInstance selectionState: BittensorSelectionState,
            @BindsInstance presentation: BittensorRootStakePresentation
        ): BittensorStakeAmountComponent
    }

    fun inject(fragment: BittensorStakeAmountFragment)
}
