package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorRootDetailsFragment

@ScreenScope
@Subcomponent(modules = [BittensorDetailsModule::class])
interface BittensorDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance portfolioPayload: BittensorPortfolioPayload?
        ): BittensorDetailsComponent
    }

    fun inject(fragment: BittensorPortfolioFragment)

    fun inject(fragment: BittensorRootDetailsFragment)
}
