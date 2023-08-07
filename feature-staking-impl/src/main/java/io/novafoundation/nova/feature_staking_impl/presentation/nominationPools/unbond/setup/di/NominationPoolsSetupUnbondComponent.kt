package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup.NominationPoolsSetupUnbondFragment

@Subcomponent(
    modules = [
        NominationPoolsSetupUnbondModule::class
    ]
)
@ScreenScope
interface NominationPoolsSetupUnbondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NominationPoolsSetupUnbondComponent
    }

    fun inject(fragment: NominationPoolsSetupUnbondFragment)
}
