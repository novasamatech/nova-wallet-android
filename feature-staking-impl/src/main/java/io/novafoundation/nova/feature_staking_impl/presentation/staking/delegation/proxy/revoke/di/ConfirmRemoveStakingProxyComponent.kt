package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyPayload

@Subcomponent(
    modules = [
        ConfirmRemoveStakingProxyModule::class
    ]
)
@ScreenScope
interface ConfirmRemoveStakingProxyComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmRemoveStakingProxyPayload,
        ): ConfirmRemoveStakingProxyComponent
    }

    fun inject(fragment: ConfirmRemoveStakingProxyFragment)
}
