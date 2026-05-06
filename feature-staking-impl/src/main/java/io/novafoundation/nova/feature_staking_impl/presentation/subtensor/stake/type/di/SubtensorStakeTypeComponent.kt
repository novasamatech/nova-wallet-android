package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.type.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.type.SubtensorStakeTypeFragment

@Subcomponent(
    modules = [
        SubtensorStakeTypeModule::class,
    ],
)
@ScreenScope
interface SubtensorStakeTypeComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): SubtensorStakeTypeComponent
    }

    fun inject(fragment: SubtensorStakeTypeFragment)
}
