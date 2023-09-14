package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.SelectPoolFragment
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload

@Subcomponent(
    modules = [
        SelectPoolModule::class
    ]
)
@ScreenScope
interface SelectPoolComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectingPoolPayload
        ): SelectPoolComponent
    }

    fun inject(fragment: SelectPoolFragment)
}
