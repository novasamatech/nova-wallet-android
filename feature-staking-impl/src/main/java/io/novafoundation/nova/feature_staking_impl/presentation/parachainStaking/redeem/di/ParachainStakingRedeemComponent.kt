package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem.ParachainStakingRedeemFragment

@Subcomponent(
    modules = [
        ParachainStakingRedeemModule::class
    ]
)
@ScreenScope
interface ParachainStakingRedeemComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ParachainStakingRedeemComponent
    }

    fun inject(fragment: ParachainStakingRedeemFragment)
}
