package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.SelectCustomPoolFragment

@Subcomponent(
    modules = [
        SelectCustomPoolModule::class
    ]
)
@ScreenScope
interface SelectCustomPoolComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SelectCustomPoolComponent
    }

    fun inject(fragment: SelectCustomPoolFragment)
}
