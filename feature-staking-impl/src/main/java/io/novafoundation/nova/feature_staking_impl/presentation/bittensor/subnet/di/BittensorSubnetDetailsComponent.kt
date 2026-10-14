package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsFragment

@ScreenScope
@Subcomponent(modules = [BittensorSubnetDetailsModule::class])
interface BittensorSubnetDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorSubnetDetailsPayload): BittensorSubnetDetailsComponent
    }

    fun inject(fragment: BittensorSubnetDetailsFragment)
}
