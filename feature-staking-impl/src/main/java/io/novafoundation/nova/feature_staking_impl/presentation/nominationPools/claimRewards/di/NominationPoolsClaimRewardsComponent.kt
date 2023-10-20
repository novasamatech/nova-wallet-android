package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.claimRewards.NominationPoolsClaimRewardsFragment

@Subcomponent(
    modules = [
        NominationPoolsClaimRewardsModule::class
    ]
)
@ScreenScope
interface NominationPoolsClaimRewardsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NominationPoolsClaimRewardsComponent
    }

    fun inject(fragment: NominationPoolsClaimRewardsFragment)
}
