package io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards.MythosClaimRewardsFragment

@Subcomponent(
    modules = [
        MythosClaimRewardsModule::class
    ]
)
@ScreenScope
interface MythosClaimRewardsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): MythosClaimRewardsComponent
    }

    fun inject(fragment: MythosClaimRewardsFragment)
}
