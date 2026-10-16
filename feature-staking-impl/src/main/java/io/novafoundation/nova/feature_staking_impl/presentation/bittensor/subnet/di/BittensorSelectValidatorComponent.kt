package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorFragment

@ScreenScope
@Subcomponent(modules = [BittensorSelectValidatorModule::class])
interface BittensorSelectValidatorComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: BittensorSelectValidatorCommunicator.Request
        ): BittensorSelectValidatorComponent
    }

    fun inject(fragment: BittensorSelectValidatorFragment)
}
