package io.novafoundation.nova.feature_settings_impl.presentation.settings.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope

@Subcomponent(
    modules = [
        SettingsModule::class
    ]
)
@ScreenScope
interface SettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): SettingsComponent
    }

    fun inject(settingsFragment: io.novafoundation.nova.feature_settings_impl.presentation.settings.SettingsFragment)
}
