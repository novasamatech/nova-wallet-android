package io.novafoundation.nova.feature_assets.presentation.balance.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.balance.list.BalanceListFragment

@Subcomponent(
    modules = [
        BalanceListModule::class
    ]
)
@ScreenScope
interface BalanceListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): BalanceListComponent
    }

    fun inject(fragment: BalanceListFragment)
}
