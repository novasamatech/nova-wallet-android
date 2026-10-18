package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetFragment

@ScreenScope
@Subcomponent(modules = [BittensorSubnetModule::class])
interface BittensorSubnetComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): BittensorSubnetComponent
    }

    fun inject(fragment: BittensorChooseSubnetFragment)
}
