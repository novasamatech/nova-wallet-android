package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorWhereToStakeFragment

@ScreenScope
@Subcomponent(modules = [BittensorSubnetModule::class])
interface BittensorSubnetComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorChooseSubnetPayload): BittensorSubnetComponent
    }

    fun inject(fragment: BittensorChooseSubnetFragment)

    fun inject(fragment: BittensorWhereToStakeFragment)
}
