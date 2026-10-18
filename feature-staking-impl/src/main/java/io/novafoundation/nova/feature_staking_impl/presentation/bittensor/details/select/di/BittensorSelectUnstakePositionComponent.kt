package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionFragment

@ScreenScope
@Subcomponent(modules = [BittensorSelectUnstakePositionModule::class])
interface BittensorSelectUnstakePositionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: BittensorSelectUnstakePositionCommunicator.Request
        ): BittensorSelectUnstakePositionComponent
    }

    fun inject(fragment: BittensorSelectUnstakePositionFragment)
}
