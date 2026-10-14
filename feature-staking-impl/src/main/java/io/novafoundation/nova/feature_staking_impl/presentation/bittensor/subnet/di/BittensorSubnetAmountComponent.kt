package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountFragment

@ScreenScope
@Subcomponent(modules = [BittensorSubnetAmountModule::class])
interface BittensorSubnetAmountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorSubnetAmountPayload): BittensorSubnetAmountComponent
    }

    fun inject(fragment: BittensorSubnetAmountFragment)
}
