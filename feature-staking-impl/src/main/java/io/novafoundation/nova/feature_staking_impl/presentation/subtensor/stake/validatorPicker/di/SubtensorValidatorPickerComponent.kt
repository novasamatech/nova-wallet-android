package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker.SubtensorValidatorPickerFragment

@Subcomponent(
    modules = [
        SubtensorValidatorPickerModule::class,
    ],
)
@ScreenScope
interface SubtensorValidatorPickerComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance netuid: Int,
        ): SubtensorValidatorPickerComponent
    }

    fun inject(fragment: SubtensorValidatorPickerFragment)
}
