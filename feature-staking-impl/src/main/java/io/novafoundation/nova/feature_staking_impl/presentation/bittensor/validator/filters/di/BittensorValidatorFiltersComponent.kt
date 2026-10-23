package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersFragment

@ScreenScope
@Subcomponent(modules = [BittensorValidatorFiltersModule::class])
interface BittensorValidatorFiltersComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: BittensorValidatorFiltersCommunicator.Request
        ): BittensorValidatorFiltersComponent
    }

    fun inject(fragment: BittensorValidatorFiltersFragment)
}
