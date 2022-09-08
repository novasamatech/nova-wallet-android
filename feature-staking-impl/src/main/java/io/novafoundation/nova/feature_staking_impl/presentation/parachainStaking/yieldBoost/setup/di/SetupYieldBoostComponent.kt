package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup.SetupYieldBoostFragment

@Subcomponent(
    modules = [
        SetupYieldBoostModule::class
    ]
)
@ScreenScope
interface SetupYieldBoostComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SetupYieldBoostComponent
    }

    fun inject(fragment: SetupYieldBoostFragment)
}
