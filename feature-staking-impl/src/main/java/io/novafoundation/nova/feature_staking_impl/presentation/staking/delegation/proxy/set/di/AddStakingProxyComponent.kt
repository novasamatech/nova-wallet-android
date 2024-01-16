package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set.AddStakingProxyFragment

@Subcomponent(
    modules = [
        AddStakingProxyModule::class
    ]
)
@ScreenScope
interface AddStakingProxyComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): AddStakingProxyComponent
    }

    fun inject(fragment: AddStakingProxyFragment)
}
