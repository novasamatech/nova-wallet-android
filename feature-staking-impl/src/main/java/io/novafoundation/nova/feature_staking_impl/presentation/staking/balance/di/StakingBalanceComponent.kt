package io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.StakingBalanceFragment

@Subcomponent(
    modules = [
        StakingBalanceModule::class
    ]
)
@ScreenScope
interface StakingBalanceComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): StakingBalanceComponent
    }

    fun inject(fragment: StakingBalanceFragment)
}
