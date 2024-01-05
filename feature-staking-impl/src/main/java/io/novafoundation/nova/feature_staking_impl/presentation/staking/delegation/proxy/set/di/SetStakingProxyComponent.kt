package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set.SetControllerFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.set.SetStakingProxyFragment

@Subcomponent(
    modules = [
        SetStakingProxyModule::class
    ]
)
@ScreenScope
interface SetStakingProxyComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): SetStakingProxyComponent
    }

    fun inject(fragment: SetStakingProxyFragment)
}
