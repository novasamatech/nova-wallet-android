package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup.NominationPoolsSetupBondMoreFragment

@Subcomponent(
    modules = [
        NominationPoolsSetupBondMoreModule::class
    ]
)
@ScreenScope
interface NominationPoolsSetupBondMoreComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NominationPoolsSetupBondMoreComponent
    }

    fun inject(fragment: NominationPoolsSetupBondMoreFragment)
}
