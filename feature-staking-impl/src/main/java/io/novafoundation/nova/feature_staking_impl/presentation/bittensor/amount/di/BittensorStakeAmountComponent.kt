package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorStakeAmountFragment

@Subcomponent(
    modules = [
        BittensorStakeAmountModule::class
    ]
)
@ScreenScope
interface BittensorStakeAmountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): BittensorStakeAmountComponent
    }

    fun inject(fragment: BittensorStakeAmountFragment)
}
