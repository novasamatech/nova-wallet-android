package io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType.SetupStakingTypeFragment

@Subcomponent(
    modules = [
        SetupStakingTypeModule::class
    ]
)
@ScreenScope
interface SetupStakingTypeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): SetupStakingTypeComponent
    }

    fun inject(setupStakingTypeFragment: SetupStakingTypeFragment)
}
