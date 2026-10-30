package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeConfirmFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeConfirmPayload

@Subcomponent(
    modules = [
        BittensorStakeConfirmModule::class
    ]
)
@ScreenScope
interface BittensorStakeConfirmComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorStakeConfirmPayload): BittensorStakeConfirmComponent
    }

    fun inject(fragment: BittensorStakeConfirmFragment)
}
