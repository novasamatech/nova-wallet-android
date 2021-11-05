package io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.PayoutsListFragment

@Subcomponent(
    modules = [
        PayoutsListModule::class
    ]
)
@ScreenScope
interface PayoutsListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): PayoutsListComponent
    }

    fun inject(fragment: PayoutsListFragment)
}
