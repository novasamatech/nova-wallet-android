package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.MoreStakingOptionsFragment

@Subcomponent(
    modules = [
        MoreStakingOptionsModule::class
    ]
)
@ScreenScope
interface MoreStakingOptionsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): MoreStakingOptionsComponent
    }

    fun inject(fragment: MoreStakingOptionsFragment)
}
