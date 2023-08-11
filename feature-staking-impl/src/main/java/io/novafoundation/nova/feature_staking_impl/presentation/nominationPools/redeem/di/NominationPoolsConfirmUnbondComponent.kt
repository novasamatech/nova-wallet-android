package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.redeem.NominationPoolsRedeemFragment

@Subcomponent(
    modules = [
        NominationPoolsRedeemModule::class
    ]
)
@ScreenScope
interface NominationPoolsRedeemComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NominationPoolsRedeemComponent
    }

    fun inject(fragment: NominationPoolsRedeemFragment)
}
