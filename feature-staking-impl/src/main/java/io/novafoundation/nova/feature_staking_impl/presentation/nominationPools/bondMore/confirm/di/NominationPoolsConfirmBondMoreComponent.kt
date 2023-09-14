package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload

@Subcomponent(
    modules = [
        NominationPoolsConfirmBondMoreModule::class
    ]
)
@ScreenScope
interface NominationPoolsConfirmBondMoreComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NominationPoolsConfirmBondMorePayload,
        ): NominationPoolsConfirmBondMoreComponent
    }

    fun inject(fragment: NominationPoolsConfirmBondMoreFragment)
}
