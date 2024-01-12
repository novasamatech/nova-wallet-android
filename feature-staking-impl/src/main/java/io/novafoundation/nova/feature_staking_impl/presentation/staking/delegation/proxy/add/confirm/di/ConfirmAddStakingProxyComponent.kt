package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyPayload

@Subcomponent(
    modules = [
        ConfirmAddStakingProxyModule::class
    ]
)
@ScreenScope
interface ConfirmAddStakingProxyComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmAddStakingProxyPayload,
        ): ConfirmAddStakingProxyComponent
    }

    fun inject(fragment: ConfirmAddStakingProxyFragment)
}
