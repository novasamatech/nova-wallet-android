package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewFragment

@ScreenScope
@Subcomponent(modules = [BittensorSubnetReviewModule::class])
interface BittensorSubnetReviewComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorSubnetReviewPayload): BittensorSubnetReviewComponent
    }

    fun inject(fragment: BittensorSubnetReviewFragment)
}
