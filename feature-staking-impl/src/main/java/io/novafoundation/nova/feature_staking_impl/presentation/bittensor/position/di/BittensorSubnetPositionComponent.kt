package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionPayload

@ScreenScope
@Subcomponent(modules = [BittensorSubnetPositionModule::class])
interface BittensorSubnetPositionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorSubnetPositionPayload): BittensorSubnetPositionComponent
    }

    fun inject(fragment: BittensorSubnetPositionFragment)
}
