package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.settings.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.settings.CustomValidatorsSettingsFragment

@Subcomponent(
    modules = [
        CustomValidatorsSettingsModule::class
    ]
)
@ScreenScope
interface CustomValidatorsSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): CustomValidatorsSettingsComponent
    }

    fun inject(fragment: CustomValidatorsSettingsFragment)
}
