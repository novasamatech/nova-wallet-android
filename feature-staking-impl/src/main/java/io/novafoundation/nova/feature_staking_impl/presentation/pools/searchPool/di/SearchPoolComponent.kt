package io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.SelectPoolFragment
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool.SearchPoolFragment
import io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool.SearchPoolViewModel

@Subcomponent(
    modules = [
        SearchPoolModule::class
    ]
)
@ScreenScope
interface SearchPoolComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectingPoolPayload
        ): SearchPoolComponent
    }

    fun inject(fragment: SearchPoolFragment)
}
