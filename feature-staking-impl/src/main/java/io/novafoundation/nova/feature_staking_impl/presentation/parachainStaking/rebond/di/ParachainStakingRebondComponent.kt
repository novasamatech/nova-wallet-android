package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.ParachainStakingRebondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload

@Subcomponent(
    modules = [
        ParachainStakingRebondModule::class
    ]
)
@ScreenScope
interface ParachainStakingRebondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ParachainStakingRebondPayload,
        ): ParachainStakingRebondComponent
    }

    fun inject(fragment: ParachainStakingRebondFragment)
}
