package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.StakingProxyListFragment

@Subcomponent(
    modules = [
        StakingProxyListModule::class
    ]
)
@ScreenScope
interface StakingProxyListComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): StakingProxyListComponent
    }

    fun inject(fragment: StakingProxyListFragment)
}
