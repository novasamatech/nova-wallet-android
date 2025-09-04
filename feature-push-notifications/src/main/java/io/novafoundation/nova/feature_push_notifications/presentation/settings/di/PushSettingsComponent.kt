package io.novafoundation.nova.feature_push_notifications.presentation.settings.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_push_notifications.presentation.settings.PushSettingsFragment

@Subcomponent(
    modules = [
        PushSettingsModule::class
    ]
)
@ScreenScope
interface PushSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): PushSettingsComponent
    }

    fun inject(fragment: PushSettingsFragment)
}
