package io.novafoundation.nova.feature_crowdloan_impl.presentation.claimControbution.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_crowdloan_impl.presentation.claimControbution.ClaimContributionFragment

@Subcomponent(
    modules = [
        ClaimContributionModule::class
    ]
)
@ScreenScope
interface ClaimContributionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ClaimContributionComponent
    }

    fun inject(fragment: ClaimContributionFragment)
}
