package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker.SubtensorSubnetPickerFragment

@Subcomponent(
    modules = [
        SubtensorSubnetPickerModule::class,
    ],
)
@ScreenScope
interface SubtensorSubnetPickerComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): SubtensorSubnetPickerComponent
    }

    fun inject(fragment: SubtensorSubnetPickerFragment)
}
