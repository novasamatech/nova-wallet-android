package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload

@ScreenScope
@Subcomponent(modules = [BittensorValidatorInfoModule::class])
interface BittensorValidatorInfoComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: BittensorValidatorInfoPayload
        ): BittensorValidatorInfoComponent
    }

    fun inject(fragment: BittensorValidatorInfoFragment)
}
