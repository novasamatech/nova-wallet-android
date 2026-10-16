package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.BittensorRootClaimFragment

@ScreenScope
@Subcomponent(modules = [BittensorRootClaimModule::class])
interface BittensorRootClaimComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): BittensorRootClaimComponent
    }

    fun inject(fragment: BittensorRootClaimFragment)
}
