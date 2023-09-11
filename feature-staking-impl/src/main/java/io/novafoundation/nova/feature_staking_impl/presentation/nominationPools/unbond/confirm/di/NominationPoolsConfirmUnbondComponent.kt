package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.confirm.NominationPoolsConfirmUnbondPayload

@Subcomponent(
    modules = [
        NominationPoolsConfirmUnbondModule::class
    ]
)
@ScreenScope
interface NominationPoolsConfirmUnbondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NominationPoolsConfirmUnbondPayload,
        ): NominationPoolsConfirmUnbondComponent
    }

    fun inject(fragment: NominationPoolsConfirmUnbondFragment)
}
