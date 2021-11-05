package io.novafoundation.nova.feature_staking_impl.presentation.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.setup.SetupStakingFragment

@Subcomponent(
    modules = [
        SetupStakingModule::class
    ]
)
@ScreenScope
interface SetupStakingComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SetupStakingComponent
    }

    fun inject(fragment: SetupStakingFragment)
}
