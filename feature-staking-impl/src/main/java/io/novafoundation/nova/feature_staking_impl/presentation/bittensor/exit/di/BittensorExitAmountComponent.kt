package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitAmountFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload

@ScreenScope
@Subcomponent(modules = [BittensorExitAmountModule::class])
interface BittensorExitAmountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorExitPositionPayload): BittensorExitAmountComponent
    }

    fun inject(fragment: BittensorExitAmountFragment)
}
